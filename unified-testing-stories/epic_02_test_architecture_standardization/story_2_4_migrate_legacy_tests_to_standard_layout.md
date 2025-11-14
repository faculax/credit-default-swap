# Story 2.4 - Migrate Legacy Tests Into Standard Layout

**As a** squad tech lead  
**I want** existing backend and frontend tests relocated into the new standardized structure  
**So that** the repository is consistent and automation scripts operate without exceptions.

## Acceptance Criteria
- Inventory compiled of all tests currently outside the approved directory structure.
- Legacy tests moved to new locations without losing history (git mv) to preserve blame.
- Build tooling and imports updated to reflect new paths.
- CI pipeline passes for migrated services with no skipped tests.
- Checklist created for squads to verify their migration readiness and completion.

## Implementation Guidance
- Use scripted migration to reduce manual errors and communicate expected effort per service.
- Coordinate migrations per service to avoid conflicts with in-flight feature branches.
- Update codeowners or review rules if directory paths change.

## Testing Strategy
- Run full backend and frontend test suites post-migration to confirm parity.
- Spot check Allure results to ensure labels and attachments still appear.
- Monitor CI for any new flaky tests introduced by path changes.

## Dependencies
- Requires directory standards from Stories 2.1 and 2.2.
