# genai-service

Minimal FastAPI + LangChain service for thesis extraction.

## Endpoints
- `GET /internal/v1/health`
- `POST /internal/v1/genai-service/extract-theses`

## Environment variables
- `GENAI_MODEL_PROVIDER=openai|ollama`
- `GENAI_MODEL_NAME=...`
- `OPENAI_API_KEY=...`
- `OLLAMA_BASE_URL=...`

## Run locally
```bash
uvicorn app:app --host 0.0.0.0 --port 8000
```

## Example request
```bash
curl -X POST http://localhost:8000/internal/v1/genai-service/extract-theses \
  -H "Content-Type: application/json" \
  -d '{
    "sourceEndpointId": 10,
    "chairId": 3,
    "chairName": "Chair of Software Engineering",
    "sourceUrl": "https://example.com/theses",
    "rawHtml": "<html><body><h1>Open Thesis Topics</h1><h2>Semantic Search for Thesis Discovery</h2><p>We offer a master thesis on semantic search using LLMs. Contact Max Mustermann at max@example.com.</p></body></html>",
    "extractedPlainText": null
  }'
```
