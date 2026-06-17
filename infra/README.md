# Infrastructure deployment

This directory contains the Terraform and Ansible configuration used by the GitHub Actions deployment workflow.

The production deployment entrypoint is:

```text
.github/workflows/deploy_azure.yml
```

It provisions/updates the Azure VM infrastructure with Terraform and then deploys the application to the VM with Ansible.


## Required GitHub secrets

The deployment workflow depends on the following repository or environment secrets.

| Secret | Used for |
| --- | --- |
| `AZURE_CREDENTIALS` | Azure service principal credentials used by `azure/login`. |
| `AZURE_TF_BACKEND_RESOURCE_GROUP` | Azure resource group containing the Terraform remote state storage account. |
| `AZURE_TF_BACKEND_STORAGE_ACCOUNT` | Azure Storage Account used for Terraform remote state. |
| `AZURE_TF_BACKEND_CONTAINER` | Blob container used for Terraform remote state. |
| `AZURE_TF_BACKEND_KEY` | Blob key/path for the Terraform state file. |
| `AZURE_VM_SSH_PUBLIC_KEY` | Public SSH key injected into the Azure VM by Terraform. |
| `AZURE_VM_SSH_PRIVATE_KEY` | Private SSH key used by Ansible to connect to the VM. |
| `CERTBOT_EMAIL` | Email address used for Let's Encrypt certificate registration when Certbot is enabled. |
| `THESIS_DB_PASSWORD` | Production password for the thesis PostgreSQL database. |
| `VECTOR_DB_PASSWORD` | Production password for the vector-search PostgreSQL database. |
| `AZURE_OPENAI_API_KEY` | API key used by the vector-search service for embeddings. |
| `OPENAI_API_KEY` | API key used by the GenAI service when the default OpenAI provider is used. |

The workflow also uses the built-in `GITHUB_TOKEN` as `GHCR_TOKEN` so the VM can pull GitHub Container Registry images.

## Workflow input

The workflow has one manual input:

| Input | Default | Meaning |
| --- | --- | --- |
| `enable_certbot` | `true` | Whether Ansible should request/configure Let's Encrypt certificates. |

If `enable_certbot` is `true`, `CERTBOT_EMAIL` must be set.

## Terraform backend

Terraform state is stored remotely in Azure Blob Storage. The backend storage account/container must already exist before the workflow runs.

The backend is configured at runtime from these secrets:

```text
AZURE_TF_BACKEND_RESOURCE_GROUP
AZURE_TF_BACKEND_STORAGE_ACCOUNT
AZURE_TF_BACKEND_CONTAINER
AZURE_TF_BACKEND_KEY
```

See [`terraform/README.md`](terraform/README.md) for local Terraform setup and backend bootstrap commands.

## Image tags

Application images are built by the CI workflow and pushed to GitHub Container Registry.

The deployment should use a commit SHA image tag so all services are deployed from the same repository revision. The reusable image build workflow already publishes SHA tags.

## Infrastructure deletion protection

Important Azure resources use Terraform lifecycle protection:

```hcl
lifecycle {
  prevent_destroy = true
}
```

This is intentional to prevent accidental deletion/replacement of the production VM and its supporting networking resources during automated deployment.

## Local operation

- Terraform details: [`terraform/README.md`](terraform/README.md)
- Ansible details: [`ansible/README.md`](ansible/README.md)
