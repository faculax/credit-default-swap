# Story 7.1 - Detect Story IDs in Branches and Commits

**As a** quality gate owner  
**I want** CI automation that extracts story IDs from branches, commits, and PR metadata  
**So that** downstream enforcement logic knows which backlog items to validate.

## Acceptance Criteria
- Script scans branch name, PR title, and commit messages to collect unique story IDs matching canonical pattern.
- Script outputs detected IDs in machine readable format (JSON) and human readable logs.
- Edge cases handled, including multiple story IDs, cherry-picks, and hotfix branches without IDs.
- CI step caches results for later jobs via workflow outputs or artifacts.
- Documentation describes how IDs are detected and how to override or ignore when necessary.

## Implementation Guidance
- Implement script in portable language (Node or Python) and package with repository tooling.
- Support configuration for optional prefixes or alternate patterns if needed in future.
- Provide developer facing messages when no IDs detected to encourage proper naming.

## Testing Strategy
- Unit tests covering detection logic for positive and negative cases.
- Integration test running script within CI environment against sample commit history.
- Manual verification on sample PR demonstrating expected outputs.

## Dependencies
- Depends on canonical story ID format from Story 1.1.
