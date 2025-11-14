# Epic 04 - Frontend Allure Integration

## Overview
Connects the React test toolchain to Allure so component, integration, and E2E suites emit the same rich telemetry and can be merged with backend results.

## Business Value
- Offers visual regression and UI flow diagnostics in the same portal as API suites.
- Makes it trivial to drill into flaky UI flows by story ID or feature tag.
- Harmonizes frontend and backend quality reporting for stakeholders.

## Scope
In Scope:
- Install and configure Allure adapters for Jest and the selected E2E framework.
- Normalize result directories and npm scripts for generating and serving reports.
- Ensure story and test type labels survive transpilation and bundling.
Out of Scope:
- Conversion to a new E2E tool; work assumes existing framework (Cypress or Playwright).
- Visual baseline management (tracked separately).

## Domain Terms
| Term | Definition |
|------|------------|
| Allure Adapter | Library that bridges the test runner with Allure output format. |
| Reporter Script | CLI command that converts raw results into an HTML report. |
| Label Decorator | Helper that attaches metadata (story, severity, tag) to tests. |

## Core Flow
Enable adapters in unit and E2E runners -> Execute tests producing Allure results -> Merge outputs across runners -> Serve combined HTML locally or ship to CI artifact.

## Stories
- Story 4.1 - Configure Allure Adapter for Jest Unit Tests
- Story 4.2 - Configure Allure Adapter for E2E Runner
- Story 4.3 - Create Shared Label Decorators for Story and Test Type Metadata
- Story 4.4 - Harmonize Frontend Allure Results Directory and npm Scripts
- Story 4.5 - Validate Frontend Allure Output in CI Artifacts

## Acceptance Criteria Mapping
| Story | Theme | Key Acceptance |
|-------|-------|----------------|
| 4.1 | Unit | Jest test run emits Allure results with test name, status, attachments. |
| 4.2 | E2E | Cypress/Playwright run produces Allure output with screenshots/video attachments. |
| 4.3 | Metadata | Helper functions ensure every test logs story ID and type labels without repeated boilerplate. |
| 4.4 | Tooling | npm scripts `test:unit:report`, `test:e2e:report`, and `test:report:merge` documented and functional. |
| 4.5 | CI | Frontend workflows upload Allure results as artifacts and expose download link in PR checks. |

## Quality Approach
- Run sample unit and E2E suites verifying Allure JSON schema compliance.
- Visual inspection of generated HTML ensuring attachments and labels display correctly.
- Regression guard that fails if adapters stop emitting results.

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Adapter incompatibility with current Jest version | Pin compatible versions and add upgrade guide for future releases. |
| Large E2E artifacts slowing CI | Enable artifact retention limits and compress attachments. |
| Developers forget to apply decorators | Wrap test helpers or configure global setup that enforces default labels. |
