-- Drop the Go-era `TEXT NOT NULL DEFAULT ''` pattern.
-- Rules:
--   * Columns that may legitimately be absent => nullable, no default.
--   * Columns that must always have a value => keep NOT NULL, drop DEFAULT '' so missing input fails loudly.
--   * Columns representing business state enums => keep NOT NULL + meaningful default.

-- pokemon_cards: names/set/number come from the sync API and must be present; rarity and image_url are optional.
ALTER TABLE pokemon_cards ALTER COLUMN name        DROP DEFAULT;
ALTER TABLE pokemon_cards ALTER COLUMN set_code    DROP DEFAULT;
ALTER TABLE pokemon_cards ALTER COLUMN set_name    DROP DEFAULT;
ALTER TABLE pokemon_cards ALTER COLUMN card_number DROP DEFAULT;
ALTER TABLE pokemon_cards ALTER COLUMN rarity      DROP DEFAULT;
ALTER TABLE pokemon_cards ALTER COLUMN rarity      DROP NOT NULL;
ALTER TABLE pokemon_cards ALTER COLUMN image_url   DROP DEFAULT;
ALTER TABLE pokemon_cards ALTER COLUMN image_url   DROP NOT NULL;

-- sealed_products: name + set always known from sync; image_url may be missing.
ALTER TABLE sealed_products ALTER COLUMN name      DROP DEFAULT;
ALTER TABLE sealed_products ALTER COLUMN set_code  DROP DEFAULT;
ALTER TABLE sealed_products ALTER COLUMN set_name  DROP DEFAULT;
ALTER TABLE sealed_products ALTER COLUMN image_url DROP DEFAULT;
ALTER TABLE sealed_products ALTER COLUMN image_url DROP NOT NULL;

-- lot_purchases: seller_name required; description + snapshot optional; status keeps 'PENDING' default.
ALTER TABLE lot_purchases ALTER COLUMN seller_name          DROP DEFAULT;
ALTER TABLE lot_purchases ALTER COLUMN description          DROP DEFAULT;
ALTER TABLE lot_purchases ALTER COLUMN description          DROP NOT NULL;
ALTER TABLE lot_purchases ALTER COLUMN lot_content_snapshot DROP DEFAULT;
ALTER TABLE lot_purchases ALTER COLUMN lot_content_snapshot DROP NOT NULL;

-- grading_submissions: submission_name + company required; submission_method optional.
ALTER TABLE grading_submissions ALTER COLUMN submission_name   DROP DEFAULT;
ALTER TABLE grading_submissions ALTER COLUMN company           DROP DEFAULT;
ALTER TABLE grading_submissions ALTER COLUMN submission_method DROP DEFAULT;
ALTER TABLE grading_submissions ALTER COLUMN submission_method DROP NOT NULL;

-- sales: most text fields are eBay-sourced and may be missing; origin + status keep enum defaults.
ALTER TABLE sales ALTER COLUMN title          DROP DEFAULT;
ALTER TABLE sales ALTER COLUMN title          DROP NOT NULL;
ALTER TABLE sales ALTER COLUMN buyer_username DROP DEFAULT;
ALTER TABLE sales ALTER COLUMN buyer_username DROP NOT NULL;
ALTER TABLE sales ALTER COLUMN image_url      DROP DEFAULT;
ALTER TABLE sales ALTER COLUMN image_url      DROP NOT NULL;
ALTER TABLE sales ALTER COLUMN order_status   DROP DEFAULT;
ALTER TABLE sales ALTER COLUMN order_status   DROP NOT NULL;
ALTER TABLE sales ALTER COLUMN attributed_to  DROP DEFAULT;
ALTER TABLE sales ALTER COLUMN attributed_to  DROP NOT NULL;

-- expenses: name required, drop '' default so empty input fails.
ALTER TABLE expenses ALTER COLUMN name DROP DEFAULT;

-- Existing '' rows stay as-is; app code should treat empty string and NULL interchangeably at the boundary.
