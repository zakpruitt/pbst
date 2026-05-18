# Code Review — PBST (May 2026)

Fresh review of the full codebase after the recent controller/entity/mapper/service overhaul.

---

## Bugs

### 1. `getFirst()` on potentially empty query results

`SaleRepository:87`, `SaleRepository:97`, `SaleRepository:102`, `VincePaymentRepository:19`, `TrackedItemRepository:76`

All the `default` methods in repositories call `.getFirst()` on native query results without checking emptiness. Aggregate queries with `COALESCE` will always return a row in Postgres, so this won't blow up *today* — but if you ever add a `WHERE` clause that filters everything out, or switch databases, you get a `NoSuchElementException` with no useful context.

**Fix:** Guard with an empty check:
```java
default double[] getConfirmedTotals() {
    List<Object[]> results = getConfirmedTotalsRaw();
    if (results.isEmpty()) return new double[]{0, 0, 0, 0};
    Object[] row = results.getFirst();
    // ...
}
```

### 2. `LotPurchase.parseSnapshot()` creates a new `ObjectMapper` every call

`LotPurchase.java:47`

```java
ObjectMapper mapper = new ObjectMapper();
return mapper.readValue(lotContentSnapshot, new TypeReference<>() {});
```

`ObjectMapper` is expensive to construct and this gets called per lot. `InventoryService` already injects a shared `ObjectMapper` — this should do the same.

**Fix:** Move parsing to `LotService` where you can inject the shared `ObjectMapper`, or make it a `private static final` on the entity.

### 3. `attachToSubmission` / `attachToSale` use non-spec JPQL path updates

`TrackedItemRepository:27`, `TrackedItemRepository:40`

```java
UPDATE TrackedItem t SET t.gradingSubmission.id = :submissionId WHERE t.id IN :itemIds
UPDATE TrackedItem t SET t.sale.id = :saleId, t.purpose = 'SOLD' WHERE t.id IN :itemIds
```

Setting `t.gradingSubmission.id` (a nested path through a `@ManyToOne`) in a JPQL `UPDATE` is Hibernate-specific behavior, not guaranteed by the JPA spec. Hibernate 6 happens to translate this to a direct FK column update, but a version bump or provider swap could break it silently.

**Fix:** Use native queries:
```java
@Query(value = "UPDATE tracked_items SET grading_submission_id = :submissionId WHERE id IN :itemIds", nativeQuery = true)
void attachToSubmission(List<Long> itemIds, Long submissionId);
```

### 4. Grading submission name race condition

`GradingService:42-46`

```java
long count = gradingRepo.countByCompany(request.getCompany());
// ...
.submissionName(String.format("%s Submission #%d", request.getCompany(), count + 1))
```

Two concurrent creates for the same company will get the same count and produce duplicate names. Not a data integrity issue (no unique constraint), but confusing for the user.

**Fix:** Generate the name after save using the auto-assigned ID, e.g. `"PSA Submission #42"`.

---

## Design Issues

### 5. `double[]` and `Object[]` as return types from repositories

`SaleRepository:86-94`, `TrackedItemRepository:75-78`, `VincePaymentRepository:18-21`

The dashboard unpacks magic indices (`confirmed[0]`, `confirmed[3]`) with no type safety. One wrong index = silent data corruption.

**Fix:** Return typed records:
```java
record ConfirmedTotals(long count, double gross, double net, double fees) {}
```

### 6. All `TrackedItem` relationships are `FetchType.EAGER`

`TrackedItem.java:37-55`

Every `TrackedItem` load eagerly fetches `lotPurchase`, `pokemonCard`, `sealedProduct`, `gradingSubmission`, and `sale` — 5 joins. The `findByPurpose` query already does `LEFT JOIN FETCH` for 4 of these, making the EAGER annotations redundant there but actively harmful everywhere else (like `findById`, `findAvailableInventory`, etc.).

**Fix:** Switch all to `FetchType.LAZY`. Existing `JOIN FETCH` queries already handle the cases where you need related data. This is probably the single biggest easy performance win.

### 7. Self-injection in `SaleService`

`SaleService:41-43`

```java
@Autowired @Lazy
private SaleService self;
```

Used for `REQUIRES_NEW` propagation on `upsertFromEbay`. Works fine but is a known anti-pattern that confuses anyone who sees it for the first time.

**Fix:** Extract `upsertFromEbay` into a small `EbaySaleUpsertService` that `SaleService` calls normally.

### 8. CSRF disabled globally

`SecurityConfig.java:26`

CSRF is off for the entire app, but most endpoints are Thymeleaf form POSTs, not a stateless API. This leaves every authenticated POST vulnerable to cross-site forgery.

**Fix:** Re-enable CSRF. Thymeleaf's `th:action` auto-includes the token. For HTMX requests, add a meta tag + `hx-headers` config to send the token as a header.

### 9. No input validation on request DTOs

No controller uses `@Valid`, no DTOs appear to have JSR-303 annotations.

- `SaleController:37` — `CreateSaleRequest` could have null title, negative amounts
- `GradingController:37` — `CreateGradingRequest` could have null company
- `SaleController:142-144` — `grossAmount`/`netAmount` accept any double (negatives, NaN)
- `GradingController:80` — `newStatus` is a raw string, not validated against `GradingStatus` values

**Fix:** Add `@NotBlank`, `@Min(0)`, etc. to DTOs; add `@Valid` to controller params.

