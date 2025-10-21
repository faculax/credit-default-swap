# 🛡️ DefectDojo Security Platform - Complete Guide

**Status:** ✅ Integrated & Ready  
**URL:** http://localhost:8081  
**Credentials:** admin / admin (⚠️ change after first login)

---

## 🎯 What is DefectDojo?

Open-source Application Security Management platform that consolidates security findings from multiple scanners into a single dashboard for vulnerability aggregation, risk scoring, trend analysis, compliance reporting, and team collaboration.

---

## ⚡ Quick Commands

```powershell
# Start DefectDojo
./defectdojo.ps1 start

# Run all security scans (Java, JavaScript, Secrets)
./defectdojo.ps1 scan

# Upload results (component-based with 5 products)
./defectdojo.ps1 upload-components

# Stop DefectDojo
./defectdojo.ps1 stop

# Clean up duplicate engagements
./defectdojo.ps1 clean
```

---

## 🏗️ Component Architecture

We use **component-based organization** with separate products for better tracking:

| Product | Technologies | Scanners |
|---------|-------------|----------|
| **Backend API** | Java/Spring Boot | SpotBugs, OWASP Dependency Check, PMD, Checkstyle |
| **Web Frontend** | React/JavaScript | npm audit, ESLint, Retire.js |
| **API Gateway** | Spring Cloud Gateway | SpotBugs, OWASP Dependency Check |
| **Risk Engine** | Java/Spring Boot | SpotBugs, OWASP Dependency Check |
| **Secret Scanning** | Cross-component | Gitleaks (100+ secret patterns) |

**Benefits:**
- Clear separation of concerns
- Component-specific metrics
- Easier tracking and remediation
- Better reporting per team/service

---

## 🔐 Secret Scanning Integration

### What Secrets Are Detected?

Gitleaks detects **100+ patterns** including:

**Credentials & Passwords:**
- Database passwords (Spring `application.yml`, `.properties`)
- Hardcoded passwords in code
- Default credentials
- Admin/root passwords

**API Keys & Tokens:**
- AWS access keys
- Azure keys
- GitHub tokens
- Slack webhooks
- JWT secrets
- API keys (Generic, Stripe, SendGrid, Twilio, etc.)

**Cryptographic Secrets:**
- Private keys (RSA, SSH, GPG)
- OAuth tokens
- Bearer tokens
- Session tokens

**Cloud Credentials:**
- AWS, Azure, GCP keys
- Docker registry credentials
- Kubernetes secrets
- Heroku API keys

### Installation Options

**Option 1: Chocolatey (Recommended)**
```powershell
choco install gitleaks -y
```

**Option 2: Our Install Script**
```powershell
.\install-gitleaks.ps1
```

**Option 3: Manual Download**
1. Download from: https://github.com/gitleaks/gitleaks/releases
2. Extract to `C:\tools\gitleaks\`
3. Add to PATH

### Configuration

Pre-configured in `.gitleaks.toml` with:
- Custom Spring Boot patterns
- JWT secret detection
- Allowlists for test files
- Enhanced accuracy rules

### Integration Status

✅ **Scanning:** Integrated in `defectdojo.ps1 scan`  
✅ **Upload:** Integrated in `defectdojo-component.ps1`  
✅ **Optional:** Gracefully skips if not installed  
✅ **Reports:** `security-reports/gitleaks-report.json`

---

## 📋 Complete Workflow

### 1️⃣ First Time Setup

```powershell
# Install Gitleaks (optional but recommended)
choco install gitleaks -y

# Start DefectDojo
./defectdojo.ps1 start

# Initialize (creates products & engagements)
./defectdojo.ps1 init

# Change admin password at http://localhost:8081
```

### 2️⃣ Regular Security Scans

```powershell
# Run all scans
./defectdojo.ps1 scan

# Upload to DefectDojo
./defectdojo.ps1 upload-components

# Review findings at http://localhost:8081
```

### 3️⃣ View Results

1. Open http://localhost:8081
2. Navigate to **Products** → Select component
3. View **Engagements** → Latest scan
4. Review **Findings** by severity

---

## 🔧 Advanced Usage

### Scan Without Gitleaks

If Gitleaks is not installed, `scan` command will:
- ✅ Show warning: "Gitleaks not installed - skipping secret scanning"
- ✅ Show install options
- ✅ Continue with other scans
- ✅ Complete successfully

### Force New Engagement

By default, multiple scans on the same day **reuse the same engagement** to avoid duplicates:

```powershell
# Reuse today's engagement (default)
./defectdojo.ps1 upload-components

# Force create new engagement
./defectdojo.ps1 upload-components -ForceNewEngagement
```

### Clean Up Duplicates

If you have duplicates from testing:

```powershell
./defectdojo.ps1 clean
```

This script:
- Groups engagements by product and date
- Keeps the most recent engagement
- Deletes older duplicates
- Shows summary of cleaned items

### Scan Individual Components

```powershell
# Backend only
cd backend
mvn spotbugs:check dependency-check:check

