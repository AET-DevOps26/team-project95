#!/usr/bin/env bash
set -euo pipefail

export SOPS_AGE_KEY_FILE="${SOPS_AGE_KEY_FILE:-$HOME/.config/sops/age/keys.txt}"

key="$1"

grep -q "$key" .sops.yaml || sed -i.bak "/^[[:space:]]*age: / s/$/,$key/" .sops.yaml
rm -f .sops.yaml.bak

sops updatekeys --yes --input-type dotenv .env.enc

echo "Added $key"
