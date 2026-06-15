output "resource_group_name" {
  description = "Resource group containing the VM resources."
  value       = azurerm_resource_group.main.name
}

output "vm_name" {
  description = "Current VM name."
  value       = azurerm_linux_virtual_machine.vm.name
}

output "public_ip_address" {
  description = "Static public IP assigned to the VM NIC."
  value       = azurerm_public_ip.vm.ip_address
}

output "ssh_command" {
  description = "SSH command for the current VM."
  value       = "ssh ${var.admin_username}@${azurerm_public_ip.vm.ip_address}"
}

