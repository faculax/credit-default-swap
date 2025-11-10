# Story 02: Secrets & SAST Checks

**As a** developer  
**I want** secret scanning and SAST to run in CI  
**So that** we prevent credential leaks and vulnerabilities before release.

## Acceptance Criteria

- [ ] Secret scanning runs on all pull requests and release branches
- [ ] SAST (Static Application Security Testing) runs on PRs and release branches
- [ ] Detected secrets block PR merges
- [ ] Critical/high severity SAST findings block PR merges
- [ ] Developers receive clear remediation guidance
- [ ] False positives can be suppressed with justification
- [ ] Local scan scripts are available

## Technical Details

### Secret Scanning Tools
- **gitleaks** - detects hardcoded secrets, API keys, tokens
- **trufflehog** - finds secrets in git history

### SAST Tools
- **Java:** SpotBugs Security plugin, Semgrep
- **TypeScript:** ESLint security plugins, Semgrep
- **Dependencies:** OWASP Dependency-Check, Snyk

### GitHub Actions Trigger
```yaml
on:
  pull_request:
  push:
    branches:
      - 'release/**'
    tags:
      - 'v*'
```

### Local Run Commands
```bash
# Secret scanning
./scripts/scan-secrets.sh

# SAST
./scripts/run-sast.sh
```

## Tasks

- [ ] Create `.github/workflows/secrets-sast.yml`
- [ ] Integrate gitleaks for secret scanning
- [ ] Add SAST tools for Java and TypeScript
- [ ] Configure severity thresholds
- [ ] Create suppression/allowlist mechanism
- [ ] Document scanning process
- [ ] Create local scan scripts

## Dependencies

- Story 01 (Linting Integration) - shares similar CI structure

## Notes

- Scan should run in parallel with linting to save time
- Consider using GitHub's native secret scanning if available
- Store SAST results as artifacts for review

## Traceability
Epic: release-chain-hardening  
Story ID: 2
