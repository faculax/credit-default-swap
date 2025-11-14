# Story 6.3 - Implement Frontend Test Jobs With Allure Artifact Uploads

**As a** frontend platform engineer  
**I want** CI jobs that run frontend unit, integration, and E2E suites with Allure artifacts  
**So that** UI regressions are visible to reviewers immediately.

## Acceptance Criteria
- CI job executes unit and integration tests headless with Allure output collected.
- E2E tests executed with necessary browser dependencies or containerized environment.
- Jobs upload Allure results, screenshots, and videos as artifacts with retention policy documented.
- Pipeline fails when tests fail or artifacts missing, including clear error messages for flaky steps.
- Job logs include summary of failing suites and direct links to artifacts.

## Implementation Guidance
- Use Playwright or Cypress provided actions to install browsers and dependencies.
- Configure caching for npm or pnpm dependencies to speed up builds.
- Optionally split unit and E2E into separate jobs to parallelize and isolate failures.

## Testing Strategy
- Dry run job on feature branch verifying artifacts accessible and complete.
- Manual Allure report check to ensure attachments accessible.
- Monitor job duration and adjust timeouts or parallelism to meet SLA.

## Dependencies
- Depends on frontend Allure integration from Epic 04 and workflow topology from Story 6.1.
