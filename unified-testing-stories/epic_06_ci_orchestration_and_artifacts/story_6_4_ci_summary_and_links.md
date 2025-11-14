# Story 6.4 - Add Combined Summary Step Linking Allure Reports in PR Checks

**As a** reviewer  
**I want** pull requests to display a concise summary with links to Allure artifacts  
**So that** I can quickly assess test health without searching through CI logs.

## Acceptance Criteria
- CI workflow posts summary comment or status check containing pass/fail counts and direct links to backend and frontend Allure artifacts.
- Summary clearly indicates when tests failed and highlights affected services.
- Comment updates or replaces previous summary on rerun to avoid duplicates.
- Failure to post summary is treated as non-blocking but alerts platform team via log warning.
- Documentation describes summary format and how to customize it.

## Implementation Guidance
- Use GitHub Actions workflow commands or REST API to create or update PR comments.
- Store summary content in JSON output to allow reuse by other automation.
- Provide fallback instructions if repository uses required status checks instead of comments.

## Testing Strategy
- Dry run on sample PR verifying summary content and link formatting.
- Automated test for summary generation script to ensure stable output structure.
- Manual review ensuring summary is readable on desktop and mobile GitHub clients.

## Dependencies
- Requires artifact uploads from Stories 6.2 and 6.3.
