# Story 3.1 - Introduce Allure Dependencies and Plugins for Spring Boot Services

**As a** backend maintainer  
**I want** Allure adapters configured in each Spring Boot service  
**So that** backend tests automatically emit rich result artifacts without manual setup.

## Acceptance Criteria
- Parent POM or Gradle convention plugin includes Allure JUnit 5 dependencies and configuration shared by all services.
- Each service builds successfully with new dependencies and no conflicts with existing test tooling.
- Running backend tests produces Allure result files in the agreed output directory.
- Documentation updated to outline dependency versions and upgrade process.
- Sample service demonstrates configuration in action.

## Implementation Guidance
- Prefer dependency management in parent POM or shared Gradle plugin to avoid duplication.
- Configure Allure annotations or extensions globally via JUnit platform where possible.
- Ensure compatibility with current versions of JUnit, Mockito, and Spring extensions.

## Testing Strategy
- Execute unit tests on at least two services verifying Allure result files are generated.
- Static analysis or build check confirming dependency versions align with security requirements.
- Manual inspection of generated results to ensure metadata fields populate correctly.

## Dependencies
- None.
