-- Remove the legacy app-level tag taxonomy.
-- Research areas are now the canonical classification dimension.
--
-- This migration intentionally does not clear thesis_proposal_research_areas.
-- Reclassification is model-driven and should be run explicitly via:
--   scripts/reclassify-research-areas.py

DROP TABLE IF EXISTS thesis_proposal_tags CASCADE;
DROP TABLE IF EXISTS tags CASCADE;

ALTER TABLE thesis_proposals DROP COLUMN IF EXISTS tags;
ALTER TABLE thesis_proposals DROP COLUMN IF EXISTS tag;
