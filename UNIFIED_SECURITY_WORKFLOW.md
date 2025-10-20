# 🛡️ Unified Security & Quality Workflow

## 🎯 Overview

We've **consolidated** all security scanning into a **single workflow** that reports to a **single SonarCloud dashboard**.

### Before (2 separate workflows ❌)
- ❌ `cds-security-quality.yml` - SpotBugs, Grype, custom checks
- ❌ `security-sonarcloud.yml` - SonarCloud analysis
- ❌ **Two separate reports** to review
- ❌ **SpotBugs findings NOT in SonarCloud**

### After (1 unified workflow ✅)
- ✅ `unified-security-analysis.yml` - Everything in one place
- ✅ **SpotBugs findings imported into SonarCloud**
- ✅ **Single dashboard** for all security & quality metrics
- ✅ **Zero-tolerance violations** still enforced

---

## 🚀 Quick Start

### 1. Configure SonarCloud Token

```bash
# Go to GitHub repository settings
Settings → Secrets and variables → Actions → New repository secret

# Add secret:
Name: SONAR_TOKEN
Value: <your-sonarcloud-token>
```

Get your token from: https://sonarcloud.io/account/security

### 2. Disable Old Workflows

Rename or delete the old workflows:

```bash
# Option 1: Disable by renaming
mv .github/workflows/cds-security-quality.yml .github/workflows/cds-security-quality.yml.disabled
mv .github/workflows/security-sonarcloud.yml .github/workflows/security-sonarcloud.yml.disabled

# Option 2: Delete (after backing up)
git mv .github/workflows/cds-security-quality.yml archive/
git mv .github/workflows/security-sonarcloud.yml archive/
```

### 3. Push and Watch

```bash
git add .github/workflows/unified-security-analysis.yml
git commit -m "feat: unified security workflow with SpotBugs + SonarCloud integration"
git push
```

The workflow runs automatically on:
- ✅ Every push to any branch
- ✅ Every pull request
- ✅ Daily at 2 AM UTC (schedule)
- ✅ Manual trigger (workflow_dispatch)

---

## 📊 What Gets Scanned

### Phase 1: Secrets Detection 🔐
- Hardcoded passwords in config files
- API keys and secrets in Java code
- Database connection strings with credentials
- ORE configuration secrets

**Result:** ❌ **Blocks workflow** if secrets found

### Phase 2: Java Backend Analysis ☕
For each service (backend, gateway, risk-engine):

1. **Build** - Compile with retry logic
2. **Tests** - Run with JaCoCo coverage
3. **SpotBugs** - Security analysis with FindSecBugs plugin
4. **Checkstyle** - Code style compliance
5. **Quality Gate** - Zero-tolerance rules enforcement
6. **Grype** - CVE vulnerability scanning
7. **SonarCloud** - **Imports all SpotBugs findings!**

**Zero-Tolerance Rules (must pass):**
- ❌ **CWE-117** - CRLF Injection in Logs
- ❌ **CWE-330** - Predictable Random Number Generation
- ❌ **CWE-176** - Improper Unicode/Locale Handling
- ❌ **CWE-209** - Information Exposure Through Errors
- ❌ **SQL Injection** - Unparameterized queries

### Phase 3: Frontend Analysis 🎨
- ESLint security rules
- XSS vulnerability detection
- npm audit for package vulnerabilities
- Jest test coverage
- SonarCloud TypeScript analysis

### Phase 4: Unified Report 📋
- Consolidated security dashboard
- Links to all SonarCloud projects
- Downloadable artifacts (SpotBugs, Grype, coverage)
- PR comments with summary

---

## 🌐 SonarCloud Dashboard

### Single Source of Truth

All findings (SpotBugs, Grype, coverage, code smells) are in **one place**:

**Organization Dashboard:**
https://sonarcloud.io/organizations/ayodeleoladeji/projects

**Individual Services:**
- Backend: https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-backend
- Gateway: https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-gateway
- Risk Engine: https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-risk-engine
- Frontend: https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-frontend

### SonarCloud Tabs Explained

| Tab | What You See |
|-----|--------------|
| **Overview** | High-level metrics: bugs, vulnerabilities, code smells, coverage |
| **Issues** | All findings including **imported SpotBugs issues** |
| **Security Hotspots** | Code requiring security review |
| **Measures** | Detailed metrics with historical trends |
| **Code** | Browse code with inline issue annotations |
| **Activity** | History of all scans and quality gate results |

---

## 🔧 How SpotBugs + SonarCloud Integration Works

### The Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. SpotBugs Runs (with FindSecBugs plugin)                 │
│    - Detects CRLF injection, SQL injection, crypto issues  │
│    - Generates: target/spotbugsXml.xml                      │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Quality Gate Checks (Zero-Tolerance Rules)              │
│    - Parses spotbugsXml.xml for critical violations        │
│    - ❌ FAILS if CRLF, Random, Unicode, SQL issues found   │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. SonarCloud Scan Runs                                     │
│    - Reads spotbugsXml.xml via:                             │
│      -Dsonar.java.spotbugs.reportPaths=target/spotbugsXml.xml│
│    - Imports all SpotBugs findings                          │
│    - Adds SonarQube's own analysis                          │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Single Dashboard in SonarCloud                          │
│    - SpotBugs issues appear in "Issues" tab                │
│    - Labeled with source: "external_spotbugs"               │
│    - Combined with SonarQube's native findings              │
└─────────────────────────────────────────────────────────────┘
```

### Key Configuration

In the workflow (`.github/workflows/unified-security-analysis.yml`):

```yaml
# Step 3: Run SpotBugs BEFORE SonarCloud
- name: Run SpotBugs security analysis
  working-directory: ${{ matrix.service }}
  run: |
    ./mvnw spotbugs:spotbugs -Dspotbugs.xmlOutput=true -B

