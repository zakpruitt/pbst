DROP INDEX IF EXISTS idx_sales_attributed_to;

ALTER TABLE sales
    DROP COLUMN attributed_to;
