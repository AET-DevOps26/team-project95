# Antigravity Instructions - Services Folder

This directory contains the core backend microservices for Project 95.

## ⚠️ CRITICAL DEPENDENCY & VERSION RULES (DO NOT BYPASS)

To prevent breaking changes, API incompatibilities, and compile regressions, the following version constraints **must be preserved at all times**. Under no circumstances should you downgrade or deprecate these versions for quick compilation/dependency resolution fixes:

- **Java Version**: Must remain at **Java >= 25** (configured in [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml)).
- **Spring Boot Version**: Must remain at **Spring Boot 4.x.x** (configured in [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml)).
- **Python Version**: Must remain at **Python >= 3.11** (configured in [genai-service/pyproject.toml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/genai-service/pyproject.toml)).
- **FastAPI Version**: Must remain at **FastAPI >= 0.115.0** (configured in [genai-service/pyproject.toml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/genai-service/pyproject.toml)).

If you hit local build or compile errors, resolve the root issue (e.g., updating vendor libraries, migrating deprecated API endpoints, or adjusting Maven/Ruff compiler flags) instead of downgrading core dependencies.

---

## 🏗️ Backend Services Overview

1. **[thesis-service](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service)**: Spring Boot 4 database-driven service for thesis management (PostgreSQL & Flyway).
2. **[vector-search-service](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/vector-search-service)**: Spring Boot 4 + Spring AI (pgvector) service for semantic searches.
3. **[scraping-service](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/scraping-service)**: Spring Boot 4 utility service for scraping external websites.
4. **[genai-service](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/genai-service)**: Python FastAPI + LangChain service for AI generation and insights.

---

## 🛠️ Global Development Standards

### Java / Spring Boot Microservices

- **Build Tool**: Maven. Manage dependency versions in the parent [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml).
- **API Specs & DTOs**: Generated via `openapi-generator-maven-plugin` from [openapi-v1.yml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/api/openapi-v1.yml). Always edit the YAML spec first, then run `mvn clean compile` to regenerate classes.
- **Testing**: Preconfigured integration testing using JUnit 5 and Testcontainers (PostgreSQL integration testing is preconfigured).

### Python Microservice (`genai-service`)

- **Web Framework**: FastAPI with Pydantic for validation.
- **AI Integrations**: LangChain, LangChain-OpenAI, LangChain-Ollama.
- **Formatting & Linting**: Ruff (target version py311). Check code using `ruff check` and `ruff format`.
- **Testing**: Managed via `pytest` and `httpx`.
