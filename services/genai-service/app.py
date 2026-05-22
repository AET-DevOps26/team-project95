import logging
import os
import re
from typing import Optional

from bs4 import BeautifulSoup
from fastapi import FastAPI, HTTPException
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_ollama import ChatOllama
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger("genai-service")

MAX_INPUT_CHARS = 15000
DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
DEFAULT_OLLAMA_MODEL = "llama3.1"


class AdvisorInput(BaseModel):
    name: Optional[str] = None
    email: Optional[str] = None
    profileUrl: Optional[str] = None


class ThesisProposalInput(BaseModel):
    title: str
    degreeType: Optional[str] = None
    originalDescription: Optional[str] = None
    aiOverview: Optional[str] = None
    researchArea: Optional[str] = None
    sourceUrl: str
    status: str = "OPEN"
    advisors: list[AdvisorInput] = Field(default_factory=list)
    tags: list[str] = Field(default_factory=list)


class GenAIExtractionRequest(BaseModel):
    sourceEndpointId: int
    chairId: int
    chairName: Optional[str] = None
    sourceUrl: str
    rawHtml: str
    extractedPlainText: Optional[str] = None


class GenAIExtractionResponse(BaseModel):
    theses: list[ThesisProposalInput] = Field(default_factory=list)
    extractionNotes: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    service: str


class ExtractionResult(BaseModel):
    theses: list[ThesisProposalInput] = Field(default_factory=list)
    extractionNotes: Optional[str] = None


app = FastAPI(
    title="GenAI Service",
    version="0.1.0",
    description="Minimal FastAPI + LangChain thesis extraction service",
)


def get_env(name: str, default: Optional[str] = None) -> Optional[str]:
    return os.getenv(name, default)


def get_required_env(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def preprocess_input(raw_html: str, extracted_plain_text: Optional[str]) -> str:
    if extracted_plain_text and extracted_plain_text.strip():
        text = extracted_plain_text
    else:
        soup = BeautifulSoup(raw_html, "html.parser")
        for tag in soup(["script", "style", "noscript"]):
            tag.decompose()
        text = soup.get_text(separator="\n")

    lines = [line.strip() for line in text.splitlines()]
    lines = [line for line in lines if line]
    cleaned = "\n".join(lines)
    cleaned = re.sub(r"[ \t]+", " ", cleaned)
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned).strip()

    return cleaned[:MAX_INPUT_CHARS]


def get_llm():
    provider = (get_env("GENAI_MODEL_PROVIDER", "openai") or "openai").lower()
    default_model = DEFAULT_OPENAI_MODEL if provider == "openai" else DEFAULT_OLLAMA_MODEL
    model_name = get_env("GENAI_MODEL_NAME", default_model) or default_model

    logger.info("Initializing LLM provider=%s model=%s", provider, model_name)

    if provider == "openai":
        api_key = get_required_env("OPENAI_API_KEY")
        return ChatOpenAI(
            model=model_name,
            api_key=api_key,
            temperature=0,
        )

    if provider == "ollama":
        base_url = get_env("OLLAMA_BASE_URL", "http://localhost:11434")
        return ChatOllama(
            model=model_name,
            base_url=base_url,
            temperature=0,
        )

    raise RuntimeError(f"Unsupported GENAI_MODEL_PROVIDER: {provider}")


def build_prompt(request: GenAIExtractionRequest, cleaned_text: str) -> str:
    return f'''
You extract structured thesis proposals from scraped university chair webpages.

Rules:
- Extract only thesis postings that are supported by the provided content.
- Do not invent missing facts.
- If multiple thesis postings are present, return multiple thesis objects.
- Ignore navigation, footer, legal text, and unrelated news if possible.
- degreeType should be BACHELOR or MASTER when clearly indicated, otherwise null.
- status should be OPEN unless the text clearly indicates otherwise.
- advisors should only include explicit names, emails, or profile links from the content.
- tags should be short topic keywords.
- aiOverview should be a concise 1-2 sentence grounded summary.
- sourceUrl should default to the provided source URL unless a more specific thesis link is clearly visible.
- originalDescription should copy or lightly consolidate the relevant description from the input, not invent new details.
- If no thesis posting can be identified confidently, return an empty theses list.

Input metadata:
- sourceEndpointId: {request.sourceEndpointId}
- chairId: {request.chairId}
- chairName: {request.chairName}
- sourceUrl: {request.sourceUrl}

Cleaned page content:
"""
{cleaned_text}
"""
'''.strip()


