# Story 05: Policy-as-Code

**As a** platform engineer  
**I want** automated policy-as-code checks in the pipeline  
**So that** infrastructure and configuration standards are consistently enforced.

## Acceptance Criteria
- [ ] Tooling selected (e.g. OPA/Conftest or similar)
- [ ] Repository contains `policies/` directory with starter rules
- [ ] CI job evaluates policies on PRs and release branches
- [ ] Failed policy evaluation blocks merge
- [ ] Clear developer guidance on fixing violations

## Example Policy Domains
- Branch naming conventions
- Docker base image restrictions
- Disallowed open network ports in compose/k8s specs
- Mandatory labels/annotations in configs
- Secrets not embedded in env files

## Tasks
- [ ] Add `policies/` folder
- [ ] Implement 3–5 initial policies
- [ ] Add CI step (`policy-check.yml`)
- [ ] Document local run (`./scripts/policy-check.sh`)
- [ ] Provide sample failure output in docs

## Implementation Notes
- Keep initial rules lightweight to encourage adoption
- Provide override/waiver mechanism with justification file

## Test Scenarios
- Introduce intentional violation → CI fails
- Fix violation → CI passes
- Waiver file present → violation ignored and logged

## UI / UX Acceptance (Provisional)
- None (pipeline only)

## Traceability
Epic: release-chain-hardening  
Story ID: 5