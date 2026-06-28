#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_DIR="$SCRIPT_DIR/open-thesis-radar"
NAMESPACE="${NAMESPACE:-open-thesis-radar}"
RELEASE="${RELEASE:-open-thesis-radar}"
IMAGE_TAG="${IMAGE_TAG:-$(git ls-remote origin refs/heads/main | awk '{print $1}')}"
SECRET_VALUES_FILE="${SECRET_VALUES_FILE:-$CHART_DIR/values-secret.local.yaml}"

: "${IMAGE_TAG:?Could not resolve IMAGE_TAG. Set IMAGE_TAG to the commit SHA image tag to deploy}"

args=(
  upgrade --install "$RELEASE" "$CHART_DIR"
  --namespace "$NAMESPACE"
  --create-namespace
  --dependency-update
  --set "global.imageTag=$IMAGE_TAG"
)

if [ -f "$SECRET_VALUES_FILE" ]; then
  args+=(--values "$SECRET_VALUES_FILE")
else
  echo "==> $SECRET_VALUES_FILE not found; deploying with secrets.create=false."
  echo "==> Required Secrets must already exist in namespace '$NAMESPACE'."
fi

echo "==> Deploying release '$RELEASE' to namespace '$NAMESPACE' with image tag '$IMAGE_TAG'"
helm "${args[@]}"
