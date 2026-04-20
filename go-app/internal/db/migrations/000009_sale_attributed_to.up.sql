ALTER TABLE sales
    ADD COLUMN attributed_to TEXT NOT NULL DEFAULT '';

CREATE INDEX idx_sales_attributed_to ON sales(attributed_to);
