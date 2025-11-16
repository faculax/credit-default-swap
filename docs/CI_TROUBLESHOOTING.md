# CI/CD Troubleshooting Runbook

This document provides guidance for troubleshooting and resolving common CI/CD issues in the CDS Platform testing infrastructure.

---

## Table of Contents

- [Quick Reference](#quick-reference)
- [Test Failures](#test-failures)
- [Artifact Issues](#artifact-issues)
- [Caching Problems](#caching-problems)
- [Timeout Issues](#timeout-issues)
- [Report Generation Failures](#report-generation-failures)
- [Performance Optimization](#performance-optimization)

---

## Quick Reference

### Workflow Timeouts

| Workflow | Job | Timeout |
|----------|-----|---------|
| Backend Tests | Backend Service | 15 min |
| Backend Tests | Gateway Service | 10 min |
| Backend Tests | Risk Engine | 15 min |
| Frontend Tests | Unit Tests | 10 min |
| Frontend Tests | Integration Tests | 10 min |
| Frontend Tests | E2E Tests | 15 min |
| Unified Reports | Merge & Publish | 20 min |

### Retry Configuration

**Maven/Surefire (Backend Services)**
- Retry count: 2 attempts
- Configuration: `-Dsurefire.rerunFailingTestsCount=2`
- Flaky tests will be marked in reports

**Cypress (Frontend E2E)**
- Retry count: 2 attempts
- Configuration: `retries=2` in cypress-io/github-action
- Screenshots captured on failure

**Jest (Frontend Unit/Integration)**
- Retry count: 3 attempts total
- Configuration: `--maxAttempts=3`
- Individual test retries via jest-retry

**Allure CLI Download**
- Retry count: 3 attempts
- Backoff: 2 seconds between attempts

---

## Test Failures

### Flaky Tests

**Symptom**: Tests pass on retry but fail initially

**Detection**:
- Check workflow output for "Flaky Tests Detected" warnings
- Review Allure report's "flaky" status
- Look for `flakes` count in test output

**Resolution**:
1. Review test logs to identify timing/race conditions
2. Add proper waits or synchronization
3. Consider increasing test timeout
4. If persistent, mark test as flaky in code

**Example Output**:
```
::warning title=Flaky Tests Detected::2 flaky test(s) detected - passed on retry
```

### Consistent Failures

**Symptom**: Tests fail even after retries

**Investigation Steps**:
1. Check workflow logs for specific error messages
2. Download Allure artifacts for detailed failure analysis
3. Review recent code changes that might have introduced regression
4. Check if failure is environment-specific (CI vs local)

**Common Causes**:
- **Backend**: Database connection issues, missing test data, dependency injection failures
- **Frontend**: Component rendering issues, API mocking problems, DOM query failures
- **E2E**: Application not fully loaded, selector changes, timing issues

**Resolution**:
1. Run tests locally with same configuration
2. Add debug logging to failing tests
3. Check for missing environment variables
4. Verify test dependencies are up to date

---

## Artifact Issues

### Missing Allure Results

**Symptom**: `if-no-files-found: error` or warning in artifact upload

**Causes**:
- Tests didn't run (build failure)
- Allure results directory not created
- Path mismatch in workflow

**Resolution**:
1. Check if tests actually executed:
   ```bash
   # Backend
   ls -la backend/target/allure-results/
   
   # Frontend
   ls -la frontend/allure-results/
   ```

2. Verify Allure configuration:
   - **Maven**: Check `allure-maven` plugin in `pom.xml`
   - **Jest**: Check `jest-allure2-reporter` in `jest.config.js`

3. Update workflow artifact path if project structure changed

### Artifact Download Failures

**Symptom**: `actions/download-artifact` fails in unified-reports workflow

**Causes**:
- Artifacts not uploaded in previous workflow run
- Artifact name mismatch
- Artifact expired (30-day retention)
- GitHub Actions API rate limiting

**Resolution**:
1. Check previous workflow run for successful artifact uploads
2. Verify artifact name matches pattern:
   - `allure-results-backend-{run_number}`
   - `allure-results-frontend-unit-{run_number}`
   - `allure-results-gateway-{run_number}`
   - `allure-results-risk-engine-{run_number}`

3. Use `continue-on-error: true` for non-critical services (already configured)

4. Check GitHub Actions UI for artifact availability

---

## Caching Problems

### Cache Miss Rate High

**Symptom**: Dependencies re-downloaded frequently, slow build times

**Investigation**:
1. Check cache hit rate in workflow logs:
   ```
   Cache restored from key: ...
   Cache not found for input keys: ...
   ```

2. Review cache key configuration:
   - Maven: `${{ runner.os }}-maven-{service}-${{ hashFiles('**/pom.xml') }}`
   - npm: `${{ runner.os }}-node-${{ hashFiles('frontend/package-lock.json') }}`
   - Cypress: `${{ runner.os }}-cypress-${{ hashFiles('frontend/package-lock.json') }}`

**Resolution**:
1. Ensure lock files (`package-lock.json`, `pom.xml`) are committed
2. Clear stale caches if dependencies changed significantly:
   - Go to GitHub Actions → Caches → Delete old caches
3. Verify cache paths are correct

### Stale Cache Issues

**Symptom**: Old dependencies used despite lock file changes

**Causes**:
- Cache key not updated when dependencies change
- Multiple cache restore keys matching old versions

**Resolution**:
1. Update cache configuration to be more specific
2. Clear all caches for affected workflow
3. Force cache rebuild by updating lock file version comment

---

## Timeout Issues

### Job Timeout

**Symptom**: Workflow cancelled with "The job running on runner ... has exceeded the maximum execution time"

**Current Timeouts**:
- Backend service tests: 15 minutes
- Gateway tests: 10 minutes
- Risk engine tests: 15 minutes
- Frontend unit tests: 10 minutes
- Frontend E2E tests: 15 minutes
- Unified report generation: 20 minutes

**Resolution**:
1. Identify which step timed out from logs
2. Check for hung processes or infinite loops
3. If legitimate slow tests, increase timeout:

```yaml
jobs:
  my-job:
    timeout-minutes: 25  # Increase job timeout
    steps:
      - name: Slow step
        timeout-minutes: 15  # Increase step timeout
```

4. Consider parallelizing tests to reduce duration

### Step Timeout

**Symptom**: Individual step times out but job continues

**Common Slow Steps**:
- npm/Maven dependency installation
- Test execution with many tests
- Allure report generation with large results
- Docker image builds

**Resolution**:
1. Check if timeout is due to actual slowness or hanging
2. Add progress logging to identify bottleneck
3. Optimize slow operations:
   - Use `npm ci --prefer-offline` for faster installs
   - Enable Maven parallel test execution
   - Split large test suites into parallel jobs

---

## Report Generation Failures

### Allure Generate Fails

**Symptom**: `allure generate` command fails with error

**Common Errors**:

#### Invalid JSON Files
```
Error: Invalid JSON in result file
```

**Resolution**:
1. Check for corrupted result files:
   ```bash
   for file in allure-results-unified/*.json; do
     jq empty "$file" 2>/dev/null || echo "Invalid: $file"
   done
   ```

2. Workflow already includes JSON validation - check which files were moved to `.invalid`

3. Investigate why invalid JSON was generated (test framework issue, disk space, etc.)

#### Missing executor.json
```
java.lang.NullPointerException at ...
```

**Resolution**:
Workflow automatically creates `executor.json` if missing. If error persists:
```bash
cat > allure-results-unified/executor.json << EOF
{
  "name": "GitHub Actions",
  "type": "github",
  "buildName": "Run #${BUILD_NUMBER}"
}
EOF
```

### GitHub Pages Deployment Fails

**Symptom**: Unified report not published to GitHub Pages

**Causes**:
- Permissions issue (Pages write, id-token write)
- Pages not enabled for repository
- Large report size (>1GB)

**Resolution**:
1. Verify GitHub Pages is enabled:
   - Repo Settings → Pages → Source: GitHub Actions

2. Check workflow permissions:
```yaml
permissions:
  contents: read
  pages: write
  id-token: write
```

3. If report too large, consider:
   - Reducing attachment sizes
   - Archiving old reports
   - Splitting into multiple deployments

---

## Performance Optimization

### Slow Test Execution

**Benchmarks** (approximate):
- Backend unit tests: ~30-60 seconds
- Gateway tests: ~10-20 seconds
- Risk engine tests: ~60-90 seconds
- Frontend unit tests: ~10-20 seconds
- Frontend E2E tests: ~2-3 minutes

**Optimization Strategies**:

1. **Parallel Execution**
   ```yaml
   # Maven
   mvn test -T 1C  # 1 thread per CPU core
   
   # Jest
   npm test -- --maxWorkers=50%
   ```

2. **Selective Test Execution**
   ```yaml
   # Run only affected tests based on changed files
   - name: Get changed files
     uses: tj-actions/changed-files@v40
   
   - name: Run affected tests
     run: mvn test -pl $(changed_modules)
   ```

3. **Test Sharding**
   ```yaml
   strategy:
     matrix:
       shard: [1, 2, 3, 4]
   steps:
     - run: npm test -- --shard=${{ matrix.shard }}/4
   ```

### Slow Dependency Installation

**Current Optimizations**:
- npm: `--prefer-offline --no-audit`
- Maven: `actions/cache` with `.m2/repository`
- Cypress: Binary cached separately

**Additional Optimizations**:
1. Use `npm ci` instead of `npm install` (already done)
2. Cache Docker layers for containerized tests
3. Use dependency proxy/mirror for faster downloads

### Large Artifact Sizes

**Monitoring**:
- Check artifact sizes in GitHub Actions UI
- Allure results typically 1-10 MB per service
- Cypress videos can be large (5-50 MB)

**Reduction Strategies**:
1. Limit screenshot/video capture to failures only (already configured)
2. Compress artifacts before upload:
   ```yaml
   - run: tar -czf allure-results.tar.gz allure-results/
   - uses: actions/upload-artifact@v4
     with:
       path: allure-results.tar.gz
   ```

3. Reduce artifact retention from 30 to 14 days for non-critical artifacts

---

## Emergency Procedures

### Complete CI Failure

**Symptom**: All workflows failing across all branches

**Steps**:
1. Check GitHub Status page for platform issues
2. Review recent workflow file changes
3. Rollback workflow changes if recently modified
4. Test workflow syntax: https://www.actionsplugin.com/checker

### Runaway Costs

**Symptom**: Excessive GitHub Actions minutes consumed

**Investigation**:
1. Check workflow run duration in Actions tab
2. Look for hung jobs or excessive retries
3. Review concurrent workflow runs

**Mitigation**:
1. Cancel running workflows if stuck
2. Temporarily disable problematic workflows
3. Add aggressive timeouts
4. Use `concurrency` to prevent duplicate runs:
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

---

## Additional Resources

- **GitHub Actions Documentation**: https://docs.github.com/actions
- **Allure Framework Docs**: https://docs.qameta.io/allure/
- **Maven Surefire Plugin**: https://maven.apache.org/surefire/maven-surefire-plugin/
- **Jest Retry**: https://www.npmjs.com/package/jest-retry
- **Cypress Retries**: https://docs.cypress.io/guides/guides/test-retries

---

## Contact

For CI/CD issues not covered here:
1. Check existing GitHub Issues
2. Review recent PRs for related changes
3. Consult platform/DevOps team
4. Create incident ticket with:
   - Workflow run link
   - Error logs
   - Steps to reproduce