def normalize_thesis(thesis: ThesisProposalInput, source_url: str) -> ThesisProposalInput:
    normalized_title = thesis.title.strip()
    normalized_degree = thesis.degreeType.strip().upper() if thesis.degreeType else None
    if normalized_degree not in {"BACHELOR", "MASTER"}:
        normalized_degree = None

    normalized_status = (thesis.status or "OPEN").strip().upper() or "OPEN"
    normalized_tags = [tag.strip() for tag in thesis.tags if tag and tag.strip()]

    normalized_advisors = [
        AdvisorInput(
            name=advisor.name.strip() if advisor.name else None,
            email=advisor.email.strip() if advisor.email else None,
            profileUrl=advisor.profileUrl.strip() if advisor.profileUrl else None,
        )
        for advisor in thesis.advisors
    ]

    return ThesisProposalInput(
        title=normalized_title,
        degreeType=normalized_degree,
        originalDescription=thesis.originalDescription.strip() if thesis.originalDescription else None,
        aiOverview=thesis.aiOverview.strip() if thesis.aiOverview else None,
        researchArea=thesis.researchArea.strip() if thesis.researchArea else None,
        sourceUrl=(thesis.sourceUrl or source_url).strip() or source_url,
        status=normalized_status,
        advisors=normalized_advisors,
        tags=normalized_tags,
    )


def run_extraction(request: GenAIExtractionRequest) -> GenAIExtractionResponse:
    cleaned_text = preprocess_input(request.rawHtml, request.extractedPlainText)
    logger.info(
        "Extraction request sourceEndpointId=%s sourceUrl=%s rawChars=%s cleanedChars=%s",
        request.sourceEndpointId,
        request.sourceUrl,
        len(request.rawHtml),
        len(cleaned_text),
    )

    if not cleaned_text:
        return GenAIExtractionResponse(theses=[], extractionNotes="Input page was empty after preprocessing.")

    llm = get_llm()
    structured_llm = llm.with_structured_output(ExtractionResult)

    result = structured_llm.invoke([
        SystemMessage(
            content=(
                "You are a precise information extraction system. "
                "Return only structured thesis data supported by the input."
            )
        ),
        HumanMessage(content=build_prompt(request, cleaned_text)),
    ])

    normalized_theses: list[ThesisProposalInput] = []
    seen_titles: set[str] = set()

    for thesis in result.theses:
        if not thesis.title or not thesis.title.strip():
            continue

        normalized = normalize_thesis(thesis, request.sourceUrl)
        title_key = normalized.title.casefold()
        if title_key in seen_titles:
            continue
        seen_titles.add(title_key)
        normalized_theses.append(normalized)

    notes = result.extractionNotes
    if not notes:
        notes = f"Extracted {len(normalized_theses)} thesis proposal(s)."

    return GenAIExtractionResponse(theses=normalized_theses, extractionNotes=notes)


@app.get("/internal/v1/health", response_model=HealthResponse)
def health_check() -> HealthResponse:
    return HealthResponse(status="UP", service="genai-service")


@app.post(
    "/internal/v1/genai-service/extract-theses",
    response_model=GenAIExtractionResponse,
)
def extract_theses(request: GenAIExtractionRequest) -> GenAIExtractionResponse:
    try:
        response = run_extraction(request)
        logger.info(
            "Extraction successful sourceEndpointId=%s theses=%s",
            request.sourceEndpointId,
            len(response.theses),
        )
        return response
    except Exception as exc:
        logger.exception(
            "Extraction failed sourceEndpointId=%s sourceUrl=%s",
            request.sourceEndpointId,
            request.sourceUrl,
        )
        raise HTTPException(status_code=422, detail=str(exc)) from exc
