# 📊 Security Reports Guide

## How to Access Security Reports

After each workflow run, comprehensive security reports are generated. Here's how to access and interpret them.

---

## 🔍 Quick Access

### Method 1: GitHub Actions Summary (Fastest)
1. Go to **Actions** tab in GitHub
2. Click on the latest **"CDS Security & Quality Assurance"** workflow run
3. Scroll down to see the **Summary** for each service
4. You'll see:
   - ✅ **OWASP Dependency Check** - Vulnerability counts
   - 🐛 **SpotBugs** - Bug counts by priority (High/Medium/Low)

### Method 2: Download Artifacts (Most Detailed)
1. Go to **Actions** tab
2. Click on the workflow run
3. Scroll to **Artifacts** section at the bottom
4. Download:
   - `backend-security-reports`
   - `gateway-security-reports`
   - `risk-engine-security-reports`
5. Unzip and open the HTML files in your browser

---

## 📁 Report Files Explained

Each service generates these reports:

### 🔐 OWASP Dependency Check Reports

| File | Description | Use Case |
|------|-------------|----------|
| `dependency-check-report.html` | **Main Report** - Open this first | Interactive HTML with all vulnerabilities |
| `dependency-check-report.json` | JSON format | For automation/parsing |

**What it shows:**
- Dependencies with known CVEs
- CVSS scores (severity ratings)
- Links to vulnerability databases
- Recommendations for fixes

**Priority:**
- **CVSS 9.0-10.0** = Critical (fix immediately)
- **CVSS 7.0-8.9** = High (fix within days)
- **CVSS 4.0-6.9** = Medium (fix within weeks)
- **CVSS 0.1-3.9** = Low (fix when convenient)

---

### 🐛 SpotBugs Security Reports

| File | Description | Use Case |
|------|-------------|----------|
| `spotbugs.html` | **Main Report** - Open this first | Interactive HTML with all bugs |
| `spotbugsXml.xml` | XML format | For automation/IDE integration |

**What it shows:**
- Security vulnerabilities in your code
- Bug patterns (SQL injection, XSS, etc.)
- Exact file and line number
- Explanation and fix recommendations

**Priority Levels:**
- **High (1)** = Critical security issues (fix immediately)
- **Medium (2)** = Important issues (fix soon)
- **Low (3)** = Minor issues (fix when refactoring)

---

## 📊 Current Known Issues (As of Latest Run)

### Risk Engine - 136 SpotBugs Issues

#### High Priority Issues (Fix First)

1. **DM_DEFAULT_ENCODING** (1 issue)
   - **File:** `OreProcessManager.java:63`
   - **Issue:** Using `new String(byte[])` without specifying charset
   - **Risk:** Data corruption across different systems
   - **Fix:** Use `new String(bytes, StandardCharsets.UTF_8)`

#### Medium Priority Issues (Fix Soon)

1. **COMMAND_INJECTION** (1 issue)
   - **File:** `OreProcessManager.java:40`
   - **Issue:** ProcessBuilder with user input
   - **Risk:** Command injection attacks
   - **Fix:** Validate/sanitize inputs, use safe command construction

2. **PATH_TRAVERSAL_IN** (Multiple issues)
   - **Files:** Various files using `Paths.get()` with user input
   - **Risk:** Users could access files outside intended directories
   - **Fix:** Validate paths, use `Path.normalize()`, check against whitelist

3. **EI_EXPOSE_REP** / **EI_EXPOSE_REP2** (30+ issues)
   - **Files:** Model classes (getters/setters)
   - **Issue:** Returning/storing mutable objects directly
   - **Risk:** External code can modify internal state
   - **Fix:** Return defensive copies or use immutable collections

4. **NP_NULL_ON_SOME_PATH** (2 issues)
   - **Files:** `OreInputBuilder.java`, `OreProcessManager.java`
   - **Issue:** Possible null pointer dereference
   - **Risk:** Null Pointer Exceptions at runtime
   - **Fix:** Add null checks or use Optional

5. **DMI_HARDCODED_ABSOLUTE_FILENAME** (3 issues)
   - **Files:** `OreInputBuilder.java`, `OreOutputParser.java`
   - **Issue:** Hardcoded file paths
   - **Risk:** Not portable across systems
   - **Fix:** Use configuration properties, relative paths

#### Low Priority Issues (Fix When Refactoring)

