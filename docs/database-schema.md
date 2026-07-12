# PostgreSQL Schema and Persistence Model

Open Thesis Radar uses two PostgreSQL databases:

| Database | Owner | Purpose | Compose service / host port |
| --- | --- | --- | --- |
| Thesis database | `thesis-service` | Canonical relational data for chairs, source endpoints, scrape runs, thesis proposals, advisors, tags, and research areas | `thesis-db` on `localhost:5432` |
| Vector database | `vector-search-service` | Embedding vectors and metadata used for semantic search | `vector-db` on `localhost:5433` |

The relational schema is created by Flyway migrations in `services/thesis-service/src/main/resources/db/migration`. The vector schema is managed by Spring AI PGVector in the vector-search service.

## Relational schema overview

```text
chairs
  ├─ source_endpoints
  │    ├─ scrape_runs
  │    └─ thesis_proposals
  └─ thesis_proposals
       ├─ thesis_proposal_advisors ── advisors
       ├─ thesis_proposal_tags ────── tags
       └─ thesis_proposal_research_areas ── research_areas
```

`chairs` and `source_endpoints` define where thesis data comes from. `scrape_runs` records scraping attempts for a source endpoint. `thesis_proposals` stores the normalized thesis records that are shown to users. Advisors, tags, and research areas are normalized and connected to proposals through join tables.

## Main tables

### `chairs`

Stores university chairs or groups that publish thesis proposals.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Primary key |
| `name` | `VARCHAR(255)` | Chair name |
| `website_url` | `VARCHAR(1024)` | Chair website |
| `registry_key` | `VARCHAR(255)` | Optional stable key from the source registry |

Indexes and constraints:

- Primary key on `id`.
- Partial unique index `ux_chairs_registry_key` for non-null registry keys.

### `source_endpoints`

Stores configured URLs that the scraping service fetches for a chair.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Primary key |
| `url` | `VARCHAR(1024)` | Scrape URL |
| `status` | `VARCHAR(255)` | Endpoint status, for example active/inactive states used by the service |
| `last_scraped_at` | `TIMESTAMPTZ` | Last scrape timestamp |
| `chair_id` | `BIGINT` | Required reference to `chairs.id` |
| `registry_key` | `VARCHAR(255)` | Optional stable key from the source registry |
| `last_content_hash` | `VARCHAR(64)` | Last observed content hash for change detection |

Indexes and constraints:

- Foreign key `chair_id -> chairs.id` with `ON DELETE CASCADE`.
- Indexes on `chair_id` and `status`.
- Partial unique index `ux_source_endpoints_registry_key` for non-null registry keys.

### `scrape_runs`

Records scraping attempts for source endpoints.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Primary key |
| `started_at` | `TIMESTAMPTZ` | Required start time |
| `finished_at` | `TIMESTAMPTZ` | Optional completion time |
| `status` | `VARCHAR(255)` | Scrape run status |
| `error_message` | `TEXT` | Error details for failed runs |
| `candidates_found` | `INTEGER` | Number of thesis candidates found; defaults to `0` |
| `source_endpoint_id` | `BIGINT` | Required reference to `source_endpoints.id` |
| `raw_html_snapshot` | `TEXT` | Raw page snapshot captured for the scrape run |

Indexes and constraints:

- Foreign key `source_endpoint_id -> source_endpoints.id` with `ON DELETE CASCADE`.
- Index on `source_endpoint_id`.

### `thesis_proposals`

Stores the canonical thesis proposals returned by the frontend API.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Primary key |
| `title` | `VARCHAR(255)` | Required title |
| `degree_type` | `VARCHAR(255)` | Bachelor/master/project/etc. classification when available |
| `original_description` | `TEXT` | Extracted source description |
| `ai_overview` | `TEXT` | AI-generated overview shown to users |
| `source_url` | `VARCHAR(1024)` | Required original thesis URL |
| `status` | `VARCHAR(255)` | Proposal status; defaults to `OPEN` |
| `last_seen_at` | `TIMESTAMPTZ` | Last time the proposal was observed during scraping |
| `chair_id` | `BIGINT` | Required reference to `chairs.id` |
| `source_endpoint_id` | `BIGINT` | Required reference to `source_endpoints.id` |

Indexes and constraints:

- Foreign key `chair_id -> chairs.id` with `ON DELETE CASCADE`.
- Foreign key `source_endpoint_id -> source_endpoints.id` with `ON DELETE CASCADE`.
- Indexes on `chair_id` and `source_endpoint_id`.

### `advisors`

Stores thesis advisors and contact information.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Primary key |
| `name` | `VARCHAR(255)` | Advisor name |
| `email` | `VARCHAR(255)` | Unique email address |
| `profile_url` | `VARCHAR(1024)` | Optional profile page |

### `tags`

Stores reusable topic tags extracted for thesis proposals.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Primary key |
| `name` | `VARCHAR(100)` | Unique tag name |

### `research_areas`

Stores normalized research area names.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `BIGSERIAL` | Primary key |
| `name` | `VARCHAR(255)` | Unique research area name |

## Join tables

Many-to-many relationships are represented with composite-key join tables.

