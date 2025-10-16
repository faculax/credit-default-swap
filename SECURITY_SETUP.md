# 🛡️ Security & Quality Analysis Setup

This document describes the comprehensive security and quality analysis configuration for the Credit Default Swap (CDS) Trading Platform.

## 📋 Overview

The platform includes multiple layers of security analysis:

1. **Secret Scanning** - Detects hardcoded credentials, API keys, and sensitive data
2. **Static Analysis** - Identifies security vulnerabilities in source code
3. **Dependency Scanning** - Checks for known vulnerabilities in dependencies
4. **Linting** - Enforces secure coding standards
5. **Dynamic Analysis** - Tests running applications for security issues

## 🏗️ Components

### Backend Services (Java/Spring Boot)

All Java services (`backend`, `gateway`, `risk-engine`) include:

#### Maven Plugins

1. **SpotBugs with FindSecBugs**
   - Security-focused static analysis
   - Detects SQL injection, XSS, command injection
   - Configuration: `spotbugs-security-include.xml`

2. **OWASP Dependency Check**
   - Scans dependencies for CVEs
   - Fails build on CVSS >= 7.0
   - Configuration: `owasp-suppressions.xml`

3. **PMD**
   - Static code analysis
   - Security and best practices rulesets
   
4. **Checkstyle**
   - Code quality and style enforcement
   - Configuration: `checkstyle.xml`

#### Running Security Checks Locally

```powershell
# Run all security checks
cd backend
./mvnw clean verify

# Run only SpotBugs
./mvnw spotbugs:check

# Run only OWASP dependency check
./mvnw dependency-check:check

# Run only PMD
./mvnw pmd:check

# Run only Checkstyle
./mvnw checkstyle:check
```

### Frontend (React/TypeScript)

The frontend includes:

#### ESLint Configuration

- **eslint-plugin-security** - Security-focused linting rules
- **@typescript-eslint** - TypeScript-specific security checks
- **jsx-a11y** - Accessibility and security for React components
- Configuration: `.eslintrc.security.js`

#### Scripts

```powershell
cd frontend

# Install dependencies
npm install

# Run security linting
npm run lint:security

# Run npm audit for dependency vulnerabilities
npm run audit:security

# Run TypeScript type checking
npm run type-check

# Run all security checks
npm run security:full

# Format code
npm run format

# Check formatting
npm run format:check
```

## 🔄 CI/CD Pipeline

### GitHub Actions Workflow

Location: `.github/workflows/cds-security-quality.yml`

The workflow runs automatically on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Daily at 2 AM UTC (scheduled)

#### Jobs

1. **secrets-detection**
   - Scans for hardcoded credentials
   - Checks database URLs, API keys, JWT secrets
   - Validates ORE configuration files

2. **java-backend-analysis**
   - Runs security analysis for all Java services
   - Matrix strategy: `backend`, `gateway`, `risk-engine`
   - Includes OWASP dependency check and SpotBugs

3. **frontend-security-lint**
   - ESLint security rules
   - npm audit
   - TypeScript type checking
   - Scans for XSS vulnerabilities

4. **infrastructure-security**
   - Docker configuration analysis
   - ORE configuration security
   - Environment variable auditing

5. **dynamic-security-testing**
   - API security testing (main branch only)
   - SQL injection tests
   - XSS vulnerability tests
   - Authentication bypass tests

6. **code-quality-summary**
   - Aggregates all security reports
   - Generates comprehensive security report
   - Fails on critical issues

## 📊 Reports

Security reports are generated in:

### Java Services

- **SpotBugs**: `target/spotbugsXml.xml`, `target/site/spotbugs.html`
- **OWASP**: `target/dependency-check-report.html`
- **PMD**: `target/pmd.xml`
- **Checkstyle**: `target/checkstyle-result.xml`

### Frontend

- **ESLint**: `eslint-report.json`
- **npm audit**: `npm-audit.json`

### CI/CD Artifacts

All reports are uploaded as GitHub Actions artifacts:
- `backend-security-reports`
- `gateway-security-reports`
- `risk-engine-security-reports`
- `frontend-security-reports`
- `dynamic-security-tests`
- `security-quality-report`

## 🔧 Configuration Files

### Java Services

