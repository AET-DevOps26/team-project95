# Azure + Terraform commands used in this setup


## Terraform backend/init

```bash
cd infra/terraform

terraform init \
  -backend-config="resource_group_name=resource-project95" \
  -backend-config="storage_account_name=ge95pad" \
  -backend-config="container_name=terraform" \
  -backend-config="key=project95/prod.tfstate"

terraform init -reconfigure \
  -backend-config="resource_group_name=resource-project95" \
  -backend-config="storage_account_name=ge95pad" \
  -backend-config="container_name=terraform" \
  -backend-config="key=project95/prod.tfstate"

```
## Terraform validation/planning/apply

```bash
terraform fmt -recursive
terraform validate
terraform plan
terraform apply
terraform apply -auto-approve

terraform apply -auto-approve \
  -var="location=italynorth" \
  -var="vm_size=Standard_B1ms"

terraform apply -auto-approve \
  -var="location=$region" \
  -var="vm_size=$size"

```
## Terraform destroy/reset

```bash
terraform destroy
terraform destroy -auto-approve
terraform destroy -target=azurerm_network_interface.vm -auto-approve

```
## Terraform state

```bash
terraform state list
terraform state show azurerm_linux_virtual_machine.vm
terraform state rm azurerm_resource_group.main
terraform state rm azurerm_subnet.main
terraform state rm azurerm_linux_virtual_machine.vm

terraform force-unlock <LOCK_ID>
terraform force-unlock -force <LOCK_ID>

```
Example lock ID used:

```bash
terraform force-unlock 7c9aa23a-24ad-4667-2f5c-52955cad79b8

```
## Terraform imports

```bash
terraform import azurerm_virtual_network.main \
  "/subscriptions/06891234-d2bc-4e5e-8200-e256e78b11c8/resourceGroups/rg-project95-prod/providers/Microsoft.Network/virtualNetworks/vnet-project95-prod"

terraform import azurerm_subnet.main \
  "/subscriptions/06891234-d2bc-4e5e-8200-e256e78b11c8/resourceGroups/rg-project95-prod/providers/Microsoft.Network/virtualNetworks/vnet-project95-prod/subnets/snet-project95-prod"

terraform import azurerm_subnet_network_security_group_association.main \
  "/subscriptions/06891234-d2bc-4e5e-8200-e256e78b11c8/resourceGroups/rg-project95-prod/providers/Microsoft.Network/virtualNetworks/vnet-project95-prod/subnets/snet-project95-prod"

```
## Terraform outputs

```bash
terraform output
terraform output public_ip_address
terraform output -raw public_ip_address
terraform output -raw ssh_command

```
## Azure login/account

```bash
az login
az account show
az account set --subscription "<subscription-id>"

```
## Azure backend/state storage bootstrap examples

```bash
az group create --name <state-rg> --location westeurope

az storage account create \
  --name <state-storage-account> \
  --resource-group <state-rg> \
  --location westeurope \
  --sku Standard_LRS

az storage container create \
  --name tfstate \
  --account-name <state-storage-account>

```
## Delete remote state blob, hard reset only

```bash
az storage blob delete \
  --account-name ge95pad \
  --container-name terraform \
  --name project95/prod.tfstate \
  --auth-mode login

```
## Azure inspect resources

```bash
az group show --name rg-project95-prod
az group show --name rg-project95-prod -o table
az group show --name rg-project95-prod --query properties.provisioningState -o tsv

az resource list --resource-group rg-project95-prod -o table

```
## Azure delete resources manually

```bash
az group delete \
  --name rg-project95-prod \
  --yes \
  --no-wait

az group wait \
  --name rg-project95-prod \
  --deleted

az vm delete \
  --resource-group rg-project95-prod \
  --name vm-project95-prod \
  --yes

az network nic delete \
  --resource-group rg-project95-prod \
  --name nic-project95-prod-vm

```
## Azure VM operational commands

```bash
az vm deallocate \
  --resource-group rg-project95-prod \
  --name vm-project95-prod

az vm start \
  --resource-group rg-project95-prod \
  --name vm-project95-prod

az vm get-instance-view \
  --resource-group rg-project95-prod \
  --name vm-project95-prod \
  --query "instanceView.statuses[?starts_with(code, 'PowerState/')].displayStatus" \
  -o tsv

```
## Azure check VM existence/disks

```bash
az vm show \
  --resource-group rg-project95-prod \
  --name vm-project95-prod \
  --query id -o tsv

```
## Azure check subnet/NSG association

```bash
az network vnet subnet show \
  --resource-group rg-project95-prod \
  --vnet-name vnet-project95-prod \
  --name snet-project95-prod \
  --query networkSecurityGroup.id \
  -o tsv

az network vnet subnet show \
  --resource-group rg-project95-prod \
  --vnet-name vnet-project95-prod \
  --name snet-project95-prod \
  --query id \
  -o tsv

```
## Azure list regions/SKUs/quota

```bash
az account list-locations \
  --query "[?metadata.geographyGroup=='Europe'].{name:name,displayName:displayName}" \
  -o table

az vm list-skus \
  --location francecentral \
  --resource-type virtualMachines \
  --all \
  --query "[?restrictions==null].name" \
  -o table

az vm list-skus \
  --location westeurope \
  --resource-type virtualMachines \
  --size Standard_B1ms \
  --query "[].{name:name, zones:locationInfo[0].zones, restrictions:restrictions}" \
  -o table

az vm list-skus \
  --location westeurope \
  --resource-type virtualMachines \
  --size Standard_B \
  --query "[?contains(name, 'Standard_B1') || contains(name, 'Standard_B2')].{name:name, restrictions:restrictions}" \
  -o table

az vm list-usage --location westeurope -o table

```
## Azure policy inspection

```bash
SUB=$(az account show --query id -o tsv)

az policy assignment list \
  --scope "/subscriptions/$SUB" \
  --query "[].{name:displayName, parameters:parameters}" \
  -o json

az policy assignment list \
  --scope "/subscriptions/$SUB" \
  --query "[?contains(displayName, 'location') || contains(displayName, 'Location')].{name:displayName, parameters:parameters}" \
  -o json

```
## SSH key generation

```bash
ssh-keygen -t ed25519 -f ~/.ssh/azure_vm_key -C "azure-vm"

ssh-keygen -t rsa -b 4096 -f ~/.ssh/project95_vm_key -C "project95-vm"

openssl genpkey -algorithm RSA -out azure_vm_key.pem -pkeyopt rsa_keygen_bits:4096
chmod 600 azure_vm_key.pem
openssl rsa -in azure_vm_key.pem -pubout -out azure_vm_key_public.pem
ssh-keygen -y -f azure_vm_key.pem > azure_vm_key.pub

```
Show public key:

```bash
cat ~/.ssh/azure_vm_key.pub
cat ~/.ssh/project95_vm_key.pub

```
## SSH into VM

```bash
ssh -i ~/.ssh/azure_vm_key azureuser@20.215.191.148
ssh -i ~/.ssh/project95_vm_key azureuser@<public-ip>
ssh azureuser@20.215.191.148

```
## Misc/local checks

```bash
curl ifconfig.me
ps aux | grep terraform
chmod +x try-apply.sh
./try-apply.sh

```
## Typo seen/correct command

Wrong:

```bash
terraform list state

```
Correct:

```bash
terraform state list
```
