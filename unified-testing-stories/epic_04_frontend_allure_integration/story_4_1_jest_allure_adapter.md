# Story 4.1 - Configure Allure Adapter for Jest Unit Tests

**As a** frontend developer  
**I want** the Jest test runner to emit Allure results automatically  
**So that** UI unit tests contribute to the unified quality reports.

## Acceptance Criteria
- Allure Jest adapter installed and configured in test setup files.
- Running `npm test` or equivalent produces Allure result files in agreed directory.
- Story and test type decorators from Epic 01 applied successfully within Jest environment.
- Adapter configuration handles TypeScript sources and source maps without breaking coverage.
- Documentation updated with setup details and troubleshooting tips.

## Implementation Guidance
- Add Allure reporter via Jest config `reporters` array or setup script.
- Ensure results directory aligns with standards defined in Epic 02.
- Provide helper module exporting common decorators for reuse in tests.

## Testing Strategy
- Execute sample unit suite verifying Allure JSON contains status, attachments, and labels.
- Confirm watch mode continues to function without excessive noise.
- Manual Allure report review to validate unit tests appear under correct service and type filters.

## Dependencies
- Requires tagging helpers from Story 1.3 and directory conventions from Story 2.2.
