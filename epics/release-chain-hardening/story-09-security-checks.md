# Story 09: Security Checks

**As a** security engineer  
**I want** automated security checks embedded in the pipeline  
**So that** vulnerabilities and misconfigurations are caught before release.

## Acceptance Criteria
- [ ] Dependency scanning integrated (e.g. OWASP, Snyk, or equivalent)
- [ ] Container image scan runs on build
- [ ] SBOM generated and stored
- [ ] Critical/high severity findings block merge
- [ ] Suppression mechanism with justification supported

## Checks (Initial)
- Dependency CVEs
- Container base image vulnerabilities
- Secret leakage (reinforced from Story 02)
- Insecure TLS usage (config lint)

## Tasks
- [ ] Add `security-scan.yml` workflow
- [ ] Script: `./scripts/run-security-scan.sh`
- [ ] Configure severity thresholds
- [ ] Add suppression/allowlist file
- [ ] Document remediation flow

## Implementation Notes
- Consolidate reports into single artifact for ease of triage
- Keep scan time under 5 minutes initially

## Test Scenarios
- Introduce known vulnerable dependency → scan flags
- Add allowed suppression → scan passes with note
- Remove suppression → scan fails

## UI / UX Acceptance (Provisional)
- None (pipeline-only feedback)

## Traceability
Epic: release-chain-hardening  
Story ID: 9