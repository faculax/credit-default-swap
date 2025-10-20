# Code Quality & Security Rules

> **Purpose**: Automated quality gate rules that MUST pass before code is merged.

## 🚦 CI/CD Quality Gates

All pull requests must pass these automated checks:

### 1. Build & Test
```bash
./mvnw clean test
# Exit code 0 required
```

### 2. SpotBugs Security Scan
```bash
./mvnw spotbugs:check
# Zero high/medium priority bugs allowed
```

### 3. Checkstyle
```bash
./mvnw checkstyle:check
# Zero violations allowed
```

### 4. OWASP Dependency Check
```bash
./mvnw dependency-check:check
# CVE score < 7.0 required
```

---

## 🔒 Security Rules (Zero Tolerance)

### CRLF Injection Prevention (CWE-117)
**Status**: MANDATORY - All violations must be fixed

```java
// ❌ VIOLATION - Will fail SpotBugs
logger.info("User {} logged in", untrustedInput);
logger.error("Failed for {} and {}", param1, param2);

// ✅ COMPLIANT
logger.info("User {} logged in", sanitizeForLog(untrustedInput));
// OR use Logback pattern: %replace(%msg){'[\r\n]', ''}
```

**Detection**: SpotBugs pattern `CRLF_INJECTION_LOGS`  
**Severity**: HIGH  
**Auto-fix**: Available via logback configuration

---

### Predictable Random (CWE-330)
**Status**: MANDATORY - Must use SecureRandom

```java
// ❌ VIOLATION
Random random = new Random();
double value = random.nextDouble();

// ✅ COMPLIANT
SecureRandom secureRandom = new SecureRandom();
double value = secureRandom.nextDouble();
```

**Detection**: SpotBugs pattern `PREDICTABLE_RANDOM`  
**Severity**: HIGH  
**Exceptions**: Demo/test code only (must be documented + suppressed)

---

### Unicode Case Operations (CWE-176)
**Status**: MANDATORY - Must specify locale

```java
// ❌ VIOLATION - Turkish locale bypass
if(input.toUpperCase().equals("ADMIN")) { }

// ✅ COMPLIANT
if(input.toUpperCase(Locale.ROOT).equals("ADMIN")) { }
// OR
if("ADMIN".equalsIgnoreCase(input)) { }
```

**Detection**: SpotBugs pattern `IMPROPER_UNICODE`  
**Severity**: MEDIUM

---

### Information Exposure (CWE-209)
**Status**: MANDATORY - Never expose internal errors

```java
// ❌ VIOLATION
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handle(Exception ex) {
    return ResponseEntity.status(500).body(ex.getMessage()); // Leak!
}

// ✅ COMPLIANT
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handle(Exception ex) {
    String id = UUID.randomUUID().toString();
    logger.error("Error [{}]", id, ex);
    return ResponseEntity.status(500)
        .body("Internal error. Ref: " + id);
}
```

**Detection**: SpotBugs pattern `INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE`  
**Severity**: HIGH

---

### SQL Injection (CWE-89)
**Status**: MANDATORY - Use parameterized queries

```java
// ❌ VIOLATION
String sql = "SELECT * FROM users WHERE name = '" + name + "'";
entityManager.createNativeQuery(sql);

// ✅ COMPLIANT
@Query("SELECT u FROM User u WHERE u.name = :name")
User findByName(@Param("name") String name);
```

**Detection**: SpotBugs pattern `SQL_INJECTION_*`  
**Severity**: CRITICAL

---

## 📋 Code Review Checklist

### Security
- [ ] No hardcoded credentials/secrets
- [ ] All inputs validated (`@Valid`, constraints)
- [ ] All endpoints secured (`@PreAuthorize`, Security Config)
- [ ] Error messages don't leak information
- [ ] Logging is sanitized
- [ ] Dependencies up-to-date (no high CVEs)

### Quality
- [ ] No code duplication (DRY principle)
- [ ] Methods < 50 lines
- [ ] Classes < 500 lines
- [ ] Cyclomatic complexity < 10
- [ ] Test coverage > 80%

### Spring Boot Specific
- [ ] DTOs have validation annotations
- [ ] Controllers return `ResponseEntity<T>`
- [ ] Services are transactional where needed
- [ ] Repositories extend Spring Data interfaces
- [ ] Configuration externalized (`application.yml`)

---

## 🛠️ Auto-Fix Scripts

### Fix CRLF Injection in Logback
Add to `src/main/resources/logback-spring.xml`:
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} - %-5level - %replace(%msg){'[\r\n]', ''}%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### Add Sanitize Utility (Per Service)
Create `src/main/java/.../util/LoggingUtil.java`:
```java
package com.creditdefaultswap.platform.util;

public final class LoggingUtil {
    private LoggingUtil() {} // Utility class
    
    public static String sanitizeForLog(Object obj) {
        if (obj == null) return "null";
        return obj.toString().replaceAll("[\r\n]", "_");
    }
}
```

### SpotBugs Suppression (Use Sparingly!)
Add to `spotbugs-security-include.xml`:
```xml
<FindBugsFilter>
    <!-- Demo service intentionally uses predictable random -->
    <Match>
        <Class name="~.*\.Demo.*Service"/>
        <Bug pattern="PREDICTABLE_RANDOM"/>
    </Match>
</FindBugsFilter>
```

---

## 📊 Metrics & Thresholds

| Metric | Threshold | Tool |
|--------|-----------|------|
| Test Coverage | ≥ 80% | JaCoCo |
| Code Duplication | ≤ 3% | PMD CPD |
| Cyclomatic Complexity | ≤ 10 | Checkstyle |
| Security Bugs (High) | 0 | SpotBugs |
| Security Bugs (Medium) | ≤ 5 | SpotBugs |
| CVE Score | < 7.0 | OWASP Dep Check |
| Code Smells | ≤ 50 | SonarQube |
| Technical Debt | ≤ 5% | SonarQube |

---

## 🚨 Breaking the Build

The following will cause CI to fail:

1. **Any high-priority SpotBugs security issue**
2. **Checkstyle violations**
3. **Test failures**
4. **CVE score ≥ 7.0 in dependencies**
5. **Code coverage drop > 5%**

---

## 🎓 Training Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE/SANS Top 25](https://cwe.mitre.org/top25/)
- [Spring Security Docs](https://docs.spring.io/spring-security/reference/)
- [Secure Coding Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html)

---

## 📞 Getting Help

If you're blocked by a quality gate:

1. **Check the logs** - CI provides detailed output
2. **Run locally** - `./mvnw verify` replicates CI
3. **Review this doc** - Most issues have solutions above
4. **Ask in #code-quality** - Team can help with suppressions
5. **Create exception request** - For legitimate exceptions, create a ticket

---

**Last Updated**: October 16, 2025  
**Maintained By**: Platform Engineering Team  
**Review Frequency**: Quarterly
