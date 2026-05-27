CREATE TABLE chairs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    website_url VARCHAR(1024) NOT NULL
);

CREATE TABLE advisors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    profile_url VARCHAR(1024)
);

CREATE TABLE research_areas (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE source_endpoints (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(1024) NOT NULL,
    status VARCHAR(255) NOT NULL,
    last_scraped_at TIMESTAMPTZ,
    chair_id BIGINT NOT NULL,
    CONSTRAINT fk_source_endpoints_chair FOREIGN KEY (chair_id) REFERENCES chairs(id) ON DELETE CASCADE
);

CREATE TABLE scrape_runs (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(255) NOT NULL,
    error_message TEXT,
    candidates_found INTEGER DEFAULT 0,
    source_endpoint_id BIGINT NOT NULL,
    CONSTRAINT fk_scrape_runs_source_endpoint FOREIGN KEY (source_endpoint_id) REFERENCES source_endpoints(id) ON DELETE CASCADE
);

CREATE TABLE thesis_proposals (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    degree_type VARCHAR(255),
    original_description TEXT,
    ai_overview TEXT,
    source_url VARCHAR(1024) NOT NULL,
    raw_html_snapshot TEXT,
    extraction_confidence REAL,
    status VARCHAR(255) NOT NULL DEFAULT 'OPEN',
    last_seen_at TIMESTAMPTZ,
    chair_id BIGINT NOT NULL,
    CONSTRAINT fk_thesis_proposals_chair FOREIGN KEY (chair_id) REFERENCES chairs(id) ON DELETE CASCADE
);

-- Join Table: Thesis Proposal <-> Advisors
CREATE TABLE thesis_proposal_advisors (
    thesis_proposal_id BIGINT NOT NULL,
    advisor_id BIGINT NOT NULL,
    PRIMARY KEY (thesis_proposal_id, advisor_id),
    CONSTRAINT fk_tpa_proposal FOREIGN KEY (thesis_proposal_id) REFERENCES thesis_proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_tpa_advisor FOREIGN KEY (advisor_id) REFERENCES advisors(id) ON DELETE CASCADE
);

-- Join Table: Thesis Proposal <-> Tags
CREATE TABLE thesis_proposal_tags (
    thesis_proposal_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (thesis_proposal_id, tag_id),
    CONSTRAINT fk_tpt_proposal FOREIGN KEY (thesis_proposal_id) REFERENCES thesis_proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_tpt_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Join Table: Thesis Proposal <-> Research Areas
CREATE TABLE thesis_proposal_research_areas (
    thesis_proposal_id BIGINT NOT NULL,
    research_area_id BIGINT NOT NULL,
    PRIMARY KEY (thesis_proposal_id, research_area_id),
    CONSTRAINT fk_tpra_proposal FOREIGN KEY (thesis_proposal_id) REFERENCES thesis_proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_tpra_research_area FOREIGN KEY (research_area_id) REFERENCES research_areas(id) ON DELETE CASCADE
);
