# Story 4.4 - Harmonize Frontend Allure Results Directory and npm Scripts

**As a** frontend build engineer  
**I want** standardized npm scripts and output paths for Allure results  
**So that** local runs and CI pipelines behave identically across machines.

## Acceptance Criteria
- npm scripts defined for `test:unit:report`, `test:e2e:report`, and `test:report:merge` with documented behavior.
- Scripts clean previous results, run tests, and place Allure artifacts in standardized directory (for example `allure-results`).
- Merge script combines unit and E2E outputs without losing attachments or labels.
- Scripts work on Windows PowerShell and Unix shells.
- README updated with usage instructions and expected outputs.

## Implementation Guidance
- Use Node scripts or cross-platform tooling (for example `rimraf`, `cross-env`) to handle cleanup and env vars.
- Consider using Allure CLI for merge or write lightweight Node script that deduplicates attachments.
- Integrate scripts into CI workflow defined in Epic 06.

## Testing Strategy
- Run scripts locally on at least two operating systems verifying artifacts appear as expected.
- CI dry run ensuring merge script uploads combined artifact successfully.
- Manual inspection of Allure report generated from merged results.

## Dependencies
- Requires adapters and decorators from Stories 4.1 through 4.3 and directory standards from Story 2.2.
