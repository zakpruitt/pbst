ALTER TABLE grading_submissions ADD COLUMN cost_per_card NUMERIC(10,2) NOT NULL DEFAULT 20.00;
ALTER TABLE grading_submissions ADD COLUMN tax_rate NUMERIC(6,5) NOT NULL DEFAULT 0.04225;
ALTER TABLE grading_submissions ADD COLUMN submission_cost NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE grading_submissions RENAME COLUMN total_grading_cost TO upcharge_total;
