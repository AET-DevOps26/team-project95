#!/usr/bin/env python3
"""Clean thesis tags/research-area assignments and reclassify theses in batches.

This is an explicit maintenance script, not part of normal application startup.

What it does when run with --apply:
1. Drops legacy app-level tag storage if it exists.
2. Clears thesis_proposal_research_areas so all theses lose old research-area links.
3. Reads theses in batches, asks a GenAI model to classify each thesis into one of the
   canonical research areas from canonical-research-areas.yml or, only if necessary,
   a broad reusable new research area.
4. Upserts the chosen research area and links it to the thesis.

Dry-run is the default. Use --apply --yes to mutate the database non-interactively.

Required Python package:
  pip install "psycopg[binary]"

Database configuration is read from either DATABASE_URL or the THESIS_DB_* variables:
  DATABASE_URL=postgresql://thesis_user:thesis_password@localhost:5432/thesis_db
  THESIS_DB_HOST=localhost THESIS_DB_PORT=5432 THESIS_DB_NAME=thesis_db ...

GenAI configuration mirrors services/genai-service:
  Azure OpenAI:
    GENAI_USE_OLLAMA=false
    AZURE_OPENAI_ENDPOINT=...
    AZURE_OPENAI_API_KEY=...
    AZURE_OPENAI_CHAT_DEPLOYMENT=gpt-5.4
    AZURE_OPENAI_API_VERSION=2024-12-01-preview

  Ollama:
    GENAI_USE_OLLAMA=true
    GENAI_MODEL_NAME=llama3.1
    OLLAMA_BASE_URL=http://localhost:11434
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any

DEFAULT_BATCH_SIZE = 10
DEFAULT_AZURE_OPENAI_DEPLOYMENT = "gpt-5.4"
DEFAULT_AZURE_OPENAI_API_VERSION = "2024-12-01-preview"
DEFAULT_OLLAMA_MODEL = "llama3.1"
MAX_TEXT_CHARS_PER_THESIS = 6000

REPO_ROOT = Path(__file__).resolve().parents[1]
TAXONOMY_PATH = (
    REPO_ROOT
    / "services"
    / "thesis-service"
    / "src"
    / "main"
    / "resources"
    / "canonical-research-areas.yml"
)


@dataclass(frozen=True)
class Thesis:
    id: int
    title: str
    degree_type: str | None
    original_description: str | None
    ai_overview: str | None
    source_url: str
    chair_name: str | None


@dataclass(frozen=True)
class Classification:
    thesis_id: int
    research_area: str | None
    rationale: str | None = None


def import_psycopg():
    try:
        import psycopg
    except ImportError as exc:  # pragma: no cover - operator-facing failure path
        raise SystemExit(
            'Missing dependency: psycopg. Install with: pip install "psycopg[binary]"'
        ) from exc
    return psycopg


def env(name: str, default: str | None = None) -> str | None:
    value = os.getenv(name)
    return value if value not in (None, "") else default


def build_dsn() -> str:
    database_url = env("DATABASE_URL")
    if database_url:
        return database_url

    host = env("THESIS_DB_HOST", "localhost")
    port = env("THESIS_DB_PORT", "5432")
    dbname = env("THESIS_DB_NAME", "thesis_db")
    user = env("THESIS_DB_USER", "thesis_user")
    password = env("THESIS_DB_PASSWORD", "thesis_password")
    return f"postgresql://{urllib.parse.quote(user)}:{urllib.parse.quote(password)}@{host}:{port}/{dbname}"


def load_canonical_research_areas(path: Path = TAXONOMY_PATH) -> list[str]:
    areas: list[str] = []
    in_research_areas = False

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line == "researchAreas:":
            in_research_areas = True
            continue
        if in_research_areas:
            if not line.startswith("- "):
                break
            area = line[2:].strip().strip('"\'')
            if area:
                areas.append(area)

    if not areas:
        raise RuntimeError(f"No research areas found in {path}")
    return areas


def research_area_key(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", value.casefold()).strip()


def is_broad_research_area(value: str) -> bool:
    candidate = value.strip()
    if len(candidate) < 3 or len(candidate) > 60:
        return False
    if not re.match(r"^[\w][\w &/+\-]*$", candidate, flags=re.UNICODE):
        return False
    if len(candidate.split()) > 4:
        return False

    lower = candidate.casefold()
    blocked_terms = ("thesis", "project", "using", "based", "towards", " for ", " with ")
    return not any(term in f" {lower} " for term in blocked_terms)


def canonicalize_research_area(value: str | None, known_areas: list[str]) -> str | None:
    if not value or not value.strip():
        return None

    candidate = value.strip()
    known_by_casefold = {area.casefold(): area for area in known_areas}
    exact = known_by_casefold.get(candidate.casefold())
    if exact:
        return exact

    candidate_key = research_area_key(candidate)
    known_by_key = {research_area_key(area): area for area in known_areas}
    normalized_exact = known_by_key.get(candidate_key)
    if normalized_exact:
        return normalized_exact

    if is_broad_research_area(candidate):
        return candidate
    return None


def request_json(url: str, payload: dict[str, Any], headers: dict[str, str]) -> dict[str, Any]:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url, data=data, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"GenAI request failed with HTTP {exc.code}: {body}") from exc


def call_azure_openai(prompt: str) -> str:
    endpoint = env("AZURE_OPENAI_ENDPOINT")
    api_key = env("AZURE_OPENAI_API_KEY")
    deployment = env("AZURE_OPENAI_CHAT_DEPLOYMENT", DEFAULT_AZURE_OPENAI_DEPLOYMENT)
    api_version = env("AZURE_OPENAI_API_VERSION", DEFAULT_AZURE_OPENAI_API_VERSION)
    max_tokens = int(env("GENAI_MAX_COMPLETION_TOKENS", "30000") or "30000")

    if not endpoint or not api_key:
        raise RuntimeError("AZURE_OPENAI_ENDPOINT and AZURE_OPENAI_API_KEY are required")

    base = endpoint.rstrip("/")
    url = f"{base}/openai/deployments/{deployment}/chat/completions?api-version={api_version}"
    payload = {
        "messages": [
            {
                "role": "system",
                "content": (
                    "You are a precise thesis-classification system. "
                    "Return only valid JSON matching the requested schema."
                ),
            },
            {"role": "user", "content": prompt},
        ],
        "temperature": 0,
        "max_completion_tokens": max_tokens,
        "response_format": {"type": "json_object"},
    }
    response = request_json(
        url,
        payload,
        {"Content-Type": "application/json", "api-key": api_key},
    )
    return response["choices"][0]["message"]["content"]


def call_ollama(prompt: str) -> str:
    base_url = env("OLLAMA_BASE_URL", "http://localhost:11434").rstrip("/")
    model = env("GENAI_MODEL_NAME", DEFAULT_OLLAMA_MODEL)
    payload = {
        "model": model,
        "stream": False,
        "format": "json",
        "messages": [
            {
                "role": "system",
                "content": (
                    "You are a precise thesis-classification system. "
                    "Return only valid JSON matching the requested schema."
                ),
            },
            {"role": "user", "content": prompt},
        ],
    }
    response = request_json(
        f"{base_url}/api/chat",
        payload,
        {"Content-Type": "application/json"},
    )
    return response["message"]["content"]


def call_genai(prompt: str) -> str:
    use_ollama = (env("GENAI_USE_OLLAMA", "false") or "false").casefold() == "true"
    return call_ollama(prompt) if use_ollama else call_azure_openai(prompt)


def compact_text(value: str | None, limit: int = MAX_TEXT_CHARS_PER_THESIS) -> str:
    if not value:
        return ""
    text = re.sub(r"\s+", " ", value).strip()
    return text[:limit]


def build_prompt(theses: list[Thesis], known_areas: list[str]) -> str:
    thesis_payload = [
        {
            "id": thesis.id,
            "title": thesis.title,
            "degreeType": thesis.degree_type,
            "chairName": thesis.chair_name,
            "sourceUrl": thesis.source_url,
            "aiOverview": compact_text(thesis.ai_overview),
            "originalDescription": compact_text(thesis.original_description),
        }
        for thesis in theses
    ]

    return f"""
