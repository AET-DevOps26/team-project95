# Bruno Workspace

This workspace contains Bruno collections for the Thesis Discovery Platform.

## Usage

1. Start the local stack from the repository root:

   ```bash
   docker compose up --build
   ```

2. Open `bruno-workspace/` in Bruno.
3. Select the `Local` environment.
4. Run requests from the `Thesis Discovery API` collection.

## Collection

- `collections/thesis-discovery-api/` — requests based on `../api/openapi-v1.yml`

## Local service variables

Defaults live in `collections/thesis-discovery-api/environments/Local.yml`:

- `thesisServiceUrl = http://localhost:8080`
- `scrapingServiceUrl = http://localhost:8081`
- `vectorSearchServiceUrl = http://localhost:8082`
- `genaiServiceUrl = http://localhost:8000`
- `thesisId = 1`
- `sourceEndpointId = 10`
- `chairId = 3`

If your local database uses different IDs, update the variables in Bruno or pass shell overrides to the test script.

## CLI

Install the Bruno CLI if you want to run the collection from the terminal:

```bash
npm install -g @usebruno/cli
```

Then run the full collection from the repository root:

```bash
./scripts/run-bruno-tests.sh
```

The script uses the `Local` Bruno environment by default. It does **not** duplicate API defaults; values come from `Local.yml` unless you explicitly override them.

Optional Bruno variable overrides:

| Shell variable | Bruno variable |
| --- | --- |
| `THESIS_SERVICE_URL` | `thesisServiceUrl` |
| `SCRAPING_SERVICE_URL` | `scrapingServiceUrl` |
| `VECTOR_SEARCH_SERVICE_URL` | `vectorSearchServiceUrl` |
| `GENAI_SERVICE_URL` | `genaiServiceUrl` |
| `THESIS_ID` | `thesisId` |
| `SOURCE_ENDPOINT_ID` | `sourceEndpointId` |
| `CHAIR_ID` | `chairId` |

Optional script configuration:

| Shell variable | Purpose | Default |
| --- | --- | --- |
| `BRUNO_ENV` | Bruno environment name | `Local` |
| `BRUNO_COLLECTION_DIR` | Collection directory | `bruno-workspace/collections/thesis-discovery-api` |
| `BRU_BIN` | Bruno CLI executable | `bru` |

Example:

```bash
THESIS_ID=42 SOURCE_ENDPOINT_ID=7 ./scripts/run-bruno-tests.sh
```

Pass regular `bru run` arguments after the script name, for example:

```bash
./scripts/run-bruno-tests.sh "Health/Thesis Service Health.yml" --output /tmp/bru-health.json --format json
```

If your `bru` executable is not on `PATH`, set `BRU_BIN`:

```bash
BRU_BIN=/path/to/bru ./scripts/run-bruno-tests.sh
```

The local services must be running before executing the collection.

## CI smoke tests

The main CI workflow starts the Docker Compose stack, waits for the required `/health` endpoints, then runs a small Bruno smoke subset:

```bash
./scripts/run-bruno-tests.sh \
  "Health/Thesis Service Health.yml" \
  "Health/Scraping Service Health.yml" \
  "Frontend API/List Theses.yml" \
  "Frontend API/List Chairs.yml" \
  "Frontend API/Get Available Filters.yml" \
  --tests-only
```

This smoke job covers health plus stable thesis-service frontend APIs and intentionally avoids AI-provider-dependent GenAI/vector requests, so it does not require hardcoded secrets.
