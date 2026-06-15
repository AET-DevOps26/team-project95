# Azure Terraform infrastructure

This Terraform stack manages the long-lived Azure VM environment for the project.

It creates and keeps:

- one Ubuntu 22.04 Linux VM
- inbound TCP ports `22`, `80`, and `443`
- one static Azure Public IP
- basic networking: resource group, VNet, subnet, NIC, NSG
- remote Terraform state in Azure Blob Storage

## Current operating model

Terraform manages one Ubuntu 22.04 VM and the supporting Azure resources.
The VM can be intentionally recreated when the OS image or VM definition changes, while the static public IP and NIC are kept stable.

The current approach is:

1. Terraform creates the VM and supporting Azure resources.
2. Terraform keeps managing the infrastructure definition.
3. Ansible/application deployment updates the VM.
4. The VM may be stopped/deallocated with Azure CLI when not needed.
5. The static public IP is kept stable across VM recreation.

Do **not** change `location`, resource names, or disk/network names casually. These changes can force replacement of protected resources.

## Protected resources

Several important resources use:

```hcl
lifecycle {
  prevent_destroy = true
}
```

This is intentional. It protects against accidental deletion of:

- resource group
- virtual network
- subnet
- public IP
- network interface

The VM itself is not protected by `prevent_destroy`, so intentional OS image or VM definition changes can recreate the VM while keeping the protected network and data resources.

Because of the protected supporting resources, `terraform destroy` is not part of the normal workflow for this project.

If you really need to destroy/recreate the whole environment, remove the relevant `prevent_destroy` blocks first and understand that the public IP may be lost.

## Remote state backend

Terraform state is stored in Azure Blob Storage.

The backend storage account/container must already exist before `terraform init` runs. This backend is separate from the VM infrastructure managed by this stack.

Example bootstrap with Azure CLI:

```bash
az group create --name <state-rg> --location <region>
az storage account create \
  --name <state-storage-account> \
  --resource-group <state-rg> \
  --location <region> \
  --sku Standard_LRS
az storage container create \
  --name <state-container> \
  --account-name <state-storage-account>
```

Initialize Terraform:

```bash
cd infra/terraform
terraform init \
  -backend-config="resource_group_name=<state-rg>" \
  -backend-config="storage_account_name=<state-storage-account>" \
  -backend-config="container_name=<state-container>" \
  -backend-config="key=project95/prod.tfstate"
```

If backend settings change:

```bash
terraform init -reconfigure \
  -backend-config="resource_group_name=<state-rg>" \
  -backend-config="storage_account_name=<state-storage-account>" \
  -backend-config="container_name=<state-container>" \
  -backend-config="key=project95/prod.tfstate"
```

## Configuration

Create a local variables file:

```bash
cp terraform.tfvars.example terraform.tfvars
```

Then edit `terraform.tfvars`.

Important values:

```hcl
project_name = "project95"
environment  = "prod"
location     = "polandcentral"

admin_username       = "azureuser"
admin_ssh_public_key = "ssh-rsa AAAA..."

vm_size = "Standard_D2s_v3"
```

The VM image version is pinned in `main.tf` under `source_image_reference`.

`terraform.tfvars` is ignored by Git and must not be committed.

## Normal workflow

```bash
cd infra/terraform
terraform fmt -recursive
terraform validate
terraform plan
terraform apply
```

After apply:

```bash
terraform output -raw public_ip_address
terraform output -raw ssh_command
```

SSH example:

```bash
ssh -i ~/.ssh/azure_vm_key azureuser@<public-ip>
```

## Stop/start the VM

Use Azure CLI for operational power state. Terraform should not be used to stop/start the VM.

Deallocate the VM to stop compute billing:

```bash
az vm deallocate \
  --resource-group rg-project95-prod \
  --name vm-project95-prod
```

Start it again:

```bash
az vm start \
  --resource-group rg-project95-prod \
  --name vm-project95-prod
```

Check power state:

```bash
az vm get-instance-view \
  --resource-group rg-project95-prod \
  --name vm-project95-prod \
  --query "instanceView.statuses[?starts_with(code, 'PowerState/')].displayStatus" \
  -o tsv
```

Running `terraform apply` after deallocating the VM should not start it again, because VM power state is operational state, not Terraform-managed desired configuration.

## If resources already exist but are missing from state

Import them instead of recreating them.

Examples:

```bash
terraform import azurerm_virtual_network.main \
  /subscriptions/<sub-id>/resourceGroups/rg-project95-prod/providers/Microsoft.Network/virtualNetworks/vnet-project95-prod

terraform import azurerm_subnet.main \
  /subscriptions/<sub-id>/resourceGroups/rg-project95-prod/providers/Microsoft.Network/virtualNetworks/vnet-project95-prod/subnets/snet-project95-prod

terraform import azurerm_subnet_network_security_group_association.main \
  /subscriptions/<sub-id>/resourceGroups/rg-project95-prod/providers/Microsoft.Network/virtualNetworks/vnet-project95-prod/subnets/snet-project95-prod
```

Then run:

```bash
terraform plan
```

Only apply if the plan is safe and does not destroy protected resources.
