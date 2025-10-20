# 🛡️ SonarQube - Quick Start Guide

## 🎯 What is SonarQube?

**SonarQube** is your all-in-one code quality and security platform that provides:
- 🔍 **SAST** - Static Application Security Testing
- 🐛 **Bug Detection** - Find reliability issues
- 📊 **Code Quality** - Maintainability metrics
- 🔒 **Security** - OWASP Top 10 coverage
- 📈 **Coverage** - Test coverage tracking
- 🎯 **Quality Gates** - Enforce standards

**Plus:** Snyk, Semgrep, Trivy, Gitleaks, and Checkov for comprehensive security scanning!

---

## ⚡ 5-Minute Setup

### Step 1: Start SonarQube (Choose One)

**Windows (PowerShell):**
```powershell
.\scripts\setup-sonarqube.ps1
```

**Linux/macOS:**
```bash
bash scripts/setup-sonarqube.sh
```

**Manual:**
```bash
docker-compose -f docker-compose.sonarqube.yml up -d
```

### Step 2: Login & Configure
1. **Open:** http://localhost:9000
2. **Login:** `admin` / `admin`
3. **⚠️ Change password immediately!**

### Step 3: Generate API Token
1. Click **User Menu** (top right) → **My Account**
2. Go to **Security** tab
3. Click **Generate Tokens**
   - Name: `GitHub Actions`
   - Type: `Global Analysis Token`
4. Click **Generate** → **Copy the token** (save it securely!)

### Step 4: Configure GitHub Secrets
Go to: **Repository Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add these:
- `SONAR_HOST_URL` = `http://localhost:9000`
- `SONAR_TOKEN` = `<paste-your-token-from-step-3>`

**Optional (for Snyk dependency scanning):**
- `SNYK_TOKEN` = `<get-free-token-from-https://snyk.io>`

### Step 5: Trigger Analysis
Push a commit or manually trigger:
```bash
git add .
git commit -m "feat: enable SonarQube integration"
git push
```

**OR** Go to: **GitHub Actions** → **Security & Quality - SonarQube** → **Run workflow**

---

## 📊 Viewing Results

### 🎨 Custom Aggregated Dashboard (New!)
**Open:** `dashboard.html` in your browser

**Features:**
- ✨ **All 4 services** in one unified view
- 📊 **Real-time metrics** from SonarQube API
- 📈 **Trend charts** showing improvement over time
- 🎯 **Summary cards** with total bugs, vulnerabilities, coverage
- 🔄 **Auto-refresh** every 5 minutes
- 🎨 **Beautiful UI** matching CDS platform colors

**Setup:**
1. Open `dashboard.html` in a browser
2. If you have a SonarQube token, add it to the `SONAR_TOKEN` variable in the file
3. Enjoy the aggregated view!

### SonarQube Native Dashboard
**Main Dashboard:** http://localhost:9000/projects

**Individual Projects:**
- **Backend:** http://localhost:9000/dashboard?id=credit-default-swap-backend
- **Gateway:** http://localhost:9000/dashboard?id=credit-default-swap-gateway
- **Risk Engine:** http://localhost:9000/dashboard?id=credit-default-swap-risk-engine
- **Frontend:** http://localhost:9000/dashboard?id=credit-default-swap-frontend

**Historical Tracking:**
- **Activity:** http://localhost:9000/project/activity?id=credit-default-swap-backend
  - Timeline graphs showing trends over time
  - Compare analyses (before/after)
  - Version markers
- **Measures History:** Drill-down metrics with custom date ranges

### GitHub
- **Actions Tab:** View workflow execution and summary
- **Pull Requests:** Automated comments with quality gate status
- **Security Tab:** Code scanning alerts from Snyk, Semgrep, Trivy

---

## 🎨 Understanding Ratings

### Quality Gate
| Status | Meaning | Action |
|--------|---------|--------|
| 🟢 **Passed** | All conditions met | Ready to merge |
| 🔴 **Failed** | Issues found | Fix before merging |

### Reliability (Bugs)
| Rating | Meaning |
|--------|---------|
| 🟢 **A** | 0 bugs - Excellent |
| 🟡 **B** | Minor bugs |
| 🟠 **C** | Major bugs |
| 🔴 **D** | Critical bugs |
| ⛔ **E** | Blocker bugs - Do not merge! |

### Security (Vulnerabilities)
| Rating | Meaning | SLA |
|--------|---------|-----|
| 🟢 **A** | 0 vulnerabilities | - |
| 🟡 **B** | Minor vulnerabilities | 30 days |
| 🟠 **C** | Major vulnerabilities | 14 days |
| 🔴 **D** | Critical vulnerabilities | 7 days |
| ⛔ **E** | Blocker vulnerabilities | 24 hours |

### Maintainability (Code Smells)
| Rating | Tech Debt | Meaning |
|--------|-----------|---------|
| 🟢 **A** | ≤5% | Excellent |
| 🟡 **B** | 6-10% | Good |
| 🟠 **C** | 11-20% | Manageable |
| 🔴 **D** | 21-50% | High |
| ⛔ **E** | >50% | Very high |

### Coverage
| Coverage | Status |
|----------|--------|
| **>80%** | 🟢 Excellent |
| **60-80%** | 🟡 Good |
| **40-60%** | 🟠 Fair |
| **<40%** | 🔴 Insufficient |

---

## 🚨 What Gets Scanned?

### 6 Security Scanners Running Automatically

