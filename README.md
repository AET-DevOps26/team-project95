# Open Thesis Radar / ThesisHub

A thesis-discovery platform for TUM students. Open Thesis Radar centralizes open thesis proposals from chair websites, enriches them with GenAI-powered extraction and summaries, and supports both filter-based and natural-language semantic search.

> TUM DevOps Project 2026 · Team Project 95

## Quick Start

```bash
# clone the repository
git clone <repository-url>
cd team-project95

# optional: create local environment file
cp .env.example .env  # if available, otherwise create .env manually

# start the full local stack
docker compose up --build
```

Local URLs:

- Frontend: http://localhost:3000
- Thesis Service: http://localhost:8080
- Scraping Service: http://localhost:8081
- Vector Search Service: http://localhost:8082
- GenAI Service: http://localhost:8000
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001
- PostgreSQL thesis database: localhost:5432
- PostgreSQL vector database: localhost:5433

## Installation

Prerequisites:

- Git
- Docker
- Docker Compose
- Java 25, if running Spring services outside Docker
- Maven, if running Spring services outside Docker
- Python 3.11+, if running the GenAI service outside Docker
- Node.js and npm, if running the frontend outside Docker

### Environment Variables

Important environment variables are grouped by their scope and purpose:

#### 1. GenAI & Embeddings Configuration

These variables configure the FastAPI GenAI and Vector Search services.

| Variable                            | Purpose                                                              |
| :---------------------------------- | :------------------------------------------------------------------- |
| `GENAI_MODEL_PROVIDER`              | Provider for GenAI (e.g. `azure`, `openai`, or `ollama`)             |
| `GENAI_USE_OLLAMA`                  | Set to `true` to use Ollama instead of Azure OpenAI (legacy switch)  |
| `GENAI_MODEL_NAME`                  | Model name to use (e.g., `llama3.1` for Ollama, `gpt-4o` for OpenAI) |
| `GENAI_MAX_COMPLETION_TOKENS`       | Maximum GenAI chat completion tokens                                 |
| `OLLAMA_BASE_URL`                   | Ollama endpoint when using local models                              |
| `AZURE_OPENAI_ENDPOINT`             | Azure OpenAI endpoint for GenAI and embeddings                       |
| `AZURE_OPENAI_API_KEY`              | Azure OpenAI API key for GenAI and embeddings                        |
| `AZURE_OPENAI_CHAT_DEPLOYMENT`      | Azure OpenAI chat deployment for GenAI                               |
| `AZURE_OPENAI_API_VERSION`          | Azure OpenAI API version for GenAI                                   |
| `AZURE_OPENAI_EMBEDDING_DEPLOYMENT` | Embedding deployment name                                            |
| `OPENAI_API_KEY`                    | Standard OpenAI API key (if using OpenAI provider directly)          |
| `PGVECTOR_DIMENSIONS`               | Embedding vector dimensions (typically `1536`)                       |

#### 2. Database Configuration

These variables configure the PostgreSQL relational and vector databases.

| Variable             | Purpose                         |
| :------------------- | :------------------------------ |
| `THESIS_DB_NAME`     | Thesis PostgreSQL database name |
| `THESIS_DB_USER`     | Thesis PostgreSQL user          |
| `THESIS_DB_PASSWORD` | Thesis PostgreSQL password      |
| `VECTOR_DB_NAME`     | Vector PostgreSQL database name |
| `VECTOR_DB_USER`     | Vector PostgreSQL user          |
| `VECTOR_DB_PASSWORD` | Vector PostgreSQL password      |

#### 3. Application & Monitoring Configuration

General application runtime settings.

| Variable                 | Purpose                                                              |
| :----------------------- | :------------------------------------------------------------------- |
| `APP_ADDITIONAL_DOMAINS` | Comma-separated list of additional domains mapped to the application |
| `GRAFANA_ADMIN_PASSWORD` | Administrator password for Grafana                                   |

