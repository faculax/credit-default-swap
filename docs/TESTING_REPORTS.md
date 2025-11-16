# 🧪 CDS Platform - Test Reports Guide

## Quick Links

- 📊 **[Latest Test Report](https://[your-org].github.io/[your-repo]/)** - Unified test results
- 🚀 **[CI Workflows](../.github/workflows/)** - Backend & Frontend CI pipelines
- 📖 **[Frontend Testing Guide](../frontend/TESTING.md)** - Running tests locally
- 📋 **[Testing PRD](../unified-testing-stories/TestingPRD.md)** - Complete strategy

---

## 📍 Accessing Test Reports

### Option 1: GitHub Pages (Recommended)

**URL:** `https://[your-org].github.io/[your-repo]/`

The unified test report is automatically published to GitHub Pages after every CI run on the `main` branch.

**What's Included:**
- ✅ Backend Service tests
- ✅ Gateway Service tests  
- ✅ Risk Engine tests
- ✅ Frontend Unit tests
- ✅ Frontend Integration tests
- ✅ Frontend E2E tests

**Navigation:**
1. Visit the landing page (index.html)
2. Click **"View Latest Test Report"** to access the full Allure report
3. Use the quick navigation cards to jump to specific sections:
   - **Overview** - High-level pass/fail summary
   - **Test Suites** - Organized by service/package
   - **Story Coverage** - Grouped by user story ID
   - **Trend Analysis** - Historical pass rate trends

### Option 2: Download CI Artifacts

If you need to inspect test results from a specific PR or branch:

1. Go to the **Actions** tab in GitHub
2. Select the workflow run (e.g., "Backend Tests" or "Frontend Tests")
3. Scroll down to **Artifacts** section
4. Download the artifact for your service:
   - `allure-results-backend-service`
   - `allure-results-gateway`
   - `allure-results-risk-engine`
   - `allure-results-frontend-unit-tests`
   - `allure-results-frontend-integration-tests`
   - `allure-results-frontend-e2e-tests`

5. Extract the ZIP file locally
6. Generate the HTML report:
   ```bash
   # Install Allure CLI if not already installed
   npm install -g allure-commandline
   
   # Generate report from downloaded results
   allure generate path/to/allure-results -o allure-report
   
   # Open in browser
   allure open allure-report
   ```

### Option 3: Local Test Execution

**Backend Services:**
```bash
# Navigate to service directory
cd backend  # or gateway, or risk-engine

# Run tests with Allure results
./mvnw clean test

# Generate Allure report
allure generate target/allure-results -o target/allure-report

# Open report
allure open target/allure-report
```

**Frontend:**
```bash
cd frontend

# Run all tests and generate merged report
npm run test:all

# Open merged report
npm run allure:open

# Or run individual test types:
npm run test:unit:report        # Unit tests only
npm run test:integration:report # Integration tests only  
npm run test:e2e:report         # E2E tests only
```

---

## 🔍 Navigating the Allure Report

### Main Sections

#### 1. Overview Dashboard
- **Location:** Click "Overview" tab or use landing page link
- **Content:** 
  - Total tests, pass/fail counts
  - Success rate percentage
  - Execution time statistics
  - Environment information

#### 2. Test Suites
- **Location:** Click "Suites" tab
- **Content:** 
  - Tests organized by Java package or file location
  - Hierarchical tree view
  - Expandable test cases with execution details

#### 3. Story Coverage (Behaviors)
- **Location:** Click "Behaviors" tab
- **Content:** 
  - Tests grouped by user story ID (e.g., `UTS-4.2`, `epic_03_story_05`)
  - Epic > Story > Test hierarchy
  - Easy traceability from requirement to test

#### 4. Trend Analysis (Timeline)
- **Location:** Click "Timeline" tab or "Graphs" > "Timeline"
- **Content:** 
  - Historical pass rate over last 20 builds
  - Flaky test detection
  - Duration trends

### Filtering Tests

Use the search bar and filters to narrow results:

**By Service:**
```
service:backend
service:frontend
service:gateway
service:risk-engine
```

**By Test Type:**
```
testType:unit
testType:integration
testType:e2e
```

**By Story ID:**
```
story:UTS-4.2
story:epic_03_story_05
```

**By Severity:**
```
severity:critical
severity:major
severity:minor
```

**Combination Example:**
```
service:frontend AND testType:e2e AND severity:critical
```

---

## 🛠️ Troubleshooting Common Issues

### Issue 1: Report Shows 404 Error

**Symptom:** GitHub Pages URL returns "404 - Not Found"

**Possible Causes:**
- GitHub Pages not enabled
- Report not yet published
- Branch mismatch

**Resolution:**
1. Check if GitHub Pages is enabled:
   - Go to **Settings** > **Pages**
   - Ensure "Source" is set to `gh-pages` branch and `/` (root) folder
2. Verify the unified-reports workflow has run successfully:
   - Go to **Actions** > **Unified Test Reports**
   - Check for green checkmark on latest run
3. Wait 2-3 minutes after workflow completion for Pages to rebuild
4. Clear browser cache and try again

### Issue 2: Missing Test Data for a Service

**Symptom:** Report shows 0 tests or missing results for backend/gateway/risk-engine/frontend

**Possible Causes:**
- Service CI workflow failed before artifact upload
- Artifact name mismatch
- Test execution skipped

**Resolution:**
1. Check the individual service workflow (Backend Tests or Frontend Tests):
   - Go to **Actions** > Select the workflow
   - Look for failed steps before "Upload Allure results"
2. Review test execution logs:
   - Expand the "Run tests" step
   - Look for test failures or compilation errors
3. Verify artifact upload step succeeded:
   - Check "Upload Allure results" step for green checkmark
4. If workflow passed but data still missing:
   - Check artifact names match expected patterns in `unified-reports.yml`
   - Expected patterns: `allure-results-{service}-*`

### Issue 3: Report History Lost (No Trend Data)

**Symptom:** Trend graphs show only current build, no historical data

**Possible Causes:**
- First-time report generation
- `gh-pages` branch deleted/reset
- History restoration step failed

**Resolution:**
1. Check if `gh-pages` branch exists:
   ```bash
   git fetch origin
   git branch -r | grep gh-pages
   ```
2. Review unified-reports workflow logs:
   - Look for "Restore history from previous report" step
   - Check for errors during history download
3. If branch was accidentally deleted:
   - History will rebuild over next 20 builds
   - No action needed, trends will accumulate naturally
4. Manual history restoration (if needed):
   ```bash
   # Checkout gh-pages branch
   git checkout gh-pages
   
   # Verify history directory exists
   ls -la history/
   
   # If missing, re-run unified-reports workflow
   ```

### Issue 4: Broken Links or Images in Report

**Symptom:** Missing images, screenshots, or broken navigation links

**Possible Causes:**
- Asset upload failure
- Relative path issues
- Missing Cypress screenshots/videos

**Resolution:**
1. For Cypress screenshots/videos:
   - Check frontend-tests workflow artifacts
   - Download `cypress-screenshots` and `cypress-videos` artifacts
   - These are stored separately (7-day retention)
2. For Allure attachments:
   - Verify files exist in `allure-results/` directory before report generation
   - Check test code includes proper Allure attachment syntax:
     ```typescript
     // Cypress
     cy.screenshot('screenshot-name');
     
     // Jest
     allure.attachment('name', data, 'image/png');
     ```
3. Re-run tests locally to reproduce and verify attachments generate correctly

### Issue 5: PR Does Not Show Test Summary

**Symptom:** Pull request lacks test result comment

**Possible Causes:**
- Workflow permission issues
- PR from forked repository
- Summary generation step failed

**Resolution:**
1. Check if PR is from a fork:
   - Forked PRs have restricted token permissions
   - Test summaries may not post automatically for security
2. Review workflow run for the PR:
   - Go to **Actions** > Find the PR's workflow run
   - Check "Create deployment summary" step for errors
3. Verify workflow has `write` permissions:
   - Check workflow YAML has:
     ```yaml
     permissions:
       contents: read
       pages: write
       id-token: write
     ```
4. Manual workaround:
   - Download the summary markdown from workflow artifacts
   - Post as PR comment manually

### Issue 6: Tests Pass Locally but Fail in CI

**Symptom:** Tests succeed on developer machine but fail in GitHub Actions

**Possible Causes:**
- Environment differences (Node/Java version)
- Missing dependencies
- Timing issues (race conditions)
- Port conflicts

**Resolution:**
1. Check versions match:
   ```bash
   # Local
   node --version  # Should match CI (18.x)
   java -version   # Should match CI (17)
   
   # CI versions defined in workflow YAML
   # actions/setup-node@v4: node-version: '18'
   # actions/setup-java@v4: java-version: '17'
   ```
2. Clean install dependencies:
   ```bash
   # Frontend
   rm -rf node_modules package-lock.json
   npm ci
   
   # Backend
   ./mvnw clean install
   ```
3. For E2E tests, increase timeouts:
   ```typescript
   // cypress/support/e2e.ts
   Cypress.config('defaultCommandTimeout', 10000); // 10 seconds
   Cypress.config('pageLoadTimeout', 60000);       // 60 seconds
   ```
4. Check for port conflicts in `docker-compose.yml`:
   - Frontend: 3000
   - Backend: 8080
   - Gateway: 8090
   - Risk Engine: 8081

---

## 📊 Understanding Test Metadata

### Story Traceability

Every test is tagged with its source user story using the `[story:ID]` format:

- **`UTS-X.Y`** - Unified Testing Story (testing infrastructure)
- **`epic_XX_story_YY`** - Feature story (business functionality)

**Example:**
```typescript
// Frontend
describe('Homepage [story:UTS-4.2] [service:frontend]', () => {
  it('should load [story:UTS-4.2] [severity:critical]', () => {
    // test code
  });
});
```

```java
// Backend
@DisplayName("[story:epic_03_story_05] CDS Trade Capture")
@Tag("service:backend")
@Tag("testType:integration")
public class TradeCaptureSuite { }
```

### Test Severities

- 🔴 **Critical** - Core functionality, must pass for release
- 🟠 **Major** - Important features, should be fixed before release
- 🟡 **Normal** - Standard functionality (default if not specified)
- 🟢 **Minor** - Nice-to-have, low priority
- ⚪ **Trivial** - Documentation, cosmetic issues

### Test Types

- **Unit** - Single class/component in isolation
- **Integration** - Multiple components, may use test database
- **E2E (End-to-End)** - Full user workflow, browser automation

### Services

- **backend** - Core backend service (Spring Boot)
- **gateway** - API Gateway service
- **risk-engine** - Risk calculation engine
- **frontend** - React frontend (all test types)

---

## 🚨 Escalation Path

If you've followed troubleshooting steps and still encounter issues:

### Level 1: Team Support
- Post in **#cds-platform-testing** Slack channel
- Tag `@qa-team` for report access questions
- Tag `@platform-team` for CI/workflow issues

### Level 2: Platform Squad
- Create GitHub issue with label `testing-infrastructure`
- Provide:
  - Workflow run URL
  - Error messages/screenshots
  - Steps to reproduce
  - Expected vs actual behavior

### Level 3: External Support
- **Allure Issues:** [allure-framework/allure2](https://github.com/allure-framework/allure2/issues)
- **GitHub Actions:** [GitHub Support](https://support.github.com/)
- **Cypress Issues:** [cypress-io/cypress](https://github.com/cypress-io/cypress/issues)

---

## 📚 Related Documentation

- **[Frontend Testing Guide](../frontend/TESTING.md)** - Detailed frontend test setup
- **[Testing PRD](../unified-testing-stories/TestingPRD.md)** - Complete testing strategy
- **[CI Workflows](./.github/workflows/)** - Workflow YAML files
- **[Story Traceability](../unified-testing-stories/epic_01_story_traceability_backbone/)** - Epic 01 implementation
- **[Allure Documentation](https://docs.qameta.io/allure/)** - Official Allure docs
- **[Cypress Documentation](https://docs.cypress.io/)** - Cypress testing framework
- **[JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)** - Java testing framework

---

## 🔄 Changelog

### 2025-01-XX - Initial Release
- Created comprehensive testing reports guide
- Documented access methods (GitHub Pages, artifacts, local)
- Added troubleshooting for 6 common issues
- Included navigation guide with filter examples
- Defined escalation path for support

---

## 💡 Tips & Best Practices

### For QA Managers
- Bookmark the GitHub Pages URL for quick access
- Review trend graphs weekly to identify flaky tests
- Use story filters to verify acceptance criteria coverage
- Download artifacts for audit/compliance requirements

### For Developers
- Run tests locally before pushing to CI
- Use `npm run test:report:merge` for comprehensive frontend report
- Tag new tests with appropriate story ID and severity
- Check PR test summary before requesting review

### For Platform Team
- Monitor unified-reports workflow for failures
- Verify all 6 artifact patterns are uploading successfully
- Review Allure CLI version compatibility quarterly
- Update this guide when workflows change

---

**Questions or feedback?** Open an issue or ping `@platform-team` in Slack.
