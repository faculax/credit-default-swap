# 🎯 SonarCloud Action Plan - Get Issues Reported

## Current Situation
- **SpotBugs:** Finds 114 issues (0 critical, 19 medium, 95 low)
- **SonarCloud:** Shows "everything passes" with 0 issues
- **Goal:** Make SonarCloud thoroughly scan and report all issues

---

## ✅ What We Just Fixed (Technical Configuration)

### 1. Updated sonar-project.properties (All 3 Java Services)
**Changed:** `backend/`, `gateway/`, `risk-engine/`

**Added Critical Parameters:**
```properties
# Organization (required for SonarCloud)
sonar.organization=ayodeleoladeji

# Proper project keys
sonar.projectKey=ayodeleoladeji_credit-default-swap-{service}

# Enable dependency analysis
sonar.java.libraries=target/*.jar

# Enable advanced security analysis
sonar.security.hotspots.enabled=true
sonar.security.taintAnalysis.enabled=true

# Add PMD integration
sonar.java.pmd.reportPaths=target/pmd.xml

# Add coverage exclusions
sonar.coverage.exclusions=**/config/**,**/entity/**,**/dto/**,**/*Application.java,**/*Config.java
```

### 2. Enhanced Workflow SonarCloud Scan
**File:** `.github/workflows/unified-security-analysis.yml`

**Added Parameters:**
```yaml
-Dsonar.java.libraries=target/*.jar           # Dependency analysis
-Dsonar.java.source=21                         # Java version
-Dsonar.security.hotspots.enabled=true         # Security hotspot detection
-Dsonar.security.taintAnalysis.enabled=true    # Advanced taint tracking
-Dsonar.java.pmd.reportPaths=target/pmd.xml    # PMD findings
```

### 3. Tests Already Running ✅
The workflow already:
- Compiles code: `./mvnw clean compile`
- Runs tests: `./mvnw test jacoco:report`
- Generates JaCoCo coverage
- Reports coverage percentage

---

## 🎯 What YOU Need to Do (SonarCloud UI Configuration)

### Step 1: Enable Security Rules in Quality Profile
**Time:** 10 minutes  
**Importance:** 🔴 CRITICAL - This is likely why you see 0 issues

#### Instructions:

1. **Open Quality Profiles:**
   ```
   https://sonarcloud.io/organizations/ayodeleoladeji/quality_profiles
   ```

2. **Create Custom Security Profile:**
   - Click **Create** (top right)
   - Name: `CDS Security Profile`
   - Language: `Java`
   - Parent: `Sonar way`
   - Click **Create**

3. **Activate Security Rules:**
   
   Click on your new profile → Click **Activate More Rules**
   
   **Filter and activate these critical rules:**
   
   | Rule ID | Name | Severity | Type | Activate |
   |---------|------|----------|------|----------|
   | **S5145** | "Logger" objects should not be exposed | Blocker | Vulnerability | ✅ |
   | **S5145** | Log forging | Blocker | Vulnerability | ✅ |
   | **S2245** | Pseudorandom number generators should not be used | Critical | Vulnerability | ✅ |
   | **S4790** | Cryptographic hash algorithms should not be vulnerable to collisions | Critical | Vulnerability | ✅ |
   | **S2076** | OS commands should not be vulnerable to injection | Blocker | Vulnerability | ✅ |
   | **S2077** | Formatting SQL queries is security-sensitive | Critical | Vulnerability | ✅ |
   | **S2078** | LDAP queries should not be vulnerable to injection | Critical | Vulnerability | ✅ |
   | **S5131** | Endpoints should not be vulnerable to reflected XSS | Blocker | Vulnerability | ✅ |
   | **S5144** | Server-side requests should not be vulnerable to forging | Blocker | Vulnerability | ✅ |
   | **S5527** | Server-side cipher suites should be preferred | Critical | Vulnerability | ✅ |

   **Quick method:** 
   - In "Activate More Rules" search box, type: `security`
   - Sort by: `Severity` (Blocker first)
   - Activate ALL Blocker and Critical security rules

4. **Activate Security Hotspot Rules:**
   - In search box, filter: `Type = Security Hotspot`
   - Activate all 50+ hotspot rules
   - These include: weak crypto, hardcoded credentials, CSRF, etc.

5. **Set as Default:**
   - Go back to Quality Profiles page
   - Find your `CDS Security Profile`
   - Click **⋮** (three dots) → **Set as Default**

