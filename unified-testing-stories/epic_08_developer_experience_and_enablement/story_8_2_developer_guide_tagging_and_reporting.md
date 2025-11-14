# Story 8.2 - Write Developer Guide Covering Tagging and Reporting Workflows

**As a** platform enablement lead  
**I want** comprehensive documentation describing tagging, running tests, and viewing reports  
**So that** engineers can onboard themselves to the unified testing platform.

## Acceptance Criteria
- Guide includes sections on story ID tagging, directory conventions, local CLI usage, and interpreting Allure results.
- Step-by-step walkthrough provided for adding a new test with story tags and verifying it in the report.
- Embedded screenshots or diagrams illustrate key UI elements of Allure and traceability matrix.
- Document linked from contributing guide, onboarding checklist, and README files where applicable.
- Content reviewed by representatives from backend, frontend, and QA teams.

## Implementation Guidance
- Structure guide in markdown with anchor links for quick navigation.
- Reference related stories and epics to maintain context and avoid duplication.
- Host images in repository under docs/assets with lightweight file sizes.

## Testing Strategy
- Run through guide with a new engineer to verify instructions yield expected result.
- Solicit asynchronous feedback via pull request review and address comments promptly.
- Schedule periodic refresh to keep guide aligned with tooling changes.

## Dependencies
- Builds on features delivered in Epics 01 through 07.
