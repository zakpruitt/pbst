-- Archive tracked items that were soft-deleted instead of losing them
UPDATE tracked_items SET purpose = 'ARCHIVED' WHERE deleted_at IS NOT NULL;

-- Hard delete soft-deleted rows from all other tables
DELETE FROM sales WHERE deleted_at IS NOT NULL;
DELETE FROM lot_purchases WHERE deleted_at IS NOT NULL;
DELETE FROM grading_submissions WHERE deleted_at IS NOT NULL;
DELETE FROM expenses WHERE deleted_at IS NOT NULL;
DELETE FROM vince_payments WHERE deleted_at IS NOT NULL;
DELETE FROM pokemon_cards WHERE deleted_at IS NOT NULL;
DELETE FROM sealed_products WHERE deleted_at IS NOT NULL;

-- Drop deleted_at indexes
DROP INDEX IF EXISTS idx_lot_purchases_deleted_at;
DROP INDEX IF EXISTS idx_tracked_items_deleted_at;
DROP INDEX IF EXISTS idx_grading_submissions_deleted_at;
DROP INDEX IF EXISTS idx_sales_deleted_at;
DROP INDEX IF EXISTS idx_pokemon_cards_deleted_at;
DROP INDEX IF EXISTS idx_sealed_products_deleted_at;
DROP INDEX IF EXISTS idx_expenses_deleted_at;

-- Drop deleted_at columns
ALTER TABLE tracked_items DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE sales DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE lot_purchases DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE grading_submissions DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE expenses DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE vince_payments DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE pokemon_cards DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE sealed_products DROP COLUMN IF EXISTS deleted_at;
