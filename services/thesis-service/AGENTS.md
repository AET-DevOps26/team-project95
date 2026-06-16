# Antigravity Instructions - thesis-service

This is the central thesis management microservice.

## ⚠️ CRITICAL DEPENDENCY & VERSION RULES (DO NOT BYPASS)
- **Java Version**: Must remain at **Java >= 25** (defined in parent [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml)).
- **Spring Boot Version**: Must remain at **Spring Boot 4.x.x** (defined in parent [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml)).
- **Do NOT downgrade** versions for quick build/compilation fixes under any circumstances.

## 🛠️ Service Architecture & Guidelines
- **Framework**: Spring Boot 4 Web, Spring Data JPA, Spring Boot Validation.
- **Database**: PostgreSQL (with Flyway migrations under `src/main/resources/db/migration`). Always create new incremental migration files for schema changes instead of altering existing ones.
- **API Specs & DTOs**: Generated via `openapi-generator-maven-plugin` from [openapi-v1.yml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/api/openapi-v1.yml).
  - When modifying APIs, edit the YAML spec first.
  - Run `mvn clean compile` to regenerate interfaces.
  - Implement/extend the generated API interfaces in your controller implementations. Do not edit generated files under `target/generated-sources/`.
- **Testing**: Preconfigured integration testing using JUnit 5, Spring Boot Testcontainers, and PostgreSQL.
