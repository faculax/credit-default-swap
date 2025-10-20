# 🛡️ SonarQube Integration Guide

## Overview

This guide explains how to set up and use **SonarQube Community Edition** for comprehensive code quality and security analysis in the Credit Default Swap platform.

**SonarQube** provides:
- 🔍 Static Application Security Testing (SAST)
- 📊 Code quality metrics and technical debt tracking
- 🐛 Bug detection
- 🔒 Security vulnerability identification
- 📈 Code coverage analysis
- 🎯 Quality gates enforcement

---

## 🚀 Quick Start

### Option 1: Automated Setup (Recommended)

**Windows (PowerShell):**
```powershell
cd credit-default-swap
.\scripts\setup-sonarqube.ps1
```

**Linux/macOS:**
```bash
cd credit-default-swap
bash scripts/setup-sonarqube.sh
```

The setup script will:
- Start SonarQube and PostgreSQL containers
- Wait for initialization
- Display access credentials
- Offer to open your browser

### Option 2: Manual Setup

```bash
# Start SonarQube
docker-compose -f docker-compose.sonarqube.yml up -d

# Wait 2-3 minutes for initialization
# Access at: http://localhost:9000
# Login: admin / admin (change immediately!)
```

---

## ⚙️ Configuration

### Step 1: Initial SonarQube Setup

1. **Login** to http://localhost:9000
   - Username: `admin`
   - Password: `admin`

2. **Change Password** (mandatory)
   - User Menu → My Account → Security → Change Password

3. **Generate API Token**
   - User Menu → My Account → Security → Generate Tokens
   - Name: `GitHub Actions`
   - Type: `Global Analysis Token` or `User Token`
   - Click `Generate` → **Copy and save the token securely**

### Step 2: Configure GitHub Secrets

Add these secrets to your repository (`Settings` → `Secrets and variables` → `Actions`):

