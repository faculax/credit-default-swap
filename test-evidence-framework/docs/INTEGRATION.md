# Integration Guide

**CI/CD, ReportPortal, and Evidence Export**

---

## 📋 Table of Contents

1. [ReportPortal Integration](#reportportal-integration)
2. [CI/CD Pipeline](#cicd-pipeline)
3. [Evidence Export & Dashboard](#evidence-export--dashboard)

---

## 🔗 ReportPortal Integration

### Quick Start with Docker ReportPortal

**⚡ 5-Minute Setup:**

```powershell
# 1. Start ReportPortal (Docker-based)
.\scripts\reportportal-start.ps1

# 2. Open UI: http://localhost:8080
# Login: default / 1q2w3e (change on first login!)

# 3. Create project "cds-platform"
# Get API token: Profile → API Keys

# 4. Configure framework
cd test-evidence-framework
cp reportportal.json.example reportportal.json
# Edit with your API token

# 5. Run tests and upload
cd ..
.\scripts\test-unified-local.ps1
cd test-evidence-framework
npm run upload-results -- --all

# 6. View results: http://localhost:8080
```

📚 **Complete Docker Setup Guide**: See `../docs/REPORTPORTAL_QUICKSTART.md`

---

### Configuration Options

**Option 1: Configuration File (Recommended)**

Create `reportportal.json` in `test-evidence-framework/`:

```json
{
  "endpoint": "http://localhost:8585",
  "token": "your-api-token-here",
  "project": "cds-platform",
  "launchName": "CDS Platform - Local Development",
  "launchDescription": "Automated test execution from local environment",
  "launchAttributes": [
    { "key": "environment", "value": "local" },
    { "key": "framework", "value": "test-evidence-framework" },
    { "key": "platform", "value": "windows" }
  ],
  "mode": "DEFAULT",
  "debug": false
}
```

**Option 2: Environment Variables**

```bash
export REPORTPORTAL_ENDPOINT=http://localhost:8585
export REPORTPORTAL_TOKEN=your-api-token-here
export REPORTPORTAL_PROJECT=cds-platform
```

### Getting API Token

**From Docker ReportPortal:**

1. Start ReportPortal: `.\scripts\reportportal-start.ps1`
2. Open http://localhost:8080
3. Login (default / 1q2w3e)
4. Click profile icon → "Profile"
5. Navigate to "API Keys" tab
6. Click "Generate API Key"
7. Copy token to `reportportal.json`

**From Remote ReportPortal:**

Same steps but use your organization's ReportPortal URL.

### Upload Test Results

**Automatic Upload (After Running Tests):**

```powershell
# Run unified test suite
.\scripts\test-unified-local.ps1

# Upload all results to ReportPortal
cd test-evidence-framework
npm run upload-results -- --all
```

**Service-Specific Upload:**

```powershell
cd test-evidence-framework

# Backend only
npm run upload-results -- \
  --service backend \
  --allure-results ../backend/target/allure-results \
  --launch-name "Backend Tests - Story 3.1"

# Frontend only
npm run upload-results -- \
  --service frontend \
  --allure-results ../frontend/allure-results \
  --launch-name "Frontend Tests - Story 3.1"

# Custom attributes
npm run upload-results -- \
  --service backend \
  --launch-name "PR #42 - Add Pricing" \
  --attributes "pr:42,story:story_7_2,epic:epic_07"
```

**Unified Upload (Recommended):**

```powershell
# Upload results from all services in one launch
npm run upload-results -- --all

# With custom launch name
npm run upload-results -- \
  --all \
  --launch-name "Epic 03 Complete - Trade Capture" \
  --attributes "epic:epic_03,milestone:v1.2"
```

### Launch Configuration

Each upload creates a "Launch" in ReportPortal with:

**Launch Name:** Descriptive identifier
- Local: "CDS Platform - Local Development"
- Story: "Story 3.1 - Trade Capture UI"
- Epic: "Epic 03 - Complete Trade Capture"
- PR: "PR #42 - Add CDS Pricing"
- CI: "CI Build #123"

**Attributes (Tags):** Key-value pairs for filtering
- `environment`: local, ci, staging, production
- `branch`: Git branch name
- `story`: story_3_1, story_4_2, etc.
- `epic`: epic_03, epic_04, etc.
- `service`: backend, frontend, gateway, risk-engine
- `buildType`: pull_request, push, manual
- `pr`: Pull request number
- `feature`: Feature name

### Docker ReportPortal Management

**Daily Commands:**

```powershell
# Start ReportPortal
.\scripts\reportportal-start.ps1

# Check status
.\scripts\reportportal-start.ps1 -Status

# View logs
.\scripts\reportportal-start.ps1 -Logs

# Stop ReportPortal
.\scripts\reportportal-start.ps1 -Stop

# Reset (clean all data)
.\scripts\reportportal-start.ps1 -Reset
```

**Service URLs:**
- **UI**: http://localhost:8080 (Web interface)
- **API**: http://localhost:8585 (REST API)
- **RabbitMQ Admin**: http://localhost:15672 (rabbitmq/rabbitmq)
- **MinIO Console**: http://localhost:9001 (minio/minio123)

**Data Persistence:**

All data stored in Docker volumes:
- Test launches and results
- User accounts and projects
- Attachments and screenshots
- Historical trends

Survives container restarts - only deleted with `-Reset` flag.

### Viewing Results in ReportPortal

**Navigate to Results:**

1. Open http://localhost:8080
2. Select project: **cds-platform**
3. Click **"Launches"** tab
4. Find your launch by:
   - Name (e.g., "Story 3.1 - Trade Capture")
   - Filter by attributes (e.g., `service:backend`)
   - Date range

**Launch Details View:**

- **Overview**: Pass/fail statistics, duration
- **Test Items**: Hierarchical test tree (Epic → Story → Test)
- **Logs**: Detailed execution logs
- **Attachments**: Screenshots, error outputs
- **History**: Compare with previous runs
- **Defects**: Linked defects and root causes

**Historical Comparison:**

Click any test → **"History"** tab:
- View pass/fail trend over time
- Identify flaky tests
- See when test started failing
- Compare execution duration

### Creating Filters & Dashboards

**Pre-configured Filters:**

Create these in ReportPortal UI (Filters → Add Filter):

| Filter Name | Query | Purpose |
|-------------|-------|---------|
| **Backend Tests** | `service:backend` | All backend test results |
| **Frontend Tests** | `service:frontend` | All frontend test results |
| **Story 3.x** | `story:story_3_*` | Epic 3 trade capture tests |
| **Failed Tests** | `status:FAILED` | All failing tests |
| **Local Runs** | `environment:local` | Developer local executions |
| **CI Runs** | `environment:ci` | GitHub Actions runs |
| **Last 7 Days** | `startTime:7d` | Recent test activity |

**Dashboard Widgets:**

Add to project dashboard (Dashboards → Add Widget):

1. **Launch Statistics Timeline** - Track pass/fail trends over time
2. **Overall Statistics** - Current sprint/week overview
3. **Failed Test Cases Top-20** - Identify most unstable tests
4. **Passing Rate per Launch** - Success rate trend
5. **Test Cases Growth Trend** - Coverage expansion tracking
6. **Component Health Check** - Per-service health (group by `service` attribute)

### ML-Powered Auto-Analysis

ReportPortal uses ML to automatically identify similar failures and suggest defect patterns.

**Enable Auto-Analysis:**

1. Go to Project Settings → Auto-Analysis
2. Toggle "Auto-Analysis" **ON**
3. Set options:
   - Minimum similarity: **95%** (recommended)
   - Analyzer mode: **ALL**
   - Auto-analysis scope: **LAUNCH**
4. Click **"Save"**

**How It Works:**

When a test fails, ReportPortal:
- Extracts error patterns from logs and stack traces
- Compares with historical failures
- Suggests similar defects (95%+ match)
- Groups related failures automatically

This speeds up root cause analysis significantly.

**Defect Types:**

Classify failures to track root causes:
- **Product Bug (PB)** - Application code defects
- **Automation Bug (AB)** - Test framework issues
- **System Issue (SI)** - Infrastructure problems
- **No Defect (ND)** - Expected behavior
- **To Investigate (TI)** - Needs analysis (default)

### Troubleshooting ReportPortal

**Upload Fails:**

```powershell
# Check ReportPortal is running
.\scripts\reportportal-start.ps1 -Status

# Verify API endpoint
curl http://localhost:8585/health

# Check token is valid (compare with Profile → API Keys)
# View upload logs with debug flag
cd test-evidence-framework
npm run upload-results -- --all --debug
```

**Common Issues:**

| Issue | Solution |
|-------|----------|
| "Unauthorized" error | Check API token is correct and not expired |
| "Project not found" | Verify project name is exactly "cds-platform" |
| "Connection refused" | Start ReportPortal: `.\scripts\reportportal-start.ps1` |
| Upload times out | Reduce batch size or increase timeout |
| No test data visible | Check Allure results exist before upload |
| UI not accessible | Wait 60s after start, check: `http://localhost:8080` |

**Reset ReportPortal (Nuclear Option):**

Clears all data and starts fresh:
```powershell
.\scripts\reportportal-start.ps1 -Reset
.\scripts\reportportal-start.ps1
# Then: create project, get new token, update reportportal.json
```

### Best Practices

**Launch Naming Convention:**
- **Local**: `Local Dev - <Story> - <Date>`
- **PR**: `PR #<num> - <Title>`
- **CI**: `CI Build #<num> - <Branch>`
- **Epic**: `Epic <num> - <Name> - <Date>`

**Attribute Strategy:**

Always include these attributes:
- `environment` (local/ci/staging/prod)
- `service` (backend/frontend/gateway/risk-engine)
- `story` or `epic` (for traceability)
- `branch` (for code correlation)

**Cleanup Policy:**
- **Keep**: Last 90 days of launches
- **Archive**: 90-180 days
- **Delete**: >180 days (unless critical reference)

### ReportPortal Resources

- **Quick Start**: `../docs/REPORTPORTAL_QUICKSTART.md` (5-minute setup)
- **Full Guide**: `../docs/REPORTPORTAL_SETUP.md` (comprehensive)
- **Official Docs**: https://reportportal.io/docs
- **Demo**: https://demo.reportportal.io (explore live instance)
- **GitHub**: https://github.com/reportportal

---

## 🚦 CI/CD Pipeline

### GitHub Actions Workflows

**Location:** `.github/workflows/`

**Two main workflows:**
1. `test-evidence.yml` - Main CI/CD pipeline
2. `deploy-evidence-dashboard.yml` - Dashboard deployment

### Main CI/CD Workflow

**Triggers:**
- Pull requests (with path filters)
- Push to main branch
- Manual dispatch (workflow_dispatch)

**Jobs (9 total):**

1. **detect-changes** - Path-based filtering (30s)
2. **build-framework** - Build TypeScript (1m)
3. **test-backend** - Maven + PostgreSQL (3-5m)
4. **test-frontend** - Jest + Allure (2-4m)
5. **test-gateway** - Maven (2-3m)
6. **test-risk-engine** - Maven (2-3m)
7. **upload-to-reportportal** - Batch upload (1-2m)
8. **comment-on-pr** - Post results (10s)
9. **deploy-dashboard** - Export + deploy (2-3m)

**Path Filters:**
```yaml
filters:
  backend: 'backend/**'
  frontend: 'frontend/**'
  gateway: 'gateway/**'
  risk-engine: 'risk-engine/**'
  framework: 'test-evidence-framework/**'
```

### Pull Request Flow

```
1. Developer pushes code
   ↓
2. GitHub Actions triggers
   ↓
3. Detect changed services (path filters)
   ↓
4. Run tests for changed services ONLY
   ↓
5. Upload results to ReportPortal
   ↓
6. Post PR comment with test summary
```

**Timing:**
- Single service: 4-7 minutes
- Multiple services: 7-10 minutes
- All services: 10-12 minutes

**PR Comment Example:**
```markdown
## Test Results for PR #42

**Branch:** feature/trade-creation  
**Commit:** abc123f

| Service | Tests | Passed | Failed | Skipped | Status |
|---|---:|---:|---:|---:|:---:|
| Backend | 45 | 43 | 2 | 0 | ❌ |
| Frontend | 32 | 32 | 0 | 0 | ✅ |
| Gateway | 18 | 18 | 0 | 0 | ⏭️ |
| Risk Engine | 0 | 0 | 0 | 0 | ⚪ |

**ReportPortal:** [View Launch](https://rp.example.com/launch/123)

---
*Automated by Test Evidence Framework*
```

### Main Branch Flow

```
1. Code merged to main
   ↓
2. GitHub Actions triggers
   ↓
3. Run FULL test suite (all services)
   ↓
4. Upload results to ReportPortal
   ↓
5. Export evidence from ReportPortal
   ↓
6. Generate HTML dashboard
   ↓
7. Deploy to GitHub Pages
```

**Timing:** 10-15 minutes (end-to-end)

### Required Secrets

Add in GitHub repository settings (Settings → Secrets and variables → Actions):

1. **REPORTPORTAL_ENDPOINT:** `https://your-reportportal.example.com`
2. **REPORTPORTAL_TOKEN:** Your ReportPortal API token
3. **REPORTPORTAL_PROJECT:** `cds-platform`

### Manual Trigger

**Via GitHub CLI:**
```bash
gh workflow run test-evidence.yml \
  --ref your-branch \
  --field run-all=true
```

**Via GitHub UI:**
1. Go to "Actions" tab
2. Select "Test Evidence" workflow
3. Click "Run workflow"
4. Select branch and options

### Selective Execution

**How it works:**

Path filters detect which services changed:
- PR changes `backend/` → Only run `test-backend` job
- PR changes `frontend/` → Only run `test-frontend` job
- PR changes `test-evidence-framework/` → Run all jobs
- Push to main → Always run all jobs

**Benefits:**
- Faster PR builds (4-7 min vs 10-12 min)
- Reduced CI costs
- Quicker feedback loop

---

## 📊 Evidence Export & Dashboard

### Export Evidence

**Local export:**
```bash
npm run export-evidence -- --output-dir ./evidence-export

# With filters
npm run export-evidence -- \
  --story-id story_3_1 \
  --services backend,frontend \
  --limit 50 \
  --output-dir ./evidence-export
```

**What it generates:**
- `index.html` - Story list with coverage badges
- `story_<id>.html` - Per-story detail pages
- `dashboard.css` - Responsive stylesheet
- JSON evidence files (metadata)

### Dashboard Features

**Story Index Page:**
- Story ID and title
- Coverage badge (✅ 100%, ⚠️ 50-99%, ❌ <50%, ⚪ not tested)
- Services tested (backend, frontend, gateway, risk-engine)
- Last test execution date
- Pass/fail status

**Story Detail Pages:**
- Story information (title, epic, services)
- Acceptance criteria list with coverage
- Test results table per service:
  - Total tests / Passed / Failed / Skipped
  - Last execution date
  - ReportPortal link
- Test history timeline:
  - Date, launch name, test counts
  - Chronological execution history

### GitHub Pages Deployment

**Setup (one-time):**

1. Enable GitHub Pages:
   - Go to repository Settings → Pages
   - Source: "GitHub Actions"
   - Save

2. Verify permissions in workflow:
   ```yaml
   permissions:
     contents: read
     pages: write
     id-token: write
   ```

**Automatic Deployment:**

Dashboard deploys automatically on push to main branch:
- `test-evidence.yml` workflow runs
- `deploy-dashboard` job executes (if tests pass)
- Evidence exported from ReportPortal
- HTML generated
- Deployed to GitHub Pages

**Manual Deployment:**

```bash
# Via CLI
gh workflow run deploy-evidence-dashboard.yml

# Via UI
# Actions → Deploy Evidence Dashboard → Run workflow
```

**Scheduled Deployment:**

Dashboard updates daily at 00:00 UTC (configured in workflow):
```yaml
schedule:
  - cron: '0 0 * * *'  # Daily at midnight UTC
```

### Accessing Dashboard

**URL:** `https://your-org.github.io/your-repo/`

**Example:**
- Organization: `faculax`
- Repository: `credit-default-swap`
- URL: `https://faculax.github.io/credit-default-swap/`

### Dashboard Styling

**Colors (from AGENTS.md):**
- Background: RGB(255, 255, 255) - White
- Primary: RGB(0, 240, 0) - Bright green
- Secondary: RGB(60, 75, 97) - Dark blue-gray
- Accent 1: RGB(0, 232, 247) - Cyan
- Accent 2: RGB(30, 230, 190) - Teal
- Accent 3: RGB(0, 255, 195) - Mint green

**Fonts:**
- Body: Arial
- Headings: Georgia

**Responsive:**
- Desktop: Full layout
- Tablet: 768px breakpoint
- Mobile: 480px breakpoint

**Accessible:**
- WCAG AA contrast compliance
- Semantic HTML
- Keyboard navigation
- Screen reader friendly

### Sharing Dashboard

**Non-Technical Stakeholders:**
1. Send dashboard URL
2. Explain badges:
   - ✅ Green = Fully tested
   - ⚠️ Yellow = Partially tested
   - ❌ Red = Needs more tests
   - ⚪ Gray = Not tested
3. Show how to click stories for details

**Technical Users:**
1. Share dashboard + ReportPortal links
2. Highlight specific stories needing attention
3. Show test execution history for trends

### Custom Filters

Export with custom filters:

```bash
# Specific stories
npm run export-evidence -- \
  --story-id story_3_1,story_3_2,story_5_1

# Specific services
npm run export-evidence -- \
  --services backend,frontend

# Recent launches only
npm run export-evidence -- --limit 20

# Combine filters
npm run export-evidence -- \
  --story-id story_3_1 \
  --services backend \
  --limit 10
```

---

## 🔧 Configuration Examples

### CI Workflow Customization

**Change Node/Java versions:**
```yaml
env:
  NODE_VERSION: '20'
  JAVA_VERSION: '21'
```

**Add new service:**
```yaml
# 1. Add path filter
filters:
  new-service: 'new-service/**'

# 2. Add test job
test-new-service:
  needs: detect-changes
  if: steps.filter.outputs.new-service == 'true' || needs.detect-changes.outputs.run-all == 'true'
  steps:
    - name: Run tests
      run: cd new-service && mvn clean test
```

**Customize PR comment:**
```yaml
- name: Generate comment
  run: |
    cat > comment.md << EOF
    ## Custom Test Results
    [Your custom format]
    EOF
```

### ReportPortal Customization

**Custom launch attributes:**
```json
{
  "launchAttributes": [
    { "key": "environment", "value": "staging" },
    { "key": "team", "value": "trading" },
    { "key": "sprint", "value": "2024-Q4-S3" }
  ]
}
```

**Service-specific launches:**
```bash
# Backend launch
npm run upload-results -- \
  --service backend \
  --launch-name "Backend Tests - PR #42" \
  --attributes environment:ci,service:backend

# Frontend launch
npm run upload-results -- \
  --service frontend \
  --launch-name "Frontend Tests - PR #42" \
  --attributes environment:ci,service:frontend
```

---

## 📈 Performance Optimization

### CI/CD Optimization

**Caching:**
- Maven dependencies: `~/.m2/repository`
- npm packages: `~/.npm`
- Build artifacts: `dist/`, `target/`

**Parallelization:**
All test jobs run in parallel (backend, frontend, gateway, risk-engine)

**Selective Execution:**
Only run tests for changed services (saves 5-10 minutes on PRs)

### ReportPortal Optimization

**Batch uploads:**
Upload all services in one job (reduces API calls)

**Attribute filtering:**
Use attributes for efficient queries (instead of full-text search)

**Launch retention:**
Configure retention policy in ReportPortal (e.g., keep 30 days)

### Dashboard Optimization

**Pagination:**
Export with `--limit` flag for large datasets

**Incremental updates:**
Only regenerate changed story pages (future enhancement)

**CDN:**
GitHub Pages uses CDN for fast delivery

---

## 🆘 Troubleshooting

### ReportPortal Connection Issues

**Problem:** Connection refused / 401 Unauthorized

**Solution:**
1. Verify endpoint URL: `curl -I $REPORTPORTAL_ENDPOINT/api/v1`
2. Check API token: `curl -H "Authorization: Bearer $TOKEN" $ENDPOINT/api/v1/user`
3. Verify project name matches ReportPortal UI
4. Check network/firewall rules

### CI Tests Fail Locally Pass

**Problem:** Tests pass locally but fail in CI

**Solution:**
1. Check environment variables in workflow
2. Verify Node/Java versions match local
3. Review PostgreSQL setup (TestContainers)
4. Increase timeouts if needed
5. Check logs: `gh run view <RUN_ID> --log`

### Dashboard Not Deploying

**Problem:** Dashboard workflow succeeds but dashboard not accessible

**Solution:**
1. Enable GitHub Pages (Settings → Pages → Source: GitHub Actions)
2. Check workflow permissions (`pages: write`, `id-token: write`)
3. Verify artifact upload step succeeded
4. Wait 2-3 minutes for deployment propagation
5. Check deployment status: `gh api repos/{owner}/{repo}/pages`

---

**Need more help?** Check [REFERENCE.md](REFERENCE.md) for comprehensive troubleshooting.
