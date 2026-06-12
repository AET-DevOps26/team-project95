#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
COLLECTION_DIR="${BRUNO_COLLECTION_DIR:-${REPO_ROOT}/bruno-workspace/collections/thesis-discovery-api}"
BRUNO_ENV="${BRUNO_ENV:-Local}"
BRU_BIN="${BRU_BIN:-bru}"

if [[ ! -d "${COLLECTION_DIR}" ]]; then
  echo "Bruno collection directory not found: ${COLLECTION_DIR}" >&2
  exit 1
fi

if ! command -v "${BRU_BIN}" >/dev/null 2>&1; then
  echo "Bruno CLI not found: ${BRU_BIN}" >&2
  echo "Install it with: npm install -g @usebruno/cli" >&2
  echo "Or set BRU_BIN to the path of your bru executable." >&2
  exit 1
fi

args=(run --env "${BRUNO_ENV}")

add_env_var_if_set() {
  local shell_name="$1"
  local bruno_name="$2"

  if [[ -v "${shell_name}" && -n "${!shell_name}" ]]; then
    args+=(--env-var "${bruno_name}=${!shell_name}")
  fi
}

# Optional overrides. If unset, Bruno uses variables from environments/Local.yml.
add_env_var_if_set THESIS_SERVICE_URL thesisServiceUrl
add_env_var_if_set SCRAPING_SERVICE_URL scrapingServiceUrl
add_env_var_if_set VECTOR_SEARCH_SERVICE_URL vectorSearchServiceUrl
add_env_var_if_set GENAI_SERVICE_URL genaiServiceUrl
add_env_var_if_set THESIS_ID thesisId
add_env_var_if_set SOURCE_ENDPOINT_ID sourceEndpointId
add_env_var_if_set CHAIR_ID chairId

args+=("$@")

cd "${COLLECTION_DIR}"
exec "${BRU_BIN}" "${args[@]}"
