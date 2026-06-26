#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

export SOPS_AGE_KEY_FILE="${SOPS_AGE_KEY_FILE:-$HOME/.config/sops/age/keys.txt}"

tmp_file="$(mktemp .env.tmp.XXXXXX)"
trap 'rm -f "$tmp_file"' EXIT
sops --input-type dotenv --output-type dotenv --decrypt .env.enc > "$tmp_file"
mv "$tmp_file" .env
trap - EXIT

echo "Created .env"
