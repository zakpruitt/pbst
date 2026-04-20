DROP INDEX IF EXISTS idx_tracked_items_sale_id;
ALTER TABLE tracked_items DROP COLUMN IF EXISTS sale_id;

DROP INDEX IF EXISTS idx_sales_status;
ALTER TABLE sales DROP COLUMN IF EXISTS status;