# Frontend only
cd frontend
npm audit --json > audit-npm.json
npx eslint . -f json -o eslint-security.json
```

---

## 📊 Reports Generated

| Component | Report Files |
|-----------|-------------|
| **Backend** | `backend/target/spotbugsXml.xml`<br>`backend/target/dependency-check-report.json`<br>`backend/target/security-reports/pmd.xml`<br>`backend/target/checkstyle-result.xml` |
| **Frontend** | `frontend/audit-npm.json`<br>`frontend/eslint-security.json`<br>`frontend/retire-report.json` |
| **Gateway** | `gateway/target/spotbugsXml.xml`<br>`gateway/target/dependency-check-report.json` |
| **Risk Engine** | `risk-engine/target/spotbugsXml.xml`<br>`risk-engine/target/dependency-check-report.json` |
| **Secrets** | `security-reports/gitleaks-report.json` |

---

## 🚨 Common Issues & Solutions

### Issue: "Gitleaks not installed"
**Solution:** Install with `choco install gitleaks -y` or run without it (other scans continue)

### Issue: Duplicate engagements showing up
**Solution:** We now reuse engagements from the same day. Old duplicates can be cleaned with `./defectdojo.ps1 clean`

### Issue: "File not found" during upload
**Solution:** Run `./defectdojo.ps1 scan` first to generate reports

### Issue: DefectDojo won't start
**Solution:** 
```powershell
./defectdojo.ps1 stop
docker system prune -f
./defectdojo.ps1 start
```

### Issue: SpotBugs XML format error
**Solution:** We use `spotbugsXml.xml` (correct format) not `spotbugs.xml`

### Issue: npm audit version compatibility
**Solution:** We use v7 format (`--json` flag) compatible with DefectDojo

---

## 🎯 Integration Features

### ✅ Implemented

- **Component-based products** - 5 separate products in DefectDojo
- **Engagement reuse** - Prevents duplicates on same day
- **Auto-close old findings** - `close_old_findings=true` flag
- **Optional Gitleaks** - Graceful degradation if not installed
- **Comprehensive scanning** - Java, JavaScript, Secrets
- **Cleanup script** - Remove historical duplicates
- **Error handling** - Continues on scan failures
- **Summary reporting** - Shows findings count per scan

### 🔐 Security Best Practices

- **No credentials in code** - Use environment variables
- **Scan before commit** - Run `scan` before pushing
- **Review secret findings** - Even test secrets can be exploited
- **Update dependencies** - Address OWASP findings promptly
- **Fix critical/high first** - Use DefectDojo risk scoring

---

## 📁 Key Files

```
defectdojo.ps1                    # Main orchestration script
defectdojo-component.ps1          # Component upload logic
cleanup-duplicates.ps1            # Remove duplicate engagements
install-gitleaks.ps1              # Gitleaks installer for Windows
.gitleaks.toml                    # Gitleaks configuration
docker-compose.local.yml          # DefectDojo containers
```

---

## 🔗 Useful Links

- **DefectDojo UI:** http://localhost:8081
- **Products:** http://localhost:8081/product
- **Gitleaks GitHub:** https://github.com/gitleaks/gitleaks
- **DefectDojo Docs:** https://documentation.defectdojo.com

---

## 💡 Tips

1. **Run scans regularly** - Ideally before every PR
2. **Check secret scan results** - Even false positives indicate patterns to avoid
3. **Use component view** - Easier to assign findings to teams
4. **Monitor trends** - DefectDojo shows security posture over time
5. **Don't ignore warnings** - Low severity today = high severity tomorrow
6. **Keep scanners updated** - `choco upgrade gitleaks -y`

---

## 🎓 What Gets Scanned?

| Scan Type | What It Finds | Severity |
|-----------|---------------|----------|
| **SpotBugs** | Java security bugs, SQL injection, XSS, weak crypto | High |
| **OWASP Dependency Check** | Vulnerable libraries (CVEs) | Critical-High |
| **PMD** | Code quality issues, security anti-patterns | Medium |
| **Checkstyle** | Insecure coding patterns | Low-Medium |
| **npm audit** | JavaScript vulnerable dependencies | Critical-High |
| **ESLint Security** | JavaScript security issues, eval, innerHTML | Medium-High |
| **Retire.js** | Outdated JS libraries with known vulnerabilities | Medium-High |
| **Gitleaks** | Hardcoded secrets, API keys, passwords | Critical |

---

## ✅ Checklist for New Users

- [ ] Install Gitleaks: `choco install gitleaks -y`
- [ ] Start DefectDojo: `./defectdojo.ps1 start`
- [ ] Change admin password at http://localhost:8081
- [ ] Run first scan: `./defectdojo.ps1 scan`
- [ ] Upload results: `./defectdojo.ps1 upload-components`
- [ ] Review findings in DefectDojo UI
- [ ] Address Critical/High severity issues
- [ ] Add to CI/CD pipeline (optional)

---

## 🔄 How Duplicate Prevention Works

### Engagement Reuse (Per Day)
- **First upload of the day:** Creates new engagement
- **Subsequent uploads same day:** Reuses existing engagement
- **New day:** Creates fresh engagement for new date

###Test Reuse (Within Engagement)
- **First upload:** Creates new test for scan type
- **Re-upload same day:** Updates existing test (re-import)
- **Benefits:** No duplicate tests, clean history, accurate trending

### Clean Up Historical Duplicates
```powershell
# Remove duplicate tests from past uploads
./defectdojo.ps1 clean-tests
```

---

**🎉 You're all set! Run `./defectdojo.ps1 scan` and `./defectdojo.ps1 upload-components` to get started.**
