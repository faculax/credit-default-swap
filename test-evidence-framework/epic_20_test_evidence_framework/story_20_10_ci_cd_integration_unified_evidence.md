# Story 20.10 – CI/CD Integration for Unified Evidence

**As the CI/CD engineer**,  
I want the entire test and evidence pipeline automated in GitHub Actions  
So that every PR and main branch build produces unified evidence in ReportPortal and an updated static dashboard.

## ✅ Acceptance Criteria
- For each service (`/frontend`, `/backend`, `/gateway`, `/risk-engine`):
  - A CI job exists that:
    - Runs the service's test suite with ReportPortal integration active.
    - Pushes evidence to ReportPortal with correct attributes (story, service, layer, dataset, environment, commit).
- On **PR builds**:
  - Relevant tests run based on changed file paths (e.g., if frontend files change, run frontend tests; if backend changes, run backend tests).
  - Optionally push evidence to a **PR-specific launch** in ReportPortal (e.g., launch name includes PR number).
  - Post a PR comment with:
    - Links to the ReportPortal launch.
    - Summary of test results per service.
    - (Optional) Link to a preview static dashboard for the PR.
- On **main branch builds**:
  - Full test suite runs across all services.
  - Evidence is pushed to ReportPortal under the main/production launch.
  - Static dashboard is regenerated via Story 20.9's export and generation pipeline.
  - Updated dashboard is deployed to GitHub Pages.
- CI workflows are configured with:
  - Correct environment variables for ReportPortal (URL, project, API token).
  - Secrets for GitHub Pages deployment (if needed).
  - Caching for dependencies (Maven, npm, etc.) to speed up builds.
- The workflow includes clear logging and failure messages so that evidence push or dashboard generation failures are easy to diagnose.

## 🧪 Test Scenarios
1. **Run backend tests in CI with ReportPortal**  
   Given a PR that changes backend code  
   When the CI workflow runs  
   Then backend tests execute, push evidence to RP under a PR-specific launch, and results appear in ReportPortal.

2. **Run frontend tests in CI with ReportPortal**  
   Given a PR that changes frontend code  
   When the CI workflow runs  
   Then frontend tests execute, push evidence to RP, and results are tagged with `serviceUnderTest=frontend`.

3. **Selective test execution on PR**  
   Given a PR that only changes `/gateway` files  
   When the CI workflow runs  
   Then only gateway tests are executed (or at minimum, other services are skipped to save time).

4. **Main branch full suite and dashboard update**  
   Given a merge to `main`  
   When the CI workflow runs  
   Then:
   - All services' tests run and push evidence to RP.
   - The static dashboard is regenerated with latest evidence.
   - The updated dashboard is published to GitHub Pages.

5. **PR comment with ReportPortal links**  
   Given a completed PR build  
   When the CI job finishes  
   Then a PR comment is posted with:
   - Link to the ReportPortal launch for that PR.
   - Summary of pass/fail counts per service.

## 🛠 Implementation Guidance
- Use GitHub Actions workflows (or adapt to other CI systems as needed).
- Define separate jobs or steps for each service, with appropriate conditionals for PR vs main.
- Use ReportPortal's launch attributes to distinguish PR builds (e.g., `launch.attribute.pr={prNumber}`).
- For selective test execution, use GitHub Actions path filters or similar CI features.
- Store RP credentials and GH Pages tokens as GitHub Secrets.
- Document the CI setup in a `CI.md` or section in the epic README.

## 📦 Deliverables
- GitHub Actions workflow files (e.g., `.github/workflows/test-evidence.yml`).
- Configuration for per-service test jobs with ReportPortal integration.
- PR-specific launch logic and PR comment script.
- Main branch workflow that includes dashboard regeneration and deployment.
- Documentation on CI setup, secrets required, and how to interpret CI logs.

## ⏭ Dependencies / Links
- Depends on Stories 20.3, 20.4, 20.5, 20.6 (generated and validated tests).
- Depends on Story 20.8 (ReportPortal Evidence Integration) for evidence push.
- Depends on Story 20.9 (Evidence Export & Static Dashboard) for dashboard generation.
- Completes the end-to-end automation of the test evidence framework.
