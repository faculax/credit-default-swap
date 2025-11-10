package terraform

deny[msg] {
  resource := input.resource_changes[_]
  resource.type == "aws_security_group_rule"
  resource.change.after.from_port == 0
  resource.change.after.to_port == 0
  resource.change.after.cidr_blocks[_] == "0.0.0.0/0"
  msg = sprintf("Security group rule allows public access (resource: %v)", [resource.address])
}
