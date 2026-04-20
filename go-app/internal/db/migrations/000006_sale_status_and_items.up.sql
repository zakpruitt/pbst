ALTER TABLE sales
    ADD COLUMN status TEXT NOT NULL DEFAULT 'STAGED';
CREATE INDEX idx_sales_status ON sales (status);

ALTER TABLE tracked_items
    ADD COLUMN sale_id BIGINT REFERENCES sales (id);
CREATE INDEX idx_tracked_items_sale_id ON tracked_items (sale_id);
