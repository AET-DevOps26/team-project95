terraform {
  # Remote state is stored in Azure Blob Storage.
  # Configure the concrete values during init, for example:
  # terraform init \
  #   -backend-config="resource_group_name=<state-rg>" \
  #   -backend-config="storage_account_name=<state-storage-account>" \
  #   -backend-config="container_name=<state-container>" \
  #   -backend-config="key=<project>/<environment>.tfstate"
  backend "azurerm" {}
}
