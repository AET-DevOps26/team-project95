variable "project_name" {
  description = "Short project name used as prefix for Azure resources."
  type        = string
  default     = "project95"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "prod"
}

variable "location" {
  description = "Azure region in which resources are created."
  type        = string
  default     = "francecentral"
}

variable "deployment_id" {
  description = "Stable VM suffix. Keep this unchanged to keep using the same VM. Changing it would require VM replacement and is blocked by prevent_destroy."
  type        = string
  default     = "001"
}

variable "admin_username" {
  description = "Admin user created on the VM."
  type        = string
  default     = "azureuser"
}

variable "admin_ssh_public_key" {
  description = "SSH public key allowed to log in as admin_username."
  type        = string
  sensitive   = true
}

variable "vm_size" {
  description = "Azure VM size."
  type        = string
  default     = "Standard_D2s"
}

variable "ssh_source_address_prefixes" {
  description = "CIDR ranges allowed to access inbound SSH port 22. Use your IP/CIDR in production instead of the default."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "web_source_address_prefixes" {
  description = "CIDR ranges allowed to access inbound HTTP/HTTPS ports 80 and 443."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "os_disk_size_gb" {
  description = "OS disk size in GiB."
  type        = number
  default     = 30
}

variable "data_disk_size_gb" {
  description = "Persistent independent data disk size in GiB."
  type        = number
  default     = 64
}

variable "data_disk_lun" {
  description = "LUN used to attach the persistent data disk to the VM."
  type        = number
  default     = 0
}

variable "tags" {
  description = "Tags applied to all resources."
  type        = map(string)
  default     = {}
}