| Table | Columns | Relationship |
| --- | --- | --- |
| `thesis_proposal_advisors` | `thesis_proposal_id`, `advisor_id` | Connects proposals to one or more advisors |
| `thesis_proposal_tags` | `thesis_proposal_id`, `tag_id` | Connects proposals to zero or more tags |
| `thesis_proposal_research_areas` | `thesis_proposal_id`, `research_area_id` | Connects proposals to zero or more research areas |

Each join table uses:

- A composite primary key over both IDs.
- A foreign key to `thesis_proposals.id` with `ON DELETE CASCADE`.
- A foreign key to the referenced lookup table with `ON DELETE CASCADE`.
- Separate indexes on both foreign key columns.

## Relationship summary

- One `chair` has many `source_endpoints`.
- One `chair` has many `thesis_proposals`.
- One `source_endpoint` has many `scrape_runs`.
- One `source_endpoint` has many `thesis_proposals`.
- One `thesis_proposal` can have many `advisors`, `tags`, and `research_areas` through join tables.
- Cascading deletes are used from chairs/source endpoints/proposals to dependent rows, so removing a source owner also removes its dependent scrape and thesis data.

During ingestion, the Thesis Service owns the relational data. The scraping workflow submits extracted results to the Thesis Service, which replaces thesis data for the relevant source/chair transactionally and then asks the Vector Search Service to refresh the corresponding embeddings.

## Flyway migration strategy

Flyway is enabled in `services/thesis-service/src/main/resources/application.yaml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    baseline-on-migrate: true
    check-location: true
    locations: classpath:db/migration
```

Important points:

- Flyway migrations are the source of truth for the thesis database schema.
- Hibernate validates the schema with `ddl-auto: validate`; it does not create or update tables automatically.
- Migration files are versioned as `V<version>__<description>.sql` and run in order.
- Applied migrations are tracked by Flyway in its schema history table.

Current migrations:

| Migration | Purpose |
| --- | --- |
| `V1__Initial_Schema.sql` | Creates chairs, advisors, research areas, tags, source endpoints, scrape runs, thesis proposals, join tables, foreign keys, and indexes |
| `V2__Move_Raw_Html_To_Scrape_Run.sql` | Moves raw HTML snapshots from thesis proposals to scrape runs and removes obsolete extraction confidence storage |
| `V3__Add_Source_Registry_Keys.sql` | Adds stable registry keys for chairs and source endpoints plus status indexing |
| `V4__Add_Last_Content_Hash.sql` | Adds content hash tracking to source endpoints |

## Local Docker persistence

Local development uses `docker-compose.yml`.

| Service | Image | Volume | Mount path |
| --- | --- | --- | --- |
| `thesis-db` | `postgres:16-alpine` | `postgres_data` | `/var/lib/postgresql/data` |
| `vector-db` | `pgvector/pgvector:pg16` | `vector_postgres_data` | `/var/lib/postgresql/data` |

Production Compose (`docker-compose.prod.yml`) uses equivalent named volumes:

| Service | Volume |
| --- | --- |
| `thesis-db` | `thesis-postgres-data` |
| `vector-db` | `vector-postgres-data` |

Named Docker volumes keep database files across container restarts and image rebuilds. Running `docker compose down -v` removes these volumes and deletes the stored database state.

## Kubernetes persistence

The Kubernetes deployment uses Helm charts for PostgreSQL databases. The recommended deployment path is `infra/helm/open-thesis-radar`.

In `infra/helm/open-thesis-radar/values.yaml`:

| Database | Chart key | Persistent volume size | Secret |
| --- | --- | ---: | --- |
| Thesis PostgreSQL | `thesis-db` | `8Gi` | `thesis-db-secret` |
| Vector PostgreSQL | `vector-db` | `10Gi` | `vector-db-secret` |

The older Helmfile values in `infra/k8s/helm-values/` use the same persistence model:

- `infra/k8s/helm-values/thesis-db-values.yml` configures `primary.persistence.size: 8Gi`.
- `infra/k8s/helm-values/vector-db-values.yml` configures `primary.persistence.size: 10Gi` and initializes the `vector` extension.

The databases are internal cluster services. They should not be exposed publicly. Passwords are supplied via Kubernetes Secrets rather than committed values.

## Vector database and embeddings

Semantic search data is stored separately from the relational thesis database.

The Vector Search Service uses Spring AI PGVector with this configuration in `services/vector-search-service/src/main/resources/application.yaml`:

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: ${PGVECTOR_INITIALIZE_SCHEMA:true}
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: ${PGVECTOR_DIMENSIONS:1536}
```

The vector database stores:

- Embedding vectors generated from thesis title, AI overview, original description, research area, degree type, and tags.
- Metadata such as `thesisId`, `chairId`, `sourceEndpointId`, `title`, `degreeType`, `researchArea`, `sourceUrl`, and `tags`.

The vector records are derived data. The source of truth for thesis details remains the Thesis Service relational PostgreSQL database. If embeddings are stale or lost, they can be regenerated from relational thesis data by reindexing through the Thesis Service and Vector Search Service.