#### 4. Deployment & Infrastructure Secrets (CI/CD)

These variables are set in GitHub Secrets/Variables and are used by the deployment workflows.

##### Azure VM (Terraform & Ansible)

| Variable                           | Purpose                                                             |
| :--------------------------------- | :------------------------------------------------------------------ |
| `AZURE_CREDENTIALS`                | Azure service principal credentials (JSON) for login                |
| `AZURE_VM_SSH_PUBLIC_KEY`          | Public SSH key configured on the Azure VM                           |
| `AZURE_VM_SSH_PRIVATE_KEY`         | Private SSH key used by Ansible to connect to the VM                |
| `CERTBOT_EMAIL`                    | Email address for Let's Encrypt TLS registration                    |
| `AZURE_TF_BACKEND_RESOURCE_GROUP`  | Azure resource group containing the Terraform state storage account |
| `AZURE_TF_BACKEND_STORAGE_ACCOUNT` | Azure storage account for Terraform remote state                    |
| `AZURE_TF_BACKEND_CONTAINER`       | Azure blob container for Terraform remote state                     |
| `AZURE_TF_BACKEND_KEY`             | Blob key/path for the Terraform state file                          |

##### Kubernetes Deployment

| Variable          | Purpose                                                                         |
| :---------------- | :------------------------------------------------------------------------------ |
| `KUBE_CONFIG_B64` | Base64-encoded Kubeconfig file used to authenticate with the Kubernetes cluster |

For local development, most variables have defaults in `docker-compose.yml`.

## Project Structure

```text
.
├── api/
│   └── openapi-v1.yml                 # Shared OpenAPI contract
├── bruno-workspace/                   # Bruno API client collections and environments
├── docs/
│   ├── architecture.md                # High-level architecture
│   ├── database-schema.md             # PostgreSQL schema and persistence model
│   └── api.html                       # Generated human-readable OpenAPI documentation
├── infra/                             # Terraform, Ansible, Helm, and K8s setups
│   ├── ansible/                       # Ansible playbooks for VM setup and config
│   ├── grafana/                       # Grafana dashboards and provisioning
│   ├── helm/                          # Helm charts for Kubernetes deployment
│   ├── k8s/                           # Kubernetes resource manifests and configs
│   ├── prometheus/                    # Prometheus scraping configs
│   └── terraform/                     # Terraform code for Azure VM deployment
├── services/
│   ├── pom.xml                        # Maven parent project
│   ├── thesis-service/                # Main backend and relational data owner
│   ├── scraping-service/              # Fetches chair pages and coordinates extraction
│   ├── vector-search-service/         # Semantic search and thesis embeddings
│   └── genai-service/                 # FastAPI + LangChain extraction service
├── user_interface/
│   └── open-thesis-radar/             # React + TypeScript + Vite frontend
├── scripts/                           # Helper shell and Python scripts
├── docker-compose.yml                 # Local development stack
└── README.md
```

## Architecture Overview

Open Thesis Radar is implemented as a microservice-based system.

| Component             | Technology                       | Responsibility                                                                |
| --------------------- | -------------------------------- | ----------------------------------------------------------------------------- |
| Frontend              | React, TypeScript, Vite          | User interface for browsing, filtering, searching, and viewing thesis details |
| Thesis Service        | Spring Boot, PostgreSQL          | Main API, thesis storage, filters, detail pages, scrape result ingestion      |
| Scraping Service      | Spring Boot                      | Fetches chair/source pages and sends raw content to the GenAI service         |
| GenAI Service         | FastAPI, LangChain               | Extracts structured thesis data from raw HTML/text and generates summaries    |
| Vector Search Service | Spring Boot, Spring AI, pgvector | Creates and queries embeddings for semantic thesis search                     |
| PostgreSQL            | PostgreSQL / pgvector            | Relational thesis data and vector storage                                     |

Main data flow:

