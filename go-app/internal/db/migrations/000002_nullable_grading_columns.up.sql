ALTER TABLE tracked_items
    ALTER COLUMN grading_company  DROP NOT NULL,
    ALTER COLUMN grade            DROP NOT NULL,
    ALTER COLUMN grading_upcharge DROP NOT NULL;
