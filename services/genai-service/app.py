import logging
import os
import re
from functools import lru_cache
from typing import Any, Optional
from urllib.parse import unquote, urlparse

from bs4 import BeautifulSoup
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_ollama import ChatOllama
from langchain_openai import AzureChatOpenAI
from pydantic import BaseModel, Field, ValidationError

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger("genai-service")

MAX_INPUT_CHARS = 40000
DEFAULT_MAX_COMPLETION_TOKENS = 30000
DEFAULT_AZURE_OPENAI_DEPLOYMENT = "gpt-5.4"
DEFAULT_AZURE_OPENAI_API_VERSION = "2024-12-01-preview"
DEFAULT_OLLAMA_MODEL = "llama3.1"

NOISE_SELECTORS = [
    "header",
    "footer",
    "nav",
    "aside",
    "form",
    "button",
    "svg",
    "picture",
    "iframe",
    "[role='navigation']",
    "[role='banner']",
    "[role='contentinfo']",
    ".c-content-area__sitenav",
    ".c-content-area__aside",
    ".navbar",
    ".breadcrumb",
    ".cookie",
    ".search",
    ".sidebar",
    ".pagination",
    ".social",
]

CONTENT_SELECTORS = [
    "#content",
    "main .c-content-area__main",
    ".c-content-area__main",
    "main",
    "article",
]


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


class ErrorResponse(BaseModel):
    message: str
    details: Optional[dict[str, Any]] = None


class DraftAdvisorInput(BaseModel):
    name: Optional[str] = None
    email: Optional[str] = None
    profileUrl: Optional[str] = None


class DraftThesisProposalInput(BaseModel):
    title: Optional[str] = None
    degreeType: Optional[str] = None
    originalDescription: Optional[str] = None
    aiOverview: Optional[str] = None
    researchArea: Optional[str] = None
    sourceUrl: Optional[str] = None
    status: Optional[str] = None
    advisors: list[DraftAdvisorInput] = Field(default_factory=list)
    tags: list[str] = Field(default_factory=list)


class ExtractionDraftResult(BaseModel):
    theses: list[DraftThesisProposalInput] = Field(default_factory=list)
    extractionNotes: Optional[str] = None


class GenAIServiceError(Exception):
    status_code = 500
    message = "Internal server error."

    def __init__(self, message: Optional[str] = None, details: Optional[dict[str, Any]] = None):
        self.message = message or self.message
        self.details = details
        super().__init__(self.message)


class ConfigError(GenAIServiceError):
    status_code = 500
    message = "GenAI service configuration error."


class UpstreamServiceError(GenAIServiceError):
    status_code = 502
    message = "GenAI provider request failed."


class ExtractionError(GenAIServiceError):
    status_code = 422
    message = "Extraction could not produce reliable structured output."


app = FastAPI(
    title="GenAI Service",
    version="0.1.0",
    description="Minimal FastAPI + LangChain thesis extraction service",
)


@app.exception_handler(GenAIServiceError)
async def handle_genai_service_error(_: Request, exc: GenAIServiceError) -> JSONResponse:
    return JSONResponse(
        status_code=exc.status_code,
        content=ErrorResponse(message=exc.message, details=exc.details).model_dump(
            exclude_none=True
        ),
    )


@app.exception_handler(RequestValidationError)
async def handle_validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
    return JSONResponse(
        status_code=422,
        content=ErrorResponse(
            message="Request validation failed.",
            details={"errors": exc.errors()},
        ).model_dump(exclude_none=True),
    )


def get_env(name: str, default: Optional[str] = None) -> Optional[str]:
    return os.getenv(name, default)


