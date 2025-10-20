# ✅ Render Deployment Checklist - Step by Step

**Deployment Started:** _________________ (fill in date/time)

---

## 🎯 Prerequisites

- [ ] Render account created at https://render.com
- [ ] Email verified
- [ ] Credit card added (required even for free tier database)

---

## 📋 Step 1: Create PostgreSQL Database

### 1.1 Navigate to Render Dashboard
- [ ] Go to: https://dashboard.render.com
- [ ] Click **New +** button (top right)
- [ ] Select **PostgreSQL**

### 1.2 Configure Database
Fill in these values:

| Field | Value to Enter |
|-------|---------------|
| **Name** | `sonarqube-db` |
| **Database** | `sonar` |
| **User** | `sonar` |
| **Region** | Choose closest: `Oregon (US West)` or `Frankfurt (Europe)` |
| **PostgreSQL Version** | `15` (default) |
| **Plan** | `Starter` ($7/month) - recommended<br>OR `Free` (limited resources) |

- [ ] Click **Create Database**
- [ ] Wait for database to provision (~2 minutes)

### 1.3 Save Database Connection Details
Once created, you'll see connection information. **COPY THESE NOW:**

```
Internal Database URL:
____________________________________________________________________

External Database URL:
____________________________________________________________________

Host:
____________________________________________________________________

Port:
____________________________________________________________________

Database:
____________________________________________________________________

Username:
____________________________________________________________________

Password:
____________________________________________________________________
```

⚠️ **IMPORTANT:** Keep this tab open or save these details - you'll need them in Step 2!

---

## 🚀 Step 2: Create SonarQube Web Service

### 2.1 Start New Web Service
- [ ] Click **New +** button
- [ ] Select **Web Service**
- [ ] Choose **Deploy an existing image from a registry**

### 2.2 Enter Image Details

**Image URL:**
```
docker.io/sonarqube:lts-community
```

- [ ] Paste the image URL above into the **Image URL** field

**Credentials:**
- [ ] Leave **BLANK** (public image, no credentials needed)
- [ ] Click **Next** or continue

### 2.3 Configure Service Details

| Field | Value to Enter |
|-------|---------------|
| **Name** | `sonarqube` |
| **Region** | ⚠️ **SAME as database** (e.g., Oregon) |
| **Branch** | Leave default |
| **Instance Type** | `Standard` (2GB RAM - $25/month)<br>⚠️ **Required** - Free tier won't work |

- [ ] Fill in the values above
- [ ] Scroll down to **Environment Variables**

### 2.4 Add Environment Variables

Click **Add Environment Variable** for each of these:

#### Variable 1: SONAR_JDBC_URL
- [ ] **Key:** `SONAR_JDBC_URL`
- [ ] **Value:** Convert your Internal Database URL:

**Step-by-step conversion:**
1. Take your **Internal Database URL** from Step 1.3
2. It looks like: `postgresql://sonar:XXXXX@dpg-XXXXX.oregon-postgres.render.com:5432/sonar`
3. Change `postgresql://` to `jdbc:postgresql://`
4. Remove the username and password part (`sonar:XXXXX@`)
5. Keep everything after the `@`

**Example:**
- Internal URL: `postgresql://sonar:abc123@dpg-xyz789.oregon-postgres.render.com:5432/sonar`
- JDBC URL: `jdbc:postgresql://dpg-xyz789.oregon-postgres.render.com:5432/sonar`

**Your JDBC URL:**
```
jdbc:postgresql://____________________________________________:5432/sonar
```

#### Variable 2: SONAR_JDBC_USERNAME
- [ ] **Key:** `SONAR_JDBC_USERNAME`
- [ ] **Value:** `sonar`

#### Variable 3: SONAR_JDBC_PASSWORD
- [ ] **Key:** `SONAR_JDBC_PASSWORD`
- [ ] **Value:** Copy the **password** from Step 1.3

#### Variable 4: SONAR_ES_BOOTSTRAP_CHECKS_DISABLE
- [ ] **Key:** `SONAR_ES_BOOTSTRAP_CHECKS_DISABLE`
- [ ] **Value:** `true`

