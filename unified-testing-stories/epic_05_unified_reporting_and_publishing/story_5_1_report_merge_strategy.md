# Story 5.1 - Define Allure Report Merge Strategy Across Modules

**As a** release manager  
**I want** a deterministic process to merge Allure results from all modules  
**So that** we present a unified quality report covering the entire platform.

## Acceptance Criteria
- Merge strategy documented including input directories, conflict resolution rules, and labeling standards.
- Script or CLI command implemented to merge multiple Allure result folders into one output directory.
- Process preserves module identifiers and avoids duplicate UUID conflicts.
- Failure conditions handled gracefully with clear error messages when inputs are missing or malformed.
- Example merge executed using backend and frontend artifacts, with resulting report reviewed and approved.

## Implementation Guidance
- Consider using Allure `allure merge` combined with custom preprocessing to add module labels.
- Normalize directory structure before merge to avoid nested subfolders.
- Generate summary metadata (services included, timestamp) for audit logging.

## Testing Strategy
- Unit tests covering merge script scenarios (duplicate tests, missing artifacts, conflicting attachments).
- Integration test running within CI to confirm merged results build successfully.
- Manual review of merged Allure report verifying filters for module/service operate correctly.

## Dependencies
- Requires standardized Allure output from Epics 03 and 04.
