#!/usr/bin/env bash
set -euo pipefail

TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-180}"
SLEEP_SECONDS="${HEALTH_SLEEP_SECONDS:-5}"

if [[ "$#" -eq 0 ]]; then
  echo "Usage: $0 <health-url> [health-url ...]" >&2
  exit 2
fi

wait_for_url() {
  local url="$1"
  local deadline=$((SECONDS + TIMEOUT_SECONDS))

  echo "Waiting for ${url} (timeout: ${TIMEOUT_SECONDS}s)"
  until curl --fail --silent --show-error --max-time 5 "${url}" >/dev/null; do
    if (( SECONDS >= deadline )); then
      echo "Timed out waiting for ${url}" >&2
      return 1
    fi
    sleep "${SLEEP_SECONDS}"
  done
  echo "${url} is healthy"
}

for url in "$@"; do
  wait_for_url "${url}"
done