**Summary - You should have 4 environment variables:**
- [ ] ✅ SONAR_JDBC_URL
- [ ] ✅ SONAR_JDBC_USERNAME
- [ ] ✅ SONAR_JDBC_PASSWORD
- [ ] ✅ SONAR_ES_BOOTSTRAP_CHECKS_DISABLE

### 2.5 Configure Advanced Settings

Scroll down to **Advanced** section:

- [ ] **Health Check Path:** `/api/system/status`
- [ ] **Port:** `9000`
- [ ] **Auto-Deploy:** `Yes` (optional - enables auto-updates)

### 2.6 Create Service
- [ ] Review all settings
- [ ] Click **Create Web Service**
- [ ] Wait for deployment (5-15 minutes first time)

**Watch the logs:**
- [ ] Click **Logs** tab to see deployment progress
- [ ] Look for: `SonarQube is operational`

---

## 🎉 Step 3: Access SonarQube

### 3.1 Get Your URL
Once deployed, Render gives you a public URL:

**Your SonarQube URL:**
```
https://sonarqube-______________.onrender.com
```

- [ ] Copy this URL
- [ ] Open in browser

### 3.2 First Login
- [ ] **Username:** `admin`
- [ ] **Password:** `admin`
- [ ] Click **Log in**

### 3.3 Change Default Password ⚠️ CRITICAL
- [ ] You'll be prompted immediately to change password
- [ ] Enter new secure password
- [ ] Save password in password manager

**New Admin Password:** _________________ (save securely!)

---

## 🔑 Step 4: Generate API Token

### 4.1 Navigate to Token Generation
- [ ] Click **User Menu** (top right, "A" icon)
- [ ] Select **My Account**
- [ ] Click **Security** tab
- [ ] Click **Generate Tokens**

### 4.2 Create Token
- [ ] **Name:** `GitHub Actions`
- [ ] **Type:** `Global Analysis Token` (or `User Token`)
- [ ] **Expires in:** `No expiration` (or `90 days` for better security)
- [ ] Click **Generate**

### 4.3 Save Token
⚠️ **You'll only see this once!**

**Your SonarQube Token:**
```
________________________________________________________________________
```

- [ ] Copy token immediately
- [ ] Save in password manager or secure notes

---

## 🔐 Step 5: Configure GitHub Secrets

### 5.1 Navigate to GitHub Secrets
- [ ] Open: https://github.com/faculax/credit-default-swap/settings/secrets/actions
- [ ] Click **New repository secret**

### 5.2 Add SONAR_HOST_URL
- [ ] **Name:** `SONAR_HOST_URL`
- [ ] **Value:** Your Render URL from Step 3.1 (e.g., `https://sonarqube-xyz.onrender.com`)
- [ ] Click **Add secret**

### 5.3 Add SONAR_TOKEN
- [ ] Click **New repository secret**
- [ ] **Name:** `SONAR_TOKEN`
- [ ] **Value:** Token from Step 4.3
- [ ] Click **Add secret**

### 5.4 Optional: Add SNYK_TOKEN
- [ ] Go to https://snyk.io and sign up (free)
- [ ] Generate API token
- [ ] Add as GitHub secret: `SNYK_TOKEN`

**GitHub Secrets Added:**
- [ ] ✅ SONAR_HOST_URL
- [ ] ✅ SONAR_TOKEN
- [ ] ⬜ SNYK_TOKEN (optional)

---

## 🧪 Step 6: Test Integration

### 6.1 Trigger Workflow
Open PowerShell and run:

```powershell
cd c:\Users\AyodeleOladeji\Documents\dev\credit-default-swap

# Make a test change
echo "# Test SonarQube integration" >> README.md

# Commit and push
git add README.md
git commit -m "test: trigger SonarQube scan on Render"
git push origin security-compliance
```

- [ ] Commands executed successfully

### 6.2 Watch GitHub Actions
- [ ] Go to: https://github.com/faculax/credit-default-swap/actions
- [ ] Click on the latest workflow run
- [ ] Watch progress (takes ~5-10 minutes)
- [ ] Verify all jobs complete successfully