### 10. `advanceStatus` accepts any string

`GradingController:80`, `GradingService:124`

No validation that `newStatus` is a real `GradingStatus`, and no enforcement of valid transitions (you could go from RETURNED back to PREPPING). Same problem exists with `LotService` status changes via the controller.

**Fix:** Accept the enum type, validate allowed transitions:
```java
private static final Map<GradingStatus, Set<GradingStatus>> TRANSITIONS = Map.of(
    PREPPING, Set.of(IN_GRADING),
    IN_GRADING, Set.of(RETURNED),
    // ...
);
```

---

## Performance

### 11. Dashboard fires 17+ separate queries

`DashboardService:38-89`

A single dashboard page load runs:
- `getTotalCostNonRejected`, `getConfirmedTotals`, `countByPurpose` x2, `getInventoryTotals`
- `getTotalsSince` x2, `getLedger` (2 inner queries)
- `getMonthlySpend`, `getMonthlyRevenue`, `countByOrigin`, `countByItemType`
- `countByStatus` x2, `findTopByNet`, `findRecent`, `findByOrderByPurchaseDateDesc`

Fine at current scale, but this will degrade as data grows.

**Short-term fix:** Add `@Transactional(readOnly = true)` to skip dirty-checking. The class already has `@Transactional` but not `readOnly`, so Hibernate tracks every loaded entity for changes it'll never flush.

**Longer-term:** Consolidate the aggregate queries into fewer calls, or cache the result.

### 12. LIKE with leading wildcard on search

`PokemonCardRepository` / `SealedProductRepository` search queries use `LIKE '%query%'`. Leading `%` forces a full table scan regardless of indexes.

**Fix (if needed):** PostgreSQL `pg_trgm` trigram index, or full-text search with `to_tsvector`.

### 13. `DashboardService` is read-write `@Transactional` but only reads

`DashboardService:23`

Class-level `@Transactional` defaults to read-write. Every entity loaded during dashboard builds gets dirty-checked on flush — wasteful since nothing is modified.

**Fix:** `@Transactional(readOnly = true)` on the class.

---

## Inconsistencies & Cleanup

### 14. Enum values stored as strings without `@Enumerated`

`TrackedItem`, `Sale`, `LotPurchase` store `purpose`, `status`, `itemType`, `origin` as plain `String` fields. Enums exist (`GradingStatus`, `SaleStatus`, `LotStatus`, `Purpose`, `ItemType`, `Origin`) but are only used for `.name()` calls. Some code uses `SaleStatus.CONFIRMED.name()`, other code uses `"CONFIRMED"` directly in JPQL queries.

This is the single biggest source of potential silent bugs — a typo compiles fine and silently fails at runtime.

### 15. `InventoryService.createItems()` uses raw `Map<String, Object>` for JSON

`InventoryService:41-46`

```java
rows = objectMapper.readValue(request.getItemsSnapshot(), new TypeReference<List<Map<String, Object>>>() {});
```

Then manually pulls out `"cost_basis"`, `"market_value"`, etc. by string key. The lot side already has a typed `SnapshotItem` for the same concept.

**Fix:** Create a typed DTO or reuse `SnapshotItem`.

### 16. `GradingService.update()` parameter order

`GradingService:107` — `update(UpdateGradingRequest request, Long id)` puts request first, but every other service method puts `id` first. Minor but inconsistent.

### 17. `ignore`, `markAsVince`, `unstage` are near-identical

`SaleService:145-162`

All three: detach items from sale, then update status/attribution. Could share a private helper:
```java
private void changeStatus(Long saleId, String status, String attribution) {
    itemRepo.detachFromSale(saleId);
    saleRepo.updateStatusAndAttribution(saleId, status, attribution);
}
```

### 18. ExpenseController computes stats in the controller

`ExpenseController:30-50` (approx)

The controller fetches all expenses, then computes totals, averages, last-30-day stats, this-month stats in-memory. This is business logic that belongs in the service. Also loads the entire expense history to compute stats that could be aggregate queries.

### 19. SaleController owns VincePayment routes

`SaleController:42-46`, `SaleController:163-167`

Vince payment CRUD lives inside `SaleController`. If VincePayment grows more endpoints this will get crowded. Worth extracting to its own controller.

### 20. `SaleController.ignore()` and `vince()` return `Object`

`SaleController:117`, `SaleController:127`

These return either `ResponseEntity` (HTMX) or `String` (redirect). The `Object` return type works but loses all type safety. Consider splitting HTMX vs full-page endpoints, or standardizing on one approach.

---

## What's Good

- **Thin controllers** — almost all business logic lives in services
- **Consistent patterns** — every domain follows the same CRUD structure
- **MapStruct usage** — clean separation of entity ↔ DTO mapping, update methods avoid manual field copies
- **`@Modifying` queries** — bulk updates via JPQL instead of load-modify-save loops
- **`MonthGroup` utility** — reusable month-grouping abstraction used across sales, grading, expenses
- **HTMX integration** — fragment returns (`:: inventory-page`) are clean
- **Exception handling** — `GlobalExceptionHandler` with proper HTTP status codes, no stack traces in responses
- **Builder pattern** — consistent use across entities and DTOs
- **`findById` helpers** — private methods with `ResourceNotFoundException` in every service, no duplicated orElseThrow logic
