# Story 08: Code Quality Gates

**As a** developer  
**I want** enforceable code quality gates in CI  
**So that** maintainability and reliability remain high as the codebase grows.

## Acceptance Criteria
- [ ] Baseline metrics captured (complexity, duplication, coverage)
- [ ] CI fails if thresholds violated (initial soft thresholds documented)
- [ ] Report artifact published for each run
- [ ] Local script available to preview gates
- [ ] Ability to override with approval (temporary waiver)

## Metrics (Initial)
- Test coverage (line + branch)
- Cyclomatic complexity (average & max per module)
- Duplicate code blocks
- Lint error count

## Tasks
- [ ] Integrate tool (e.g. SonarQube local scan or lightweight analyzer)
- [ ] Add `quality-gates.yml` workflow
- [ ] Script: `./scripts/run-quality-gates.sh`
- [ ] Define thresholds in config file
- [ ] Document waiver procedure

## Implementation Notes
- Start with reporting mode then enforce after baseline stabilized
- Exclude generated sources

## Test Scenarios
- Introduce duplication → gate fails
- Reduce coverage below threshold → gate fails
- Waiver file present → gate passes with warning

## UI / UX Acceptance (Provisional)
- None (CI artifact consumption only)

## Traceability
Epic: release-chain-hardening  
Story ID: 8