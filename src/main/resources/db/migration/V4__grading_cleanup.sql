-- gradingUpcharge is a primitive double on the GradedDetails embeddable: a row with a
-- grading company but a NULL upcharge fails to load. Backfill only graded rows — a fully
-- NULL embeddable must stay NULL so Hibernate keeps treating those items as ungraded.
UPDATE tracked_items SET grading_upcharge = 0
WHERE grading_upcharge IS NULL AND (grading_company IS NOT NULL OR grade IS NOT NULL);

-- tax_rate was never read or written by the app.
ALTER TABLE grading_submissions DROP COLUMN tax_rate;

-- GradingStatus is only PREPPING -> IN_GRADING -> RETURNED; normalize any legacy
-- rows before a load can hit a value the enum no longer has.
UPDATE grading_submissions SET status = 'PREPPING'
WHERE status IN ('ACCEPTED', 'REJECTED', 'PENDING');