Each service (`backend`, `gateway`, `risk-engine`) has:

```
service/
├── pom.xml                          # Maven configuration with security plugins
├── spotbugs-security-include.xml    # SpotBugs security patterns
├── owasp-suppressions.xml           # OWASP false positive suppressions
└── checkstyle.xml                   # Code quality rules
```

### Frontend

```
frontend/
├── .eslintrc.security.js            # ESLint security configuration
├── .prettierrc.json                 # Code formatting rules
└── package.json                     # Scripts and dependencies
```

## 🎯 Security Patterns Detected

### Java/Spring Boot

- SQL injection vulnerabilities
- XSS (Cross-Site Scripting)
- Command injection
- Path traversal
- Hardcoded passwords/keys
- Weak encryption
- Insecure deserialization
- XML External Entity (XXE)
- Cookie security issues
- Trust boundary violations

### React/TypeScript

- XSS via `dangerouslySetInnerHTML`
- Insecure `target="_blank"` without `noopener`
- Eval and code injection
- Insecure randomness
- Non-literal regular expressions
- Object injection
- Hardcoded API endpoints
- Insecure storage (localStorage/sessionStorage)

## 🚀 Best Practices

### For Developers

1. **Run checks locally before committing**
   ```powershell
   # Backend
   cd backend; ./mvnw verify
   
   # Frontend
   cd frontend; npm run security:full
   ```

2. **Address security issues immediately**
   - Critical (CVSS >= 9.0): Fix within 24 hours
   - High (CVSS >= 7.0): Fix within 1 week
   - Medium (CVSS >= 4.0): Fix within 1 month

3. **Never commit secrets**
   - Use environment variables
   - Use secure vaults (Azure Key Vault, AWS Secrets Manager)
   - Never hardcode credentials in code or config files

4. **Review dependency updates**
   - Check for security patches weekly
   - Update dependencies regularly
   - Test thoroughly after updates

### For CDS Platform Specific

1. **Trade Data Validation**
   - Always validate and sanitize trade inputs
   - Use parameterized queries for database access
   - Validate reference entity names and identifiers

2. **ORE Integration Security**
   - Validate file paths for ORE configurations
   - Sanitize market data inputs
   - Prevent command injection in ORE execution

3. **API Security**
   - Implement authentication on all endpoints
   - Use HTTPS in production
   - Implement rate limiting
   - Validate all request parameters

## 📝 Suppressions

### When to Suppress

Suppressions should only be used for:
- **False positives**: When the scanner incorrectly flags secure code
- **Accepted risks**: When the risk is acknowledged and accepted
- **Low severity in test code**: For testing dependencies

### How to Suppress

#### OWASP Dependency Check

Add to `owasp-suppressions.xml`:

```xml
<suppress>
    <notes>Justification for suppression</notes>
    <packageUrl regex="true">^pkg:maven/group/artifact@.*$</packageUrl>
    <cve>CVE-YYYY-XXXXX</cve>
</suppress>
```

#### SpotBugs

Add exclusion to `spotbugs-security-include.xml`:

```xml
<Match>
    <Class name="com.example.SafeClass"/>
    <Bug pattern="SPECIFIC_PATTERN"/>
</Match>
```

#### ESLint

Add comment in code:

```typescript
// eslint-disable-next-line security/detect-object-injection
const value = obj[key];
```

## 🔄 Maintenance

### Weekly

- Review security reports from CI/CD
- Check for new dependency updates
- Address high-severity issues

### Monthly

- Review and update suppressions
- Audit security configurations
- Update security tools and plugins
- Review access logs and security incidents

### Quarterly

- Comprehensive security audit
- Update security policies
- Review and update this documentation
- Penetration testing (recommended)

## 📞 Support

For security issues or questions:
- Create an issue in the repository
- Tag with `security` label
- For critical security issues, follow responsible disclosure

## 🔗 Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [React Security Best Practices](https://reactjs.org/docs/dom-elements.html#dangerouslysetinnerhtml)
- [FindSecBugs Documentation](https://find-sec-bugs.github.io/)
- [ESLint Security Plugin](https://github.com/eslint-community/eslint-plugin-security)

---

**Last Updated**: October 2025  
**Version**: 1.0.0  
**Maintainer**: CDS Platform Security Team