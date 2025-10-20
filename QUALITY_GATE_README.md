# 🛡️ Quality Gate System

## Overview

The CDS Platform enforces **zero-tolerance security policies** through automated quality gates at multiple levels:

1. **Pre-Commit Hook** - Fast checks before committing
2. **Local Quality Gate** - Comprehensive checks before pushing
3. **CI/CD Pipeline** - Automated checks on GitHub Actions

---

## 🚀 Quick Start

### 1. Install Pre-Commit Hook (Recommended)

```bash
# Linux/Mac
cp git-hooks/pre-commit.sample .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Windows (PowerShell as Administrator)
Copy-Item git-hooks\pre-commit.sample .git\hooks\pre-commit
```

This will automatically check your code before each commit.

### 2. Run Local Quality Gate

Before pushing to the repository, run the full quality gate:

```bash
# Linux/Mac
./quality-gate-check.sh              # Check all services
./quality-gate-check.sh backend      # Check specific service

# Windows (PowerShell)
.\quality-gate-check.ps1             # Check all services
.\quality-gate-check.ps1 -Service backend  # Check specific service
```

---

## 📋 Quality Gate Rules

### Zero-Tolerance Security Rules (Build-Breaking)

These violations will **fail the build** and must be fixed immediately:

| Rule | Vulnerability | CWE | Severity |
|------|--------------|-----|----------|
| **CRLF Injection** | Log forgery through untrusted input | CWE-117 | 🔴 CRITICAL |
| **Predictable Random** | Using `java.util.Random` in security contexts | CWE-330 | 🔴 CRITICAL |
| **Unicode Handling** | Missing `Locale.ROOT` in case operations | CWE-176 | 🔴 HIGH |
| **Information Exposure** | Exposing stack traces to clients | CWE-209 | 🔴 HIGH |
| **SQL Injection** | String concatenation in queries | CWE-89 | 🔴 CRITICAL |

### Warning-Level Checks (Should Fix)

These violations generate **warnings** but don't fail the build:

- Client-controlled authorization
- Disabled security features (CORS, CSRF)
- Weak cryptographic algorithms
- Sensitive data in logs
- Test coverage below 80%

---

## 🔍 Quality Metrics

| Metric | Threshold | Status |
|--------|-----------|--------|
| **Test Coverage** | ≥ 80% | 🟢 Enforced |
| **SpotBugs High Priority** | 0 | 🔴 Zero-Tolerance |
| **SpotBugs Medium Priority** | < 10 | 🟡 Monitored |
| **CVE Severity** | < 7.0 (High) | 🔴 Zero-Tolerance |
| **Build Success** | 100% | 🟢 Required |

---

## 🛠️ Remediation Guide

### 1. CRLF Injection

**❌ Vulnerable Code:**
```java
logger.info("User {} logged in", userInput);
```

**✅ Fixed Code:**
```java
logger.info("User {} logged in", sanitizeForLog(userInput));

private String sanitizeForLog(Object obj) {
    return obj == null ? "null" : obj.toString().replaceAll("[\r\n]", "_");
}
```

### 2. Predictable Random

**❌ Vulnerable Code:**
```java
Random random = new Random();
String token = String.valueOf(random.nextInt());
```

**✅ Fixed Code:**
```java
private static final SecureRandom secureRandom = new SecureRandom();
String token = String.valueOf(secureRandom.nextInt());
```

### 3. Unicode Handling

**❌ Vulnerable Code:**
```java
if (frequency.toUpperCase().equals("QUARTERLY")) { }
```

**✅ Fixed Code:**
```java
if (frequency.toUpperCase(Locale.ROOT).equals("QUARTERLY")) { }
// OR better:
if ("QUARTERLY".equalsIgnoreCase(frequency)) { }
```

### 4. Information Exposure

**❌ Vulnerable Code:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleError(Exception ex) {
    return ResponseEntity.status(500)
        .body(new ErrorResponse(ex.getMessage())); // Exposes internals
}
```

**✅ Fixed Code:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleError(Exception ex) {
    String correlationId = UUID.randomUUID().toString();
    logger.error("Error [{}]: {}", correlationId, ex.getMessage(), ex);
    
    return ResponseEntity.status(500).body(new ErrorResponse(
        "An internal error occurred. Reference: " + correlationId
    ));
}
```

### 5. SQL Injection

**❌ Vulnerable Code:**
```java
String query = "SELECT * FROM trades WHERE id = '" + tradeId + "'";
```

**✅ Fixed Code:**
```java
@Query("SELECT t FROM Trade t WHERE t.id = :id")
Trade findByTradeId(@Param("id") String tradeId);
```

---

## 📊 Local Quality Gate Output