Classify each thesis into exactly one research area.

Rules:
- Prefer exactly one of the known research areas whenever any listed area can reasonably describe the thesis.
- Treat the known research-area list as the canonical taxonomy.
- Prefer a slightly broader known area over creating a new label.
- If uncertain between a known area and a new area, choose the closest known area.
- Only create a new researchArea when the thesis absolutely cannot be matched to any known area.
- New research areas must be broad, reusable academic fields.
- Do not use thesis titles, project names, chair-specific projects, methods, technologies, product names, or long phrases as researchArea.
- Return one classification for every input thesis id.

Known research areas:
{json.dumps(known_areas, ensure_ascii=False, indent=2)}

Return JSON with this exact shape:
{{
  "classifications": [
    {{"id": 123, "researchArea": "Machine Learning", "rationale": "short reason"}}
  ]
}}

Theses:
{json.dumps(thesis_payload, ensure_ascii=False, indent=2)}
""".strip()


def parse_classifications(raw: str, known_areas: list[str]) -> list[Classification]:
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"Model did not return valid JSON: {raw[:500]}") from exc

    items = payload.get("classifications")
    if not isinstance(items, list):
        raise RuntimeError(f"Model JSON missing classifications array: {payload}")

    classifications: list[Classification] = []
    for item in items:
        if not isinstance(item, dict):
            continue
        thesis_id = item.get("id")
        research_area = canonicalize_research_area(item.get("researchArea"), known_areas)
        rationale = item.get("rationale")
        if isinstance(thesis_id, int):
            classifications.append(
                Classification(
                    thesis_id=thesis_id,
                    research_area=research_area,
                    rationale=rationale if isinstance(rationale, str) else None,
                )
            )
    return classifications


def fetch_theses(conn: Any, limit: int | None = None) -> list[Thesis]:
    query = """
        SELECT tp.id,
               tp.title,
               tp.degree_type,
               tp.original_description,
               tp.ai_overview,
               tp.source_url,
               c.name AS chair_name
        FROM thesis_proposals tp
        LEFT JOIN chairs c ON c.id = tp.chair_id
        ORDER BY tp.id
    """
    params: tuple[Any, ...] = ()
    if limit is not None:
        query += " LIMIT %s"
        params = (limit,)

    with conn.cursor() as cur:
        cur.execute(query, params)
        return [Thesis(*row) for row in cur.fetchall()]


def cleanup_database(conn: Any, prune_orphan_research_areas: bool) -> None:
    with conn.cursor() as cur:
        cur.execute("DROP TABLE IF EXISTS thesis_proposal_tags CASCADE")
        cur.execute("DROP TABLE IF EXISTS tags CASCADE")
        cur.execute("ALTER TABLE thesis_proposals DROP COLUMN IF EXISTS tags")
        cur.execute("ALTER TABLE thesis_proposals DROP COLUMN IF EXISTS tag")
        cur.execute("TRUNCATE TABLE thesis_proposal_research_areas")
        if prune_orphan_research_areas:
            cur.execute(
                """
                DELETE FROM research_areas ra
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM thesis_proposal_research_areas tpra
                    WHERE tpra.research_area_id = ra.id
                )
                """
            )
    conn.commit()


def get_or_create_research_area_id(conn: Any, name: str) -> int:
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM research_areas WHERE lower(name) = lower(%s) LIMIT 1", (name,))
        row = cur.fetchone()
        if row:
            return row[0]

        cur.execute(
            "INSERT INTO research_areas(name) VALUES (%s) RETURNING id",
            (name,),
        )
        return cur.fetchone()[0]


def link_research_area(conn: Any, thesis_id: int, research_area: str) -> None:
    research_area_id = get_or_create_research_area_id(conn, research_area)
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO thesis_proposal_research_areas(thesis_proposal_id, research_area_id)
            VALUES (%s, %s)
            ON CONFLICT DO NOTHING
            """,
            (thesis_id, research_area_id),
        )


