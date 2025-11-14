# Story 7.3 - Enforce Backend Coverage Thresholds With JaCoCo

**As a** backend quality owner  
**I want** automated enforcement of JaCoCo coverage thresholds in CI  
**So that** backend teams maintain agreed levels of line and branch coverage.

## Acceptance Criteria
- JaCoCo configured to generate XML reports for unit and integration suites across services.
- CI step evaluates coverage metrics against agreed thresholds (for example 75 percent line, 60 percent branch).
- Build fails when thresholds not met and outputs detailed summary per module.
- Thresholds configurable via central file and documented process exists for adjusting them.
- Coverage reports uploaded as artifacts for review.

## Implementation Guidance
- Use Maven or Gradle plugins to aggregate coverage when multiple modules exist.
- Provide script to parse JaCoCo XML and produce human friendly summary for logs.
- Consider separate thresholds for different modules if necessary but maintain central configuration.

## Testing Strategy
- Unit tests for coverage parsing script using sample JaCoCo XML.
- CI dry run verifying failure when artificially lowering coverage.
- Manual review of coverage artifact to ensure data accessible for debugging.

## Dependencies
- Builds on CI infrastructure from Epic 06; interacts with enforcement logic in Story 7.2.
