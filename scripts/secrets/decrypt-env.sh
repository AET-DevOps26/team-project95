#!/usr/bin/env bash
set -euo pipefail

export SOPS_AGE_KEY_FILE="${SOPS_AGE_KEY_FILE:-$HOME/.config/sops/age/keys.txt}"

sops --input-type dotenv --output-type dotenv --decrypt .env.enc > .env

echo "Created .env"
