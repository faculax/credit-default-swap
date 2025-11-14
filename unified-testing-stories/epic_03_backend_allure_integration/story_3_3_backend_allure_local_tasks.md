# Story 3.3 - Provide Gradle or Maven Tasks for Local Allure Generation

**As a** backend engineer  
**I want** simple build commands to run tests and generate Allure HTML locally  
**So that** I can investigate failures without waiting on CI artifacts.

## Acceptance Criteria
- Build tool exposes documented commands (for example `./mvnw test allure:serve` or `./gradlew test allureReport`).
- Commands clean previous artifacts and open or indicate location of generated HTML report.
- Local tasks support selective execution (unit only, integration only) while still producing Allure output.
- README updated with usage instructions and troubleshooting tips.
- Commands verified on Windows, macOS, and Linux environments.

## Implementation Guidance
- Use Allure Maven plugin or Allure Gradle plugin depending on build system.
- Wrap commands in platform friendly scripts if necessary to simplify invocation.
- Ensure tasks fail fast when Allure CLI is missing and provide instructions for installation or automated download.

## Testing Strategy
- Manual run on representative developer machines capturing screenshots or logs.
- Automated smoke job executing local tasks in CI container to ensure deterministic behavior.
- Documentation peer review to confirm clarity.

## Dependencies
- Requires Allure dependencies and standardized result directory from Stories 3.1 and 3.2.
