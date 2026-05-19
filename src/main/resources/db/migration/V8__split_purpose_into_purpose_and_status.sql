ALTER TABLE tracked_items ADD COLUMN status TEXT NOT NULL DEFAULT 'AVAILABLE';

UPDATE tracked_items SET status = 'IN_GRADING', purpose = 'INVENTORY' WHERE purpose = 'IN_GRADING';
UPDATE tracked_items SET status = 'SOLD', purpose = 'INVENTORY' WHERE purpose = 'SOLD';
UPDATE tracked_items SET status = 'ARCHIVED', purpose = 'INVENTORY' WHERE purpose = 'ARCHIVED';
UPDATE tracked_items SET status = 'AVAILABLE' WHERE purpose IN ('INVENTORY', 'PERSONAL_COLLECTION', 'PENDING_GRADE');

CREATE INDEX idx_tracked_items_status ON tracked_items(status);
