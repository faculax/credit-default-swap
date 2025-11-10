package k8s

deny[msg] {
  input.kind == "Pod"
  container := input.spec.containers[_]
  container.securityContext.allowPrivilegeEscalation == true
  msg = sprintf("container %v allows privilege escalation", [container.name])
}