def get_required_env(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise ConfigError(
            message="GenAI service configuration error.",
            details={"missingEnvironmentVariable": name},
        )
    return value


def get_fragment_container(
    soup: BeautifulSoup, source_url: Optional[str]
) -> Optional[BeautifulSoup]:
    if not source_url:
        return None

    fragment = unquote(urlparse(source_url).fragment)
    if not fragment:
        return None

    anchor = soup.find(id=fragment)
    if not anchor:
        return None

    container = anchor
    if getattr(anchor, "name", None) not in {"article", "section", "div", "main"}:
        container = anchor.find_parent(["article", "section", "div", "main"]) or anchor
    if not container.get_text(strip=True):
        return None

    return BeautifulSoup(str(container), "html.parser")


def extract_relevant_html(soup: BeautifulSoup, source_url: Optional[str] = None) -> BeautifulSoup:
    for tag in soup(["script", "style", "noscript", "template"]):
        tag.decompose()

    for selector in NOISE_SELECTORS:
        for tag in soup.select(selector):
            tag.decompose()

    fragment_content = get_fragment_container(soup, source_url)
    if fragment_content:
        return fragment_content

    for selector in CONTENT_SELECTORS:
        content = soup.select_one(selector)
        if content and content.get_text(strip=True):
            return BeautifulSoup(str(content), "html.parser")

    return soup


def normalize_text(text: str) -> str:
    lines = [re.sub(r"[ \t]+", " ", line.strip()) for line in text.splitlines()]
    lines = [line for line in lines if line]

    deduplicated_lines: list[str] = []
    seen_short_lines: set[str] = set()
    for line in lines:
        # Navigation and table boilerplate tends to repeat as short labels. Keep long
        # descriptive lines even if repeated because repeated thesis sections can be meaningful.
        if len(line) <= 80:
            line_key = line.casefold()
            if line_key in seen_short_lines:
                continue
            seen_short_lines.add(line_key)
        deduplicated_lines.append(line)

    cleaned = "\n".join(deduplicated_lines)
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned).strip()
    return cleaned[:MAX_INPUT_CHARS]


def preprocess_input(
    raw_html: str, extracted_plain_text: Optional[str], source_url: Optional[str] = None
) -> str:
    if extracted_plain_text and extracted_plain_text.strip():
        return normalize_text(extracted_plain_text)

    soup = BeautifulSoup(raw_html, "html.parser")
    relevant_soup = extract_relevant_html(soup, source_url)
    text = relevant_soup.get_text(separator="\n")
    return normalize_text(text)


@lru_cache(maxsize=1)
def get_llm():
    use_ollama = (get_env("GENAI_USE_OLLAMA", "false") or "false").lower() == "true"
    max_tokens = int(get_env("GENAI_MAX_COMPLETION_TOKENS", str(DEFAULT_MAX_COMPLETION_TOKENS)))

    if use_ollama:
        model = get_env("GENAI_MODEL_NAME", DEFAULT_OLLAMA_MODEL) or DEFAULT_OLLAMA_MODEL
        logger.info("Initializing LLM provider=ollama model=%s", model)
        return ChatOllama(
            model=model,
            base_url=get_env("OLLAMA_BASE_URL", "http://localhost:11434"),
            temperature=0,
        )

    deployment = get_env("AZURE_OPENAI_CHAT_DEPLOYMENT") or DEFAULT_AZURE_OPENAI_DEPLOYMENT
    logger.info("Initializing LLM provider=azure-openai deployment=%s", deployment)
    return AzureChatOpenAI(
        azure_endpoint=get_required_env("AZURE_OPENAI_ENDPOINT"),
        azure_deployment=deployment,
        api_key=get_required_env("AZURE_OPENAI_API_KEY"),
        api_version=get_env("AZURE_OPENAI_API_VERSION", DEFAULT_AZURE_OPENAI_API_VERSION),
        max_completion_tokens=max_tokens,
        temperature=0,
    )


