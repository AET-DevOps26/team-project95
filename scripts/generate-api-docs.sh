#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v npx >/dev/null 2>&1; then
  echo "Error: npx is required to generate API docs. Install Node.js/npm and try again." >&2
  exit 1
fi

npx --yes @redocly/cli build-docs \
  "$ROOT_DIR/api/openapi-v1.yml" \
  --output "$ROOT_DIR/docs/api.html"