| Secret Name | Description | Example Value |
|-------------|-------------|---------------|
| `SONAR_HOST_URL` | Your SonarQube server URL | `http://localhost:9000` or `https://sonarqube.yourcompany.com` |
| `SONAR_TOKEN` | Your SonarQube API token | `sqp_1234567890abcdef...` |
| `SNYK_TOKEN` | Snyk API token (optional) | `abc123...` (from https://snyk.io) |

### Step 3: Verify Integration

The workflow `.github/workflows/security-sonarqube.yml` will automatically run on:
- ✅ Push to `main`, `develop`, or `security-compliance` branches
- ✅ Pull requests to `main` or `develop`
- ✅ Weekly schedule (Mondays at 2 AM UTC)
- ✅ Manual trigger from GitHub Actions tab

---

## 📊 Understanding the Results

### SonarQube Dashboard

Navigate to http://localhost:9000/projects to see all analyzed projects:

| Project | Description |
|---------|-------------|
| `credit-default-swap-backend` | Backend service analysis |
| `credit-default-swap-gateway` | Gateway service analysis |
| `credit-default-swap-risk-engine` | Risk engine analysis |
| `credit-default-swap-frontend` | Frontend React app analysis |

### Key Metrics

**Quality Gate Status:**
- 🟢 **Passed**: Code meets all quality criteria
- 🔴 **Failed**: One or more conditions not met

**Reliability Rating (Bugs):**
- A = 0 bugs
- B = at least 1 minor bug
- C = at least 1 major bug
- D = at least 1 critical bug
- E = at least 1 blocker bug

**Security Rating (Vulnerabilities):**
- A = 0 vulnerabilities
- B = at least 1 minor vulnerability
- C = at least 1 major vulnerability
- D = at least 1 critical vulnerability
- E = at least 1 blocker vulnerability

**Maintainability Rating (Code Smells):**
- A = 0-5% technical debt ratio
- B = 6-10%
- C = 11-20%
- D = 21-50%
- E = >50%

**Coverage:**
- Percentage of code covered by tests
- Target: >80% for new code

---

## 🎯 Security Scanners Integrated

### 1. **SonarQube** (Code Quality + SAST)
- **Languages**: Java, JavaScript, TypeScript
- **Detects**: Bugs, vulnerabilities, code smells, security hotspots
- **Reports**: OWASP Top 10, SANS Top 25, CWE coverage
- **Coverage**: Unit test coverage via JaCoCo (Java) and Jest (Frontend)

### 2. **Snyk** (Dependency Security)
- **Scans**: Maven dependencies (Java), npm packages (Frontend), Docker containers
- **Detects**: Known vulnerabilities (CVEs) in dependencies
- **Features**: Auto-fix PRs, license compliance
- **Integration**: Results in GitHub Security tab

### 3. **Semgrep** (Security Patterns)
- **Rules**: OWASP Top 10, language-specific security patterns
- **Detects**: Injection flaws, authentication issues, insecure configurations
- **Speed**: Very fast, suitable for pre-commit hooks
- **Integration**: SARIF reports to GitHub Code Scanning

### 4. **Trivy** (Container & Filesystem)
- **Scans**: Docker images, filesystem vulnerabilities, misconfigurations
- **Detects**: OS packages, language dependencies, IaC issues
- **Severity**: CRITICAL, HIGH, MEDIUM, LOW
- **Database**: Updated daily

### 5. **Gitleaks** (Secret Detection)
- **Scans**: Git history for exposed secrets
- **Detects**: API keys, passwords, tokens, private keys
- **Prevents**: Credential leaks in commits

### 6. **Checkov** (Infrastructure as Code)
- **Scans**: Dockerfiles, Kubernetes manifests, Terraform
- **Detects**: Security misconfigurations
- **Policies**: CIS benchmarks, best practices

---

## 🔧 Local Analysis

### Backend Services (Java)

```bash
cd backend  # or gateway, or risk-engine

# Run tests with coverage
./mvnw clean verify -Pcoverage

# Run SonarQube analysis
./mvnw sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN_HERE

# View results at http://localhost:9000/dashboard?id=credit-default-swap-backend
```

### Frontend (React/TypeScript)

```bash
cd frontend

# Install dependencies
npm ci

# Run tests with coverage
npm run test -- --coverage --watchAll=false

# Run SonarQube analysis
npm install -g sonarqube-scanner
sonar-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN_HERE

# View results at http://localhost:9000/dashboard?id=credit-default-swap-frontend
```

---

## 🚨 Quality Gates

### Default Quality Gate Conditions

The build **FAILS** if:
- New code has **Coverage < 80%**
- New code has **Duplications > 3%**
- **Security Rating worse than A** (any vulnerabilities)
- **Reliability Rating worse than A** (any bugs)
- **Security Hotspots** need review

### Custom Quality Gate

You can customize quality gates in SonarQube:
1. Go to **Quality Gates**
2. Click **Create**
3. Add conditions (e.g., "Blocker Issues = 0")
4. Set as default or assign to projects

---

## 📈 Viewing Results in GitHub

### Pull Request Comments

When you create a PR, the workflow automatically posts a comment with:
- ✅ Quality gate status
- 📊 Links to SonarQube dashboards
- 🔍 Links to security scan results
- 📈 Metrics summary

### GitHub Security Tab

Navigate to: `https://github.com/YOUR_ORG/credit-default-swap/security`

**Code Scanning Alerts:**
- Semgrep findings
- Snyk vulnerabilities
- Trivy container issues
- Checkov IaC problems

**Dependabot Alerts:**
- Dependency vulnerabilities detected by GitHub

**Secret Scanning:**
- Gitleaks findings

---

## 🔄 Workflow Customization

### Adjust Scan Frequency

Edit `.github/workflows/security-sonarqube.yml`:

```yaml
on:
  schedule:
    - cron: '0 2 * * 1'  # Weekly on Monday
    # Change to '0 2 * * *' for daily scans
```

### Skip Specific Scanners

Comment out jobs you don't need:

```yaml
jobs:
  # snyk-security:
  #   name: Snyk - Dependency Security
  #   ... (commented out)
```

### Adjust Quality Gate Thresholds

In `sonar-project.properties`:

```properties
# Fail build if coverage < 80%
sonar.coverage.minimum=80

# Fail build on any blocker issues
sonar.qualitygate.wait=true
```

---

## 🛠️ Troubleshooting

### SonarQube Won't Start

**Symptom:** Container keeps restarting

**Solution:**
```bash
# Check logs
docker-compose -f docker-compose.sonarqube.yml logs sonarqube

# On Linux: Increase vm.max_map_count
sudo sysctl -w vm.max_map_count=262144

# Restart
docker-compose -f docker-compose.sonarqube.yml restart
```

### "Quality Gate Failed" Error

**Symptom:** Build fails with quality gate error

**Solution:**
1. Check SonarQube dashboard for specific failures
2. Fix critical/blocker issues
3. Increase test coverage for new code
4. Review and update quality gate conditions if too strict

### Token Authentication Failed

**Symptom:** `401 Unauthorized` in workflow logs

**Solution:**
1. Regenerate token in SonarQube UI
2. Update `SONAR_TOKEN` secret in GitHub
3. Ensure token has "Execute Analysis" permission

### No Coverage Data

**Symptom:** Coverage shows 0% despite having tests

**Solution:**

**Java:**
```bash
# Ensure jacoco-maven-plugin is configured in pom.xml
# Run tests with coverage profile
./mvnw clean verify -Pcoverage

# Verify jacoco.xml exists
ls target/site/jacoco/jacoco.xml
```

**Frontend:**
```bash
# Ensure jest.config.js has coverage reporters
npm run test -- --coverage

# Verify lcov.info exists
ls coverage/lcov.info
```

### Snyk Scan Fails

**Symptom:** Snyk step fails in workflow

**Solution:**
1. Sign up for free Snyk account at https://snyk.io
2. Get API token from Account Settings
3. Add `SNYK_TOKEN` to GitHub Secrets
4. Or comment out Snyk job if not needed

---

## 🎯 Best Practices

### For Developers

✅ **Run analysis locally** before pushing:
```bash
./mvnw clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=YOUR_TOKEN
```

✅ **Fix issues immediately**:
- Address blocker and critical issues before committing
- Review security hotspots
- Aim for >80% coverage on new code

✅ **Review PR comments**:
- Check SonarQube quality gate status
- Review new issues introduced
- Fix before requesting reviews

✅ **Write tests**:
- Maintain high code coverage
- Test edge cases and error conditions

### For Security Team

✅ **Monitor trends**:
- Track security rating over time
- Review security hotspots weekly
- Prioritize OWASP Top 10 issues

✅ **Set realistic gates**:
- Balance strictness with developer productivity
- Adjust thresholds based on team maturity

✅ **Regular audits**:
- Review dependency vulnerabilities monthly
- Update vulnerable dependencies promptly
- Export reports for compliance

### For DevOps

✅ **Backup SonarQube data**:
```bash
docker-compose -f docker-compose.sonarqube.yml exec sonar-db \
  pg_dump -U sonar sonar > sonarqube_backup_$(date +%Y%m%d).sql
```

✅ **Monitor performance**:
- Track analysis duration
- Optimize slow scans
- Scale resources if needed

✅ **Update regularly**:
```bash
# Update to latest LTS
docker-compose -f docker-compose.sonarqube.yml pull
docker-compose -f docker-compose.sonarqube.yml up -d
```

---

## 📚 Additional Resources

### Official Documentation
- **SonarQube**: https://docs.sonarqube.org/latest/
- **Snyk**: https://docs.snyk.io/
- **Semgrep**: https://semgrep.dev/docs/
- **Trivy**: https://aquasecurity.github.io/trivy/
- **Gitleaks**: https://github.com/gitleaks/gitleaks
- **Checkov**: https://www.checkov.io/documentation.html

### Security Standards
- **OWASP Top 10**: https://owasp.org/www-project-top-ten/
- **CWE Top 25**: https://cwe.mitre.org/top25/
- **SANS Top 25**: https://www.sans.org/top25-software-errors/

### Community
- **SonarQube Community**: https://community.sonarsource.com/
- **Stack Overflow**: https://stackoverflow.com/questions/tagged/sonarqube

---

## 🎉 Quick Reference

### Useful Commands

```bash
# Start SonarQube
docker-compose -f docker-compose.sonarqube.yml up -d

# Stop SonarQube
docker-compose -f docker-compose.sonarqube.yml down

# View logs
docker-compose -f docker-compose.sonarqube.yml logs -f

# Restart
docker-compose -f docker-compose.sonarqube.yml restart

# Remove all data (reset)
docker-compose -f docker-compose.sonarqube.yml down -v

# Check status
curl http://localhost:9000/api/system/status

# Backup database
docker-compose -f docker-compose.sonarqube.yml exec sonar-db \
  pg_dump -U sonar sonar > backup.sql

# Restore database
docker-compose -f docker-compose.sonarqube.yml exec -T sonar-db \
  psql -U sonar sonar < backup.sql
```

### Important URLs

| Resource | URL |
|----------|-----|
| **SonarQube Dashboard** | http://localhost:9000 |
| **Backend Project** | http://localhost:9000/dashboard?id=credit-default-swap-backend |
| **Gateway Project** | http://localhost:9000/dashboard?id=credit-default-swap-gateway |
| **Risk Engine Project** | http://localhost:9000/dashboard?id=credit-default-swap-risk-engine |
| **Frontend Project** | http://localhost:9000/dashboard?id=credit-default-swap-frontend |
| **Snyk Dashboard** | https://app.snyk.io/ |
| **GitHub Security** | https://github.com/faculax/credit-default-swap/security |

---

**Happy secure coding! 🛡️**
