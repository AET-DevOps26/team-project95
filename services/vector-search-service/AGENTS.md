# Antigravity Instructions - vector-search-service

This is the vector search microservice responsible for semantic and embedding-based search for theses.

## ⚠️ CRITICAL DEPENDENCY & VERSION RULES (DO NOT BYPASS)
- **Java Version**: Must remain at **Java >= 25** (defined in parent [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml)).
- **Spring Boot Version**: Must remain at **Spring Boot 4.x.x** (defined in parent [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml)).
- **Do NOT downgrade** versions for quick build/compilation fixes under any circumstances.

## 🛠️ Service Architecture & Guidelines
- **Framework**: Spring Boot 4 Web, Spring AI (`spring-ai-starter-model-openai`, `spring-ai-starter-vector-store-pgvector`).
- **API Specs & DTOs**: Generated via `openapi-generator-maven-plugin` from [openapi-v1.yml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/api/openapi-v1.yml).
  - When modifying APIs, edit the YAML spec first.
  - Run `mvn clean compile` to regenerate interfaces.
  - Implement/extend the generated API interfaces in your controller implementations. Do not edit generated files under `target/generated-sources/`.
- **Testing**: Preconfigured integration testing using JUnit 5.
