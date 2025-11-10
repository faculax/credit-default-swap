# Story 01: Linting Integration

**As a** developer  
**I want** Ili's linting to run on PRs and release branches/tags  
**So that** we catch code issues early without exceeding CI quotas.

## Acceptance Criteria

- [ ] GitHub Actions workflow runs linting on all pull requests
- [ ] Linting runs on pushes to release branches (`release/**`)
- [ ] Linting runs on release tags (`v*`)
- [ ] Linting does NOT run on every push to `main` or `develop`
- [ ] Local linting script is documented and easy to run
- [ ] Linting failures block PR merges
- [ ] CI quota usage is tracked and optimized

## Technical Details

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

### Linting Tools
- Java: Checkstyle, SpotBugs
- TypeScript/React: ESLint, Prettier
- Shell scripts: shellcheck
- Markdown: markdownlint

### Local Run Command
```bash
./scripts/run-lint.sh
```

## Tasks

- [ ] Create `.github/workflows/lint.yml`
- [ ] Create `scripts/run-lint.sh` for local execution
- [ ] Configure lint rules in project root
- [ ] Document linting process in `README.md`
- [ ] Add pre-commit hook template (optional)

## Dependencies

- None

## Notes

- Consider caching dependencies to speed up CI runs
- Exclude generated code and vendor directories from linting

## Traceability
Epic: release-chain-hardening  
Story ID: 1
