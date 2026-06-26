#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

export SOPS_AGE_KEY_FILE="${SOPS_AGE_KEY_FILE:-$HOME/.config/sops/age/keys.txt}"

mkdir -p "$(dirname "$SOPS_AGE_KEY_FILE")"
[[ -f "$SOPS_AGE_KEY_FILE" ]] || age-keygen -o "$SOPS_AGE_KEY_FILE"
chmod 600 "$SOPS_AGE_KEY_FILE"

public_key="$(awk '/^# public key: / { print $4; exit }' "$SOPS_AGE_KEY_FILE")"
if [[ -z "$public_key" || ! "$public_key" =~ ^age1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]+$ ]]; then
  echo "Could not extract a valid age public key from $SOPS_AGE_KEY_FILE" >&2
  exit 1
fi

[[ -f .sops.yaml ]] || cat > .sops.yaml <<EOF
creation_rules:
  - path_regex: ^\\.env(\\.enc)?$
    age: $public_key
EOF

[[ -f .env ]] || cp .env.example .env

tmp_file="$(mktemp .env.enc.tmp.XXXXXX)"
trap 'rm -f "$tmp_file"' EXIT
sops --filename-override .env.enc --input-type dotenv --output-type dotenv --encrypt .env > "$tmp_file"
mv "$tmp_file" .env.enc
trap - EXIT

echo "Created .env.enc"
echo "Your public key: $public_key"