| Scanner | What It Scans | Finds |
|---------|---------------|-------|
| **SonarQube** | Java + TypeScript code | Bugs, vulnerabilities, code smells, security hotspots |
| **Snyk** | Dependencies + Containers | CVEs in npm/Maven packages, Docker images |
| **Semgrep** | Source code patterns | OWASP Top 10, injection flaws, auth issues |
| **Trivy** | Containers + Filesystem | OS vulnerabilities, misconfigurations |
| **Gitleaks** | Git history | Exposed API keys, passwords, tokens |
| **Checkov** | Infrastructure | Dockerfile security, K8s misconfigurations |

---

## 🔧 Local Analysis (Before Pushing)

### Backend/Gateway/Risk-Engine (Java)

```bash
cd backend  # or gateway, or risk-engine

# Run tests with coverage
./mvnw clean verify -Pcoverage

# Analyze with SonarQube
./mvnw sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN

# View results
# http://localhost:9000/dashboard?id=credit-default-swap-backend
```

### Frontend (React/TypeScript)

```bash
cd frontend

# Run tests with coverage
npm run test -- --coverage --watchAll=false

# Analyze with SonarQube
npx sonar-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN

# View results
# http://localhost:9000/dashboard?id=credit-default-swap-frontend
```

---

## 🛠️ Common Commands

### SonarQube Management

**Start:**
```bash
docker-compose -f docker-compose.sonarqube.yml up -d
```

**Stop:**
```bash
docker-compose -f docker-compose.sonarqube.yml down
```

**View logs:**
```bash
docker-compose -f docker-compose.sonarqube.yml logs -f sonarqube
```

**Restart:**
```bash
docker-compose -f docker-compose.sonarqube.yml restart
```

**Reset (delete all data):**
```bash
docker-compose -f docker-compose.sonarqube.yml down -v
```

**Check status:**
```bash
curl http://localhost:9000/api/system/status
```

**Windows PowerShell:**
```powershell
Invoke-RestMethod http://localhost:9000/api/system/status
```

### Workflow Management

**Trigger manual scan:**
```
GitHub → Actions → Security & Quality - SonarQube → Run workflow
```

**View latest results:**
```bash
# Using GitHub CLI
gh run list --workflow=security-sonarqube.yml
gh run view <run-id>
```

---

## 🔍 Troubleshooting

### "Quality Gate Failed"
**Cause:** Code doesn't meet quality standards

**Fix:**
1. Check SonarQube dashboard for specific issues
2. Address blocker and critical issues first
3. Increase test coverage if below 80%
4. Review and fix security vulnerabilities

### "Token Authentication Failed"
**Cause:** Invalid or expired token

**Fix:**
1. Login to SonarQube → My Account → Security → Generate new token
2. Update GitHub secret `SONAR_TOKEN`

### "Coverage 0%"
**Cause:** Coverage reports not generated

**Java Fix:**
```bash
# Ensure pom.xml has jacoco-maven-plugin
./mvnw clean verify -Pcoverage
ls target/site/jacoco/jacoco.xml  # Should exist
```

**Frontend Fix:**
```bash
npm run test -- --coverage
ls coverage/lcov.info  # Should exist
```

### "Container Keeps Restarting"
**Cause:** Insufficient resources

**Fix (Linux/macOS):**
```bash
sudo sysctl -w vm.max_map_count=262144
docker-compose -f docker-compose.sonarqube.yml restart
```

**Fix (Windows):**
- Docker Desktop usually handles this automatically
- Check Docker Desktop → Settings → Resources → increase memory to 4GB

### "No Snyk Results"
**Cause:** Missing Snyk token

**Fix:**
1. Sign up at https://snyk.io (free tier available)
2. Get API token from Account Settings
3. Add `SNYK_TOKEN` to GitHub Secrets
4. **OR** comment out Snyk job in workflow if not needed

---

## 🎯 Best Practices

### Before Committing
✅ Run local analysis to catch issues early
✅ Fix blocker and critical issues immediately
✅ Ensure test coverage >80% for new code
✅ Review security hotspots

### During PR Review
✅ Check quality gate status in PR comment
✅ Review new issues introduced
✅ Address reviewer feedback on quality
✅ Don't merge if quality gate fails

### Regular Maintenance
✅ Review SonarQube dashboard weekly
✅ Track technical debt trends
✅ Update dependencies with vulnerabilities monthly
✅ Export reports for compliance audits

---

## 📚 Quick Links

| Resource | Link |
|----------|------|
| **Full Documentation** | [.github/SONARQUBE_INTEGRATION.md](.github/SONARQUBE_INTEGRATION.md) |
| **SonarQube Dashboard** | http://localhost:9000 |
| **Snyk Dashboard** | https://app.snyk.io/ |
| **GitHub Security** | https://github.com/faculax/credit-default-swap/security |
| **Workflow File** | [.github/workflows/security-sonarqube.yml](.github/workflows/security-sonarqube.yml) |
| **SonarQube Docs** | https://docs.sonarqube.org/ |
| **Snyk Docs** | https://docs.snyk.io/ |

---

## 🎉 You're All Set!

Every code push now automatically:
1. ✅ Scans for security vulnerabilities
2. ✅ Checks code quality and bugs
3. ✅ Measures test coverage
4. ✅ Enforces quality gates
5. ✅ Posts results to PR comments
6. ✅ Updates GitHub Security tab

**Result:** Enterprise-grade security and quality, free and open source! 🛡️

---

## 💡 Pro Tips

- **IDE Integration:** Install SonarLint plugin for VS Code, IntelliJ, or Eclipse for real-time feedback
- **Pre-commit Hooks:** Use Semgrep in pre-commit hooks for fast local checks
- **Notifications:** Configure SonarQube email notifications for new issues
- **Custom Rules:** Create custom SonarQube rules for project-specific standards
- **API Access:** Use SonarQube Web API for custom reporting and integration

**Happy secure coding! 🚀**