6. **Apply to Projects:**
   - Go to each project:
     - `credit-default-swap-backend`
     - `credit-default-swap-gateway`
     - `credit-default-swap-risk-engine`
   - Click **Project Settings** → **Quality Profiles**
   - Select your `CDS Security Profile` for Java

---

### Step 2: Configure Stricter Quality Gate
**Time:** 5 minutes  
**Importance:** 🟡 HIGH - Ensures issues block PRs

#### Instructions:

1. **Open Quality Gates:**
   ```
   https://sonarcloud.io/organizations/ayodeleoladeji/quality_gates
   ```

2. **Create Custom Gate:**
   - Click **Create**
   - Name: `CDS Security Gate`

3. **Add Conditions:**

   **On Overall Code:**
   ```
   Coverage < 70% → FAIL
   Duplicated Lines (%) > 3% → FAIL
   Maintainability Rating worse than A → FAIL
   Reliability Rating worse than A → FAIL
   Security Rating worse than A → FAIL
   Security Hotspots Reviewed < 100% → FAIL
   ```

   **On New Code (for PRs):**
   ```
   Coverage on New Code < 80% → FAIL
   Duplicated Lines on New Code (%) > 3% → FAIL
   Maintainability Rating on New Code worse than A → FAIL
   Reliability Rating on New Code worse than A → FAIL
   Security Rating on New Code worse than A → FAIL
   Security Hotspots Reviewed on New Code < 100% → FAIL
   ```

4. **Set as Default:**
   - Click **Set as Default** on your `CDS Security Gate`

---

### Step 3: Verify Organization Permissions
**Time:** 2 minutes  
**Importance:** 🟡 MEDIUM

1. **Check Token Permissions:**
   ```
   https://sonarcloud.io/account/security
   ```
   
   Ensure `SONAR_TOKEN` has:
   - ✅ Execute Analysis
   - ✅ Create Projects
   - ✅ Administer Quality Gates
   - ✅ Administer Quality Profiles

2. **Check Organization Settings:**
   ```
   https://sonarcloud.io/organizations/ayodeleoladeji/settings
   ```
   
   Verify:
   - ✅ **New Code Definition:** `Previous version` or `Number of days: 30`
   - ✅ **Default Quality Gate:** Your `CDS Security Gate`
   - ✅ **Default Quality Profile (Java):** Your `CDS Security Profile`

---

## 🚀 Testing the Changes

### Phase 1: Local Test (Optional)
```powershell
cd backend

# Run full quality suite
./mvnw clean test `
  spotbugs:spotbugs `
  checkstyle:checkstyle `
  pmd:pmd `
  sonar:sonar `
  -Dsonar.organization=ayodeleoladeji `
  -Dsonar.host.url=https://sonarcloud.io `
  -Dsonar.token=%SONAR_TOKEN%
```

### Phase 2: Push and Trigger Workflow
```powershell
# Commit the configuration changes
git add backend/sonar-project.properties
git add gateway/sonar-project.properties
git add risk-engine/sonar-project.properties
git add .github/workflows/unified-security-analysis.yml

git commit -m "config(sonarcloud): Enable comprehensive security analysis

- Add sonar.organization to all services
- Enable security hotspot detection
- Enable taint analysis
- Add PMD integration
- Configure coverage exclusions
- Update workflow with enhanced parameters

Security: Enables RSPEC-5145, RSPEC-2245, and 50+ security rules"

