ALTER TABLE chairs
ADD COLUMN registry_key VARCHAR(255);

CREATE UNIQUE INDEX ux_chairs_registry_key
ON chairs (registry_key)
WHERE registry_key IS NOT NULL;

ALTER TABLE source_endpoints
ADD COLUMN registry_key VARCHAR(255);

CREATE UNIQUE INDEX ux_source_endpoints_registry_key
ON source_endpoints (registry_key)
WHERE registry_key IS NOT NULL;

CREATE INDEX idx_source_endpoints_status
ON source_endpoints (status);
