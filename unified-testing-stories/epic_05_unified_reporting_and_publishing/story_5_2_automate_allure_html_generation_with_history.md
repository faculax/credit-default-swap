# Story 5.2 - Automate Allure HTML Generation With History Retention

**As a** platform engineer  
**I want** the build pipeline to generate Allure HTML and preserve history data  
**So that** trend charts remain accurate across successive builds.

## Acceptance Criteria
- Pipeline step generates Allure HTML using merged results and copies `history` directory from previous run when available.
- History retention logic ensures files are persisted between builds (for example via artifact download or branch checkout).
- Generated report includes latest trend charts and timeline data without errors.
- Build fails if HTML generation step encounters errors or missing inputs.
- Documentation explains how history retention works and how to recover if history is lost.

## Implementation Guidance
- Use GitHub Actions artifact download or dedicated storage to retrieve prior `history` folder before generating new report.
- Consider storing pointer file with timestamp and commit hash for traceability.
- Ensure scripts handle first run gracefully when no history exists.

## Testing Strategy
- Execute consecutive pipeline runs verifying history persists and charts update.
- Automated test for script handling of missing history directory.
- Manual verification of generated HTML to confirm trend data is visible.

## Dependencies
- Depends on merge process from Story 5.1.
