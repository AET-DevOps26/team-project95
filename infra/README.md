# Infrastructure deployment

This directory contains the Terraform and Ansible setup for the Azure production environment.

## Workflow model

Infrastructure changes are checked in two stages:

1. **Pull request:** `ci.yml` calls `.github/workflows/terraform_plan.yml` when `github.event_name == 'pull_request'`.
   - The plan workflow logs into Azure.
   - It initializes the remote Terraform backend.
   - It runs `terraform validate`.
   - It runs `terraform plan -no-color`.
   - The PR author/reviewers can inspect the workflow logs before merging.

2. **After merge to `main`:** deployment is done by `.github/workflows/deploy_azure.yml`.
   - Terraform runs `terraform apply -auto-approve` from the merged `main` revision.
   - Terraform updates the Azure infrastructure.
   - The workflow reads Terraform outputs such as the VM public IP.
   - The VM is started if necessary.
   - Ansible deploys the application to the VM.

The Ansible playbooks configure Docker on the VM, write the production environment file, pull the GHCR images, start the Docker Compose stack, and optionally configure Certbot/HTTPS. See [`ansible/README.md`](ansible/README.md) for local Ansible usage and operational commands.

The PR plan is meant as a review gate: infrastructure changes should be visible in CI before they are merged. The apply step is still automated because GitHub Actions cannot answer Terraform's interactive `yes` prompt.

## GitHub secrets

The workflows depend on the following GitHub repository or environment secrets.

| Secret | Used by | Purpose |
| --- | --- | --- |
| `AZURE_CREDENTIALS` | Terraform plan, deploy | Azure service principal credentials for `azure/login`. |
| `AZURE_TF_BACKEND_RESOURCE_GROUP` | Terraform plan, deploy | Resource group containing the Terraform state storage account. |
| `AZURE_TF_BACKEND_STORAGE_ACCOUNT` | Terraform plan, deploy | Azure Storage Account used for Terraform remote state. |
| `AZURE_TF_BACKEND_CONTAINER` | Terraform plan, deploy | Blob container used for Terraform remote state. |
| `AZURE_TF_BACKEND_KEY` | Terraform plan, deploy | Blob key/path for the Terraform state file. |
| `AZURE_VM_SSH_PUBLIC_KEY` | Terraform plan, deploy | Public SSH key configured on the Azure VM. Passed as `TF_VAR_admin_ssh_public_key`. |
| `AZURE_VM_SSH_PRIVATE_KEY` | Deploy | Private SSH key used by Ansible to connect to the Azure VM. |
| `CERTBOT_EMAIL` | Deploy | Email address used for Let's Encrypt certificate registration. Required when Certbot is enabled. |
| `THESIS_DB_PASSWORD` | Deploy | Production password for the thesis PostgreSQL database. |
| `VECTOR_DB_PASSWORD` | Deploy | Production password for the vector-search PostgreSQL database. |
| `AZURE_OPENAI_ENDPOINT` | Deploy | Azure OpenAI endpoint used by the GenAI service and vector-search service. |
| `AZURE_OPENAI_API_KEY` | Deploy | API key used by the GenAI service and vector-search service. |

The deploy workflow also uses GitHub's built-in token as `GHCR_TOKEN` so the VM can pull images from GitHub Container Registry.

## Terraform backend

Terraform state is stored remotely in Azure Blob Storage. The backend storage resources must already exist before either workflow runs.

Both the plan and deploy workflows initialize Terraform with:

```text
AZURE_TF_BACKEND_RESOURCE_GROUP
AZURE_TF_BACKEND_STORAGE_ACCOUNT
AZURE_TF_BACKEND_CONTAINER
AZURE_TF_BACKEND_KEY
```

See [`terraform/README.md`](terraform/README.md) for local backend setup and Terraform usage.

## Deployment input

`deploy_azure.yml` has one manual input:

| Input | Default | Meaning |
| --- | --- | --- |
| `enable_certbot` | `true` | Whether Ansible should request/configure Let's Encrypt certificates. |

## Safety notes

Important Azure resources are protected with Terraform `prevent_destroy` lifecycle rules. This prevents accidental destruction of the production VM and supporting network resources during automated applies.

Application images are deployed with the commit SHA tag:

```yaml
IMAGE_TAG: ${{ github.sha }}
```

This keeps the deployed service images aligned with the Git commit being deployed.
