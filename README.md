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

Important environment variables include:

| Variable                            | Purpose                                        |
| ----------------------------------- | ---------------------------------------------- |
| `OPENAI_API_KEY`                    | OpenAI API key used by the GenAI service       |
| `GENAI_MODEL_PROVIDER`              | `openai` or `ollama`                           |
| `GENAI_MODEL_NAME`                  | Model used for thesis extraction and summaries |
| `OLLAMA_BASE_URL`                   | Ollama endpoint when using local models        |
| `THESIS_DB_NAME`                    | Thesis PostgreSQL database name                |
| `THESIS_DB_USER`                    | Thesis PostgreSQL user                         |
| `THESIS_DB_PASSWORD`                | Thesis PostgreSQL password                     |
| `VECTOR_DB_NAME`                    | Vector PostgreSQL database name                |
| `VECTOR_DB_USER`                    | Vector PostgreSQL user                         |
| `VECTOR_DB_PASSWORD`                | Vector PostgreSQL password                     |
| `AZURE_OPENAI_ENDPOINT`             | Embedding provider endpoint for vector search  |
| `AZURE_OPENAI_API_KEY`              | API key for embeddings                         |
| `AZURE_OPENAI_EMBEDDING_DEPLOYMENT` | Embedding deployment name                      |
| `PGVECTOR_DIMENSIONS`               | Embedding vector dimensions                    |

For local development, most variables have defaults in `docker-compose.yml`.

## Project Structure

```text
.
├── api/
│   └── openapi-v1.yml                 # Shared OpenAPI contract
├── docs/
│   ├── architecture.md                # High-level architecture
├── services/
│   ├── pom.xml                        # Maven parent project
│   ├── thesis-service/                # Main backend and relational data owner
│   ├── scraping-service/              # Fetches chair pages and coordinates extraction
│   ├── vector-search-service/         # Semantic search and thesis embeddings
│   └── genai-service/                 # FastAPI + LangChain extraction service
├── user_interface/
│   └── open-thesis-radar/             # React + TypeScript + Vite frontend
├── scripts/
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

## API Contract

The canonical API specification is:

```text
api/openapi-v1.yml
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
| `/internal/v1/health`                       | Health checks                         |

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

Docker Compose validation:

```bash
docker compose config
```

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

# Weekly Documentation

Weekly meeting notes live in [`docs/weekly-updates/`](docs/weekly-updates/). For each meeting, copy [`docs/weekly-updates/template.md`](docs/weekly-updates/template.md), name the new file with the ISO meeting date (`YYYY-MM-DD.md`), and include status updates plus related issues/PRs where possible.
