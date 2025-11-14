# Story 2.3 - Introduce Shared Test Type Label Schema

**As a** platform architect  
**I want** a shared schema that defines valid test type labels and metadata  
**So that** all test runners annotate results consistently for reporting and governance.

## Acceptance Criteria
- Schema file created (JSON or YAML) listing allowed test types, descriptions, and default severity levels.
- Backend and frontend projects reference schema to populate Allure labels.
- Validation script ensures tests only emit labels defined in schema.
- Change management process documented for updating schema with new types.
- Sample report demonstrates uniform test type labeling across multiple suites.

## Implementation Guidance
- Store schema near repository root and expose helpers to read it in Node and JVM environments.
- Provide typed interfaces or classes generated from schema to reduce runtime errors.
- Integrate validation into CI pipeline as a post-test step.

## Testing Strategy
- Unit tests verifying schema parsing and validation logic.
- Integration test running representative suites to confirm Allure output aligns with schema.
- Manual review by QA to confirm schema matches current taxonomy.

## Dependencies
- Complements Stories 1.2 through 1.4 for labeling; no hard dependency.
