# Story 3.2 - Standardize Backend Allure Results Directory Structure

**As a** DevOps engineer  
**I want** every backend service to write Allure artifacts to the same relative path  
**So that** CI pipelines can collect results without bespoke configuration per service.

## Acceptance Criteria
- Agreed path defined (for example `target/allure-results`) and enforced across services.
- Build scripts clean directory before test runs to prevent stale artifacts.
- Services with custom build logic updated to conform to path standard.
- CI configuration simplified to collect artifacts from consistent location.
- Developer documentation notes where to find local results and how to serve them.

## Implementation Guidance
- Use Maven Surefire and Failsafe plugin configuration or Gradle test tasks to point to directory.
- Provide helper goals or tasks to remove previous results when running locally.
- Validate path choice works on Windows, macOS, and Linux environments.

## Testing Strategy
- Run backend tests on multiple services verifying results appear in the standard directory.
- CI dry run ensuring artifact upload step succeeds without path overrides.
- Manual check that local HTML generation works when pointed to standardized path.

## Dependencies
- Builds on Story 3.1 configuration work.
