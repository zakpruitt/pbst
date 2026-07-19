-- attributed_to follows the empty-string convention like every other text column;
-- NOT NULL lets queries match plain '' instead of (IS NULL OR = '').
UPDATE sales SET attributed_to = '' WHERE attributed_to IS NULL;
ALTER TABLE sales ALTER COLUMN attributed_to SET DEFAULT '';
ALTER TABLE sales ALTER COLUMN attributed_to SET NOT NULL;