### Successful Run
```
╔════════════════════════════════════════════════════════════╗
║          CDS Platform Quality Gate Check                  ║
╚════════════════════════════════════════════════════════════╝

📋 Checking service: backend

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 Analyzing: backend
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📦 Step 1: Building backend...
✅ Build successful

🐛 Step 2: Running SpotBugs security analysis...
   Found 0 total issues

🚨 Step 3: Checking Zero-Tolerance Security Rules...

   Rule 1 - CRLF Injection: ✅ PASS
   Rule 2 - Predictable Random: ✅ PASS
   Rule 3 - Unicode Handling: ✅ PASS
   Rule 4 - Information Exposure: ✅ PASS
   Rule 5 - SQL Injection: ✅ PASS

🔍 Step 4: Checking for Anti-Patterns...

   Anti-Pattern 1 - Client Auth: ✅ PASS
   Anti-Pattern 2 - Disabled Security: ✅ PASS
   Anti-Pattern 3 - Weak Crypto: ✅ PASS
   Anti-Pattern 4 - Sensitive Logging: ✅ PASS

🧪 Step 5: Running Unit Tests with Coverage...
   Test Coverage: 85%
   ✅ Coverage meets 80% threshold

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Summary for backend:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ QUALITY GATE PASSED
   Critical Violations: 0
   Warnings: 0

╔════════════════════════════════════════════════════════════╗
║                     OVERALL SUMMARY                        ║
╚════════════════════════════════════════════════════════════╝

✅ ALL QUALITY GATES PASSED

   Total Critical Violations: 0
   Total Warnings: 0

🎉 Your code meets all security standards!

✅ Safe to commit
```

### Failed Run
```
╔════════════════════════════════════════════════════════════╗
║          CDS Platform Quality Gate Check                  ║
╚════════════════════════════════════════════════════════════╝

📋 Checking service: backend

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 Analyzing: backend
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🚨 Step 3: Checking Zero-Tolerance Security Rules...

   Rule 1 - CRLF Injection: ❌ FAIL (5 violations)
   Rule 2 - Predictable Random: ✅ PASS
   Rule 3 - Unicode Handling: ❌ FAIL (2 violations)
   Rule 4 - Information Exposure: ❌ FAIL (1 violations)
   Rule 5 - SQL Injection: ✅ PASS

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Summary for backend:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ QUALITY GATE FAILED
   Critical Violations: 8
   Warnings: 2

╔════════════════════════════════════════════════════════════╗
║                     OVERALL SUMMARY                        ║
╚════════════════════════════════════════════════════════════╝

❌ QUALITY GATE FAILED

   Total Critical Violations: 8
   Total Warnings: 2

📋 Action Required:
   1. Review SpotBugs reports in target/site/spotbugs.html
   2. Consult CODE_QUALITY_RULES.md for remediation steps
   3. Consult AGENTS.md for security standards

🚫 DO NOT COMMIT until all critical violations are fixed
```

---

## 🔄 CI/CD Integration

### GitHub Actions Workflow

The quality gates are automatically enforced in the CI/CD pipeline:

**Location:** `.github/workflows/cds-security-quality.yml`

**Triggers:**
- Push to any branch
- Pull requests to `main` or `develop`
- Daily scheduled scan at 2 AM UTC

**Stages:**
1. **Secrets Detection** - Scan for hardcoded credentials
2. **Java Backend Analysis** - SpotBugs, Grype, quality gates
3. **Frontend Security** - ESLint, npm audit
4. **Infrastructure Security** - Docker, Kubernetes config checks
5. **Dynamic Security Testing** - Runtime API testing

**Artifacts:**
- SpotBugs HTML reports
- Grype vulnerability reports
- Test coverage reports
- Security summary report

### Quality Gate Enforcement

The workflow will **fail** if:
- Secrets detected in codebase
- SpotBugs High-Priority issues found
- CVE with severity ≥ 7.0
- Zero-tolerance security rules violated
- Build fails

---

## 🎓 Training Resources

### Internal Documentation
- **AGENTS.md** - Security standards and coding guidelines
- **.github/CODE_QUALITY_RULES.md** - Detailed quality rules
- **.github/AI_AGENT_INSTRUCTIONS.md** - Code generation rules

### External Resources
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE Top 25](https://cwe.mitre.org/top25/)
- [SpotBugs Bug Patterns](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

---

## 🚨 Emergency Bypass

**Only use in true emergencies:**

```bash
# Bypass pre-commit hook
git commit --no-verify -m "Emergency fix"

# CI/CD will still enforce quality gates
```

**Note:** CI/CD quality gates cannot be bypassed. All code must eventually meet standards.

---

## 📞 Support

If you encounter issues with quality gates:

1. **Check this README** for remediation steps
2. **Review SpotBugs HTML report** in `target/site/spotbugs.html`
3. **Consult AGENTS.md** for security patterns
4. **Ask the team** in the collaboration channel

---

## 🔄 Updating Quality Gates

To modify quality rules:

1. Update rules in `.github/CODE_QUALITY_RULES.md`
2. Update workflow in `.github/workflows/cds-security-quality.yml`
3. Update this README with new rules
4. Notify the team of changes

---

**Last Updated:** 2025-01-20  
**Version:** 1.0.0
