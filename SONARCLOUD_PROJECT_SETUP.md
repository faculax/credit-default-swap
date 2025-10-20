# 🔧 SonarCloud Project Setup Guide

## Current Issue
Your workflow runs successfully, but SonarCloud shows no data because the projects don't exist yet.

**Error in logs:**
```
ERROR Could not find a default branch for project with key 'ayodeleoladeji_credit-default-swap-frontend'. 
Make sure project exists.
```

## 📋 Step-by-Step Setup

### 1. Login to SonarCloud
Go to: https://sonarcloud.io
- Click "Log in" (top right)
- Use your GitHub account to authenticate

### 2. Create Organization (if not already done)
- Click the "+" icon (top right) → "Analyze new project"
- If you don't see your organization `ayodeleoladeji`, create it:
  - Click "+" → "Create new organization"
  - Choose "Import from GitHub"
  - Select organization: `ayodeleoladeji` (or your GitHub username)
  - Accept the terms and create

### 3. Create Projects Manually

You need to create **4 separate projects**:

#### Project 1: Backend Service
- Click "+" → "Analyze new project" → "Create a project manually"
- **Organization:** `ayodeleoladeji`
- **Project key:** `ayodeleoladeji_credit-default-swap-backend`
- **Display name:** `CDS Platform - backend`
- Click "Set Up"
- Choose: "With GitHub Actions" (already configured in your repo)
- **Set Main Branch:** `main` or `security-compliance` (your primary branch)

#### Project 2: Gateway Service
- Click "+" → "Analyze new project" → "Create a project manually"
- **Organization:** `ayodeleoladeji`
- **Project key:** `ayodeleoladeji_credit-default-swap-gateway`
- **Display name:** `CDS Platform - gateway`
- Click "Set Up"
- Choose: "With GitHub Actions"
- **Set Main Branch:** `main` or `security-compliance`

#### Project 3: Risk Engine Service
- Click "+" → "Analyze new project" → "Create a project manually"
- **Organization:** `ayodeleoladeji`
- **Project key:** `ayodeleoladeji_credit-default-swap-risk-engine`
- **Display name:** `CDS Platform - risk-engine`
- Click "Set Up"
- Choose: "With GitHub Actions"
- **Set Main Branch:** `main` or `security-compliance`

#### Project 4: Frontend
- Click "+" → "Analyze new project" → "Create a project manually"
- **Organization:** `ayodeleoladeji`
- **Project key:** `ayodeleoladeji_credit-default-swap-frontend`
- **Display name:** `CDS Platform - Frontend`
- Click "Set Up"
- Choose: "With GitHub Actions"
- **Set Main Branch:** `main` or `security-compliance`

### 4. Verify SONAR_TOKEN Secret

Go to your GitHub repository:
- Navigate to: **Settings** → **Secrets and variables** → **Actions**
- Verify you have a secret named: `SONAR_TOKEN`
- If not, create it:
  1. In SonarCloud, go to: **Account** → **Security** → **Generate Token**
  2. Name: `GitHub Actions Credit Default Swap`
  3. Type: **Global Analysis Token** (or scoped to your organization)
  4. Generate and copy the token
  5. Add it to GitHub Secrets as `SONAR_TOKEN`

### 5. Trigger a New Workflow Run

After creating all 4 projects:

```powershell
# Make a small change to trigger the workflow
git commit --allow-empty -m "chore: trigger SonarCloud analysis after project setup"
git push
```

Or manually trigger from GitHub:
- Go to: **Actions** → **SonarCloud Analysis** → **Run workflow**

### 6. Verify Results

After the workflow completes (~3-5 minutes):
- Visit: https://sonarcloud.io/organizations/ayodeleoladeji/projects
- You should see all 4 projects with analysis data
- Each project should show:
  - ✅ Bugs count
  - ✅ Vulnerabilities
  - ✅ Code Smells
  - ✅ Coverage %
  - ✅ Duplications %

## 🎯 Expected Project Structure

After setup, you'll have:

```
SonarCloud Organization: ayodeleoladeji
├── ayodeleoladeji_credit-default-swap-backend
├── ayodeleoladeji_credit-default-swap-gateway
├── ayodeleoladeji_credit-default-swap-risk-engine
└── ayodeleoladeji_credit-default-swap-frontend
```

## 🔗 Quick Links

Once setup is complete:

- **Organization Dashboard:** https://sonarcloud.io/organizations/ayodeleoladeji/projects
- **Backend Analysis:** https://sonarcloud.io/project/overview?id=ayodeleoladeji_credit-default-swap-backend
- **Gateway Analysis:** https://sonarcloud.io/project/overview?id=ayodeleoladeji_credit-default-swap-gateway
- **Risk Engine Analysis:** https://sonarcloud.io/project/overview?id=ayodeleoladeji_credit-default-swap-risk-engine
- **Frontend Analysis:** https://sonarcloud.io/project/overview?id=ayodeleoladeji_credit-default-swap-frontend

## 🚨 Troubleshooting

### Issue: "Project key already exists"
- The project may have been created automatically
- Go to: https://sonarcloud.io/organizations/ayodeleoladeji/projects
- Look for the project and configure its main branch

### Issue: "Organization not found"
- Verify organization name matches in both:
  - SonarCloud UI
  - Workflow file (`-Dsonar.organization=ayodeleoladeji`)

### Issue: "Token authentication failed"
- Regenerate token in SonarCloud
- Update `SONAR_TOKEN` secret in GitHub
- Ensure token has correct permissions (Analysis scope)

### Issue: "Still no data after workflow succeeds"
- Check workflow logs for actual errors (not just summary)
- Verify project keys match exactly between workflow and SonarCloud
- Ensure main branch is configured in each project
- Check that builds actually completed (tests ran, coverage generated)

## ✅ Success Checklist

- [ ] SonarCloud organization created: `ayodeleoladeji`
- [ ] 4 projects created manually on SonarCloud
- [ ] Main branch configured for each project
- [ ] `SONAR_TOKEN` secret exists in GitHub repo
- [ ] Workflow triggered after project creation
- [ ] All 4 projects show data on SonarCloud dashboard
- [ ] No errors in workflow logs

---

**Next Steps After Setup:**
1. Review quality gates for each project
2. Set up branch protection rules
3. Configure quality gate conditions (optional)
4. Review and fix critical/high severity issues