1. **CRLF_INJECTION_LOGS** (90+ issues)
   - **Risk:** Log injection attacks
   - **Fix:** Sanitize user input before logging
   - **Note:** Low risk if logs are not user-facing

2. **UPM_UNCALLED_PRIVATE_METHOD** (6 issues)
   - **Risk:** Dead code, maintenance burden
   - **Fix:** Remove unused methods

3. **DM_CONVERT_CASE** (6 issues)
   - **Risk:** Locale-dependent behavior
   - **Fix:** Use `toLowerCase(Locale.ROOT)`

---

## 🎯 How to Interpret the Summary

After each run, check the workflow summary:

```
## 🔒 Security Analysis Summary - risk-engine

### 📦 OWASP Dependency Check
- **Vulnerabilities Found:** 0

### 🐛 SpotBugs Security Issues
- **Total Issues:** 136
- **High Priority:** 1
- **Medium Priority:** 35
- **Low Priority:** 100

📄 Download the HTML report from artifacts to see detailed findings.
```

### What This Means:
- **136 total issues** = Needs attention, but workflow continues
- **1 High priority** = Fix this first (encoding issue)
- **35 Medium priority** = Fix soon (security best practices)
- **100 Low priority** = Fix gradually (code quality)

---

## 🔧 How to Fix Issues

### Step 1: Download Reports
```bash
# From Actions page, download artifacts
# Or run locally:
cd risk-engine
./mvnw spotbugs:check
# Report generated at: target/site/spotbugs.html
```

### Step 2: Open HTML Report
- Double-click `spotbugs.html`
- Click on each bug category
- Read the explanation and see exact line numbers

### Step 3: Fix Issues
Example fix for DM_DEFAULT_ENCODING:

**Before:**
```java
new String(process.getInputStream().readAllBytes())
```

**After:**
```java
new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
```

### Step 4: Verify Fix
```bash
./mvnw spotbugs:check
```

### Step 5: Commit
```bash
git add .
git commit -m "fix: Address SpotBugs security issues in OreProcessManager"
git push
```

---

## 📈 Tracking Progress

### Current Status
| Service | SpotBugs Issues | OWASP Vulnerabilities | Status |
|---------|-----------------|----------------------|--------|
| Backend | TBD | TBD | 🔄 Analysis pending |
| Gateway | TBD | TBD | 🔄 Analysis pending |
| Risk Engine | 136 | 0 | ⚠️ Needs fixes |

### Goal
| Priority | Target | Timeline |
|----------|--------|----------|
| High | 0 issues | Within 1 week |
| Medium | < 10 issues | Within 1 month |
| Low | < 50 issues | Ongoing |

---

## 🚨 When to Block Merges

Currently, the workflow uses `continue-on-error: true` so it won't block your work. However, consider blocking merges when:

1. **Critical/High CVSS vulnerabilities** (CVSS ≥ 7.0)
2. **High priority SpotBugs issues** (Priority 1)
3. **Secrets detected** in code

To enforce this, we can set `continue-on-error: false` after initial cleanup.

---

## 💡 Pro Tips

### Tip 1: Filter by Priority
When viewing SpotBugs HTML report:
- Click on **"High"** category first
- Fix those before moving to Medium/Low

### Tip 2: Batch Similar Issues
Many issues are the same pattern repeated:
- Fix one `EI_EXPOSE_REP` issue
- Apply the pattern to all others
- Saves time!

### Tip 3: Suppress False Positives
If SpotBugs flags something incorrectly:
```java
@SuppressFBWarnings(value = "EI_EXPOSE_REP", 
                    justification = "This is intentional for DTO serialization")
public List<String> getItems() {
    return items;
}
```

### Tip 4: Use IDE Integration
Install SpotBugs plugin in your IDE:
- **IntelliJ IDEA**: SpotBugs plugin
- **VS Code**: SpotBugs extension
- **Eclipse**: SpotBugs built-in

See issues while coding, not just in CI!

---

## 📞 Questions?

- **What is SpotBugs?** Static analysis tool that finds bugs in Java code
- **What is OWASP Dependency Check?** Scans dependencies for known vulnerabilities (CVEs)
- **Why 136 issues?** These are pre-existing issues, now visible thanks to the security pipeline
- **Are these all critical?** No, only 1 is High priority, 35 Medium, 100 Low
- **Will this block my work?** No, `continue-on-error: true` is set

---

**Last Updated:** October 16, 2025  
**Workflow Version:** 2.0  
**Maintained By:** Security & DevOps Team
