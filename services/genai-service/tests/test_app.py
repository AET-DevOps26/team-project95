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

    response = client.get("/health")

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


def test_preprocess_input_prefers_main_content_and_removes_navigation():
    raw_html = """
    <html>
      <body>
        <header>University Header</header>
        <nav>Teaching\nOld Lectures\nMenu</nav>
        <main>
          <div id="content">
            <h1>Open Thesis Topics</h1>
            <p>Build a thesis discovery system.</p>
          </div>
        </main>
        <footer>Legal notice</footer>
      </body>
    </html>
    """

    cleaned = preprocess_input(raw_html, extracted_plain_text=None)

    assert "University Header" not in cleaned
    assert "Old Lectures" not in cleaned
    assert "Legal notice" not in cleaned
    assert cleaned == "Open Thesis Topics\nBuild a thesis discovery system."


def test_preprocess_input_prefers_source_url_fragment_when_present():
    raw_html = """
    <main>
      <section id="old-topic"><h2>Archived Topic</h2></section>
      <section id="c55667"><h2>Open Fragment Topic</h2><p>Current thesis details.</p></section>
    </main>
    """

    cleaned = preprocess_input(
        raw_html,
        extracted_plain_text=None,
        source_url="https://example.com/theses/#c55667",
    )

    assert "Archived Topic" not in cleaned
    assert cleaned == "Open Fragment Topic\nCurrent thesis details."


def test_preprocess_input_deduplicates_repeated_short_lines():
    raw_html = """
    <main>
      <p>MA</p><p>MA</p><p>BA</p><p>BA</p>
      <p>Unique thesis description with enough detail to keep.</p>
    </main>
    """

    cleaned = preprocess_input(raw_html, extracted_plain_text=None)

    assert cleaned.splitlines() == [
        "MA",
        "BA",
        "Unique thesis description with enough detail to keep.",
    ]


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
    assert "Known research areas to prefer exactly" in prompt
    assert "Cleaned page content" in prompt


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
    )

    normalized = normalize_thesis(
        draft,
        source_url="https://example.com/default",
        known_research_areas=["Information Retrieval"],
    )

    assert normalized.title == "Semantic Search for Thesis Discovery"
    assert normalized.degreeType == "MASTER"
    assert normalized.originalDescription == "Build a semantic search system."
    assert normalized.aiOverview == "A concise overview."
    assert normalized.researchArea == "Information Retrieval"
    assert normalized.sourceUrl == "https://example.com/default"
    assert normalized.status == "OPEN"
    assert normalized.advisors[0].name == "Max Mustermann"
    assert normalized.advisors[0].email == "max@example.com"
    assert normalized.advisors[0].profileUrl == "https://example.com/max"


def test_normalize_thesis_drops_unknown_degree_type():
    draft = DraftThesisProposalInput(
        title="Topic",
        degreeType="Diploma",
        sourceUrl="https://example.com/topic",
    )

    normalized = normalize_thesis(
        draft, source_url="https://example.com/default", known_research_areas=[]
    )

    assert normalized.degreeType is None


def test_normalize_thesis_rejects_missing_title():
    draft = DraftThesisProposalInput(title="  ")

    with pytest.raises(ExtractionError):
        normalize_thesis(draft, source_url="https://example.com/default", known_research_areas=[])


def test_normalize_thesis_snaps_research_area_to_known_taxonomy():
    draft = DraftThesisProposalInput(
        title="Topic",
        researchArea="machine-learning",
        sourceUrl="https://example.com/topic",
    )

    normalized = normalize_thesis(
        draft,
        source_url="https://example.com/default",
        known_research_areas=["Machine Learning", "Robotics"],
    )

    assert normalized.researchArea == "Machine Learning"


def test_normalize_thesis_does_not_snap_vague_short_area_to_unrelated_taxonomy():
    draft = DraftThesisProposalInput(
        title="Topic",
        researchArea="data",
        sourceUrl="https://example.com/topic",
    )

    normalized = normalize_thesis(
        draft,
        source_url="https://example.com/default",
        known_research_areas=["Database Systems", "Robotics"],
    )

    assert normalized.researchArea == "data"


def test_normalize_thesis_drops_noisy_unknown_research_area_when_taxonomy_exists():
    draft = DraftThesisProposalInput(
        title="Topic",
        researchArea="Agile Amphibious Locomotion Project with Miniature Legged Robot",
        sourceUrl="https://example.com/topic",
    )

    normalized = normalize_thesis(
        draft,
        source_url="https://example.com/default",
        known_research_areas=["Robotics", "Artificial Intelligence"],
    )

    assert normalized.researchArea is None
