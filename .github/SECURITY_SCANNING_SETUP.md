# 🚀 Quick Start: GitHub Actions Security Scanning

## ✅ What You Just Set Up

Your repository now has **automated security scanning** that runs on every push, pull request, and daily at 2 AM UTC.

## 🔐 Required: Add GitHub Secret

**⚠️ IMPORTANT:** You need to add GitHub secrets for the workflow to function properly:

### Step-by-Step:

1. **Go to your repository on GitHub**
   ```
   https://github.com/faculax/credit-default-swap
   ```

2. **Navigate to Settings → Secrets and variables → Actions**
   
3. **Click "New repository secret"**

4. **Add the following secrets:**

   **Secret 1: DEFECTDOJO_TOKEN** (Required)
   - Name: `DEFECTDOJO_TOKEN`
   - Value: `<your-defectdojo-api-token>`
   - Purpose: Authenticates with DefectDojo to upload scan results
   - How to get: Login to DefectDojo → Click your username (top right) → API v2 Key
   - Format: Just the token value (e.g., `a277b6a2a979fd8f71838337f40c5887da72d94d`)
   
   **Secret 2: NVD_API_KEY** (Highly Recommended)
   - Name: `NVD_API_KEY`
   - Value: `<your-nvd-api-key>`
   - Purpose: Speeds up OWASP Dependency Check scans (from hours to minutes)
   - Get one free at: https://nvd.nist.gov/developers/request-an-api-key
   
5. **Click "Add secret"** for each one

## 🎬 Testing the Workflow

### Option 1: Push a Commit (Automatic)

The workflow will run automatically on your next push to `main`, `dev`, or `security-compliance`:

```bash
# Make a small change
echo "# Test" >> README.md
git add README.md
git commit -m "test: Trigger security scan workflow"
git push
```

### Option 2: Manual Trigger

1. Go to: https://github.com/faculax/credit-default-swap/actions
2. Click on **"Security Compliance Scanning"** workflow
3. Click **"Run workflow"**
4. Select branch and click **"Run workflow"**

## 📊 Viewing Results

### In GitHub Actions

1. Go to **Actions** tab: https://github.com/faculax/credit-default-swap/actions
2. Click on the running/completed workflow
3. View:
   - ✅ Each scan job (Backend, Gateway, Frontend, etc.)
   - 📥 Download artifacts (scan reports)
   - 📈 Summary with DefectDojo link

### In DefectDojo

1. Visit: https://defectdojo-s74m.onrender.com
2. Login with your credentials
3. View **Dashboard** → **Credit Default Swap Platform**

## 🔍 What Gets Scanned

| Component | Scanners |
|-----------|----------|
| **Backend** | OWASP Dependency Check, SpotBugs, Checkstyle |
| **Gateway** | OWASP Dependency Check, SpotBugs, Checkstyle |
| **Risk Engine** | OWASP Dependency Check, SpotBugs, Checkstyle |
| **Frontend** | npm audit, ESLint Security, Retire.js |
| **Secrets** | Gitleaks |

## 🛡️ Security Gate

The workflow will:
- ❌ **BLOCK** merge if secrets are detected
- ⚠️ **WARN** on critical vulnerabilities (doesn't block)
- ℹ️ **INFO** on high/medium vulnerabilities

## 📝 Workflow Files

```
.github/workflows/
├── security-scan.yml         # Main workflow
└── README.md                  # Detailed documentation

compliance/scripts/
└── upload-to-defectdojo.py   # Python uploader for DefectDojo API
```

## 🎯 Next Steps

### 1. Verify Workflow Runs Successfully

After adding the `DEFECTDOJO_PASSWORD` secret:
- Push a commit or manually trigger the workflow
- Check Actions tab for green checkmarks
- Verify results appear in DefectDojo

### 2. Review DefectDojo Findings

- Login to DefectDojo
- Review any critical/high vulnerabilities
- Create issues/tickets for remediation

### 3. Configure Branch Protection (Optional)

Require security scans to pass before merging:

1. Go to **Settings → Branches**
2. Add rule for `main` branch
3. Check "Require status checks to pass"
4. Select "Security Quality Gate"

## 🔧 Customization

### Change Scan Schedule

Edit `.github/workflows/security-scan.yml`:

```yaml
schedule:
  # Run at different time (e.g., 6 AM UTC)
  - cron: '0 6 * * *'
```

### Adjust Security Gate

Edit the `security-gate` job to change what blocks merges:

```yaml
# Current: Blocks on secrets, warns on critical CVEs
# To block on critical CVEs too:
if [ "$CRITICAL" -gt 0 ]; then
  echo "❌ FAILED: Critical vulnerabilities found!"
  exit 1
fi
```

## 🆘 Troubleshooting

### "Authentication Failed" Error

**Cause:** `DEFECTDOJO_TOKEN` secret not set or invalid

**Fix:**
1. Login to DefectDojo: https://defectdojo-s74m.onrender.com
2. Click your username (top right) → **API v2 Key**
3. Copy the API token (e.g., `a277b6a2a979fd8f71838337f40c5887da72d94d`)
4. Add it as GitHub secret: `DEFECTDOJO_TOKEN`
5. Make sure DefectDojo is accessible (not sleeping on Render.com)
6. Rerun the workflow

### "NVD API Key Warning" or Very Slow OWASP Scans

**Warning Message:** `An NVD API Key was not provided - it is highly recommended to use an NVD API key`

**Cause:** `NVD_API_KEY` secret not set

**Impact:**
- ❌ Scans take 30-60+ minutes (or timeout)
- ❌ Rate limited by NVD (only 5 requests per 30 seconds)
- ❌ May fail to download complete CVE database

**Fix:**
1. Get a free NVD API key: https://nvd.nist.gov/developers/request-an-api-key
2. Add it as GitHub secret: `NVD_API_KEY`
3. Rerun the workflow
4. ✅ Scans will now complete in 2-5 minutes

### "No Scan Results Found"

**Cause:** Scan jobs failed to generate reports

**Fix:**
1. Check individual scan job logs in GitHub Actions
2. Look for Maven/npm errors
3. Test locally: `./defectdojo.ps1 scan`

### DefectDojo Timeout

**Cause:** DefectDojo on Render.com may be sleeping (free tier)

**Fix:**
1. Visit https://defectdojo-s74m.onrender.com to wake it
2. Wait 30-60 seconds
3. Rerun workflow

## 📚 More Information

See detailed documentation: `.github/workflows/README.md`

## 🎉 Benefits Over Local Workflow

| Feature | Local (`./defectdojo.ps1`) | GitHub Actions |
|---------|---------------------------|----------------|
| **Automation** | Manual | ✅ Automatic on push/PR |
| **Speed** | Sequential | ✅ Parallel execution |
| **CI/CD Integration** | Manual | ✅ Built-in |
| **Branch Protection** | Manual | ✅ Can block merges |
| **Scheduled Scans** | Manual | ✅ Daily at 2 AM |
| **Team Visibility** | Local only | ✅ All team members |

**You can still use `./defectdojo.ps1` for local development!**

---

## ✨ Success Criteria

Your setup is complete when:

1. ✅ `DEFECTDOJO_PASSWORD` secret added
2. ✅ Workflow runs successfully in Actions tab
3. ✅ Scan results appear in DefectDojo dashboard
4. ✅ Security gate passes (no secrets detected)

**Next:** Make a test commit and watch your automated security pipeline in action! 🚀