# Step 7: SonarCloud imports SpotBugs findings
- name: SonarCloud Scan (imports SpotBugs findings)
  working-directory: ${{ matrix.service }}
  run: |
    ./mvnw sonar:sonar \
      -Dsonar.java.spotbugs.reportPaths=target/spotbugsXml.xml \
      # ... other params
```

In `sonar-project.properties`:

```properties
# SpotBugs integration
sonar.java.spotbugs.reportPaths=target/spotbugsXml.xml
```

In `spotbugs-security-include.xml`:

```xml
<!-- Zero-Tolerance Rules -->
<Match>
    <Bug pattern="CRLF_INJECTION_LOGS"/>
</Match>
<Match>
    <Bug pattern="PREDICTABLE_RANDOM"/>
</Match>
<Match>
    <Bug pattern="DM_CONVERT_CASE"/>
</Match>
<!-- ... more patterns -->
```

---

## 📥 Downloading Reports

### From GitHub Actions

1. Go to: https://github.com/faculax/credit-default-swap/actions
2. Click on latest workflow run
3. Scroll to **Artifacts** section
4. Download:
   - `backend-security-reports` - SpotBugs XML/HTML, JaCoCo coverage, Grype CVEs
   - `gateway-security-reports`
   - `risk-engine-security-reports`
   - `frontend-security-reports` - npm audit, Jest coverage
   - `unified-security-report` - Consolidated markdown report

### Reports Included

| Artifact | Contains |
|----------|----------|
| **spotbugsXml.xml** | Machine-readable SpotBugs findings |
| **spotbugs.html** | Human-readable SpotBugs report with remediation tips |
| **jacoco/** | Test coverage HTML report |
| **grype-report.json** | CVE vulnerability scan (JSON) |
| **grype-report.txt** | CVE vulnerability scan (table format) |
| **npm-audit.json** | Frontend package vulnerabilities |

---

## 🚨 Troubleshooting

### Issue: SpotBugs findings NOT in SonarCloud

**Symptoms:**
- Workflow passes
- SpotBugs report shows issues
- SonarCloud dashboard is empty or missing SpotBugs findings

**Solution:**
```bash
# 1. Check SpotBugs report was generated
ls risk-engine/target/spotbugsXml.xml

# 2. Verify sonar-project.properties has correct path
grep "spotbugs.reportPaths" risk-engine/sonar-project.properties

# 3. Check workflow step order (SpotBugs MUST run before SonarCloud)
# In .github/workflows/unified-security-analysis.yml:
# Step 3: Run SpotBugs
# Step 7: SonarCloud (comes AFTER SpotBugs)

# 4. Re-run workflow
git commit --allow-empty -m "trigger: re-run unified security scan"
git push
```

### Issue: Quality Gate Fails but SonarCloud Passes

This is **expected behavior**! The workflow has **stricter zero-tolerance rules** than SonarCloud's default quality gate.

**Zero-Tolerance Rules (workflow-specific):**
- CRLF injection → ❌ Immediate failure
- Predictable Random → ❌ Immediate failure
- Improper case conversion → ❌ Immediate failure

**SonarCloud Quality Gate (default):**
- May not include these specific rules
- Configurable per project

**Solution:** This is intentional. The workflow enforces stricter rules based on `AGENTS.md` guidelines.

### Issue: Workflow Takes Too Long

**Normal duration:** 10-15 minutes for all services

**Optimization tips:**
```yaml
# Run services in parallel (already configured)
strategy:
  fail-fast: false  # Don't stop other services if one fails
  matrix:
    service: [backend, gateway, risk-engine]
```

**If still slow:**
- Check Grype scan (can be slow on first run)
- Review test suite execution time
- Consider caching optimization

---

## 📚 References

- **AGENTS.md** - Security standards and zero-tolerance rules
- **SpotBugs Documentation** - https://spotbugs.readthedocs.io/
- **FindSecBugs Patterns** - https://find-sec-bugs.github.io/bugs.htm
- **SonarCloud Docs** - https://docs.sonarcloud.io/
- **Grype Scanner** - https://github.com/anchore/grype

---

## 🎉 Benefits of Unified Workflow

| Before | After |
|--------|-------|
| 2 separate workflows | ✅ **1 unified workflow** |
| SpotBugs findings isolated | ✅ **Imported into SonarCloud** |
| Multiple dashboards to check | ✅ **Single SonarCloud dashboard** |
| Inconsistent quality gates | ✅ **Consistent zero-tolerance rules** |
| Hard to track history | ✅ **SonarCloud historical trends** |
| Manual correlation needed | ✅ **Automatic correlation** |

---

## 💡 Next Steps

1. ✅ **Configure SONAR_TOKEN** in GitHub secrets
2. ✅ **Disable old workflows** (rename or delete)
3. ✅ **Push changes** and watch unified workflow run
4. ✅ **Open SonarCloud dashboard** and explore findings
5. ✅ **Address critical issues** flagged in SonarCloud
6. ✅ **Monitor trends** over time in SonarCloud

---

**Questions?** Check `AGENTS.md` for security guidelines or open an issue!

---

*Unified workflow = Single scan + Single dashboard + Complete visibility* 🚀
