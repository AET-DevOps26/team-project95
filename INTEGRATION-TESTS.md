# Project 95 Thesis System — Integration Testing Guide

This document presents a comprehensive audit of the integration tests implemented within [services/thesis-service](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service) and [services/scraping-service](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/scraping-service), followed by a prioritized list of high-impact integration tests recommended to improve test coverage for core functionality.

---

## 🔍 Core Technology & Testing Stack

Both microservices are built on **Spring Boot 4** and **Java 25**. Testing is executed via **JUnit 5**, using:
- **Spring Boot Test** (`@SpringBootTest`) to bootstrap the context.
- **MockMvc** for testing REST API controllers without starting a live web server (lightweight integration).
- **Testcontainers (PostgreSQL)** for running relational database tests inside a real PostgreSQL environment on-demand.
- **MockRestServiceServer** for mock-asserting outbound HTTP client REST calls.
- **In-Memory H2 Database** as a fast fallback profile (`test`) when postgres containers are not needed.

---

## 📊 Currently Implemented Tests

Here is a summary of the test coverage currently residing in the repository.

### 1. `thesis-service` Integration Tests (5 Classes)

These tests boot up the full or sliced Spring context to verify transactional logic, DB configurations, and registry configurations.

*   [FlywayMigrationIntegrationTest.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/test/java/com/project95/thesis/thesis/FlywayMigrationIntegrationTest.java)
    *   **Scope**: Database initialization.
    *   **Focus**: Uses Testcontainers PostgreSQL to check that Flyway migrations apply cleanly, Hibernate validation is satisfied, and entities can be written to/read from the schema.
*   [ThesisIngestionIntegrationTest.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/test/java/com/project95/thesis/thesis/service/ThesisIngestionIntegrationTest.java)
    *   **Scope**: Database-driven business logic.
    *   **Focus**: Verifies relational updates in [ThesisManagementService.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/main/java/com/project95/thesis/thesis/service/ThesisManagementService.java). It asserts transactional rollback upon ingestion failure, duplicate handling, and deletion of orphaned thesis proposals.
*   [FilterControllerIntegrationTest.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/test/java/com/project95/thesis/thesis/controller/FilterControllerIntegrationTest.java)
    *   **Scope**: Public filters API endpoint.
    *   **Focus**: Boots up MockMvc (under H2) to query `/api/v1/chairs` and `/api/v1/filters`. Asserts that metadata (degrees, areas, tags, chairs) is aggregated correctly.
*   [InternalThesisControllerIntegrationTest.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/test/java/com/project95/thesis/thesis/controller/InternalThesisControllerIntegrationTest.java)
    *   **Scope**: Internal API endpoint subset.
    *   **Focus**: Boots up MockMvc (under H2) to verify `GET /internal/v1/thesis-service/source-endpoints`. Asserts that retired endpoints are excluded from the scraping schedule.
*   [SourceRegistryIntegrationTest.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/test/java/com/project95/thesis/thesis/sourceconfig/SourceRegistryIntegrationTest.java)
    *   **Scope**: Source configuration runner.
    *   **Focus**: Verifies that the JSON-based source registry syncs correctly with the database. Tests key behaviors like idempotency, updating existing sources, and auto-retiring obsolete ones.

### 2. `scraping-service` Integration Tests (0 Classes)

The web scraping service **does not currently have any Spring Boot integration tests**.
*   It only contains a single unit/slice test: [ScrapeCoordinationServiceTest.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/scraping-service/src/test/java/com/project95/thesis/scraping/service/ScrapeCoordinationServiceTest.java).
*   While this unit test mocks the `RestClient` to ensure the scrape loop communicates correctly under standard success conditions, it does **not** load the Spring context, test controller mappings, verify serialization behavior, or test async exception handlers.

---

## 📈 Recommended New Integration Tests (Ranked by Priority)

To avoid useless bloat, we focus strictly on the core operations of the application: **Search Queries**, **Ingestion Pipelines**, **External Synchronization**, and **Async Automation**.

