# Story 4.5 - Validate Frontend Allure Output in CI Artifacts

**As a** QA automation engineer  
**I want** frontend CI jobs to publish Allure artifacts for every run  
**So that** reviewers can access reports directly from pull requests.

## Acceptance Criteria
- CI workflow uploads frontend Allure results and merged HTML report as artifacts on PR and main branch builds.
- Workflow surfaces direct download link or summary comment referencing artifact location.
- Failures in artifact upload stage cause pipeline to fail with clear error messaging.
- Artifact retention policy documented (for example keep 30 days or last 20 runs).
- Manual QA checklist updated referencing where to locate frontend artifacts.

## Implementation Guidance
- Extend GitHub Actions workflow to include upload steps after unit and E2E jobs run.
- Optionally generate quick summary (pass rate, failing suites) to post as PR comment using GitHub REST API.
- Ensure artifact size stays within GitHub limits via compression or pruning attachments.

## Testing Strategy
- Dry run workflow on feature branch verifying artifacts accessible to reviewers.
- Automated test or script to validate presence of expected files inside artifact archive.
- Manual verification by QA lead confirming instructions are accurate.

## Dependencies
- Builds on scripts from Story 4.4 and adapters from Stories 4.1 and 4.2.
