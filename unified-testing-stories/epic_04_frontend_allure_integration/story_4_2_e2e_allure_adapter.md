# Story 4.2 - Configure Allure Adapter for E2E Runner

**As a** QA engineer  
**I want** the E2E framework (Cypress or Playwright) integrated with Allure  
**So that** end-to-end flows contribute rich evidence to the unified report.

## Acceptance Criteria
- Allure plugin installed and configured for chosen E2E framework.
- Running `npm run test:e2e` (or equivalent) produces Allure result files including screenshots and video references.
- Story and test type decorators applied consistently to E2E specs.
- Test artifacts (screenshots, videos) linked within Allure report without broken references.
- Documentation updated showing how to run E2E tests locally with Allure output.

## Implementation Guidance
- For Cypress, configure `@shelex/cypress-allure-plugin`; for Playwright, use `allure-playwright` reporter.
- Ensure environment variables for attachable assets configured correctly in CI and local runs.
- Provide helper to auto-tag specs based on directory or metadata block.

## Testing Strategy
- Execute sample E2E test locally and in CI verifying attachments appear in Allure UI.
- Validate reruns overwrite prior artifacts cleanly to avoid confusion.
- Manual review of Allure report to confirm E2E tests display under correct filters.

## Dependencies
- Depends on tagging helpers from Story 1.3 and directory conventions from Story 2.2.