def batched(values: list[Thesis], size: int) -> list[list[Thesis]]:
    return [values[index : index + size] for index in range(0, len(values), size)]


def confirm_or_exit(args: argparse.Namespace, thesis_count: int) -> None:
    if not args.apply or args.yes:
        return

    print("This will mutate the configured thesis database:")
    if not args.skip_cleanup:
        print("  - drop legacy tag table/columns if present")
        print("  - clear all thesis research-area links")
    print(f"  - reclassify {thesis_count} theses in batches of {args.batch_size}")
    typed = input("Type 'reclassify research areas' to continue: ").strip()
    if typed != "reclassify research areas":
        print("Aborted.")
        sys.exit(1)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="Mutate the database. Default is dry-run.")
    parser.add_argument("--yes", action="store_true", help="Skip interactive confirmation.")
    parser.add_argument("--skip-cleanup", action="store_true", help="Do not drop tags or clear old research-area links.")
    parser.add_argument("--prune-orphan-research-areas", action="store_true", help="Delete research_areas no longer linked after cleanup.")
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
    parser.add_argument("--limit", type=int, help="Process only the first N theses, useful for testing.")
    parser.add_argument("--sleep", type=float, default=0.0, help="Seconds to sleep between GenAI calls.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.batch_size < 1:
        raise SystemExit("--batch-size must be >= 1")

    known_areas = load_canonical_research_areas()
    print(f"Loaded {len(known_areas)} canonical research areas from {TAXONOMY_PATH}")

    psycopg = import_psycopg()
    dsn = build_dsn()
    with psycopg.connect(dsn) as conn:
        theses = fetch_theses(conn, args.limit)
        print(f"Loaded {len(theses)} theses from database")
        confirm_or_exit(args, len(theses))

        if not args.apply:
            print("Dry-run: database cleanup and classification writes will be skipped.")

        total_skipped = 0
        classified_research_areas: dict[int, str] = {}
        known_area_keys = {research_area_key(area) for area in known_areas}

        for batch_number, batch in enumerate(batched(theses, args.batch_size), start=1):
            print(f"Classifying batch {batch_number} ({len(batch)} theses)...")
            prompt = build_prompt(batch, known_areas)
            raw_response = call_genai(prompt)
            classifications = parse_classifications(raw_response, known_areas)
            by_id = {classification.thesis_id: classification for classification in classifications}

            for thesis in batch:
                classification = by_id.get(thesis.id)
                research_area = classification.research_area if classification else None
                if not research_area:
                    total_skipped += 1
                    print(f"  - thesis {thesis.id}: no valid research area returned; skipped")
                    continue

                print(f"  - thesis {thesis.id}: {research_area}")
                classified_research_areas[thesis.id] = research_area

                area_key = research_area_key(research_area)
                if area_key not in known_area_keys:
                    known_areas.append(research_area)
                    known_area_keys.add(area_key)

            if args.sleep > 0:
                time.sleep(args.sleep)

        total_linked = 0
        if args.apply:
            if not args.skip_cleanup:
                print("Cleaning legacy tags and clearing thesis research-area links...")
                cleanup_database(conn, args.prune_orphan_research_areas)

            print("Writing classified research-area links...")
            for thesis_id, research_area in classified_research_areas.items():
                link_research_area(conn, thesis_id, research_area)
                total_linked += 1
            conn.commit()

        print("Done.")
        print(f"Classified theses: {len(classified_research_areas)}")
        print(f"Linked research areas: {total_linked if args.apply else 0}")
        print(f"Skipped theses without valid classification: {total_skipped}")


if __name__ == "__main__":
    main()
