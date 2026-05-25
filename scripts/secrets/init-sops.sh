#!/usr/bin/env bash
set -euo pipefail

export SOPS_AGE_KEY_FILE="${SOPS_AGE_KEY_FILE:-$HOME/.config/sops/age/keys.txt}"

mkdir -p "$(dirname "$SOPS_AGE_KEY_FILE")"
[[ -f "$SOPS_AGE_KEY_FILE" ]] || age-keygen -o "$SOPS_AGE_KEY_FILE"
chmod 600 "$SOPS_AGE_KEY_FILE"

public_key="$(grep 'public key:' "$SOPS_AGE_KEY_FILE" | sed 's/^# public key: //')"

[[ -f .sops.yaml ]] || cat > .sops.yaml <<EOF
creation_rules:
  - path_regex: ^\\.env(\\.enc)?$
    age: $public_key
EOF

[[ -f .env ]] || cp .env.example .env

sops --filename-override .env.enc --input-type dotenv --output-type dotenv --encrypt .env > .env.enc

echo "Created .env.enc"
echo "Your public key: $public_key"
