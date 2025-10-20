# 🚀 Quick Commands - SonarCloud Setup

## Step 1: Add SONAR_TOKEN to GitHub

1. Get your token from SonarCloud (you should have received this during setup)
2. Go to: https://github.com/faculax/credit-default-swap/settings/secrets/actions
3. Click **New repository secret**
4. Name: `SONAR_TOKEN`
5. Value: Paste your token
6. Click **Add secret**

---

## Step 2: Commit and Push

Open PowerShell and run these commands:

```powershell
# Navigate to project
cd c:\Users\AyodeleOladeji\Documents\dev\credit-default-swap

# Check what files were changed
git status

# Stage all changes
git add .

# Commit with descriptive message
git commit -m "feat: migrate to SonarCloud for hosted code quality analysis

- Add sonar.organization to all pom.xml files (backend, gateway, risk-engine)
- Update GitHub Actions workflow to use SonarCloud instead of self-hosted
- Configure project keys with ayodeleoladeji_ prefix
- Update all SonarQube references to SonarCloud
- Remove dependency on SONAR_HOST_URL secret

Benefits:
- No infrastructure to manage (free us from Render deployment)
- Zero ongoing costs for public repository
- Automatic updates and maintenance
- Enterprise-grade performance
- Accessible from anywhere"

# Push to trigger workflow
git push origin security-compliance
```

---

## Step 3: Watch Progress

After pushing, monitor these:

### GitHub Actions:
```
https://github.com/faculax/credit-default-swap/actions
```

### SonarCloud Dashboard:
```
https://sonarcloud.io/organizations/ayodeleoladeji/projects
```

---

## Expected Timeline:

- **0-2 min:** Workflow starts
- **3-8 min:** Building and testing all services
- **9-12 min:** SonarCloud analysis completes
- **12-15 min:** All security scans finish
- **15 min:** Quality gate summary generated

---

## Quick Access URLs:

### Your Projects:
```
Backend:     https://sonarcloud.io/project/overview?id=ayodeleoladeji_credit-default-swap-backend
Gateway:     https://sonarcloud.io/project/overview?id=ayodeleoladeji_credit-default-swap-gateway
Risk Engine: https://sonarcloud.io/project/overview?id=ayodeleoladeji_credit-default-swap-risk-engine
Frontend:    https://sonarcloud.io/project/overview?id=ayodeleoladeji_credit-default-swap-frontend
```

### Other Dashboards:
```
GitHub Security:  https://github.com/faculax/credit-default-swap/security
Code Scanning:    https://github.com/faculax/credit-default-swap/security/code-scanning
Snyk Dashboard:   https://app.snyk.io/
```

---

## If Something Goes Wrong:

### Check GitHub Actions logs:
```powershell
# View workflow status
start https://github.com/faculax/credit-default-swap/actions
```

### Common Issues:

**"Authentication failed"**
- Solution: Verify SONAR_TOKEN is correctly added to GitHub secrets

**"Project not found"**
- Solution: Projects are auto-created on first analysis - just wait

**"Quality gate failed"**
- Solution: This is NORMAL - you have 111 bugs to fix! View details in SonarCloud

---

## After First Successful Run:

You'll have:
- ✅ 4 projects visible in SonarCloud
- ✅ Quality metrics for each service
- ✅ List of bugs, vulnerabilities, code smells
- ✅ Code coverage reports
- ✅ Security hotspots identified
- ✅ PR decoration working (on future PRs)

---

*Ready? Copy the commands above and run them!* 🚀