def build_prompt(request: GenAIExtractionRequest, cleaned_text: str) -> str:
    return f'''
You extract structured thesis proposals from scraped university chair webpages.

Rules:
- Extract only thesis postings that are supported by the provided content.
- Do not invent missing facts.
- The title should be copied exactly as is.
- If multiple thesis postings are present, return multiple thesis objects.
- Ignore navigation, footer, legal text, and unrelated news.
- degreeType should be [BACHELOR, MASTER, PHD] when clearly indicated, otherwise null.
- status should be OPEN unless the text clearly indicates otherwise.
- advisors should only include explicit names, emails, or profile links from the content.
- tags should be short topic keywords.
- aiOverview should be a concise 1-2 sentence grounded summary.
- sourceUrl should default to the provided source URL unless a more specific thesis link is clearly visible.
- originalDescription should copy the relevant description from the input, not invent new details.
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


def normalize_thesis(thesis: DraftThesisProposalInput, source_url: str) -> ThesisProposalInput:
    if not thesis.title or not thesis.title.strip():
        raise ExtractionError(details={"reason": "Model returned a thesis item without a title."})

    normalized_title = thesis.title.strip()
    normalized_degree = thesis.degreeType.strip().upper() if thesis.degreeType else None
    if normalized_degree not in {"BACHELOR", "MASTER", "PHD"}:
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
        originalDescription=thesis.originalDescription.strip()
        if thesis.originalDescription
        else None,
        aiOverview=thesis.aiOverview.strip() if thesis.aiOverview else None,
        researchArea=thesis.researchArea.strip() if thesis.researchArea else None,
        sourceUrl=(thesis.sourceUrl or source_url).strip() or source_url,
        status=normalized_status,
        advisors=normalized_advisors,
        tags=normalized_tags,
    )


def run_extraction(request: GenAIExtractionRequest) -> GenAIExtractionResponse:
    cleaned_text = preprocess_input(request.rawHtml, request.extractedPlainText, request.sourceUrl)
    logger.info(
        "Extraction request sourceEndpointId=%s sourceUrl=%s rawChars=%s cleanedChars=%s",
        request.sourceEndpointId,
        request.sourceUrl,
        len(request.rawHtml),
        len(cleaned_text),
    )

    if not cleaned_text:
        return GenAIExtractionResponse(
            theses=[], extractionNotes="Input page was empty after preprocessing."
        )

    llm = get_llm()
    structured_llm = llm.with_structured_output(ExtractionDraftResult)

    try:
        result = structured_llm.invoke(
            [
                SystemMessage(
                    content=(
                        "You are a precise information extraction system. "
                        "Return only structured thesis data supported by the input."
                    )
                ),
                HumanMessage(content=build_prompt(request, cleaned_text)),
            ]
        )
    except GenAIServiceError:
        raise
    except Exception as exc:
        raise UpstreamServiceError(details={"errorType": type(exc).__name__}) from exc

    normalized_theses: list[ThesisProposalInput] = []
    seen_titles: set[str] = set()

    for thesis in result.theses:
        try:
            normalized = normalize_thesis(thesis, request.sourceUrl)
        except ExtractionError:
            continue
        except ValidationError as exc:
            raise ExtractionError(details={"validationErrors": exc.errors()}) from exc

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
@app.get("/health", response_model=HealthResponse)
@app.get("/ready", response_model=HealthResponse)
def health_check() -> HealthResponse:
    return HealthResponse(status="UP", service="genai-service")


@app.post(
    "/internal/v1/genai-service/extract-theses",
    response_model=GenAIExtractionResponse,
    operation_id="extractThesesFromRawContent",
    responses={
        422: {"model": ErrorResponse},
        500: {"model": ErrorResponse},
        502: {"model": ErrorResponse},
    },
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
    except GenAIServiceError:
        logger.exception(
            "Extraction failed sourceEndpointId=%s sourceUrl=%s",
            request.sourceEndpointId,
            request.sourceUrl,
        )
        raise
    except Exception as exc:
        logger.exception(
            "Unexpected extraction failure sourceEndpointId=%s sourceUrl=%s",
            request.sourceEndpointId,
            request.sourceUrl,
        )
        raise UpstreamServiceError(details={"errorType": type(exc).__name__}) from exc
