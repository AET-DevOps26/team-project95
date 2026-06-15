# Kubernetes Deployment Work Packages

This document gives a high-level roadmap for deploying **Open Thesis Radar / ThesisHub** to the AET / DevOps chair Kubernetes cluster in a simple, reproducible way.

The goal is to stay close to the Kubernetes workshop style: understandable manifests, small steps, conservative resource usage, and one-command redeployment from Git.

## 0. Orientation and constraints

- Confirm access to the AET cluster with the copied `~/.kube/config`.
- Identify the team project / namespace setup in Rancher.
- Understand the cluster constraints:
  - shared teaching infrastructure,
  - limited CPU and memory,
  - no hardcoded secrets,
  - no public databases or admin/debug endpoints,
  - reproducible deployment from Git,
  - deployment must survive a clean cluster wipe.
- Decide which environment this branch targets first:
  - local Docker Compose remains the development baseline,
  - Kubernetes manifests become the deployment baseline for the chair cluster.

## 1. Inventory the current application

- List all deployable components:
  - frontend,
  - thesis-service,
  - scraping-service,
  - genai-service,
  - vector-search-service,
  - PostgreSQL database.
- Check which components already have Dockerfiles and which images are built in CI.
- Identify runtime configuration for every component:
  - ports,
  - environment variables,
  - database connection settings,
  - service-to-service URLs,
  - AI provider settings,
  - Azure/vector search settings,
  - scraping schedule settings.
- Separate required configuration from optional/development-only configuration.

## 2. Decide the first deployable slice

- Start with the smallest useful deployment instead of deploying everything at once.
- Suggested first slice:
  - PostgreSQL,
  - thesis-service,
  - frontend.
- Add the remaining services later:
  - genai-service,
  - scraping-service,
  - vector-search-service.
- Keep this staged approach so debugging stays manageable.

## 3. Choose the Kubernetes packaging approach

- Use a simple approach that matches the workshop knowledge.
- Preferred first version:
  - raw Kubernetes manifests,
  - organized with Kustomize.
- Keep manifests in Git under `infra/`.
- Aim for one deployment command at the end, such as applying one Kustomize directory.
- Avoid Helm unless the raw-manifest setup becomes too repetitive.

## 4. Create the Kubernetes structure

- Create a clear infra layout for Kubernetes resources.
- Define base resources for each service:
  - Deployment,
  - Service,
  - ConfigMap where appropriate,
  - Secret references where appropriate,
  - resource requests and limits.
- Define database resources:
  - PostgreSQL Deployment or StatefulSet,
  - Service,
  - PersistentVolumeClaim with a small, justified size.
- Define environment-specific overlays if needed:
  - local/test cluster,
  - AET production-like cluster.

## 5. Handle configuration and secrets properly

- Move non-sensitive runtime configuration into ConfigMaps or manifest variables.
- Move sensitive values into Kubernetes Secrets.
- Do not commit real credentials.
- Decide how secrets are provided for deployment:
  - manually during early testing,
  - later via GitHub Actions secrets for reproducible CD.
- Document which secrets are required.
- Keep external dependencies minimal and explicit.

## 6. Define resource requests and limits

- Add resource requests and limits to every container.
- Start conservatively to respect the fair-use policy.
- Keep total namespace requests below:
  - `4 vCPU`,
  - `6 GB memory`.
- Prefer one replica per service initially.
- Increase resources only after observing real usage.
- Make sure crash-looping or unused workloads are cleaned up quickly.

## 7. Add internal networking

- Expose backend services only inside the cluster unless they need to be public.
- Let services communicate through Kubernetes Service DNS names.
- Keep PostgreSQL internal only.
- Keep write/admin/internal APIs private where possible.
- Public exposure should initially be limited to the frontend and possibly the public thesis API path.

## 8. Add ingress and TLS

- Define how users will access the deployed app from outside the cluster.
- Add an Ingress for the frontend / public entrypoint.
- Use the cluster-provided `letsencrypt-prod` issuer for TLS.
- Avoid self-signed certificates for the production-facing deployment.
- Ensure no database, debug UI, or internal service is exposed publicly.

## 9. Add database initialization and reproducible seed data

- Ensure the database schema is created automatically.
- Decide how required seed/test data is loaded:
  - application migrations,
  - init job,
  - startup import,
  - or another automated mechanism.
- Avoid manual `psql`, `kubectl exec`, or port-forward steps as part of the final deployment.
- Make sure the project still works after a complete namespace wipe.

## 10. Test the deployment manually from the branch

- Apply the manifests to the team namespace.
- Check pod startup and logs.
- Verify service-to-service communication.
- Verify frontend access through the browser.
- Verify key application flows:
  - search/browse theses,
  - thesis detail page,
  - health endpoints,
  - database persistence,
  - optional scraping / GenAI / vector search if enabled.
- Iterate on manifests until the deployment is stable.

## 11. Add cleanup and operational commands

- Provide simple commands for:
  - deploy/update,
  - inspect status,
  - view logs,
  - clean up resources.
- Keep these commands documented in the repository.
- Make it easy to scale down or delete nonessential workloads when not needed.

## 12. Connect CI/CD

- Review the existing GitHub Actions workflows.
- Ensure CI builds and tests all relevant components.
- Ensure Docker images are built and pushed to a registry accessible by the cluster.
- Add CD after the manifests are stable:
  - deploy automatically on merge to `main`,
  - use GitHub Actions secrets for kubeconfig and credentials,
  - avoid hardcoded tokens or environment-specific values.
- Keep the deployment workflow reproducible and maintainable.

## 13. Finalize one-command redeployment

- Make the final deployment work from a clean cluster state.
- The final bring-up should require one main command or one CI/CD trigger.
- All manifests, configuration templates, and deployment scripts must be in Git.
- All required secrets must be documented and injectable.
- Test the process by deleting/recreating the namespace or resources and redeploying.

## 14. Update project documentation

- Update the repository README with the final deployment instructions.
- Document:
  - required tools,
  - target namespace,
  - required secrets,
  - deployment command,
  - cleanup command,
  - how CI/CD deploys,
  - how tutors can reproduce the deployment.
- Keep the final instructions short; ideally they should read like: run one command or trigger one workflow.

## 15. End deliverable

At the end of this branch, the repository should contain:

- Kubernetes manifests or Kustomize setup under `infra/`.
- Resource requests and limits for all deployed containers.
- No committed real secrets.
- Internal-only database and internal service networking.
- Public frontend access through Ingress with TLS.
- Reproducible database initialization / seed data if needed.
- Updated CI/CD for build/test and deployment.
- README documentation for redeployment after a cluster wipe.
- A deployment that respects the AET cluster fair-use policy.
