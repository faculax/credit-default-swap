# GitHub Actions Setup Guide - Security Pipeline

## How the Pipeline Works

### Automatic Triggers

The security pipeline will **automatically run** when:

1. **Push to ANY branch** ✅
   ```bash
   git push origin <any-branch-name>
   ```

2. **Pull Request to main or develop**
   ```bash
   # When you create a PR targeting main or develop
   ```

3. **Daily Schedule**
   - Runs every day at 2 AM UTC
   - Catches any new vulnerabilities in dependencies

### Configuration File

Location: `.github/workflows/cds-security-quality.yml`

```yaml
on:
  push:
    branches: [ '**' ]  # Run on ANY branch push
  pull_request:
    branches: [ main, develop ]  # Run on PRs to these branches
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM UTC
```

---

## How to Test It

### Step 1: Commit Your Changes

```powershell
# Add all security configuration files
git add .

# Commit with a descriptive message
git commit -m "feat: add comprehensive security and quality analysis

- Add GitHub Actions security pipeline
- Configure SpotBugs, OWASP, PMD for Java services
- Add ESLint security rules for frontend
- Include documentation and helper scripts"

# Push to your branch
git push origin security-compliance
```

### Step 2: View the Pipeline Running

1. Go to your GitHub repository: `https://github.com/faculax/credit-default-swap`

2. Click on the **"Actions"** tab at the top

3. You should see a workflow run named "CDS Platform Security & Quality Analysis"

4. Click on it to see the progress

### Step 3: Monitor the Jobs

The pipeline runs 6 jobs in parallel:

```
┌─────────────────────────────────────────┐
│  Secrets & Sensitive Data Detection    │
│  ├─ Scans for hardcoded credentials    │
│  └─ Checks database URLs, API keys     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Java Backend Security Analysis         │
│  ├─ backend service                     │
│  ├─ gateway service                     │
│  └─ risk-engine service                 │
│      ├─ SpotBugs security analysis      │
│      ├─ OWASP dependency check          │
│      ├─ PMD static analysis             │
│      └─ Checkstyle code quality         │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Frontend Security & Linting            │
│  ├─ ESLint security rules               │
│  ├─ TypeScript type checking            │
│  ├─ npm audit                           │
│  └─ XSS vulnerability detection         │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Infrastructure Security Analysis       │
│  ├─ Docker configuration                │
│  ├─ ORE configuration security          │
│  └─ Environment variable audit          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Dynamic Application Security Testing   │
│  ├─ API security tests (main only)     │
│  ├─ SQL injection tests                 │
│  └─ Authentication bypass tests         │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Security & Quality Summary             │
│  ├─ Aggregates all reports              │
│  ├─ Generates SECURITY_REPORT.md        │
│  └─ Fails if critical issues found      │
└─────────────────────────────────────────┘
```

---

## Understanding the Results

### Green Checkmark ✓
- All security checks passed
- No critical vulnerabilities found
- Safe to merge

### Yellow Warning ⚠
- Some warnings found
- Review the details
- May still be safe to merge

### Red X ✗
- Critical issues found
- Build failed
- **DO NOT merge** until fixed

---

## Viewing Security Reports

### In GitHub Actions UI

1. Click on a completed workflow run
2. Scroll down to **"Artifacts"** section
3. Download the reports:
   - `backend-security-reports`
   - `gateway-security-reports`
   - `risk-engine-security-reports`
   - `frontend-security-reports`
   - `security-quality-report`

### Report Files Include

- **SpotBugs**: `spotbugsXml.xml`, `spotbugs.html`
- **OWASP**: `dependency-check-report.html`
- **PMD**: `pmd.xml`
- **Checkstyle**: `checkstyle-result.xml`
- **ESLint**: `eslint-report.json`
- **npm audit**: `npm-audit.json`
- **Summary**: `SECURITY_REPORT.md`

---

## Customizing the Pipeline

### Run on Specific Branches Only

Edit `.github/workflows/cds-security-quality.yml`:

```yaml
on:
  push:
    branches: [ main, develop, feature/* ]  # Specific patterns
```

### Disable Dynamic Testing on Every Run

Dynamic testing can be slow. To run only on main:

```yaml
dynamic-security-testing:
  name: Dynamic Application Security Testing
  runs-on: ubuntu-latest
  if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```

### Skip Pipeline on Specific Commits

Add to commit message:

```bash
git commit -m "docs: update README [skip ci]"
```

Or use:
```bash
git commit -m "docs: update README [skip actions]"
```

### Adjust Failure Thresholds

Edit `pom.xml` in each Java service:

```xml
<!-- Fail build on CVSS >= 7.0 (default) -->
<failBuildOnCVSS>7</failBuildOnCVSS>

<!-- Change to 9.0 for critical only -->
<failBuildOnCVSS>9</failBuildOnCVSS>
```

---

## Common Issues and Solutions

### Issue: Maven wrapper not found

**Solution**: Add Maven wrappers to Java services:

```powershell
cd backend
mvn wrapper:wrapper

cd ../gateway
mvn wrapper:wrapper

cd ../risk-engine
mvn wrapper:wrapper
```

### Issue: Node modules not cached

**Solution**: The pipeline automatically caches dependencies. First run will be slower.

### Issue: Too many ESLint warnings

**Solution**: Suppress non-critical warnings:

```typescript
// eslint-disable-next-line security/detect-object-injection
const value = obj[key];
```

### Issue: Pipeline taking too long

**Solution**: 
1. Comment out OWASP dependency check in workflow (it's slow)
2. Run it manually once a week
3. Keep other fast checks running on every push

---

## Pipeline Status Badge

Add to your README.md:

```markdown
[![Security Analysis](https://github.com/faculax/credit-default-swap/actions/workflows/cds-security-quality.yml/badge.svg)](https://github.com/faculax/credit-default-swap/actions/workflows/cds-security-quality.yml)
```

This shows the current status of your security pipeline!

---

## What Happens When Pipeline Fails

### On Push
1. GitHub marks the commit with a ✗
2. You receive an email notification
3. Check the Actions tab for details
4. Fix the issue and push again

### On Pull Request
1. PR shows "Checks failed"
2. Cannot merge if required checks are enabled
3. Review the failure details
4. Push fixes to the PR branch
5. Pipeline re-runs automatically

---

## Enabling Required Status Checks

Make security checks mandatory before merging:

1. Go to repository **Settings**
2. Click **Branches** in left sidebar
3. Add rule for `main` branch
4. Enable "Require status checks to pass"
5. Select:
   - ✓ Secrets & Sensitive Data Detection
   - ✓ Java Backend Security Analysis
   - ✓ Frontend Security & Linting
   - ✓ Infrastructure Security Analysis

Now PRs cannot be merged until all checks pass!

---

## Local vs CI/CD

### Local Script (Faster feedback)
```powershell
.\security-check.ps1
```
- Runs in ~5-10 minutes
- Skips slow OWASP checks
- Good for development

### CI/CD Pipeline (Complete checks)
- Runs in ~15-30 minutes
- Includes all security scans
- Runs on every push
- Required for merge

**Best Practice**: Run local script before committing, let CI/CD catch anything you missed!

---

## Next Steps

1. ✅ Commit and push your changes
2. ✅ Watch the pipeline run in GitHub Actions
3. ✅ Review any issues found
4. ✅ Add status badge to README
5. ✅ Enable required checks for main branch
6. ✅ Share with team!

---

**Questions?** Check `SECURITY_SETUP.md` for detailed documentation.