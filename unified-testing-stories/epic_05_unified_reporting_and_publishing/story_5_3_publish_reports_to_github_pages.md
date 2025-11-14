# Story 5.3 - Publish Reports to GitHub Pages From Main Branch Pipeline

**As a** product owner  
**I want** the latest Allure report automatically published to GitHub Pages  
**So that** stakeholders can access quality data via a stable URL.

## Acceptance Criteria
- GitHub Actions workflow deploys merged Allure HTML to dedicated Pages branch (for example `gh-pages`) on successful main branch builds.
- Deployment includes `history` directory to maintain trends.
- Workflow handles concurrent builds safely (queue, lock, or latest-wins strategy documented).
- Published site accessible via expected URL and includes cache busting to surface latest report.
- Deployment failures surface as pipeline failures with clear remediation steps.

## Implementation Guidance
- Use official `actions/deploy-pages` or `peaceiris/actions-gh-pages` with appropriate permissions.
- Validate GitHub Pages settings (branch, folder) and document configuration.
- Consider storing commit hash in published site for reference.

## Testing Strategy
- Dry run deployment to staging branch before switching to production Pages branch.
- Manual verification of published site across desktop and mobile browsers.
- Monitor GitHub Pages build logs for errors and capture success criteria.

## Dependencies
- Requires Stories 5.1 and 5.2 to provide merged report and history.
