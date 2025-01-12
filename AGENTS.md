# collectingwithzak Tracker - Project Context & AI Agent Guidelines

## 1. Project Mission & Business Model
"collectingwithzak" is a personal inventory, financial tracking, and margin-calculation web application designed specifically for a lean Pokémon TCG flipping and collecting business.

**The Core Business Loop:**
* **Acquisition:** Buy bulk lots of Pokémon cards (often Japanese) at 60% to 80% of current market price.
* **Comping:** Use the "Comping Sheet" to value incoming lots, applying custom percentages to items to build an offer.
* **Skimming ("Hits"):** Extract high-value or highly desirable individual cards from these lots. These are marked as **Tracked Items** for the **Personal Collection (PC)** or sent to grading companies (PSA, BGS, TAG, CGC).
* **Liquidation (Stitching):** The remaining raw cards from various purchased lots are stitched together into new bulk lots and auctioned off on eBay.
* **Goal:** Keep overhead low. Track cumulative money spent (Lots), cumulative money grossed (Sales), and actual net take-home (after seller fees), while maintaining an active ledger of unrealized PC/Graded value.

## 2. Technology Stack
* **Backend:** Java 17, Spring Boot 3.2+
* **Database:** H2 Database (File-based). Spring Data JPA / Hibernate for ORM.
* **Frontend:** HTMX and Thymeleaf. Server-side rendering only. **Strictly NO React.** The UI must be sleek, fast, and simple for internal solo use.
* **Mapping:** MapStruct (configured to disable Lombok builders via `@Mapper(builder = @Builder(disableBuilder = true))`) and Lombok.
* **HTTP Client:** Spring Boot 3 `RestClient`.

## 3. Domain Model & Database Schema
The schema decouples bulk acquisitions from bulk sales, while tracking individual high-value items meticulously.

### Core Entities:
1.  **`LotPurchase`**: Tracks the initial acquisition. Contains `purchaseDate`, `totalCost`, `description`, `status` (PENDING, ACCEPTED, REJECTED), and a JSON `lotContentSnapshot`.
2.  **`Sale`**: Tracks money coming in (e.g., an eBay payout). Contains `saleDate`, `grossAmount`, `ebayFees`, `shippingCost`, and `netAmount`.
3.  **`PokemonCard`**: A local dictionary/cache of official Pokémon card data (Set, Name, Rarity, Card Number) and synced TCGPlayer/Market prices and `imageUrl`.
4.  **`TrackedItem`**: The core inventory unit for "Hits" pulled from lots.
    * Can be a `RAW_CARD`, `GRADED_CARD`, or `SEALED_PRODUCT`.
    * Links to `LotPurchase` (where it came from) and `PokemonCard` (what it is).
    * Contains `Purpose` enum (`PERSONAL_COLLECTION`, `TO_GRADE`, `IN_GRADING`, `GRADED_INVENTORY`).
    * Uses an `@Embedded` object (`GradedDetails`) for slab info (Company, Grade, Upcharge).
    * Tracks `costBasis` (calculated at intake) and `marketValueAtPurchase`.
5.  **`GradingSubmission`**: Tracks batches of cards sent to grading companies, including `totalGradingCost`, `submissionMethod`, and `SubmissionStatus`.

## 4. UI & Design Standards
* **Branding:** "collectingwithzak"
* **Color Palette:**
    * Primary Green: `#15884d` (Actions, Success)
    * Secondary Pink: `#d63b83` (Tracked Items, Grading)
    * Tertiary Blue: `#11b2d6` (Slabs, Info)
    * Neutral Gray: `#e0dbd8` (Backgrounds, Borders)
    * Warning Gold: `#d5ad2d` (Alerts)
* **Layout:** Centered and constrained (`max-w-4xl`), similar to a Twitter feed.
* **Interactions:** Heavily HTMX-driven. Partial page updates using Thymeleaf fragments. Minimal custom JavaScript.

## 5. Architectural & Coding Standards
* **DTO Isolation:** Services accept and return DTOs. Entities are encapsulated within the service layer.
* **MapStruct:** Use MapStruct for all DTO-to-Entity conversions.
* **Lombok:** Use `@Data`, `@Builder`, `@NoArgsConstructor`, and `@AllArgsConstructor`.
* **Logging:** Use `@Slf4j`.
* **Error Handling:** Centralized via `GlobalExceptionHandler` (API/JSON) and `ViewExceptionHandler` (UI/HTML).

## 6. Key Workflows
* **Lot Intake:** Use the "Comping Sheet" to add items, calculate an offer, and mark items for tracking.
* **Grading:** Create submissions from "To Grade" inventory, advance status, and finalize with grades and upcharges.
* **Inventory:** View all tracked items from `ACCEPTED` lots, with live market price syncing.
