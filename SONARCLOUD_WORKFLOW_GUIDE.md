# 🌐 SonarCloud-Only Analysis Workflow

## Overview

This is a **streamlined SonarCloud-focused workflow** that runs separately from the unified security analysis. It's ideal for quick code quality checks without the full security scanning overhead.

## File Created

**Location:** `.github/workflows/sonarcloud-analysis.yml`

---

## 🎯 Purpose

### When to Use This Workflow
- **Quick quality checks** on feature branches
- **Focused SonarCloud analysis** without SpotBugs overhead
- **Manual triggers** for on-demand analysis (`workflow_dispatch`)
- **PR quality gates** for main/develop branches

### When to Use Unified Workflow Instead
- Full security analysis needed (secrets scan, Grype, etc.)
- Comprehensive security reports required
- Scheduled nightly scans
- Release branch validations

---

## 🔧 Key Features

### ✅ What's Included

1. **Separate Jobs for Each Service**
   - Backend (Java 21)
   - Gateway (Java 21)
   - Risk Engine (Java 21)
   - Frontend (Node 20)

2. **Comprehensive Analysis**
   - ✅ Test execution with coverage
   - ✅ SpotBugs security scanning (SARIF format)
   - ✅ Checkstyle code style checking
   - ✅ SonarCloud native analyzers
   - ✅ Quality gate enforcement

3. **Smart Report Import**
   - Conditional import of SpotBugs SARIF
   - Conditional import of JaCoCo coverage
   - Conditional import of Checkstyle results
   - Falls back gracefully if reports missing

4. **Artifact Upload**
   - All analysis reports saved as artifacts
   - 30-day retention
   - Downloadable for offline review

5. **Summary Dashboard**
   - Overall status per service
   - Direct links to SonarCloud dashboards
   - GitHub Actions summary page

---

## 📊 Comparison: SonarCloud vs Unified Workflow

| Feature | SonarCloud Workflow | Unified Workflow |
|---------|---------------------|------------------|
| **Secrets Scanning** | ❌ No | ✅ Yes (blocking) |
| **SpotBugs** | ✅ Yes (SARIF) | ✅ Yes (XML + SARIF) |
| **SonarCloud** | ✅ Yes | ✅ Yes |
| **Grype CVE Scan** | ❌ No | ✅ Yes |
| **npm Audit** | ❌ No | ✅ Yes |
| **PR Comments** | ❌ No | ✅ Yes (detailed) |
| **Execution Time** | ~10-15 min | ~20-30 min |
| **Best For** | Quick checks | Full security audit |

---

## 🚀 Usage

### Automatic Triggers

```yaml
on:
  push:
    branches:
      - main
      - develop
      - security-compliance
  pull_request:
    types: [opened, synchronize, reopened]
  workflow_dispatch:  # Manual trigger
```

### Manual Trigger

1. Go to: **Actions** tab in GitHub
2. Select: **SonarCloud Analysis** workflow
3. Click: **Run workflow**
4. Select branch
5. Click: **Run workflow** button

---

## 📋 What Gets Analyzed

### Backend, Gateway, Risk Engine (Java Services)

**Build Phase:**
```bash
./mvnw clean compile -DskipTests -B
```

**Test Phase:**
```bash
./mvnw test jacoco:report -B
```

**Security Phase:**
```bash
./mvnw spotbugs:spotbugs -B
```

**Quality Phase:**
```bash
./mvnw checkstyle:check -B
```

**SonarCloud Phase:**
```bash
./mvnw sonar:sonar \
  -Dsonar.projectKey=ayodeleoladeji_credit-default-swap-{service} \
  -Dsonar.organization=ayodeleoladeji \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
  -Dsonar.sarifReportPaths=target/spotbugs.sarif \
  -Dsonar.java.checkstyle.reportPaths=target/checkstyle-result.xml \
  -Dsonar.qualitygate.wait=true
```

### Frontend (TypeScript/React)

**Test Phase:**
```bash
npm run test -- --coverage --watchAll=false --passWithNoTests
```

**SonarCloud Phase:**
```bash
sonarsource/sonarcloud-github-action@master \
  -Dsonar.projectKey=ayodeleoladeji_credit-default-swap-frontend \
  -Dsonar.organization=ayodeleoladeji \
  -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info \
  -Dsonar.qualitygate.wait=true
```

---

## 🔍 Viewing Results

### SonarCloud Dashboards

After workflow completes, view results:

| Service | Dashboard URL |
|---------|--------------|
| **Backend** | https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-backend |
| **Gateway** | https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-gateway |
| **Risk Engine** | https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-risk-engine |
| **Frontend** | https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-frontend |

### GitHub Actions Summary

1. Go to **Actions** tab
2. Click on workflow run
3. Scroll to bottom for **Summary** section
4. View status table and dashboard links

### Download Artifacts

1. Go to workflow run page
2. Scroll to **Artifacts** section
3. Download:
   - `backend-analysis-reports`
   - `gateway-analysis-reports`
   - `risk-engine-analysis-reports`
   - `frontend-analysis-reports`
   - `sonarcloud-analysis-summary`

---

## ⚙️ Configuration

### Environment Variables

