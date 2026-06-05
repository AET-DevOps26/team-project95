import pytest
from fastapi.testclient import TestClient

from app import (
    MAX_INPUT_CHARS,
    DraftAdvisorInput,
    DraftThesisProposalInput,
    ExtractionError,
    GenAIExtractionRequest,
    app,
    build_prompt,
    normalize_thesis,
    preprocess_input,
)


def test_health_endpoint_returns_up_status():
    client = TestClient(app)

    response = client.get("/internal/v1/health")

    assert response.status_code == 200
    assert response.json() == {"status": "UP", "service": "genai-service"}


def test_preprocess_input_strips_html_noise_and_collapses_whitespace():
    raw_html = """
    <html>
      <head><style>.hidden { display: none; }</style></head>
      <body>
        <script>alert('ignore me')</script>
        <h1>Open Thesis</h1>
        <p>  Semantic   Search   for   Theses  </p>
      </body>
    </html>
    """

    cleaned = preprocess_input(raw_html, extracted_plain_text=None)

    assert "alert" not in cleaned
    assert "display" not in cleaned
    assert cleaned == "Open Thesis\nSemantic Search for Theses"


def test_preprocess_input_prefers_extracted_plain_text():
    cleaned = preprocess_input(
        raw_html="<h1>Ignored HTML</h1>",
        extracted_plain_text="\n  Provided   text\n\nwith spacing  ",
    )

    assert cleaned == "Provided text\nwith spacing"


def test_preprocess_input_truncates_long_content():
    cleaned = preprocess_input(raw_html="x" * (MAX_INPUT_CHARS + 100), extracted_plain_text=None)

    assert len(cleaned) == MAX_INPUT_CHARS


def test_build_prompt_contains_metadata_and_cleaned_content():
    request = GenAIExtractionRequest(
        sourceEndpointId=10,
        chairId=3,
        chairName="Chair of Software Engineering",
        sourceUrl="https://example.com/theses",
        rawHtml="<p>ignored here</p>",
    )

    prompt = build_prompt(request, "Cleaned thesis content")

    assert "sourceEndpointId: 10" in prompt
    assert "chairId: 3" in prompt
    assert "chairName: Chair of Software Engineering" in prompt
    assert "sourceUrl: https://example.com/theses" in prompt
    assert "Cleaned thesis content" in prompt


def test_normalize_thesis_trims_fields_and_defaults_source_url():
    draft = DraftThesisProposalInput(
        title="  Semantic Search for Thesis Discovery  ",
        degreeType=" master ",
        originalDescription="  Build a semantic search system.  ",
        aiOverview="  A concise overview.  ",
        researchArea="  Information Retrieval  ",
        sourceUrl=None,
        status=None,
        advisors=[
            DraftAdvisorInput(
                name="  Max Mustermann  ",
                email="  max@example.com  ",
                profileUrl="  https://example.com/max  ",
            )
        ],
        tags=[" search ", "", " ai "],
    )

    normalized = normalize_thesis(draft, source_url="https://example.com/default")

    assert normalized.title == "Semantic Search for Thesis Discovery"
    assert normalized.degreeType == "MASTER"
    assert normalized.originalDescription == "Build a semantic search system."
    assert normalized.aiOverview == "A concise overview."
    assert normalized.researchArea == "Information Retrieval"
    assert normalized.sourceUrl == "https://example.com/default"
    assert normalized.status == "OPEN"
    assert normalized.tags == ["search", "ai"]
    assert normalized.advisors[0].name == "Max Mustermann"
    assert normalized.advisors[0].email == "max@example.com"
    assert normalized.advisors[0].profileUrl == "https://example.com/max"


def test_normalize_thesis_drops_unknown_degree_type():
    draft = DraftThesisProposalInput(
        title="Topic",
        degreeType="Diploma",
        sourceUrl="https://example.com/topic",
    )

    normalized = normalize_thesis(draft, source_url="https://example.com/default")

    assert normalized.degreeType is None


def test_normalize_thesis_rejects_missing_title():
    draft = DraftThesisProposalInput(title="  ")

    with pytest.raises(ExtractionError):
        normalize_thesis(draft, source_url="https://example.com/default")
