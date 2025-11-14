# Story 7.2 - Fail CI When Story IDs Lack Tagged Tests

**As a** tech lead  
**I want** CI to fail when a referenced story ID has no associated automated tests  
**So that** we maintain the Definition of Done without manual policing.

## Acceptance Criteria
- CI step reads detected story IDs and traceability matrix (or raw Allure results) to confirm at least one passing test per story.
- Build fails with clear message listing story IDs missing coverage and guidance for remediation.
- Allow configurable override mechanism (approved label or environment variable) for exceptional cases, with audit log of overrides.
- Step integrates into both PR and main branch workflows.
- Documentation updates include Definition of Done requirement and how enforcement behaves.

## Implementation Guidance
- Reuse traceability matrix artifact from Story 1.5 when available, or fall back to parsing Allure results directly.
- Provide exit codes that distinguish between missing coverage and other errors (for example artifact not found).
- Log instructions for running local checks before pushing.

## Testing Strategy
- Unit tests for enforcement logic covering scenarios with coverage, without coverage, and missing artifacts.
- Integration test pipeline verifying failure occurs when tests absent and passes when coverage present.
- Manual review of failure messaging for clarity.

## Dependencies
- Requires Story ID detection (Story 7.1) and traceability matrix generation (Story 1.5).
