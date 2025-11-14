# Story 1.2 - Backend Test Tagging and Allure Story Labels

**As a** backend engineer  
**I want** convenient annotations to tag JUnit tests with story identifiers  
**So that** Allure reports show which backend tests satisfy each backlog item.

## Acceptance Criteria
- Shared annotation or helper introduced (for example `@StoryId("PROJ-123")`) mapping to Allure story labels.
- Annotation automatically applies both story identifier and test type label (unit, integration, contract) when possible.
- Existing representative backend tests updated to use new annotation pattern.
- Allure results for backend suites display linked story IDs in the report UI.
- Documentation added describing how to apply the annotation and available test type options.

## Implementation Guidance
- Package shared annotation in a reusable module or parent POM to distribute across services.
- Ensure annotation works for parameterized tests and nested test classes.
- Provide examples for unit, integration, and contract suites.

## Testing Strategy
- Automated test verifying annotation emits expected Allure labels (inspect generated JSON from sample run).
- Manual Allure report review confirming story ID filter works.
- Regression checks on existing tests to ensure no failures introduced by new annotation.

## Dependencies
- Requires canonical story identifier format from Story 1.1.
