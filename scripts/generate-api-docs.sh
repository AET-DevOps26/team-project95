#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

npx --yes @redocly/cli build-docs \
  "$ROOT_DIR/api/openapi-v1.yml" \
  --output "$ROOT_DIR/docs/api.html"
