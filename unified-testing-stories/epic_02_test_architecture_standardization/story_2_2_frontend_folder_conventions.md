# Story 2.2 - Define Frontend Test Folder and Naming Conventions

**As a** frontend chapter lead  
**I want** consistent directories and naming patterns for unit, integration, and E2E tests  
**So that** scripts and contributors can quickly locate and run the desired suites.

## Acceptance Criteria
- Standard layout documented (for example `src/__tests__/unit`, `src/__tests__/integration`, `e2e`).
- npm scripts updated to target each directory explicitly.
- Jest and E2E runner configurations updated to include/exclude files based on naming convention.
- Example components migrated to new structure to demonstrate the pattern.
- Lint or CI check prevents misplacement of new tests outside approved folders.

## Implementation Guidance
- Configure Jest `testMatch` patterns and TypeScript path aliases for new directories.
- Update E2E runner config (Cypress or Playwright) to look for specs in `e2e` folder.
- Provide code mod or script to help teams move existing specs with minimal friction.

## Testing Strategy
- Execute unit, integration, and E2E npm scripts verifying only expected tests run.
- Snapshot documentation to ensure directory diagrams render correctly.
- Manual review of sample PR ensuring developer workflow remains smooth.

## Dependencies
- None.