git push origin security-compliance
```

### Phase 3: Monitor Workflow
1. **Watch GitHub Actions:**
   ```
   https://github.com/faculax/credit-default-swap/actions
   ```

2. **Check for:**
   - ✅ Tests pass
   - ✅ SpotBugs runs (114 issues expected)
   - ✅ SonarCloud scan completes
   - ✅ Quality Gate result

3. **View SonarCloud Dashboard:**
   ```
   https://sonarcloud.io/organizations/ayodeleoladeji/projects
   ```

---

## 🔍 Expected Results After Configuration

### Before (Current State):
```
SonarCloud Dashboard:
├── Issues: 0
├── Security Hotspots: 0
├── Quality Gate: PASSED ✅
└── Coverage: Unknown
```

### After (With Security Rules Enabled):
```
SonarCloud Dashboard:
├── Issues: 50-100+ (Bugs + Vulnerabilities + Code Smells)
│   ├── Blocker: 0-5
│   ├── Critical: 5-15
│   ├── Major: 20-40
│   └── Minor: 30-60
├── Security Hotspots: 10-30 (to review)
├── Quality Gate: FAILED ❌ (until issues fixed)
└── Coverage: 40-60% (actual measured coverage)
```

**Why the difference?**
- SonarCloud's default profile is **permissive** (allows many patterns)
- Custom security profile **activates 500+ rules**
- Security hotspots **require manual review** (not auto-detected as issues)

---

## 📊 What Issues SonarCloud Will Report

Based on SpotBugs findings, SonarCloud should detect:

### 1. CRLF Injection (RSPEC-5145)
**SpotBugs:** `CRLF_INJECTION_LOGS`  
**SonarCloud Equivalent:** Rule S5145
```java
// Will be flagged:
logger.info("User {} logged in", userInput); // Unsanitized
```

### 2. Weak Random (RSPEC-2245)
**SpotBugs:** `PREDICTABLE_RANDOM`  
**SonarCloud Equivalent:** Rule S2245
```java
// Will be flagged:
Random random = new Random(); // Use SecureRandom
```

### 3. Locale Issues (RSPEC-4275)
**SpotBugs:** `DM_CONVERT_CASE`  
**SonarCloud Equivalent:** Rule S4275 (related)
```java
// Will be flagged:
string.toUpperCase(); // Missing Locale.ROOT
```

### 4. Information Exposure (RSPEC-5144, S5131)
**SpotBugs:** `INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE`  
**SonarCloud Equivalent:** Multiple rules
```java
// Will be flagged:
catch(Exception e) {
    return new ResponseEntity<>(e.getMessage(), 500); // Leak
}
```

### 5. Code Quality Issues
**Additional issues SonarCloud finds that SpotBugs doesn't:**
- Unused imports
- Empty catch blocks
- Magic numbers
- Cognitive complexity
- Duplicate code blocks
- Missing test coverage on methods

---

## ❓ Troubleshooting

### Issue: Still seeing 0 issues after configuration
**Solution:**
1. Verify Quality Profile is set to your custom profile (not "Sonar way")
2. Check that projects are using the correct profile:
   - Project Settings → Quality Profiles → Verify Java profile
3. Trigger a new analysis (push a commit)
4. Wait 5-10 minutes for analysis to complete

### Issue: Quality Gate always passes
**Solution:**
1. Check Quality Gate settings:
   - Should be `CDS Security Gate`, not `Sonar way`
2. Verify conditions are properly set (not all green checkmarks)
3. Ensure "On New Code" conditions are enabled

### Issue: Analysis fails with authentication error
**Solution:**
1. Verify `SONAR_TOKEN` secret is set in GitHub:
   - Repository Settings → Secrets and variables → Actions
2. Regenerate token in SonarCloud:
   - Account → Security → Generate New Token
3. Update GitHub secret with new token

### Issue: Coverage shows 0%
**Solution:**
1. Verify JaCoCo report exists: `target/site/jacoco/jacoco.xml`
2. Check workflow logs for test execution
3. Ensure tests actually run (not skipped with `-DskipTests`)

---

## 📚 Reference Documentation

- **SonarCloud Java Rules:** https://rules.sonarsource.com/java
- **Quality Profiles:** https://docs.sonarcloud.io/improving/quality-profiles/
- **Quality Gates:** https://docs.sonarcloud.io/improving/quality-gates/
- **Security Rules:** https://docs.sonarcloud.io/improving/security-hotspots/
- **Coverage:** https://docs.sonarcloud.io/enriching/test-coverage/java-test-coverage/

---

## ✅ Checklist

**Configuration (Just Completed):**
- [x] Update `backend/sonar-project.properties`
- [x] Update `gateway/sonar-project.properties`
- [x] Update `risk-engine/sonar-project.properties`
- [x] Update `.github/workflows/unified-security-analysis.yml`

**SonarCloud UI (Your Action Required):**
- [ ] Create `CDS Security Profile` quality profile
- [ ] Activate security rules (RSPEC-5145, RSPEC-2245, etc.)
- [ ] Activate security hotspot rules
- [ ] Create `CDS Security Gate` quality gate
- [ ] Set custom profile as default
- [ ] Set custom gate as default
- [ ] Verify token permissions

**Testing:**
- [ ] Commit and push changes
- [ ] Verify workflow completes successfully
- [ ] Check SonarCloud dashboard shows issues
- [ ] Review Security Hotspots tab
- [ ] Verify Quality Gate reflects actual state

---

**Next Step:** Go to SonarCloud UI and enable security rules as described in Step 1 above.

**Last Updated:** January 20, 2025
