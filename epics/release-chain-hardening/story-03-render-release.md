# Story 03: Render on Release Only

**As a** maintainer  
**I want** documentation rendering to trigger only on manual release or release branches  
**So that** we avoid unnecessary CI/CD usage and quota consumption.

## Acceptance Criteria

- [ ] Documentation/diagram rendering triggers on manual workflow dispatch
- [ ] Rendering triggers on pushes to release branches (`release/**`)
- [ ] Rendering triggers on release tags (`v*`)
- [ ] Rendering does NOT trigger on pushes to `main`, `develop`, or feature branches
- [ ] Rendered artifacts are committed back to the repo
- [ ] Render process includes proper versioning/timestamps

## Technical Details

### Current Workflow
The `generate-schema-diagram.yml` workflow currently runs on:
- Pushes to `main`, `develop`, `data-model`
- Changes to migration files or workflow itself

### New Trigger Configuration
```yaml
on:
  workflow_dispatch:  # Manual trigger
  push:
    branches:
      - 'release/**'
    tags:
      - 'v*'
```

### Workflows to Update
- `.github/workflows/generate-schema-diagram.yml`
- Any other doc generation workflows (if applicable)

## Tasks

- [ ] Update `generate-schema-diagram.yml` trigger conditions
- [ ] Test manual workflow dispatch
- [ ] Test automatic trigger on release branch
- [ ] Document the new release process
- [ ] Update `AGENTS.md` or `CONTRIBUTING.md` with release workflow

## Dependencies

- None

## Notes

- This change will significantly reduce CI/CD quota usage
- Developers can still manually trigger the workflow for testing
- Consider creating a `release-checklist.md` that includes running this workflow

## Traceability
Epic: release-chain-hardening  
Story ID: 3
