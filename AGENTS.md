# Project 95 Thesis System - Agent Guidelines

Welcome to the Project 95 Thesis System repository. This file serves as the main entry point for agent instructions.

## 📁 Repository Structure Overview

- **[services/](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services)**: Contains all backend microservices (Spring Boot & Python GenAI).
- **[user_interface/](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/user_interface)**: Frontend application/interface files.
- **[api/](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/api)**: OpenAPI specifications.
- **[infra/](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/infra)**: Infrastructure and deployment configurations.

---

## 🧭 Navigating Instructions

To ensure modularity and context-specific behavior, agents must follow the instructions located within the subdirectory instruction files:

1. **Backend / Microservices**: For any changes, dependency modifications, or development inside `services/`, you **must** review the guidelines in **[services/AGENTS.md](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/AGENTS.md)**.
2. **Individual Services**: Each microservice has its own specific `AGENTS.md` detailing implementation and framework rules:
   - [services/thesis-service/AGENTS.md](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/thesis-service/AGENTS.md)
   - [services/vector-search-service/AGENTS.md](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/vector-search-service/AGENTS.md)
   - [services/scraping-service/AGENTS.md](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/scraping-service/AGENTS.md)
   - [services/genai-service/AGENTS.md](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/genai-service/AGENTS.md)

---

## ⚠️ Core Workspace Rules

- **Modularity**: Keep backend logic isolated to the respective microservice. Do not introduce tight coupling between services.
- **Containerization**: Use the root-level [docker-compose.yml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/docker-compose.yml) and [docker-compose.prod.yml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/docker-compose.prod.yml) to spin up the local environment.
