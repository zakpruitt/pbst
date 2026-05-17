# PBST Code Review — Controllers & Services

## Table of Contents
- [LoginController](#logincontroller)
- [DashboardController / DashboardService](#dashboardcontroller--dashboardservice)
- [InventoryController / InventoryService](#inventorycontroller--inventoryservice)
- [GradingController / GradingService](#gradingcontroller--gradingservice)
- [LotController / LotService](#lotcontroller--lotservice)
- [SaleController / SaleService](#salecontroller--saleservice)
- [ExpenseController / ExpenseService](#expensecontroller--expenseservice)
- [SearchController / SearchService](#searchcontroller--searchservice)
- [VincePaymentService (no dedicated controller)](#vincepaymentservice)
- [Cross-Cutting Issues](#cross-cutting-issues)
- [Suggestions](#suggestions)

---

## LoginController

**File:** `controller/LoginController.java`

| Method | Route | Service Call |
|--------|-------|-------------|
| `login()` | `GET /login` | None — returns view |

**Notes:** Clean, nothing to change.

---

## DashboardController / DashboardService

**File:** `controller/DashboardController.java`, `service/DashboardService.java`

### Controller

| Method | Route | Service Call |
|--------|-------|-------------|
| `dashboard(model)` | `GET /` | `dashboardService.getDashboardData()` |

Clean 1:1 mapping. Controller is thin.

### Service — `getDashboardData()`

This is a large aggregation method that makes **14 separate database calls** in one shot:

1. `lotRepo.getTotalCostNonRejected()`
2. `saleRepo.getConfirmedTotals()`
3. `itemRepo.countByPurpose("IN_GRADING")`
4. `itemRepo.countByPurpose("INVENTORY")`
5. `itemRepo.getInventoryTotals()`
6. `saleRepo.getTotalsSince(7d)`
7. `saleRepo.getTotalsSince(30d)`
8. `vincePaymentService.getLedger()` — itself makes 2 more queries
9. `lotRepo.getMonthlySpend(12)`
10. `saleRepo.getMonthlyRevenue(12)`
11. `saleRepo.countByOrigin()`
12. `itemRepo.countByItemType()`
13. `gradingRepo.countByStatus()`
14. `lotRepo.countByStatus()`
15. `saleRepo.findTopByNet(5)`
16. `saleRepo.findRecent(5)`
17. `lotRepo.findByOrderByPurchaseDateDesc(5)`

> **CALLOUT — Performance:** 17+ queries per page load. Works fine now but will get slow. If dashboard latency becomes noticeable, consider: (a) a single native query that returns multiple aggregates, (b) a materialized view, or (c) caching the result for a few minutes with `@Cacheable`.

> **CALLOUT — Two separate `countByPurpose` calls (lines 44-45):** These could be combined into a single `GROUP BY purpose` query that returns all counts at once, similar to `countByItemType()`.

The `buildMonthLabels()` and `fillSeries()` helper methods are only used inside `getDashboardData()` — this is fine, they belong here.

---

## InventoryController / InventoryService

**File:** `controller/InventoryController.java`, `service/InventoryService.java`

### Controller

| Method | Route | Service Call |
|--------|-------|-------------|
| `index(purpose, request, model)` | `GET /inventory` | `inventoryService.getByPurpose(purpose)` |
| `rowPartial(...)` | `GET /inventory/partials/row` | None — builds DTO in controller |
| `newForm(model)` | `GET /inventory/new` | None |
| `create(request)` | `POST /inventory` | `inventoryService.createItems(request)` |
| `editForm(id, model)` | `GET /inventory/{id}/edit` | `inventoryService.getById(id)` |
| `update(id, request)` | `POST /inventory/{id}` | `inventoryService.update(id, request)` |
| `delete(id)` | `POST /inventory/{id}/delete` | `inventoryService.delete(id)` |

### Service

| Method | Called From | Notes |
|--------|------------|-------|
| `createItems(request)` | `InventoryController.create()` | Parses JSON snapshot, creates TrackedItems |
| `getById(id)` | `InventoryController.editForm()` | Single item lookup |
| `getByPurpose(purpose)` | `InventoryController.index()` | Gets items and splits by type |
| `update(id, request)` | `InventoryController.update()` | Uses MapStruct `updateEntity` + manual graded details |
| `delete(id)` | `InventoryController.delete()` | Returns purpose for redirect |
| `findById(id)` | (private) | Shared lookup helper |

> **CALLOUT — `index()` has logic that belongs in the service (line 30-39):** The controller unpacks `InventorySplitResponse` into individual model attributes and computes `totalCost`/`totalMarket`. The `sumCost`/`sumMarket` calls are view logic that's fine in the controller, but the manual unpacking of every field from the split response is boilerplate. Consider just passing the `InventorySplitResponse` directly as a single model attribute (`model.addAttribute("split", split)`) and accessing fields in the template with `split.rawItems`, etc.

> **CALLOUT — `rowPartial()` builds an `InventoryRowPreset` with 10 params:** This is purely view scaffolding. Fine for now but getting bloated. If more params get added, consider accepting a flat request DTO instead of 10 `@RequestParam`s.

> **CALLOUT — `createItems()` uses raw `Map<String, Object>` for JSON parsing (line 41):** The snapshot JSON is parsed into `List<Map<String, Object>>` and then manually pulled apart with string keys like `"cost_basis"`, `"market_value"`, etc. This is fragile — one key typo silently breaks things. The lot side already has `SnapshotItem` for this purpose. Consider reusing or creating a typed DTO for inventory snapshots.

---

## GradingController / GradingService

**File:** `controller/GradingController.java`, `service/GradingService.java`

### Controller

| Method | Route | Service Call |
|--------|-------|-------------|
| `index(model)` | `GET /grading` | `gradingService.getAll()` |
| `newForm(model)` | `GET /grading/new` | `gradingService.getNewFormData()` |
| `create(request)` | `POST /grading` | `gradingService.createWithItems(request)` |
| `detail(id, model)` | `GET /grading/{id}` | `gradingService.getByIdWithItems(id)` |
| `editForm(id, model)` | `GET /grading/{id}/edit` | `gradingService.getEditFormData(id)` |
| `update(id, request)` | `POST /grading/{id}` | `gradingService.update(request, id)` |
| `advanceStatus(id, newStatus)` | `POST /grading/{id}/advance` | `gradingService.advanceStatus(id, newStatus)` |
| `recordReturn(id, request)` | `POST /grading/{id}/return` | `gradingService.recordReturn(id, request.getGrades())` |
| `delete(id)` | `POST /grading/{id}/delete` | `gradingService.delete(id)` |

Clean 1:1 mapping throughout. Controller is thin.

### Service

| Method | Called From | Notes |
|--------|------------|-------|
| `createWithItems(request)` | `GradingController.create()` | Creates submission, attaches items |
| `getAll()` | `GradingController.index()` | |
| `getByIdWithItems(id)` | `GradingController.detail()` | |
| `getNewFormData()` | `GradingController.newForm()` | Returns available items for form |
| `getEditFormData(id)` | `GradingController.editForm()` | Returns submission + available items |
| `update(request, id)` | `GradingController.update()` | Uses MapStruct `updateEntity` |
| `delete(id)` | `GradingController.delete()` | |
| `advanceStatus(id, newStatus)` | `GradingController.advanceStatus()` | |
| `recordReturn(submissionId, grades)` | `GradingController.recordReturn()` | |
| `findById(id)` | (private) | |

> **CALLOUT — `advanceStatus()` does no validation on `newStatus` (service line 131):** The controller passes the raw string from the form. There's no check that `newStatus` is a valid `GradingStatus` value, or that the transition is valid (e.g. you could go from RETURNED back to PREPPING). Consider validating against the enum and enforcing allowed transitions.

> **CALLOUT — `getNewFormData()` and `getEditFormData()` share the item-splitting logic (lines 76-82, 94-101):** Both methods fetch inventory items, filter by RAW_CARD and GRADED_CARD, and build a `GradingFormData`. The filtering pattern (`TrackedItemResponse.filterByType(all, RAW_CARD)` / `filterByType(all, GRADED_CARD)`) is duplicated. This same pattern also appears in `SaleService.getConfirmFormData()`. If you add more item types or form data shapes, this will spread further.

> **CALLOUT — Parameter order inconsistency: `update(request, id)` vs convention `update(id, request)`:** Every other service method puts `id` first, but `GradingService.update()` puts `request` first. The controller calls it as `gradingService.update(request, id)`. Minor but inconsistent.

---

## LotController / LotService

**File:** `controller/LotController.java`, `service/LotService.java`

### Controller

| Method | Route | Service Call |
|--------|-------|-------------|
| `index(model)` | `GET /lots` | `lotService.getAll()` |
| `rowPartial(...)` | `GET /lots/partials/row` | None — builds SnapshotItem in controller |
| `newForm(model)` | `GET /lots/new` | None |
| `create(request)` | `POST /lots` | `lotService.create(request)` |
| `detail(id, model)` | `GET /lots/{id}` | `lotService.getById(id)` |
| `editForm(id, model)` | `GET /lots/{id}/edit` | `lotService.getById(id)` |
| `update(id, request)` | `POST /lots/{id}` | `lotService.update(id, request)` |
| `updateStatus(id, action)` | `POST /lots/{id}/status` | `lotService.accept(id)` or `lotService.reject(id)` |
| `delete(id)` | `POST /lots/{id}/delete` | `lotService.delete(id)` |

### Service

| Method | Called From | Notes |
|--------|------------|-------|
| `create(request)` | `LotController.create()` | MapStruct `toEntity` |
| `getById(id)` | `LotController.detail()`, `editForm()` | |
| `getAll()` | `LotController.index()` | |
| `update(id, request)` | `LotController.update()` | MapStruct `updateEntity` |
| `accept(id)` | `LotController.updateStatus()` | Parses snapshot, creates TrackedItems |
| `reject(id)` | `LotController.updateStatus()` | |
| `delete(id)` | `LotController.delete()` | |
| `snapshotToTrackedItem(lot, item)` | (private) | Converts snapshot to entity |
| `findById(id)` | (private) | |

> **CALLOUT — `updateStatus()` violates 1:1 rule (controller line 95-102):** The controller has an if/else that dispatches to `accept()` or `reject()` based on the `action` param. This decision should live in the service. A single `lotService.updateStatus(id, action)` that handles the branching internally would be cleaner.

> **CALLOUT — `rowPartial()` builds a `SnapshotItem` manually with 12 setter calls (lines 42-54):** Same issue as InventoryController's `rowPartial()`. Both controllers have HTMX row-partial endpoints that manually assemble a DTO from many `@RequestParam`s. These are parallel patterns that could share a consistent approach.

> **CALLOUT — `detail()` and `editForm()` make the exact same service call (lines 71, 80):** Both call `lotService.getById(id)` and add `lot` + `snapshotItems` to the model. The only difference is the returned view. If the templates diverge that's fine, but note the duplication.

---

## SaleController / SaleService

**File:** `controller/SaleController.java`, `service/SaleService.java`

### Controller

| Method | Route | Service Call |
|--------|-------|-------------|
| `index(view, model)` | `GET /sales` | `saleService.getAll(view)`, `saleService.countStaged()`, conditionally `vincePaymentService.getLedger()`, `vincePaymentService.getAll()` |
| `newForm(model)` | `GET /sales/new` | None |
| `staging(model)` | `GET /sales/staging` | `saleService.getStaged()` |
| `create(request)` | `POST /sales` | `saleService.create(request)` |
| `detail(id, model)` | `GET /sales/{id}` | `saleService.getByIdWithItems(id)` |
| `confirmForm(id, from, model)` | `GET /sales/{id}/confirm` | `saleService.getConfirmFormData(id)` |
| `confirm(id, itemIds, from)` | `POST /sales/{id}/confirm` | `saleService.confirmWithItems(id, itemIds)` |
| `ignore(id, hx)` | `POST /sales/{id}/ignore` | `saleService.ignore(id)` |
| `vince(id, from, hx)` | `POST /sales/{id}/vince` | `saleService.markAsVince(id)` |
| `updateAmounts(id, gross, net)` | `PATCH /sales/{id}/amounts` | `saleService.updateAmounts(id, gross, net)` |
| `unstage(id)` | `POST /sales/{id}/unstage` | `saleService.unstage(id)` |
| `delete(id)` | `POST /sales/{id}/delete` | `saleService.delete(id)` |
| `createVincePayment(request)` | `POST /sales/vince/payments` | `vincePaymentService.create(request)` |
| `deleteVincePayment(id)` | `POST /sales/vince/payments/{id}/delete` | `vincePaymentService.delete(id)` |

### Service

| Method | Called From | Notes |
|--------|------------|-------|
| `create(request)` | `SaleController.create()` | |
| `getByIdWithItems(id)` | `SaleController.detail()` | |
| `getAll(view)` | `SaleController.index()` | Routes to confirmed/ignored/vince |
| `getStaged()` | `SaleController.staging()` | |
| `countStaged()` | `SaleController.index()` | |
| `getConfirmFormData(id)` | `SaleController.confirmForm()` | |
| `confirmWithItems(saleId, itemIds)` | `SaleController.confirm()` | |
| `ignore(saleId)` | `SaleController.ignore()` | |
| `markAsVince(saleId)` | `SaleController.vince()` | |
| `updateAmounts(saleId, gross, net)` | `SaleController.updateAmounts()` | |
| `unstage(saleId)` | `SaleController.unstage()` | |
| `delete(saleId)` | `SaleController.delete()` | |
| `syncFromEbay(orders)` | eBay sync job | |
| `upsertFromEbay(sale)` | `syncFromEbay()` | Uses MapStruct `updateFromEbay` |

This is the biggest controller/service pair and the most complex.

> **CALLOUT — `index()` calls 4 different service methods (lines 34-49):** `getAll(view)`, `countStaged()`, `vincePaymentService.getLedger()`, `vincePaymentService.getAll()`. The vince calls are conditional which is fine, but `getAll` + `countStaged` always run together. Consider bundling them — the service could return a wrapper with both the sale list and the staged count.

> **CALLOUT — SaleController owns Vince payment routes (lines 149-159):** `POST /sales/vince/payments` and `POST /sales/vince/payments/{id}/delete` are Vince-specific CRUD operations living inside SaleController. If VincePayment grows more endpoints (edit, list, detail), this will get messy. Consider a dedicated `VincePaymentController` or at minimum acknowledge that SaleController is doing double duty.

> **CALLOUT — `ignore()` and `vince()` return `Object` (lines 106, 114):** These methods return either `ResponseEntity` (for HTMX) or a `String` redirect. Returning `Object` from a controller method works but loses type clarity. Consider using `ResponseEntity<?>` or splitting into two separate endpoints (one for HTMX, one for full-page).

> **CALLOUT — `ignore`, `markAsVince`, `unstage` all follow the same pattern:** Each one calls `itemRepo.detachFromSale(saleId)` then `saleRepo.updateStatusAndAttribution(...)` with different params. These could share a private `changeStatus(saleId, status, attribution)` helper in the service.

> **CALLOUT — `self` injection for `@Transactional(propagation = REQUIRES_NEW)` (line 41-43):** `SaleService` injects itself lazily to allow `syncFromEbay()` to call `upsertFromEbay()` in a new transaction. This is the standard Spring workaround but it's worth knowing about — it means `upsertFromEbay` must stay `public` even though it's only called internally.

---

## ExpenseController / ExpenseService

**File:** `controller/ExpenseController.java`, `service/ExpenseService.java`

### Controller

| Method | Route | Service Call |
|--------|-------|-------------|
| `index(model)` | `GET /expenses` | `expenseService.getAll()` |
| `newExpense()` | `GET /expenses/new` | None |
| `create(request)` | `POST /expenses` | `expenseService.create(request)` |
| `delete(id)` | `POST /expenses/{id}/delete` | `expenseService.delete(id)` |

### Service

| Method | Called From | Notes |
|--------|------------|-------|
| `create(request)` | `ExpenseController.create()` | Defaults date to today |
| `getAll()` | `ExpenseController.index()` | |
| `delete(id)` | `ExpenseController.delete()` | |

> **CALLOUT — `index()` has heavy computation in the controller (lines 30-50):** The controller computes `total`, `avg`, `last30`, `thisMonth`, `total30`, `totalMonth`, `count30`, `countMonth` — all from the full expense list. This is business/view logic that should live in the service. A dedicated `getExpensePageData()` method that returns a wrapper DTO with all these computed values would make the controller thin and testable.

> **CALLOUT — Fetches ALL expenses to compute stats:** `getAll()` loads every expense ever, then the controller filters in-memory for last-30-days and this-month. If the expense table grows, this won't scale. Consider adding repo methods like `findSince(date)` or computing stats via aggregate queries.

---

## SearchController / SearchService

**File:** `controller/SearchController.java`, `service/SearchService.java`

### Controller

| Method | Route | Service Call |
|--------|-------|-------------|
| `searchCards(q)` | `GET /api/v1/cards/search` | `searchService.searchCards(q)` |
| `searchSealed(q)` | `GET /api/v1/sealed/search` | `searchService.searchSealed(q)` |

### Service

| Method | Called From | Notes |
|--------|------------|-------|
| `searchCards(query)` | `SearchController.searchCards()` | Empty check + repo + mapper |
| `searchSealed(query)` | `SearchController.searchSealed()` | Same pattern |

Clean and simple. No issues.

---

## VincePaymentService

**File:** `service/VincePaymentService.java` — **No dedicated controller**

| Method | Called From | Notes |
|--------|------------|-------|
| `create(request)` | `SaleController.createVincePayment()` | Defaults date/type |
| `getAll()` | `SaleController.index()` (vince view) | |
| `getLedger()` | `SaleController.index()` (vince view), `DashboardService.getDashboardData()` | |
| `delete(id)` | `SaleController.deleteVincePayment()` | |

> **CALLOUT — Called from 2 different places:** `getLedger()` is used by both `SaleController` and `DashboardService`. This is one of the few genuinely shared service methods, which is fine — it's reused logic computing a real domain concept.

---

## Cross-Cutting Issues

### 1. Inconsistent HTMX handling

`InventoryController.index()` checks for `HX-Request` header and returns a fragment. `SaleController.ignore()` and `vince()` check for `HX-Request` and return `ResponseEntity.ok("")`. No other controllers do any HTMX-aware response handling. If the app is HTMX-driven, this should be consistent — either all list endpoints support partial responses, or none do.

### 2. Status values are stringly-typed

All statuses are stored and compared as raw strings (`"PREPPING"`, `"CONFIRMED"`, `"ACCEPTED"`, etc.) even though enums exist (`GradingStatus`, `SaleStatus`, `LotStatus`). The enums are only used for `.name()` calls to get the string. The entities, repositories, and services all pass strings around. This means:
- No compile-time safety on status transitions
- Typos in status strings would silently fail
- `advanceStatus(id, newStatus)` accepts any string

Consider using the enum types in entities and letting JPA handle the conversion with `@Enumerated(EnumType.STRING)`.

### 3. The "detach then re-attach" pattern is repeated everywhere

- `GradingService.update()`: `detachFromSubmission` → `attachToSubmission`
- `SaleService.confirmWithItems()`: `detachFromSale` → `attachToSale`
- `SaleService.ignore/markAsVince/unstage()`: `detachFromSale` → update status

These all follow the same delete-all-then-reinsert pattern for many-to-one relationships. This works but generates unnecessary DELETE + INSERT queries when the set hasn't changed. Not urgent, but worth knowing.

### 4. `SnapshotItem` vs raw `Map<String, Object>` for JSON parsing

`LotService.accept()` uses the typed `SnapshotItem` class to parse JSON (via `lot.parseSnapshot()`). `InventoryService.createItems()` uses raw `Map<String, Object>` with string keys. These are doing the same conceptual thing — parsing a JSON snapshot of items into tracked entities. The inventory side should use a typed DTO too.

### 5. No edit/update for expenses

You can create and delete expenses, but there's no edit. Every other entity (inventory, grading, lots, sales) supports editing. If this is intentional (delete and re-create), fine. Otherwise it's a missing feature.

### 6. Delete methods don't validate existence

`ExpenseService.delete()`, `SaleService.delete()`, `LotService.delete()` all call `repo.deleteById(id)` without checking if the entity exists. Spring's `deleteById` silently does nothing if the ID doesn't exist. This is probably fine for your use case but means the user gets a successful redirect even if the ID was bogus.

### 7. `rowPartial` duplication between Inventory and Lot controllers

Both `InventoryController.rowPartial()` and `LotController.rowPartial()` build a form-row DTO from many `@RequestParam`s for HTMX insertion. The approach is identical, the param lists overlap heavily. These could share a pattern — or at minimum, both should use a flat request DTO instead of 8-10 individual params.

---

## Suggestions

### Quick Wins (low effort, high clarity)

1. **Move ExpenseController stats into service:** Pull the `total`/`avg`/`last30`/`thisMonth` computation out of `ExpenseController.index()` into a `getExpensePageData()` service method.

2. **Consolidate lot accept/reject into one service method:** `LotController.updateStatus()` should call a single `lotService.updateStatus(id, action)` instead of branching in the controller.

3. **Fix `GradingService.update()` parameter order:** Change from `update(request, id)` to `update(id, request)` to match every other service.

4. **Type the inventory snapshot:** Replace the `Map<String, Object>` JSON parsing in `InventoryService.createItems()` with a typed DTO (like `SnapshotItem` or a new `InventorySnapshotRow`).

### Medium Effort

5. **Bundle `SaleController.index()` service calls:** Create a `SaleService.getIndexData(view)` method that returns the sale list + staged count together (and optionally the vince data if view=vince).

6. **Use enums in entities instead of strings for statuses:** Switch entity fields from `String status` to `GradingStatus status` (etc.) with `@Enumerated(EnumType.STRING)`. Eliminates the stringly-typed comparison problem.

7. **Add status transition validation:** `GradingService.advanceStatus()` should validate that the new status is a legal transition from the current status.

8. **Extract a `VincePaymentController`:** Move the `/sales/vince/payments` routes to their own controller since they're operating on a different domain entity.

### Larger Redesigns

9. **Dashboard query optimization:** The dashboard makes 17+ queries per load. If latency matters, explore combining some into fewer queries or adding a cache layer.

10. **Consistent HTMX response strategy:** Decide on a pattern (fragment returns? empty 200s? HX-Trigger headers?) and apply it uniformly across all controllers.
