-- Move raw_html_snapshot from thesis_proposals to scrape_runs
ALTER TABLE scrape_runs ADD COLUMN IF NOT EXISTS raw_html_snapshot TEXT;

-- Remove the column from thesis_proposals
ALTER TABLE thesis_proposals DROP COLUMN IF EXISTS raw_html_snapshot;

-- Remove extraction_confidence from thesis_proposals (previously removed from OpenAPI/Domain without migration)
ALTER TABLE thesis_proposals DROP COLUMN IF EXISTS extraction_confidence;
