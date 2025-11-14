# Story 1.4 - Unified Label Conventions Across Test Types

**As a** platform engineer  
**I want** a single source of truth for mapping test types and services to Allure labels  
**So that** reports remain consistent regardless of which runner produces the results.

## Acceptance Criteria
- Central configuration file enumerates valid test types (unit, integration, contract, e2e) and service identifiers.
- Backend and frontend tagging helpers consume the same configuration or package.
- Validation step checks that every test label emitted belongs to the approved list and fails builds otherwise.
- Documentation includes table of allowed labels and guidance for adding new ones through change control.
- Sample merged Allure report demonstrates consistent labeling across at least two services.

## Implementation Guidance
- Publish configuration as JSON or YAML consumed by both Node and JVM tooling, or generate language specific artifacts from a single template.
- Provide utilities to validate labels during test execution or as part of artifact post-processing.
- Establish review process for expanding label taxonomy.

## Testing Strategy
- Automated tests verifying configuration is loaded correctly in both frontend and backend environments.
- Integration test running a subset of suites and asserting that merged Allure output contains only approved labels.
- Manual report review confirming label filters behave as expected.

## Dependencies
- Builds on tagging helpers from Stories 1.2 and 1.3.
