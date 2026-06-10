#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

: "${THESIS_DB_HOST:=thesis-db}"
: "${THESIS_DB_PORT:=5432}"
: "${THESIS_DB_NAME:=thesis_db}"
: "${THESIS_DB_USER:=thesis_user}"
: "${THESIS_DB_PASSWORD:=thesis_password}"
: "${SOURCE_ENDPOINTS_JSON:=$SCRIPT_DIR/source-endpoints.json}"

if [ ! -f "$SOURCE_ENDPOINTS_JSON" ]; then
  echo "Seed file not found: $SOURCE_ENDPOINTS_JSON" >&2
  exit 1
fi

export PGPASSWORD="$THESIS_DB_PASSWORD"
json_payload="$(cat "$SOURCE_ENDPOINTS_JSON")"

psql \
  --host="$THESIS_DB_HOST" \
  --port="$THESIS_DB_PORT" \
  --dbname="$THESIS_DB_NAME" \
  --username="$THESIS_DB_USER" \
  --set=ON_ERROR_STOP=1 \
  --set=source_endpoints_json="$json_payload" <<'SQL'
WITH input_endpoints AS (
    SELECT DISTINCT ON (website_url)
        name,
        website_url
    FROM jsonb_to_recordset(:'source_endpoints_json'::jsonb)
        AS endpoint(name text, "websiteUrl" text)
        CROSS JOIN LATERAL (SELECT endpoint."websiteUrl" AS website_url) normalized
    WHERE NULLIF(BTRIM(name), '') IS NOT NULL
      AND NULLIF(BTRIM(website_url), '') IS NOT NULL
    ORDER BY website_url, name
),
inserted_chairs AS (
    INSERT INTO chairs (name, website_url)
    SELECT input.name, input.website_url
    FROM input_endpoints input
    WHERE NOT EXISTS (
        SELECT 1
        FROM chairs chair
        WHERE chair.name = input.name
          AND chair.website_url = input.website_url
    )
    RETURNING id, name, website_url
),
seed_chairs AS (
    SELECT id, name, website_url
    FROM inserted_chairs
    UNION
    SELECT chair.id, chair.name, chair.website_url
    FROM chairs chair
    JOIN input_endpoints input
      ON input.name = chair.name
     AND input.website_url = chair.website_url
),
inserted_source_endpoints AS (
    INSERT INTO source_endpoints (url, status, chair_id)
    SELECT seed_chair.website_url, 'ACTIVE', seed_chair.id
    FROM seed_chairs seed_chair
    WHERE NOT EXISTS (
        SELECT 1
        FROM source_endpoints source_endpoint
        WHERE source_endpoint.url = seed_chair.website_url
    )
    RETURNING id
)
SELECT
    (SELECT COUNT(*) FROM input_endpoints) AS endpoints_in_seed_file,
    (SELECT COUNT(*) FROM inserted_chairs) AS chairs_inserted,
    (SELECT COUNT(*) FROM inserted_source_endpoints) AS source_endpoints_inserted;
SQL
