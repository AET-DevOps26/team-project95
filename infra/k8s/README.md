# Kubernetes Deployment Inventory

This is the current deployment inventory derived from `docker-compose.yml`, Dockerfiles, CI workflows, and application configuration.

## Target namespace

- Kubernetes namespace: `open-thesis-radar`
- Rancher project: `devops26-team-project95`
- Namespace quota: `4 CPU / 6 GiB memory`

## Existing containerized components

All main application components already have Dockerfiles.

| Component | Docker image source | Internal port | Role |
| --- | --- | ---: | --- |
| `frontend` | `user_interface/open-thesis-radar/Dockerfile` | `80` | React app served by nginx |
| `thesis-service` | `services/thesis-service/Dockerfile` | `8080` | Main backend and relational data owner |
| `scraping-service` | `services/scraping-service/Dockerfile` | `8081` | Scrapes source websites and calls GenAI |
| `genai-service` | `services/genai-service/Dockerfile` | `8000` | FastAPI extraction/summarization service |
| `vector-search-service` | `services/vector-search-service/Dockerfile` | `8082` | Semantic/vector retrieval service |
| `thesis-db` | `postgres:16-alpine` | `5432` | PostgreSQL database for thesis-service |
| `vector-db` | `pgvector/pgvector:pg16` | `5432` | PGVector database for vector-search-service |

## Current image build pipeline

The existing reusable workflow `.github/workflows/build_images.yml` builds these images:

- `frontend`
- `thesis-service`
- `scraping-service`
- `vector-search-service`
- `genai-service`

On `main`, CI pushes them to GitHub Container Registry using this pattern:

```text
ghcr.io/<owner>/<repository>/<image>:latest
ghcr.io/<owner>/<repository>/<image>:<commit-sha>
```

The database images are external public images and are not built by the project.

Kubernetes application manifests use an `IMAGE_TAG` placeholder instead of `latest`. By default, `deploy-k8s.sh` uses the latest remote `main` commit SHA as the image tag. You can override it explicitly, for example:

```bash
IMAGE_TAG=<commit-sha> ./deploy-k8s.sh
```

## Important runtime configuration

### Frontend

- Served by nginx on port `80`.
- Uses relative `/api/...` calls.
- nginx proxies `/api/` to `http://thesis-service:8080`.
- In Kubernetes, the frontend can keep calling the internal Kubernetes Service named `thesis-service`.

### Thesis service

Required configuration:

- `THESIS_DB_HOST`
- `THESIS_DB_PORT`
- `THESIS_DB_NAME`
- `THESIS_DB_USER`
- `THESIS_DB_PASSWORD`
- `THESIS_VECTOR_SEARCH_URL`

Notes:

- Uses Flyway migrations.
- `spring.jpa.hibernate.ddl-auto=validate`, so schema must come from migrations.
- Talks to vector search through `THESIS_VECTOR_SEARCH_URL`.

### Vector search service

Required configuration:

- `PGVECTOR_URL`
- `PGVECTOR_USERNAME`
- `PGVECTOR_PASSWORD`
- `AZURE_OPENAI_ENDPOINT`
- `AZURE_OPENAI_API_KEY`
- `AZURE_OPENAI_EMBEDDING_DEPLOYMENT`
- `AZURE_OPENAI_EMBEDDING_MODEL`
- `PGVECTOR_DIMENSIONS`

Notes:

- Uses PGVector as its vector store.
- Depends on external Azure/OpenAI-compatible embedding credentials unless configured otherwise.

### Scraping service

Required configuration:

- `APP_SERVICES_MAIN_THESIS`
- `APP_SERVICES_GENAI`
- `CLIENT_CONNECT_TIMEOUT`
- `CLIENT_READ_TIMEOUT`

Notes:

- Runs scheduled scraping.
- Calls both thesis-service and genai-service.
- Should not be the first component exposed publicly.

### GenAI service

Required configuration:

- `GENAI_USE_OLLAMA`
- `AZURE_OPENAI_ENDPOINT`
- `AZURE_OPENAI_API_KEY`
- `AZURE_OPENAI_CHAT_DEPLOYMENT`
- `AZURE_OPENAI_API_VERSION`
- `GENAI_MAX_COMPLETION_TOKENS`

Notes:

- External AI credentials must become Kubernetes Secrets / GitHub Actions secrets, not committed manifests.

## Suggested first Kubernetes slice

To keep the deployment simple and debuggable, deploy in this order.

### Slice 1: minimal user-facing app

Deploy first:

1. `thesis-db`
2. `vector-db`
3. `vector-search-service`
4. `thesis-service`
5. `frontend`

Reason: `thesis-service` currently depends on `vector-search-service`, and `vector-search-service` depends on its PGVector database.

This slice should let us verify:

- database startup,
- Flyway migrations,
- backend health,
- frontend-to-backend proxying,
- public frontend access later through Ingress.

### Slice 2: AI extraction path

Add later:

1. `genai-service`
2. `scraping-service`

Reason: this introduces external AI credentials and scraping behavior, so it is better to add only after the core app is stable.

## Security and exposure decisions

- Publicly expose only the frontend through Ingress.
- Keep `thesis-service`, `vector-search-service`, `scraping-service`, `genai-service`, `thesis-db`, and `vector-db` internal-only through ClusterIP Services.
- Do not expose PostgreSQL or PGVector outside the cluster.
- Store database passwords and API keys in Kubernetes Secrets.
- Keep non-sensitive service URLs and names in ConfigMaps or plain Deployment env vars.

## Resource approach

Start with one replica per component.

Use conservative resource requests and limits for every container. The namespace quota is `4 CPU / 6 GiB`, so initial requests should stay far below that.

Initial rough target for total requests should be below about:

- `1.5 CPU`
- `2.5 GiB memory`

Then adjust based on observed usage.
