#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="open-thesis-radar"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_TAG="${IMAGE_TAG:-$(git ls-remote origin refs/heads/main | awk '{print $1}')}"
: "${IMAGE_TAG:?Could not resolve IMAGE_TAG. Set IMAGE_TAG to the commit SHA image tag to deploy}"

RENDERED_DIR="$(mktemp -d)"
trap 'rm -rf "$RENDERED_DIR"' EXIT
cp -R "$SCRIPT_DIR/deployments" "$SCRIPT_DIR/services" "$SCRIPT_DIR/jobs" "$RENDERED_DIR/"
cp "$SCRIPT_DIR/ingress.yml" "$RENDERED_DIR/ingress.yml"
find "$RENDERED_DIR" -type f -name '*.yml' -exec sed -i "s/:IMAGE_TAG/:${IMAGE_TAG}/g" {} +

echo "==> Deploying image tag: $IMAGE_TAG"

echo "==> Creating namespace (if not exists)..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

echo "==> Applying secrets..."
if [ -f "$SCRIPT_DIR/secrets.yaml" ]; then
  kubectl apply -f "$SCRIPT_DIR/secrets.yaml"
else
  echo "ERROR: $SCRIPT_DIR/secrets.yaml not found."
  echo "       Copy $SCRIPT_DIR/secrets.example.yaml, fill in values, and save as secrets.yaml"
  exit 1
fi

echo "==> Installing/upgrading Helm releases (databases)..."
helmfile -f "$SCRIPT_DIR/helmfile.yaml" apply

echo "==> Applying deployments, services, and ingress..."
kubectl apply -f "$RENDERED_DIR/deployments/"
kubectl apply -f "$RENDERED_DIR/services/"
kubectl apply -f "$RENDERED_DIR/ingress.yml"

echo ""
echo "✅ Deployment complete!"
echo "   Pods:    kubectl get pods -n $NAMESPACE"
echo "   Ingress: kubectl get ingress -n $NAMESPACE"
echo "   Cert:    kubectl get certificate -n $NAMESPACE"
