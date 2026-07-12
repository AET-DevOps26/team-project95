from pathlib import Path

import yaml
from fastapi.testclient import TestClient

import app as genai_app
from app import GenAIExtractionResponse, ThesisProposalInput, app


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
CANONICAL_OPENAPI = REPOSITORY_ROOT / "api" / "openapi-v1.yml"
GENAI_EXTRACT_PATH = "/internal/v1/genai-service/extract-theses"


def load_canonical_openapi() -> dict:
    return yaml.safe_load(CANONICAL_OPENAPI.read_text(encoding="utf-8"))


def json_schema_ref(operation: dict, status_code: str) -> str:
    return operation["responses"][status_code]["content"]["application/json"]["schema"]["$ref"]


def request_body_ref(operation: dict) -> str:
    return operation["requestBody"]["content"]["application/json"]["schema"]["$ref"]


def test_fastapi_openapi_matches_canonical_genai_contract():
    canonical = load_canonical_openapi()
    generated = app.openapi()

    canonical_operation = canonical["paths"][GENAI_EXTRACT_PATH]["post"]
    generated_operation = generated["paths"][GENAI_EXTRACT_PATH]["post"]

    assert generated_operation["operationId"] == canonical_operation["operationId"]
    assert request_body_ref(generated_operation) == request_body_ref(canonical_operation)
    assert json_schema_ref(generated_operation, "200") == json_schema_ref(
        canonical_operation, "200"
    )
    assert json_schema_ref(generated_operation, "422") == json_schema_ref(
        canonical_operation, "422"
    )

    for schema_name in [
        "GenAIExtractionRequest",
        "GenAIExtractionResponse",
        "ThesisProposalInput",
        "AdvisorInput",
        "ErrorResponse",
    ]:
        assert schema_name in generated["components"]["schemas"]
        assert schema_name in canonical["components"]["schemas"]


def test_extract_theses_response_matches_documented_shape(monkeypatch):
    client = TestClient(app)

    def fake_run_extraction(_request):
        return GenAIExtractionResponse(
            theses=[
                ThesisProposalInput(
                    title="Semantic Search Thesis",
                    sourceUrl="https://example.com/theses/semantic-search",
                    degreeType="MASTER",
                    status="OPEN",
                    tags=["Semantic Search"],
                )
            ],
            extractionNotes="Mocked extraction for contract validation.",
        )

    monkeypatch.setattr(genai_app, "run_extraction", fake_run_extraction)

    response = client.post(
        GENAI_EXTRACT_PATH,
        json={
            "sourceEndpointId": 7,
            "chairId": 3,
            "chairName": "AI Chair",
            "sourceUrl": "https://example.com/theses",
            "rawHtml": "<main><h1>Semantic Search Thesis</h1></main>",
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "theses": [
            {
                "title": "Semantic Search Thesis",
                "degreeType": "MASTER",
                "originalDescription": None,
                "aiOverview": None,
                "researchArea": None,
                "sourceUrl": "https://example.com/theses/semantic-search",
                "status": "OPEN",
                "advisors": [],
                "tags": ["Semantic Search"],
            }
        ],
        "extractionNotes": "Mocked extraction for contract validation.",
    }
