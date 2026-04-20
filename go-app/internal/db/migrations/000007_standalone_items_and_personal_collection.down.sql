UPDATE tracked_items
SET purpose = 'PENDING_GRADE'
WHERE purpose = 'PERSONAL_COLLECTION';

ALTER TABLE tracked_items DROP COLUMN item_type;
-- lot_purchase_id stays nullable on down; restoring NOT NULL would fail with any standalone items.
