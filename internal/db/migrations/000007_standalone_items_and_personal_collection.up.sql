ALTER TABLE tracked_items
    ADD COLUMN item_type TEXT NOT NULL DEFAULT 'RAW_CARD';
UPDATE tracked_items
SET item_type = 'SEALED_PRODUCT'
WHERE sealed_product_id IS NOT NULL;
UPDATE tracked_items
SET item_type = 'GRADED_CARD'
WHERE grading_company <> '';

ALTER TABLE tracked_items
    ALTER COLUMN lot_purchase_id DROP NOT NULL;

UPDATE tracked_items
SET purpose = 'PERSONAL_COLLECTION'
WHERE purpose = 'PENDING_GRADE';
