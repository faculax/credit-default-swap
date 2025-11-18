# 🚀 Quick Start - ReportPortal Test Tracking

Get persistent test result tracking with historical trends in 5 minutes.

## Prerequisites

- Docker Desktop installed and running
- 4GB RAM available for Docker
- Ports available: 8080, 8585, 5432, 5672, 9200, 9000

## 1. Start ReportPortal

```powershell
.\scripts\reportportal-start.ps1
```

**First run**: Downloads Docker images (~2-3 minutes)  
**Subsequent runs**: Starts in ~30 seconds

## 2. Access UI

Open: **http://localhost:8080**

**Default credentials:**
- Username: `default`
- Password: `1q2w3e`

⚠️ **Change password on first login!**

## 3. Create Project

1. Click **"Administrate"** → **"Projects"**
2. Click **"Add New Project"**
3. Enter name: `cds-platform`
4. Click **"Add"**

## 4. Get API Token

1. Click **profile icon** (top right) → **"Profile"**
2. Go to **"API Keys"** tab
3. Click **"Generate API Key"**
4. Copy the token

## 5. Configure Framework

```powershell
cd test-evidence-framework
cp reportportal.json.example reportportal.json
```

Edit `reportportal.json`:
```json
{
  "endpoint": "http://localhost:8585",
  "token": "YOUR_ACTUAL_TOKEN_HERE",
  "project": "cds-platform"
}
```

## 6. Run Tests & Upload

```powershell
# Run all tests
cd ..
.\scripts\test-unified-local.ps1

# Upload results to ReportPortal
cd test-evidence-framework
npm run upload-results -- --all
```

## 7. View Dashboard

Go back to http://localhost:8080 → **"Launches"** tab

You'll see your test results categorized by:
- Epic
- Story
- Service (backend/frontend/gateway/risk-engine)
- Test status (passed/failed/skipped)

---

## Daily Commands

```powershell
# Start
.\scripts\reportportal-start.ps1

# Check status
.\scripts\reportportal-start.ps1 -Status

# View logs
.\scripts\reportportal-start.ps1 -Logs

# Stop
.\scripts\reportportal-start.ps1 -Stop

# Reset all data (clean slate)
.\scripts\reportportal-start.ps1 -Reset
```

---

## What You Get

### Test Result Tracking
- ✅ All test executions stored with timestamps
- ✅ Pass/fail trends over time
- ✅ Flaky test identification
- ✅ Test execution history

### Categorization
- 📊 Group by Epic/Story
- 🎯 Filter by service
- 🔍 Search by test name
- 🏷️ Custom tags/attributes

### ML-Powered Analysis
- 🤖 Auto-detect similar failures
- 🔗 Link related defects
- 📈 Failure pattern recognition
- 💡 Suggested fixes

### Dashboards & Widgets
- 📊 Pass rate timeline
- ⚠️ Top failing tests
- 📈 Test coverage growth
- 🎯 Success rate per launch

### Integrations
- 🔔 Slack/Email notifications
- 📝 JIRA defect linking
- 🔄 CI/CD pipeline integration
- 📊 Custom webhooks

---

## Troubleshooting

### UI not loading?
```powershell
# Check if services are healthy
docker compose -f docker-compose.reportportal.yml ps

# View API logs
docker logs reportportal-api
```

### Upload failing?
1. Check token is correct in `reportportal.json`
2. Verify project name is exactly `cds-platform`
3. Ensure ReportPortal is running: http://localhost:8080

### Need fresh start?
```powershell
# Nuclear option - deletes all data
.\scripts\reportportal-start.ps1 -Reset
.\scripts\reportportal-start.ps1
```

---

## Next Steps

1. **Create custom filters**: Backend tests, Story 3.x, Failed tests
2. **Set up dashboard**: Add widgets for pass rate, trends, top failures
3. **Enable ML auto-analysis**: Project Settings → Auto-Analysis
4. **Configure notifications**: Slack alerts on high failure rate
5. **Integrate with CI/CD**: Upload results from GitHub Actions

---

## Full Documentation

- **Complete Setup**: [docs/REPORTPORTAL_SETUP.md](docs/REPORTPORTAL_SETUP.md)
- **Integration Guide**: [test-evidence-framework/docs/INTEGRATION.md](test-evidence-framework/docs/INTEGRATION.md)
- **ReportPortal Docs**: https://reportportal.io/docs

---

## Architecture

```
ReportPortal Stack (8 containers)
├── UI (8080)              - Web interface
├── API (8585)             - REST API
├── PostgreSQL (5432)      - Data storage
├── RabbitMQ (5672/15672)  - Message broker
├── Elasticsearch (9200)   - Log indexing
├── MinIO (9000/9001)      - Attachments (S3)
├── Analyzer               - ML failure analysis
└── Metrics                - Statistics aggregation
```

All data persists in Docker volumes - survives restarts!

---

**Ready?** Start now: `.\scripts\reportportal-start.ps1` 🚀