```yaml
env:
  JAVA_VERSION: '21'          # Java version for backend services
  NODE_VERSION: '20'          # Node version for frontend
  SONAR_ORGANIZATION: 'ayodeleoladeji'  # Your SonarCloud org
  SONAR_HOST_URL: 'https://sonarcloud.io'
```

### Required Secrets

Set these in GitHub repo **Settings → Secrets → Actions**:

| Secret | Description | Example |
|--------|-------------|---------|
| `SONAR_TOKEN` | SonarCloud authentication token | `squ_1234567890abcdef...` |

### How to Get SONAR_TOKEN

1. Go to: https://sonarcloud.io/account/security
2. Generate token with name: `GitHub Actions - credit-default-swap`
3. Copy token
4. Add to GitHub Secrets

---

## 🎨 Key Improvements Over Original

### 1. Updated Java Version
```diff
- java-version: '17'
+ java-version: '21'
```

### 2. Updated Node Version
```diff
- node-version: '18'
+ node-version: '20'
```

### 3. Added SARIF Import
```diff
+ if [ -f "target/spotbugs.sarif" ]; then
+   SONAR_ARGS="$SONAR_ARGS -Dsonar.sarifReportPaths=target/spotbugs.sarif"
+ fi
```

### 4. Added Comprehensive Logging
```diff
+ echo "📡 Running SonarCloud analysis..."
+ echo "✅ JaCoCo coverage report found"
+ echo "✅ SpotBugs SARIF report found"
+ echo "🌐 View: https://sonarcloud.io/dashboard?id=..."
```

### 5. Added Test Execution
```diff
+ - name: Run tests with coverage
+   run: ./mvnw test jacoco:report -B
```

### 6. Added SpotBugs Scanning
```diff
+ - name: Run SpotBugs security analysis
+   run: ./mvnw spotbugs:spotbugs -B || true
```

### 7. Added Checkstyle
```diff
+ - name: Run Checkstyle
+   run: ./mvnw checkstyle:check -B || true
```

### 8. Added Summary Job
```diff
+ analysis-summary:
+   needs: [sonarcloud-backend, ...]
+   run: Generate summary report
```

---

## 🔧 Troubleshooting

### Issue: Workflow fails with "SONAR_TOKEN not found"

**Solution:**
```bash
# Check secret exists
gh secret list

# If not, add it
gh secret set SONAR_TOKEN --body "squ_your_token_here"
```

### Issue: Quality Gate fails

**Possible causes:**
1. SonarCloud security rules not enabled → See `HOW_TO_ENABLE_SONARCLOUD_SECURITY_PROFILE.md`
2. Code coverage below threshold → Run more tests
3. New bugs/vulnerabilities introduced → Fix code issues

**Solution:**
1. Check SonarCloud dashboard for details
2. Filter issues by "On New Code"
3. Fix issues and re-run workflow

### Issue: SpotBugs SARIF not imported

**Check:**
```bash
# Verify SARIF file exists in artifacts
# Download artifact and check: target/spotbugs.sarif

# Verify pom.xml has:
<formats>
  <format>xml</format>
  <format>sarif</format>
</formats>
```

### Issue: No coverage data shown

**Check:**
```bash
# Verify JaCoCo plugin in pom.xml
# Verify tests actually run (not skipped)
# Check artifact contains: target/site/jacoco/jacoco.xml
```

---

## ✅ Verification Checklist

Before first run:

- [ ] `SONAR_TOKEN` secret added to GitHub
- [ ] All services have latest `pom.xml` with SpotBugs 4.8.3.1
- [ ] All services have SARIF format enabled in SpotBugs config
- [ ] All `sonar-project.properties` updated with organization
- [ ] SonarCloud projects created for all 4 services
- [ ] SonarCloud quality profile configured with security rules

After first run:

- [ ] All 4 jobs completed successfully
- [ ] Summary report shows ✅ for all services
- [ ] SonarCloud dashboards accessible
- [ ] Artifacts uploaded correctly
- [ ] Quality gates evaluated (may fail if issues exist)

---

## 📚 Related Documentation

- **HOW_TO_ENABLE_SONARCLOUD_SECURITY_PROFILE.md** - Enable security rules
- **SARIF_INTEGRATION_COMPLETE.md** - SARIF format setup
- **SONARCLOUD_ACTION_PLAN.md** - Complete SonarCloud guide
- **FINAL_CHECKLIST.md** - Overall setup checklist

---

## 💡 Best Practices

### 1. Run Before Opening PR
```bash
# Trigger manually before creating PR
gh workflow run sonarcloud-analysis.yml --ref your-branch
```

### 2. Monitor Quality Trends
- Check SonarCloud dashboard weekly
- Address new issues promptly
- Don't accumulate technical debt

### 3. Use Both Workflows
- **This workflow:** Quick daily checks
- **Unified workflow:** Full security audit before releases

### 4. Keep Rules Updated
- Review SonarCloud quality profile monthly
- Activate new security rules as they're added
- Adjust thresholds based on project maturity

---

**Created:** January 20, 2025  
**Based on:** `unified-security-analysis.yml`  
**Purpose:** Streamlined SonarCloud-only analysis