1. The frontend queries the Thesis Service for thesis search, filtering, and detail views.
2. The Scraping Service requests source endpoints from the Thesis Service.
3. The Scraping Service downloads chair pages.
4. Raw page content is sent to the GenAI Service.
5. The GenAI Service returns structured thesis proposals.
6. The Scraping Service submits extracted theses to the Thesis Service.
7. The Thesis Service stores the relational data and coordinates vector reindexing.
8. Natural-language search is delegated to the Vector Search Service.

For more details, see [`docs/architecture.md`](docs/architecture.md).

The PostgreSQL schema, relationships, Flyway migrations, and persistence setup are documented in [`docs/database-schema.md`](docs/database-schema.md).

## Monitoring & Observability

The application stack includes a fully configured monitoring and alerting
pipeline to track health, performance, and API metrics:

- **Prometheus:** Scrapes metrics at fixed-second intervals from:
  - Spring Boot Actuator endpoints (`/actuator/prometheus`) on the
    `thesis-service`, `scraping-service`, and `vector-search-service`.
  - FastAPI metrics (`/metrics`) on the `genai-service`.
  - Nginx ingress/frontend connection metrics via `nginx-exporter` (scraping `frontend:9113`).
- **Grafana:** Visualizes collected metrics. It is provisioned automatically
  with pre-configured data sources and dashboards located in `infra/grafana/`.
- **Alerting & Logging:** Grafana-managed Alert Rules and monitoring
  configurations are detailed under the deployment configurations in the `infra/` folder.

## API Contract

The canonical API specification is:

```text
api/openapi-v1.yml
```

Human-readable API documentation generated from that contract is available at [`docs/api.html`](docs/api.html). Open this file in a browser to inspect endpoints, schemas, and examples without reading the raw YAML.

Regenerate the static API documentation after OpenAPI changes with:

```bash
./scripts/generate-api-docs.sh
```

Important endpoint groups:

| Endpoint Group                              | Purpose                               |
| ------------------------------------------- | ------------------------------------- |
| `/api/v1/theses/search`                     | Search and filter theses              |
| `/api/v1/theses/{thesisId}`                 | Get thesis details                    |
| `/api/v1/chairs`                            | List chairs                           |
| `/api/v1/filters`                           | Get available filter options          |
| `/internal/v1/thesis-service/...`           | Internal thesis ingestion/source APIs |
| `/internal/v1/scraping-service/scrape`      | Trigger scraping                      |
| `/internal/v1/genai-service/extract-theses` | Extract thesis data from raw content  |
| `/internal/v1/vector-search-service/...`    | Semantic search and vector indexing   |
| `/health`                                   | Health checks                         |

Generated OpenAPI sources must not be edited manually.

## Local Runtime

Start all services:

```bash
docker compose up --build
```

Stop the stack:

```bash
docker compose down
```

Stop the stack and remove volumes:

```bash
docker compose down -v
```

### Run the Frontend Separately

```bash
cd user_interface/open-thesis-radar
npm install
npm run dev
```

Frontend development server:

```text
http://localhost:5173
```

### Run Java Services Separately

From the Maven parent:

```bash
cd services
mvn clean package
```

Run individual services from their module directories or through your IDE.

### Run the GenAI Service Separately

```bash
cd services/genai-service
uvicorn app:app --host 0.0.0.0 --port 8000
```

## Testing and Quality Checks

Frontend:

```bash
cd user_interface/open-thesis-radar
npm install
npm run lint
npm run build
```

Backend:

```bash
cd services
mvn test
```

Build all backend modules:

```bash
cd services
mvn clean package
```

OpenAPI contract linting:

```bash
npx --yes @redocly/cli lint api/openapi-v1.yml
```

Integration / Smoke Tests (Bruno):

You can run automated integration and smoke tests against local services using the Bruno CLI.
Prerequisites:

```bash
npm install -g @usebruno/cli
```

Run tests:

```bash
./scripts/run-bruno-tests.sh
```

OpenAPI contract validation runs at multiple levels:

