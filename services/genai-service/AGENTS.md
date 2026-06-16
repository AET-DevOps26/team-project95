# Antigravity Instructions - genai-service

This is the Python-based microservice that coordinates AI generation and insight extraction.

## ⚠️ CRITICAL DEPENDENCY & VERSION RULES (DO NOT BYPASS)

- **Python Version**: Must remain at **Python >= 3.11** (configured in [pyproject.toml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/genai-service/pyproject.toml)).
- **FastAPI Version**: Must remain at **FastAPI >= 0.115.0** (configured in [pyproject.toml](file:///home/tomas/TUM/Master/1FS/DevOps/team-project95/services/genai-service/pyproject.toml)).
- **Do NOT downgrade** versions for quick build/dependency fixes under any circumstances.

## 🛠️ Service Architecture & Guidelines

- **Framework**: FastAPI (with Uvicorn as web server, and Pydantic v2 for data validation and schema definition).
- **AI Integrations**: LangChain Core/Community (`>=0.3.0`), LangChain-OpenAI, LangChain-Ollama.
- **Code Style & Quality**: Ruff is configured for checking and formatting.
  - Target version: `py311`.
  - Line length limit: 100.
  - Run linting via `ruff check` and formatting via `ruff format` to maintain code cleanliness.
- **Testing**: Preconfigured with `pytest` and `httpx` for integration testing.
