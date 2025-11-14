# Story 2.1 - Define Backend Test Folder and Package Conventions

**As a** backend chapter lead  
**I want** a prescribed directory and package layout for backend test suites  
**So that** new services follow the same structure and build tooling discovers suites reliably.

## Acceptance Criteria
- Standard directory hierarchy documented (for example `src/test/java/.../unit`, `integration`, `contract`).
- Parent build configuration updated to include new source sets or include patterns.
- Existing reference service migrated to follow structure as example.
- Lint or CI check fails when new backend tests are added outside approved directories.
- Documentation includes diagrams and sample package declarations for each test type.

## Implementation Guidance
- Update Maven or Gradle configurations to map directories to logical source sets.
- Provide IDE run configuration templates reflecting new structure.
- Publish migration checklist for teams to adopt structure incrementally.

## Testing Strategy
- Build verification ensuring unit and integration tasks only pick up intended tests.
- Automated check verifying directories exist in scaffolded service template.
- Manual sanity run of sample service to confirm imports and context loads remain valid.

## Dependencies
- None.