- CI lints the canonical contract with Redocly.
- Spring controller tests validate representative real MockMvc requests/responses against `api/openapi-v1.yml` for Thesis, Scraping, and Vector Search services.
- GenAI tests compare FastAPI-generated OpenAPI metadata with the canonical GenAI endpoints and exercise a mocked extraction response.
- The frontend uses generated OpenAPI TypeScript types in `src/api.ts`; CI checks that generated file stays in sync.

Useful local commands:

```bash
cd services
mvn -pl thesis-service,scraping-service,vector-search-service -Dtest=OpenApiContractTest,PublicThesisControllerIntegrationTest,PublicThesisSearchIntegrationTest,InternalThesisControllerIntegrationTest,ScrapeControllerIntegrationTest,VectorSearchControllerTest test

cd services/genai-service
pytest tests/test_contract.py

cd user_interface/open-thesis-radar
npm run check:api
```

Docker Compose validation:

```bash
docker compose config
```

## Deployments & Infrastructure

The project includes two production deployment paths under the `infra/` folder,
automated via GitHub Actions workflows:

1. **Azure VM (Terraform & Ansible):**
   - Infrastructure is provisioned via Terraform and deployed using Ansible playbooks.
   - See [`infra/README.md`](infra/README.md) for local Ansible/Terraform setup and commands.
2. **Kubernetes (Helm):**
   - The application can be deployed using the Helm chart located in `infra/helm/open-thesis-radar`.
   - See [`infra/k8s/README.md`](infra/k8s/README.md) for deployment guidelines and Kubernetes instructions.

## Service Ownership and Responsibilities

Current working areas should be kept transparent through GitHub issues, project board tickets, PR reviews, and weekly updates.

| Area                  | Owner(s)                                 | Responsibility                                                             |
| --------------------- | ---------------------------------------- | -------------------------------------------------------------------------- |
| Frontend              | `<Zé Afonso / ZeAfonso21>`               | React UI, thesis search page, thesis detail page, frontend API integration |
| Thesis Service        | `<Tomás Soares / TomasMSoares>`          | Main backend API, relational thesis data, filters, scrape result ingestion |
| Scraping Service      | `<Tomás Soares / TomasMSoares>`          | Chair/source crawling, scrape scheduling, GenAI coordination               |
| GenAI Service         | `<Martin Steinmayer / MartinSteinmayer>` | Thesis extraction, summary generation, model/provider configuration        |
| Vector Search Service | `<Martin Steinmayer / MartinSteinmayer>` | Embeddings, semantic retrieval, vector reindexing                          |
| DevOps / Integration  | `<All 3>`                                | Docker Compose, environment setup, CI/CD, deployment documentation         |
| API Contract          | `<All 3>`                                | OpenAPI specification and generated service interfaces                     |

## Planning and Backlog

The product backlog should be maintained through issues.

Initial user stories include:

1. As a student, I want to browse all currently available open theses in one platform.
2. As a student, I want to filter thesis proposals by degree type, research area, chair, and keywords.
3. As a student, I want to open a thesis detail page.
4. As a student, I want to describe thesis interests in natural language.
5. As a student, I want to see AI-generated thesis overviews.
6. As a student, I want to see why a thesis matches my query.

Future work and next steps should be tracked as backlog tickets.

## Notes for Contributors

- Keep generated OpenAPI sources out of manual edits.
- Keep service boundaries clear:
  - GenAI extracts only.
  - Thesis Service owns relational data.
  - Vector Search Service owns embeddings and semantic retrieval.
  - Scraping Service coordinates ingestion.
- Prefer configuration through environment variables.
- Document important architectural decisions in `docs/`.

## Weekly Documentation

Weekly meeting notes live in [`docs/weekly-updates/`](docs/weekly-updates/).
For each meeting, copy [`docs/weekly-updates/template.md`](docs/weekly-updates/template.md),
name the new file with the ISO meeting date (`YYYY-MM-DD.md`),
and include status updates plus related issues/PRs where possible.
