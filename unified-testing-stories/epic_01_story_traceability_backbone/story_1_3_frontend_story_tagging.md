# Story 1.3 - Frontend Test Tagging and Decorators

**As a** frontend engineer  
**I want** lightweight helpers to tag Jest and E2E specs with story identifiers  
**So that** the Allure report reflects coverage for each user story across UI flows.

## Acceptance Criteria
- Helper function or decorator added that wraps Jest and E2E test definitions with story metadata.
- Decorator applies both story identifier and test type labels in Allure output.
- Representative unit, integration, and E2E tests updated to use helper.
- Allure results generated from frontend suites display story IDs and remain mergeable with backend outputs.
- Frontend contributor guide updated to show tagging examples for component and E2E tests.

## Implementation Guidance
- Implement decorator in a shared utility package imported by unit and E2E runners.
- Ensure compatibility with TypeScript typings and linting rules.
- Provide backwards-compatible defaults to ease migration of existing tests.

## Testing Strategy
- Run sample unit and E2E suites verifying Allure JSON contains expected labels.
- Snapshot or automated assertion ensuring helper adds metadata even when nested describe blocks are used.
- Manual Allure UI review confirming frontend tests appear when filtering by story ID.

## Dependencies
- Requires canonical story identifier format from Story 1.1.
