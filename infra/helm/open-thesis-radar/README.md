# Open Thesis Radar Helm Chart

This chart deploys the full Open Thesis Radar stack:

- frontend
- thesis-service
- scraping-service
- genai-service
- vector-search-service
- thesis PostgreSQL database
- vector PostgreSQL database
- ingress

## One-command deployment

For a fresh namespace where Helm should also create the required Kubernetes Secrets, copy the example secret values file and fill in real values:

```bash
cp infra/helm/open-thesis-radar/values-secret.example.yaml infra/helm/open-thesis-radar/values-secret.local.yaml
```

Then deploy with one command:

```bash
IMAGE_TAG=<commit-sha> ./infra/helm/deploy-helm.sh
```

Or call Helm directly:

```bash
helm upgrade --install open-thesis-radar infra/helm/open-thesis-radar \
  --namespace open-thesis-radar \
  --create-namespace \
  --dependency-update \
  -f infra/helm/open-thesis-radar/values-secret.local.yaml \
  --set global.imageTag=<commit-sha>
```

If `IMAGE_TAG` is omitted, `deploy-helm.sh` uses the latest remote `main` commit SHA, matching the old Kubernetes script behavior.

## Production-style deployment with externally managed Secrets

If Secrets already exist in the target namespace, keep `secrets.create=false` and deploy with:

```bash
helm upgrade --install open-thesis-radar infra/helm/open-thesis-radar \
  --namespace open-thesis-radar \
  --create-namespace \
  --dependency-update \
  --set global.imageTag=<commit-sha>
```

Required external Secrets by default:

- `thesis-db-secret`, key `password`
- `vector-db-secret`, key `password`
- `azure-openai-secret`, keys `endpoint` and `api-key`

## Optional source registry sync job

The old `jobs/sync-source-registry.yml` is represented as an optional Helm hook. Enable it with:

```bash
--set syncSourceRegistry.enabled=true
```

It runs after install/upgrade and is deleted after succeeding.

## Notes

The chart keeps service names compatible with the old Kubernetes manifests: `thesis-service`, `vector-search-service`, `genai-service`, `scraping-service`, `user-interface`, `thesis-db`, and `vector-db`.