### 6.3 Check SonarQube Results
- [ ] Go back to your Render SonarQube URL
- [ ] Click **Projects**
- [ ] You should see 4 projects:
  - [ ] credit-default-swap-backend
  - [ ] credit-default-swap-gateway
  - [ ] credit-default-swap-risk-engine
  - [ ] credit-default-swap-frontend
- [ ] Click each to see analysis results

---

## 📊 Step 7: View Results in All Dashboards

### 7.1 SonarQube Native UI
- [ ] Projects overview: `https://your-sonarqube.onrender.com/projects`
- [ ] Backend project: Click project → view bugs, vulnerabilities, code smells
- [ ] Activity timeline: Click **Activity** tab for history

### 7.2 Custom Dashboard
- [ ] Open `dashboard.html` in browser
- [ ] Update JavaScript to use your Render URL:
  ```javascript
  const SONAR_HOST = 'https://sonarqube-xyz.onrender.com';
  const SONAR_TOKEN = 'your-token-from-step-4';
  ```
- [ ] Refresh page to see aggregated view

### 7.3 GitHub Security Tab
- [ ] Go to: https://github.com/faculax/credit-default-swap/security/code-scanning
- [ ] View alerts from Snyk, Semgrep, Trivy, etc.

---

## 🎯 Step 8: Configure Projects (Optional)

### 8.1 Set Quality Gates
For each project in SonarQube:
- [ ] Project → **Project Settings** → **Quality Gates**
- [ ] Select: `Sonar way` (default) or create custom
- [ ] Save

### 8.2 Configure Notifications
- [ ] User Menu → **My Account** → **Notifications**
- [ ] Enable: "Background tasks in failure"
- [ ] Enable: "New issues"
- [ ] Save

---

## ✅ Deployment Complete!

**Deployment Finished:** _________________ (fill in date/time)

### 📝 Summary

**Your SonarQube Instance:**
- URL: _______________________________________________
- Username: admin
- Password: [Saved securely]
- API Token: [Saved securely]

**Costs:**
- PostgreSQL: $7/month (Starter) or $0 (Free)
- SonarQube: $25/month (Standard)
- **Total: ~$32/month**

### 🎉 What You Can Now Do:

✅ Every push triggers automatic security scanning
✅ View results in 3 dashboards (SonarQube, custom HTML, GitHub)
✅ Track bugs, vulnerabilities, code smells over time
✅ Enforce quality gates on pull requests
✅ Get notifications for new issues
✅ Access from anywhere (not just local machine)

---

## 🐛 Troubleshooting

### Issue: "Cannot connect to database"
- [ ] Check SONAR_JDBC_URL format is correct
- [ ] Verify database and SonarQube are in same region
- [ ] Check database is running (Render Dashboard → sonarqube-db)
- [ ] Verify password is correct

### Issue: "Out of memory" or service keeps restarting
- [ ] Upgrade to Standard plan (2GB RAM minimum)
- [ ] Check logs in Render Dashboard → sonarqube → Logs

### Issue: "Workflow fails with authentication error"
- [ ] Verify SONAR_HOST_URL in GitHub secrets (no trailing slash)
- [ ] Verify SONAR_TOKEN is correct
- [ ] Regenerate token if needed (Step 4)

### Issue: "No projects showing up"
- [ ] Wait 5 minutes after first workflow run
- [ ] Check workflow completed successfully
- [ ] Verify sonar-project.properties files exist in each service

---

## 📚 Next Steps

- [ ] Read `SONARQUBE_UI_GUIDE.md` for detailed UI walkthrough
- [ ] Review quality gate configuration
- [ ] Fix existing vulnerabilities (original 111 from SpotBugs)
- [ ] Set up Slack/email notifications
- [ ] Consider upgrading database to Starter plan for better performance
- [ ] Bookmark your SonarQube URL

---

## 🆘 Need Help?

- **Render Support:** https://render.com/docs/web-services
- **SonarQube Docs:** https://docs.sonarqube.org/latest/
- **Repository Issues:** https://github.com/faculax/credit-default-swap/issues

---

*Deployment checklist created: October 20, 2025*