### 🏆 Priority 1: `thesis-service` Public Search & Hybrid Queries
*   **Target**: [PublicThesisController.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/main/java/com/project95/thesis/thesis/controller/PublicThesisController.java) and [ThesisSearchService.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/main/java/com/project95/thesis/thesis/service/ThesisSearchService.java)
*   **Why it's high priority**: This is the primary feature used by the user interface/end-users. The search logic merges standard SQL where-clauses (JPA Specifications) with a REST call to `vector-search-service` for natural language searches.
*   **What to test**:
    1.  **Relational Filtering**: Search theses by tags, research area, status, degree type, and chair.
    2.  **Hybrid Search Flow**: Mock the REST client response for `/internal/v1/vector-search-service/search` (returning a list of matched thesis IDs and scores). Verify that the database queries those specific IDs, re-ranks them by their semantic score, and returns the paginated response.
    3.  **Resilience / Fallback**: Simulate a `vector-search-service` failure (e.g., HTTP 500 or connection timeout) during search. Verify that the search gracefully falls back to relational-only matching instead of throwing an HTTP 500.

### 🥈 Priority 2: `scraping-service` Asynchronous Scraping Trigger
*   **Target**: [ScrapeController.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/scraping-service/src/main/java/com/project95/thesis/scraping/controller/ScrapeController.java) & [ScrapeCoordinationService.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/scraping-service/src/main/java/com/project95/thesis/scraping/service/ScrapeCoordinationService.java)
*   **Why it's high priority**: The scraping trigger is asynchronous (using a `TaskExecutor` thread pool) and immediately returns `202 ACCEPTED` to prevent blocking the caller. Since there are currently no integration tests for this service, REST endpoint behavior and thread dispatch are completely unvalidated.
*   **What to test**:
    1.  **Asynchronous Response**: POSTing to `/internal/v1/scraping-service/scrape` immediately returns `202 ACCEPTED` with a JSON body indicating the cycle started.
    2.  **Mocked E2E Execution**: Use `@SpringBootTest(webEnvironment = WebEnvironment.MOCK)` and verify (e.g. using `MockRestServiceServer` or thread-blocking waits like `Awaitility`) that the async runner executes and visits all mocked API dependencies:
        *   GET endpoints from `thesis-service`.
        *   GET HTML from the target academic site.
        *   POST raw HTML extraction to `genai-service`.
        *   PUT thesis proposals replacement to `thesis-service`.
        *   POST run log back to `thesis-service`.
    3.  **Error Propagation**: If a network failure occurs (e.g. target website is down), verify that the async task still completes gracefully and fires a `FAILED` scrape run log payload to the `thesis-service`.

### 🥉 Priority 3: `thesis-service` Ingestion Pipeline & Vector Sync Resilience
*   **Target**: `PUT /internal/v1/thesis-service/source-endpoints/{sourceEndpointId}/theses` via [InternalThesisController.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/main/java/com/project95/thesis/thesis/controller/InternalThesisController.java) & [ThesisCoordinationService.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/main/java/com/project95/thesis/thesis/service/ThesisCoordinationService.java)
*   **Why it's high priority**: When new theses are scraped, the application saves them to the relational database and syncs them to the vector search service. The design dictates that if vector search synchronization fails, the relational database changes must **still commit** (non-blocking resilience).
*   **What to test**:
    1.  **Successful Flow**: Verify putting proposals deletes existing records, inserts new ones, pushes documents to the mocked `/internal/v1/vector-search-service/.../index` endpoint, and returns a success response.
    2.  **Partial Failure (Vector Service Down)**: Mock the vector service endpoint to throw an exception or return a 5xx error. Assert that the database update **still commits successfully** (the theses remain in the database), and the API response contains the vector sync warning/error message.
    3.  **Invalid Inputs (Controller level)**: Send malformed data (like missing required titles) and check that validation constraints reject it with `400 Bad Request` prior to execution.

### 🏅 Priority 4: `thesis-service` Scrape Run Logging & Schema Validation
*   **Target**: `POST /internal/v1/thesis-service/scrape-runs` via [InternalThesisController.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/main/java/com/project95/thesis/thesis/controller/InternalThesisController.java) & [ScrapeRunService.java](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/src/main/java/com/project95/thesis/thesis/service/ScrapeRunService.java)
*   **Why it's high priority**: This endpoint logs history and statistics (success/failure status, error messages, candidates found, HTML snapshots). It is untested at the controller, validation, and database level together.
*   **What to test**:
    1.  **Persistence Integrity**: Verify posting a log saves it, updates the parent `SourceEndpoint`'s `lastScrapedAt` timestamp, and stores long raw HTML snapshots safely (testing DB column mapping size constraints).
    2.  **Validation Rule Enforcement**: Verify that missing required values (e.g. `status` or `startedAt`) trigger a `400 Bad Request` validation error.
