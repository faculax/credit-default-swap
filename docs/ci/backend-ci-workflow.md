# Backend CI Workflow Guide

**Epic 06: CI Orchestration and Artifacts**  
**Story 6.2: Backend CI Jobs with Allure Artifacts**  
**Version:** 1.0  
**Last Updated:** November 14, 2025

This guide explains the automated GitHub Actions workflow for backend service testing, artifact collection, and result reporting.

---

## Table of Contents

1. [Overview](#overview)
2. [Workflow Structure](#workflow-structure)
3. [How It Works](#how-it-works)
4. [Artifacts](#artifacts)
5. [Using Allure Reports](#using-allure-reports)
6. [Troubleshooting](#troubleshooting)
7. [Local Testing](#local-testing)

---

## Overview

The **Backend Tests** workflow automatically runs unit and integration tests for all three backend services whenever code changes are pushed or a PR is opened.

**Services Tested:**
- ✅ **Backend Service** (CDS Platform) - 53 tests
- ✅ **Gateway Service** - 1 test
- ⚠️ **Risk Engine** - 17 tests (7 known failures, non-blocking)

**Key Features:**
- 🔄 **Parallel execution** - All services test simultaneously
- 💾 **Maven caching** - Faster builds with dependency caching
- 📊 **Allure results** - Detailed test reports with artifacts
- 📈 **Test summaries** - Pass/fail counts in workflow UI
- ✅ **Status checks** - Blocks PR merge if critical tests fail

---

## Workflow Structure

### Trigger Conditions

The workflow runs on:

```yaml
# Pull requests to main/develop/unified-testing-reporting
on:
  pull_request:
    branches: [main, develop, unified-testing-reporting]
    paths:
      - 'backend/**'
      - 'gateway/**'
      - 'risk-engine/**'
      - '.github/workflows/backend-tests.yml'
  
  # Direct pushes to main/develop/unified-testing-reporting
  push:
    branches: [main, develop, unified-testing-reporting]
    paths:
      - 'backend/**'
      - 'gateway/**'
      - 'risk-engine/**'
  
  # Manual trigger from Actions UI
  workflow_dispatch:
```

**Why these triggers?**
- **PR trigger:** Validates code before merge
- **Push trigger:** Ensures main branch stays green
- **Path filter:** Only runs when backend code changes (not on docs/frontend changes)
- **Manual trigger:** Allows on-demand test runs

### Job Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Backend Tests Workflow                    │
└─────────────────────────────────────────────────────────────┘
                             │
                ┌────────────┼────────────┐
                │            │            │
         ┌──────▼──────┐ ┌──▼─────┐ ┌───▼────────────┐
         │  Backend    │ │Gateway │ │  Risk Engine   │
         │  Service    │ │Service │ │  (non-blocking)│
         │  Tests      │ │ Tests  │ │     Tests      │
         └──────┬──────┘ └──┬─────┘ └───┬────────────┘
                │            │            │
                └────────────┼────────────┘
                             │
                      ┌──────▼──────┐
                      │   Combined  │
                      │   Summary   │
                      └─────────────┘
```

**Jobs:**
1. **backend-service-tests** (15 min timeout)
   - Runs all backend tests
   - Uploads Allure and Surefire reports
   - Fails pipeline if tests fail

2. **gateway-service-tests** (10 min timeout)
   - Runs gateway tests
   - Uploads Allure and Surefire reports
   - Fails pipeline if tests fail

3. **risk-engine-tests** (15 min timeout)
   - Runs risk engine tests
   - `continue-on-error: true` (doesn't block pipeline)
   - Uploads results even if tests fail
   - Known issues documented

4. **backend-summary** (always runs)
   - Aggregates results from all services
   - Generates markdown summary
   - Lists available artifacts
   - Fails only if critical services (backend/gateway) fail

---

## How It Works

### Execution Flow

For each service job:

```
1. Checkout code
   ↓
2. Set up JDK 17 with Maven cache
   ↓
3. Restore Maven dependencies from cache (~30s vs 2-3 min)
   ↓
4. Run tests: mvn clean test -B
   ↓
5. Extract test counts (tests, failures, errors, skipped)
   ↓
6. Verify Allure results directory exists
   ↓
7. Upload Allure results as artifact (30-day retention)
   ↓
8. Upload Surefire XML reports (7-day retention)
   ↓
9. Add test summary to GitHub workflow UI
```

### Test Result Extraction

The workflow parses Surefire XML reports to extract metrics:

```bash
# From TEST-*.xml files:
TESTS=$(grep -oP 'tests="\K[0-9]+' target/surefire-reports/TEST-*.xml)
FAILURES=$(grep -oP 'failures="\K[0-9]+' ...)
ERRORS=$(grep -oP 'errors="\K[0-9]+' ...)
SKIPPED=$(grep -oP 'skipped="\K[0-9]+' ...)
```

These appear as:
- **Workflow logs:** Notice annotations
- **Job summary:** Markdown table with counts
- **Combined summary:** Overall build status

### Caching Strategy

**Maven Repository Cache:**
```yaml
key: ${{ runner.os }}-maven-backend-${{ hashFiles('**/pom.xml') }}
restore-keys: |
  ${{ runner.os }}-maven-backend-
  ${{ runner.os }}-maven-
```

**How it works:**
- **Cache key** based on OS + service + pom.xml hash
- **Cache hit:** Dependencies restored in ~30 seconds
- **Cache miss:** Downloads dependencies (~2-3 minutes), saves cache for next run
- **Partial match:** Uses `restore-keys` for partial cache restoration

**Benefits:**
- ⚡ 80-90% faster builds after first run
- 💰 Reduced network usage and Maven Central load
- 🔄 Automatic invalidation when dependencies change (pom.xml hash)

---

## Artifacts

### Available Artifacts

After each workflow run, six artifacts are uploaded:

| Artifact Name | Contents | Retention | Size | Purpose |
|---------------|----------|-----------|------|---------|
| `allure-results-backend-{run}` | Backend Allure JSON | 30 days | ~500KB | Detailed test results |
| `allure-results-gateway-{run}` | Gateway Allure JSON | 30 days | ~50KB | Detailed test results |
| `allure-results-risk-engine-{run}` | Risk Engine Allure JSON | 30 days | ~200KB | Detailed test results |
| `surefire-reports-backend-{run}` | Backend Surefire XML | 7 days | ~100KB | Raw test data |
| `surefire-reports-gateway-{run}` | Gateway Surefire XML | 7 days | ~10KB | Raw test data |
| `surefire-reports-risk-engine-{run}` | Risk Engine Surefire XML | 7 days | ~50KB | Raw test data |

**Where to find artifacts:**
1. Go to **Actions** tab in GitHub
2. Click on the workflow run
3. Scroll to **Artifacts** section at bottom
4. Click artifact name to download

### Artifact Structure

**Allure Results Structure:**
```
allure-results-backend-123/
├── {uuid}-result.json        # Test result (one per test method)
├── {uuid}-container.json     # Test container (one per test class)
├── {uuid}-attachment.txt     # Attachments (logs, screenshots)
└── environment.properties    # Environment metadata
```

**Surefire Reports Structure:**
```
surefire-reports-backend-123/
├── TEST-com.creditdefaultswap.backend.service.CDSTradeServiceTest.xml
├── TEST-com.creditdefaultswap.backend.repository.TradeRepositoryTest.xml
└── ...
```

---

## Using Allure Reports

### Download and Generate Report

**Step 1: Download artifacts from GitHub**
1. Navigate to workflow run in Actions tab
2. Download desired `allure-results-*` artifact(s)
3. Extract zip to your workspace

**Step 2: Generate HTML report locally**

```bash
# Option A: Single service report
cd backend/
# Copy downloaded results to target/allure-results/
cp -r ~/Downloads/allure-results-backend-123/* target/allure-results/
mvn allure:serve

# Option B: View all services together (Story 3.4 - future)
# Will be covered when multi-service merging is implemented
```

**Step 3: View report**
- Browser opens automatically showing Allure report
- Navigate through Overview, Suites, Timeline, etc.
- Click failed tests to see stack traces and attachments

### Understanding Report Views

**Overview Dashboard:**
- Pass rate percentage
- Total tests, failures, errors
- Duration trends (if history exists)
- Test status pie chart

**Suites View:**
- Tests organized by package/class
- Expandable tree structure
- Click test name to see details

**Timeline View:**
- Horizontal bars showing test execution
- Useful for identifying slow tests
- Shows parallel vs sequential execution

**Categories View:**
- Failures grouped by exception type
- "Product defects" vs "Test defects" vs "System issues"

---

## Troubleshooting

### Workflow Failing - Quick Diagnosis

**Check workflow summary first:**

1. Open failed workflow run
2. Look at "Backend Test Summary" section
3. Identify which service failed
4. Check that service's job logs

### Common Issues

#### Issue 1: Maven Dependency Download Fails

**Symptoms:**
```
[ERROR] Failed to execute goal ... Could not resolve dependencies
```

**Causes:**
- Maven Central temporarily unavailable
- Network connectivity issue in GitHub runner
- Corrupt cache

**Solutions:**
```yaml
# Solution: The workflow already retries Maven commands
# If persistent, manually invalidate cache:
# 1. Go to Actions → Caches
# 2. Delete caches with "maven" in name
# 3. Re-run workflow
```

#### Issue 2: No Allure Results Found

**Symptoms:**
```
Error: Allure results directory not found at backend/target/allure-results
```

**Causes:**
- Tests didn't run (compilation failed)
- Allure listener not configured
- Maven clean removed directory before upload

**Solutions:**
1. Check if tests compiled: Look for "BUILD FAILURE" earlier in logs
2. Verify Allure configuration: Ensure `pom.xml` has Allure listener
3. Check step order: Verify upload happens after tests, before clean

#### Issue 3: Risk Engine Tests Blocking Pipeline

**Symptoms:**
```
Risk engine job marked as failed, blocking PR merge
```

**Cause:**
- `continue-on-error: true` may have been removed accidentally

**Solution:**
```yaml
# Verify this setting exists in risk-engine-tests job:
risk-engine-tests:
  continue-on-error: true  # ← Must be present
```

#### Issue 4: Cache Not Restoring

**Symptoms:**
- Every build downloads all Maven dependencies (~2-3 minutes)
- No "Cache restored" message in logs

**Causes:**
- Cache key changed (pom.xml modified)
- Cache expired (7-day default retention)
- Cache storage limit reached (10GB per repo)

**Solutions:**
1. Check cache keys match: Compare current run key with previous
2. Accept first-run slowness: Cache builds on first execution
3. Clean old caches: Go to Actions → Caches and delete unused ones

### Debugging Steps

**For failing tests:**

```bash
# Step 1: Download artifact
# Go to Actions → Workflow Run → Download allure-results-{service}

# Step 2: Extract and view locally
cd {service}/
cp -r ~/Downloads/allure-results-*/* target/allure-results/
mvn allure:serve

# Step 3: Identify failure
# - Check stack traces
# - Review attachments
# - Compare with local test run

# Step 4: Reproduce locally
mvn clean test -Dtest=FailingTestClass#failingMethod

# Step 5: Fix and push
# Tests should pass in next CI run
```

**For workflow issues:**

1. **Check workflow logs:**
   - Expand each step to see output
   - Look for red "X" marks
   - Check "Annotations" tab for errors/warnings

2. **Validate workflow YAML:**
   ```bash
   # Install actionlint
   brew install actionlint  # macOS
   
   # Check workflow syntax
   actionlint .github/workflows/backend-tests.yml
   ```

3. **Test workflow changes:**
   - Create draft PR with workflow changes
   - Trigger manually with workflow_dispatch
   - Monitor run before merging

---

## Local Testing

### Validate Before Pushing

**Test workflow locally with act:**

```bash
# Install act (GitHub Actions local runner)
brew install act  # macOS
choco install act-cli  # Windows

# Run backend-service-tests job
act pull_request -j backend-service-tests

# Run all backend jobs
act pull_request -W .github/workflows/backend-tests.yml
```

**Note:** `act` runs in Docker, so results may differ slightly from GitHub-hosted runners.

### Pre-Push Checklist

Before pushing backend code changes:

- [ ] Tests pass locally: `mvn clean test`
- [ ] Allure results generated: `ls target/allure-results/`
- [ ] No compilation errors
- [ ] pom.xml changes are valid (if modified)
- [ ] Workflow YAML syntax valid (if modified workflow)

### Simulating CI Environment

**Match CI environment locally:**

```bash
# Use same Java version
sdk use java 17.0.9-tem  # SDKMAN
# Or: export JAVA_HOME=/path/to/jdk-17

# Use same Maven opts
export MAVEN_OPTS="-Xmx2048m -Xms512m"

# Run with batch mode
mvn clean test -B

# Check Allure results
ls -la target/allure-results/
```

---

## Workflow Configuration

### Environment Variables

```yaml
env:
  JAVA_VERSION: '17'           # JDK version for all services
  MAVEN_OPTS: -Xmx2048m -Xms512m  # JVM heap settings
```

**Why these values:**
- **Java 17:** Current LTS version, used by all services
- **Maven heap:** 2GB max prevents OOM during compilation/tests
- **Maven min heap:** 512MB reduces startup overhead

### Timeout Settings

```yaml
timeout-minutes: 15  # backend-service-tests
timeout-minutes: 10  # gateway-service-tests
timeout-minutes: 15  # risk-engine-tests
```

**Why these values:**
- **Backend/Risk Engine:** ~5 min typical, 15 min allows for slow CI runners
- **Gateway:** ~2 min typical, 10 min is generous buffer
- **Prevents hanging:** Workflow won't hang indefinitely if tests deadlock

### Artifact Retention

```yaml
retention-days: 30  # Allure results
retention-days: 7   # Surefire reports
```

**Why different retention:**
- **Allure (30 days):** Detailed reports for historical analysis
- **Surefire (7 days):** Raw XML for quick debugging, not needed long-term

---

## Related Documentation

- **[Backend Allure Setup Guide](backend-allure-setup.md)** - Allure configuration details
- **[Backend Triage Playbook](backend-allure-triage-playbook.md)** - Debugging test failures
- **[Test Architecture](test-architecture.md)** - Testing standards

---

## Future Enhancements

**Story 3.4 (Epic 03):**
- Merge Allure results from all services into single report
- Currently each service has separate artifact

**Story 5.3 (Epic 05):**
- Publish Allure reports to GitHub Pages
- Persistent URL for latest main branch report

**Story 6.4 (Epic 06):**
- Add PR comment with test summary and Allure links
- Inline failure snippets in PR checks

**Story 6.5 (Epic 06):**
- Add automatic retry for flaky tests
- Implement test result caching between runs

---

## Feedback

Have issues or suggestions for the CI workflow? Submit a PR or create an issue in the repository.

**Quick Links:**
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven Caching Best Practices](https://docs.github.com/en/actions/guides/building-and-testing-java-with-maven#caching-dependencies)
- [Allure Framework Documentation](https://docs.qameta.io/allure/)
