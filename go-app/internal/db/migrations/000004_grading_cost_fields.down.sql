ALTER TABLE grading_submissions RENAME COLUMN upcharge_total TO total_grading_cost;
ALTER TABLE grading_submissions DROP COLUMN submission_cost;
ALTER TABLE grading_submissions DROP COLUMN tax_rate;
ALTER TABLE grading_submissions DROP COLUMN cost_per_card;
