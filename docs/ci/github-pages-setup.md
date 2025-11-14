# GitHub Pages Setup for Allure Reports

**Epic 05: Unified Reporting and GitHub Pages**  
**Story 5.3: Publish Reports to GitHub Pages**  
**Version:** 1.0  
**Last Updated:** November 14, 2025

This guide covers the automated publishing of Allure test reports to GitHub Pages, making them accessible via a public URL.

---

## Table of Contents

1. [Overview](#overview)
2. [Repository Configuration](#repository-configuration)
3. [How It Works](#how-it-works)
4. [Accessing Reports](#accessing-reports)
5. [Report Structure](#report-structure)
6. [Troubleshooting](#troubleshooting)
7. [Manual Deployment](#manual-deployment)

---

## Overview

**What it does:**
- Automatically publishes Allure test reports to GitHub Pages after successful main branch builds
- Merges results from all backend services (backend, gateway, risk-engine) into a single unified report
- Maintains historical trends across builds for regression analysis
- Provides a stable public URL for stakeholders to access quality metrics

**When it runs:**
- Only on `main` branch after tests pass
- After at least one critical service (backend or gateway) succeeds
- Deploys even if risk-engine has failures (non-blocking)

**Report URL:**
```
https://faculax.github.io/credit-default-swap/
```

---

## Repository Configuration

### Step 1: Enable GitHub Pages

The workflow requires GitHub Pages to be properly configured in your repository.

**Required Settings:**

1. Go to **Settings** → **Pages** in your GitHub repository
2. Under **Source**, select:
   - **Source:** GitHub Actions
   - **NOT** "Deploy from a branch" (we use Actions workflow)

3. Click **Save**

**Screenshot reference:**
```
┌─────────────────────────────────────────┐
│ GitHub Pages                            │
├─────────────────────────────────────────┤
│ Source: ◉ GitHub Actions                │
│                                         │
│ ✓ Your site is live at:                │
│ https://faculax.github.io/credit-d...  │
└─────────────────────────────────────────┘
```

### Step 2: Grant Workflow Permissions

The workflow needs specific permissions to deploy to Pages.

**Required Permissions:**

1. Go to **Settings** → **Actions** → **General**
2. Scroll to **Workflow permissions**
3. Select: **Read and write permissions**
4. Check: ☑ **Allow GitHub Actions to create and approve pull requests**
5. Click **Save**

**Why these permissions:**
- `pages: write` - Deploy content to GitHub Pages
- `id-token: write` - Verify deployment authenticity
- `contents: read` - Access repository code and artifacts

### Step 3: Verify Environments

The workflow creates a `github-pages` environment automatically.

**To verify:**

1. Go to **Settings** → **Environments**
2. You should see `github-pages` listed after first deployment
3. Optional: Add protection rules (e.g., require reviews before deployment)

---

## How It Works

### Deployment Flow

```
┌─────────────────────────────────────────────────────────┐
│  1. Backend Tests Run (on main branch)                  │
│     - backend-service-tests                             │
│     - gateway-service-tests                             │
│     - risk-engine-tests                                 │
└────────────────┬────────────────────────────────────────┘
                 │
                 ├─ Uploads: allure-results-backend-{run}
                 ├─ Uploads: allure-results-gateway-{run}
                 └─ Uploads: allure-results-risk-engine-{run}
                 │
┌────────────────▼────────────────────────────────────────┐
│  2. Deploy Pages Job (if tests pass)                    │
│     - Downloads all Allure artifacts                    │
│     - Merges results into single directory              │
│     - Fetches previous history from gh-pages branch     │
│     - Generates unified HTML report with Allure CLI     │
│     - Adds build metadata (commit, timestamp, etc.)     │
│     - Uploads to GitHub Pages                           │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────┐
│  3. Published Report                                     │
│     URL: https://faculax.github.io/credit-default-swap/ │
│     - Unified view of all backend services              │
│     - Historical trends maintained                      │
│     - Build metadata included                           │
└─────────────────────────────────────────────────────────┘
```

### Key Steps in Workflow

**1. Download Artifacts (from previous jobs)**
```yaml
- uses: actions/download-artifact@v4
  with:
    path: allure-artifacts
    pattern: allure-results-*
```
Downloads all Allure result artifacts from test jobs.

**2. Merge Results**
```bash
mkdir -p allure-results-merged
cp -r allure-artifacts/allure-results-backend-*/  allure-results-merged/
cp -r allure-artifacts/allure-results-gateway-*/  allure-results-merged/
cp -r allure-artifacts/allure-results-risk-engine-*/ allure-results-merged/
```
Combines all service results into one directory for unified reporting.

**3. Restore History**
```bash
git fetch origin gh-pages:gh-pages
git checkout gh-pages -- allure-report/history
cp -r allure-report/history allure-results-merged/history
```
Preserves historical data for trend analysis (optional, continues on error).

**4. Generate HTML Report**
```bash
wget https://github.com/allure-framework/allure2/releases/download/2.25.0/allure-2.25.0.tgz
tar -zxf allure-2.25.0.tgz
./allure-2.25.0/bin/allure generate allure-results-merged --clean -o allure-report
```
Creates the HTML report using Allure CLI.

**5. Add Metadata**
```json
{
  "buildNumber": "123",
  "commitSha": "abc123...",
  "commitShort": "abc123",
  "branch": "main",
  "timestamp": "2025-11-14T12:00:00Z",
  "repositoryUrl": "https://github.com/faculax/credit-default-swap",
  "runUrl": "https://github.com/faculax/credit-default-swap/actions/runs/123"
}
```
Stored as `metadata.json` and `BUILD_INFO.txt` in report root.

**6. Deploy to Pages**
```yaml
- uses: actions/deploy-pages@v4
```
Publishes the `allure-report` directory to GitHub Pages.

### Concurrency Strategy

**Latest-wins approach:**
- GitHub Pages always shows the most recent successful deployment
- No queuing or locking needed (Pages handles concurrency)
- Previous deployments are replaced entirely
- History is preserved in `gh-pages` branch between deployments

---

## Accessing Reports

### Public URL

**Primary Report:**
```
https://faculax.github.io/credit-default-swap/
```

This URL always shows the latest report from the `main` branch.

### Report Views

Once you open the URL, you'll see the Allure report interface:

**1. Overview Dashboard**
- Total tests, pass rate, failure rate
- Duration and trend graphs (if history exists)
- Test categories breakdown

**2. Suites View**
- Tests organized by service and package
- Backend Service tests
- Gateway Service tests  
- Risk Engine tests
- Expandable tree structure

**3. Timeline View**
- Visual timeline of test execution
- Shows parallel vs sequential tests
- Identifies slow tests

**4. Behaviors View**
- Tests grouped by features/stories (if tagged)
- Story-to-test mapping

**5. Categories View**
- Failures grouped by type
- Product defects vs test defects

### Build Information

**Metadata location:**
- `metadata.json` - Machine-readable build info
- `BUILD_INFO.txt` - Human-readable summary

**To check which build is deployed:**
```bash
curl https://faculax.github.io/credit-default-swap/metadata.json
```

Example response:
```json
{
  "buildNumber": "42",
  "commitSha": "abc123def456...",
  "commitShort": "abc123d",
  "branch": "main",
  "timestamp": "2025-11-14T15:30:00Z"
}
```

---

## Report Structure

### Directory Layout

```
https://faculax.github.io/credit-default-swap/
├── index.html                    # Main report page
├── metadata.json                 # Build information
├── BUILD_INFO.txt                # Human-readable build info
├── app.js                        # Allure report application
├── styles.css                    # Report styling
├── data/
│   ├── test-cases/              # Individual test details
│   ├── attachments/             # Logs, screenshots
│   └── suites.json              # Test suite data
├── history/
│   ├── history.json             # Trend data
│   └── history-trend.json       # Graph data
└── widgets/
    ├── summary.json             # Summary statistics
    └── ...
```

### Services in Report

**Backend Service (com.creditdefaultswap.backend)**
- Unit tests from `backend/src/test/java/`
- ~53 tests covering CDS trade capture, lifecycle, position management

**Gateway Service (com.vibe.gateway)**
- Unit tests from `gateway/src/test/java/`
- ~1 test covering API gateway routing and health checks

**Risk Engine (com.creditdefaultswap.riskengine)**
- Unit tests from `risk-engine/src/test/java/`
- ~17 tests (7 known failures tracked separately)
- Non-blocking failures clearly marked

### Historical Trends

**How history works:**
- Each deployment preserves results in `gh-pages` branch under `allure-report/history/`
- Next deployment fetches previous history before generating new report
- Allure automatically maintains up to 20 historical builds
- Trend graphs show pass rate, duration, and test count over time

**To clear history:**
```bash
# Manually delete gh-pages branch (requires repo admin)
git push origin --delete gh-pages
# Next deployment will start fresh
```

---

## Troubleshooting

### Issue 1: "404 - Page Not Found"

**Symptoms:**
- GitHub Pages URL returns 404 error
- Site appears not to be published

**Possible Causes:**
1. GitHub Pages not enabled in repository settings
2. First deployment hasn't completed yet
3. Workflow permissions insufficient

**Solutions:**

**Check Pages settings:**
```
Settings → Pages → Source should be "GitHub Actions"
```

**Check deployment status:**
```
Actions → Select workflow run → Check "deploy-pages" job
Look for green checkmark and deployment URL in summary
```

**Verify permissions:**
```
Settings → Actions → General → Workflow permissions
Should be "Read and write permissions"
```

**Force re-deployment:**
```bash
# Push empty commit to main to trigger workflow
git commit --allow-empty -m "Trigger Pages deployment"
git push origin main
```

### Issue 2: "Report is outdated"

**Symptoms:**
- Report shows old build number or commit
- Recent tests not appearing in report

**Causes:**
- Browser caching old version
- Deployment in progress but not complete

**Solutions:**

**Check build number:**
```bash
curl https://faculax.github.io/credit-default-swap/metadata.json
# Compare buildNumber with latest GitHub Actions run number
```

**Clear browser cache:**
- Chrome: Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)
- Or open in incognito/private window

**Wait for deployment:**
- Deployment takes 2-5 minutes after tests complete
- Check workflow run for "Deploy to GitHub Pages" step completion

### Issue 3: "No Allure results found to merge"

**Symptoms:**
- Deployment job fails with error: "No Allure results found to merge"
- No report published

**Causes:**
- Test jobs didn't upload artifacts
- Test jobs failed before artifact upload
- Artifact naming mismatch

**Solutions:**

**Check test job artifacts:**
```
Actions → Workflow run → Scroll to Artifacts section
Should see: allure-results-backend-{N}, allure-results-gateway-{N}, etc.
```

**Verify at least one service passed:**
```
Check that backend-service-tests OR gateway-service-tests shows green checkmark
```

**Review test job logs:**
```
Click failed test job → Look for "Verify Allure results" step
Check if target/allure-results/ directory exists and contains *-result.json files
```

### Issue 4: "History not preserved"

**Symptoms:**
- Trend graphs show only current build
- No historical data in report

**Expected Behavior:**
- First deployment will have no history (this is normal)
- Subsequent deployments should show trends

**Causes:**
- First-ever deployment (expected)
- `gh-pages` branch deleted or reset
- History fetch step failed silently (non-fatal)

**To verify history exists:**
```bash
# Check gh-pages branch
git fetch origin gh-pages
git ls-tree -r gh-pages --name-only | grep history
# Should show: allure-report/history/history.json, etc.
```

**Note:** History fetch uses `continue-on-error: true`, so deployment succeeds even without history.

### Issue 5: "Deployment takes too long"

**Symptoms:**
- Deployment job running for >10 minutes
- No visible progress

**Typical Duration:**
- Download artifacts: 30-60 seconds
- Merge results: 10-20 seconds
- Generate HTML: 1-2 minutes
- Upload to Pages: 1-2 minutes
- **Total: 3-5 minutes normal**

**If exceeds 10 minutes:**

**Check Actions logs:**
```
Look for steps stuck or looping
Common culprit: "Download previous history" if git fetch hangs
```

**Cancel and retry:**
```
Actions → Select run → Cancel workflow
Re-run all jobs
```

### Issue 6: "403 Forbidden" when accessing report

**Symptoms:**
- URL loads but returns 403 Forbidden error

**Causes:**
- Repository is private and Pages requires authentication
- GitHub Pages not enabled properly

**Solutions:**

**For private repos:**
- GitHub Pages for private repos requires GitHub Pro or Enterprise
- Or make repository public

**Check Pages status:**
```
Settings → Pages
Should show "Your site is published at https://..."
```

---

## Manual Deployment

### Trigger Deployment Without Code Changes

If you need to republish reports without new commits:

**Option 1: Manual workflow trigger**
```
1. Go to Actions tab
2. Select "Backend Tests" workflow
3. Click "Run workflow"
4. Select "main" branch
5. Click "Run workflow"
```

**Option 2: Empty commit**
```bash
git checkout main
git pull
git commit --allow-empty -m "Republish GitHub Pages"
git push origin main
```

### Deploy from Local Allure Results

For testing or one-off deployments:

```bash
# Generate report locally
mvn clean test
mvn allure:report

# Deploy to gh-pages branch manually (requires git push access)
git checkout --orphan gh-pages
git rm -rf .
cp -r target/allure-report/* .
git add .
git commit -m "Manual Pages deployment"
git push origin gh-pages --force
git checkout main
```

**Warning:** Manual deployment bypasses workflow automation and doesn't preserve metadata.

---

## Best Practices

### For Developers

1. **Always merge to main through PRs** - Direct pushes may skip deployment if CI doesn't trigger
2. **Check Pages URL after merge** - Verify your tests appear in published report (within 5 min)
3. **Don't delete gh-pages branch** - Contains history for trend analysis

### For DevOps

1. **Monitor Pages build times** - Alert if deployment exceeds 10 minutes
2. **Set up branch protection** - Require tests to pass before merge
3. **Enable deployment notifications** - Configure Slack/email alerts for failures

### For Stakeholders

1. **Bookmark the Pages URL** - Always access latest report from same link
2. **Check BUILD_INFO.txt** - Verify report freshness before analysis
3. **Review trend graphs** - Look for patterns in pass rate over time

---

## Related Documentation

- **[Backend CI Workflow](backend-ci-workflow.md)** - Test execution and artifact generation
- **[Backend Allure Setup](../testing/backend-allure-setup.md)** - Local Allure configuration
- **[TestingPRD](../../unified-testing-stories/TestingPRD.md)** - Overall testing strategy

---

## Support

**Report Issues:**
- GitHub Issues: https://github.com/faculax/credit-default-swap/issues
- Tag with `ci`, `github-pages`, or `allure`

**Check Status:**
- GitHub Status: https://www.githubstatus.com/ (Pages service status)
- Workflow History: Repository → Actions tab

---

## Appendix: Workflow YAML Reference

The GitHub Pages deployment is part of `.github/workflows/backend-tests.yml`:

```yaml
deploy-pages:
  name: Deploy Reports to GitHub Pages
  runs-on: ubuntu-latest
  needs: [backend-service-tests, gateway-service-tests, risk-engine-tests]
  if: github.ref == 'refs/heads/main' && (needs.backend-service-tests.result == 'success' || needs.gateway-service-tests.result == 'success')
  
  permissions:
    pages: write
    id-token: write
    contents: read
  
  environment:
    name: github-pages
    url: ${{ steps.deployment.outputs.page_url }}
  
  steps:
    - name: Checkout code
    - name: Download all Allure artifacts
    - name: Merge Allure results
    - name: Download previous history
    - name: Generate Allure HTML report
    - name: Add metadata to report
    - name: Setup Pages
    - name: Upload artifact for Pages
    - name: Deploy to GitHub Pages
    - name: Add Pages URL to summary
```

**Key Configuration:**
- **Trigger:** `if: github.ref == 'refs/heads/main'` - Only deploys on main branch
- **Dependencies:** Requires at least one critical service to pass
- **Permissions:** Explicitly grants Pages deployment rights
- **Environment:** Uses `github-pages` environment for deployment tracking
- **Outputs:** Deployment URL available in workflow summary
