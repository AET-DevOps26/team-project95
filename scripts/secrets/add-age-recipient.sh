#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

export SOPS_AGE_KEY_FILE="${SOPS_AGE_KEY_FILE:-$HOME/.config/sops/age/keys.txt}"

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 age1..." >&2
  exit 1
fi

key="$1"
if [[ ! "$key" =~ ^age1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]+$ ]]; then
  echo "Invalid age public key: $key" >&2
  exit 1
fi

grep -Fq "$key" .sops.yaml || sed -i.bak "/^[[:space:]]*age: / s/$/,$key/" .sops.yaml
rm -f .sops.yaml.bak

sops updatekeys --yes --input-type dotenv .env.enc

echo "Added $key"
