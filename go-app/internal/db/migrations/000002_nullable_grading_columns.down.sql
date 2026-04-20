ALTER TABLE tracked_items
    ALTER COLUMN grading_company  SET NOT NULL,
    ALTER COLUMN grade            SET NOT NULL,
    ALTER COLUMN grading_upcharge SET NOT NULL;
