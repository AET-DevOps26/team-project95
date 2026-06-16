# Antigravity Instructions - scraping-service

This is the web scraping microservice responsible for fetching metadata and thesis descriptions from external academic sites.

## ⚠️ CRITICAL DEPENDENCY & VERSION RULES (DO NOT BYPASS)
- **Java Version**: Must remain at **Java >= 25** (defined in parent [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml)).
- **Spring Boot Version**: Must remain at **Spring Boot 4.x.x** (defined in parent [pom.xml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/pom.xml)).
- **Do NOT downgrade** versions for quick build/compilation fixes under any circumstances.

## 🛠️ Service Architecture & Guidelines
- **Framework**: Spring Boot 4 Web, RestClient, Validation.
- **API Specs & DTOs**: Generated via `openapi-generator-maven-plugin` from [openapi-v1.yml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/api/openapi-v1.yml).
  - When modifying APIs, edit the YAML spec first.
  - Run `mvn clean compile` to regenerate interfaces.
  - Implement/extend the generated API interfaces in your controller implementations. Do not edit generated files under `target/generated-sources/`.
- **Testing**: Preconfigured testing using JUnit 5 and Spring Boot Test utilities.
