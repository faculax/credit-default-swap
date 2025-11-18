# ReportPortal Setup Guide

## Quick Start (5 minutes)

### 1. Start ReportPortal

```powershell
# Start ReportPortal Docker stack
.\scripts\reportportal-start.ps1

# Wait for services to initialize (2-3 minutes on first run)
```

### 2. Initial Setup

1. **Open ReportPortal UI**: http://localhost:8080
2. **Login**:
   - Username: `default`
   - Password: `1q2w3e`
   - ⚠️ **Change password immediately on first login!**

3. **Create Project**:
   - Click "Administrate" → "Projects" → "Add New Project"
   - Name: `cds-platform`
   - Type: `INTERNAL`
   - Click "Add"

4. **Get API Token**:
   - Click profile icon (top right) → "Profile"
   - Navigate to "API Keys" tab
   - Click "Generate API Key"
   - Copy the token (you'll need it next)

### 3. Configure Test Evidence Framework

```powershell
# Create config file from example
cd test-evidence-framework
cp reportportal.json.example reportportal.json

# Edit reportportal.json and replace YOUR_API_TOKEN with actual token
```

**reportportal.json**:
```json
{
  "endpoint": "http://localhost:8585",
  "token": "YOUR_ACTUAL_API_TOKEN_HERE",
  "project": "cds-platform",
  "launchName": "CDS Platform - Local Development"
}
```

### 4. Upload Test Results

```powershell
# Run tests first
cd ..
.\scripts\test-unified-local.ps1

# Upload to ReportPortal
cd test-evidence-framework
npm run upload-results -- --all
```

### 5. View Results

Open http://localhost:8080 → Select "cds-platform" → Click "Launches"

---

## Architecture

ReportPortal runs 8 Docker containers:

| Service | Port | Purpose |
|---------|------|---------|
| **UI** | 8080 | Web interface |
| **API** | 8585 | REST API |
| **PostgreSQL** | 5432 | Test data storage |
| **RabbitMQ** | 5672, 15672 | Message broker + Admin UI |
| **Elasticsearch** | 9200 | Log indexing & search |
| **MinIO** | 9000, 9001 | Attachment storage (S3-compatible) |
| **Analyzer** | - | ML-based failure analysis |
| **Metrics** | - | Statistics aggregation |

---

## Daily Workflow

### Start ReportPortal
```powershell
.\scripts\reportportal-start.ps1
```

### Check Status
```powershell
.\scripts\reportportal-start.ps1 -Status
```

### View Logs
```powershell
.\scripts\reportportal-start.ps1 -Logs
```

### Stop ReportPortal
```powershell
.\scripts\reportportal-start.ps1 -Stop
```

### Reset (Delete All Data)
```powershell
.\scripts\reportportal-start.ps1 -Reset
```

---

## Integration with Test Evidence Framework

### Automatic Upload (Recommended)

Add to `test-evidence-framework/package.json`:

```json
{
  "scripts": {
    "test:upload": "npm run upload-results -- --all"
  }
}
```

Run tests and upload:
```powershell
.\scripts\test-unified-local.ps1
cd test-evidence-framework
npm run test:upload
```

### Manual Upload by Service

```powershell
cd test-evidence-framework

# Backend only
npm run upload-results -- --service backend

# Frontend only  
npm run upload-results -- --service frontend

# All services
npm run upload-results -- --all
```

### Custom Launch Configuration

```powershell
# Story-specific launch
npm run upload-results -- \
  --service frontend \
  --launch-name "Story 3.1 - Trade Capture UI" \
  --attributes "story:story_3_1,epic:epic_03"

# PR validation
npm run upload-results -- \
  --all \
  --launch-name "PR #42 - Add CDS Pricing" \
  --attributes "pr:42,branch:feature/cds-pricing"
```

---

## Organizing Test Results

### Launch Attributes (Tags)

Use attributes to categorize and filter launches:

```json
{
  "launchAttributes": [
    { "key": "environment", "value": "local" },
    { "key": "story", "value": "story_3_1" },
    { "key": "epic", "value": "epic_03" },
    { "key": "service", "value": "backend" },
    { "key": "feature", "value": "trade-capture" }
  ]
}
```

### Pre-configured Filters

Create filters in ReportPortal UI:

| Filter Name | Query |
|-------------|-------|
| **Backend Tests** | `service:backend` |
| **Frontend Tests** | `service:frontend` |
| **Story 3.x** | `story:story_3_*` |
| **Failed Tests** | `status:FAILED` |
| **Local Runs** | `environment:local` |

### Widget Dashboard

Create dashboard with widgets:

1. **Launch Statistics Timeline** - Track pass/fail trends
2. **Overall Statistics** - Current sprint overview
3. **Failed Test Cases Top-20** - Most unstable tests
4. **Passing Rate per Launch** - Success rate trend
5. **Test Cases Growth Trend** - Test coverage expansion

---

## Advanced Features

### ML-Powered Auto-Analysis

ReportPortal Analyzer automatically:
- Identifies similar failures across launches
- Suggests defect patterns
- Links related test failures
- Recommends defect types

**Enable Auto-Analysis**:
1. Go to Project Settings → Auto-Analysis
2. Toggle "Auto-Analysis" ON
3. Set minimum similarity threshold (default: 95%)
4. Click "Save"

### Integration Patterns

Pattern suggestions based on failure history and ML analysis.

### Defect Types

Customize defect classification:
- **Product Bug** (PB) - Application defects
- **Automation Bug** (AB) - Test framework issues  
- **System Issue** (SI) - Infrastructure problems
- **No Defect** (ND) - Expected behavior
- **To Investigate** (TI) - Needs analysis

### Notification Rules

Configure Slack/Email alerts:
1. Go to Project Settings → Notifications
2. Add rule: "Notify on launch finish if fail rate > 10%"
3. Select Slack channel or email recipients

---

## Troubleshooting

### Services Not Starting

```powershell
# Check Docker resources
docker system df

# Clean up if needed
docker system prune -a

# Restart ReportPortal
.\scripts\reportportal-start.ps1 -Clean
```

### UI Not Accessible

```powershell
# Check service health
docker compose -f docker-compose.reportportal.yml ps

# View API logs
docker logs reportportal-api

# View UI logs
docker logs reportportal-ui
```

### Upload Fails

1. **Check API token**: Ensure token in `reportportal.json` is valid
2. **Verify project exists**: Project name must match exactly
3. **Check endpoint**: Should be `http://localhost:8585` for local
4. **View upload logs**: Check test-evidence-framework logs

### Database Migration Issues

```powershell
# Reset database (WARNING: deletes all data)
.\scripts\reportportal-start.ps1 -Reset

# Restart fresh
.\scripts\reportportal-start.ps1
```

---

## Data Persistence

ReportPortal data is stored in Docker volumes:

- `reportportal-postgres-data` - Test launches, users, projects
- `reportportal-elasticsearch-data` - Log indices
- `reportportal-minio-data` - Screenshots, attachments
- `reportportal-rabbitmq-data` - Message queue state

**Backup volumes**:
```powershell
docker run --rm \
  -v reportportal-postgres-data:/data \
  -v ${PWD}/backup:/backup \
  alpine tar czf /backup/reportportal-backup.tar.gz /data
```

**Restore volumes**:
```powershell
docker run --rm \
  -v reportportal-postgres-data:/data \
  -v ${PWD}/backup:/backup \
  alpine tar xzf /backup/reportportal-backup.tar.gz -C /
```

---

## Performance Tuning

### For Large Projects (1000+ tests)

Edit `docker-compose.reportportal.yml`:

```yaml
api:
  environment:
    JAVA_OPTS: -Xmx2g  # Increase from 1g to 2g

elasticsearch:
  environment:
    ES_JAVA_OPTS: -Xms1g -Xmx1g  # Increase from 512m
```

Then restart:
```powershell
.\scripts\reportportal-start.ps1 -Clean
```

---

## Security Best Practices

1. **Change default password** immediately
2. **Rotate API tokens** every 90 days
3. **Use HTTPS** in production (requires reverse proxy)
4. **Limit project access** - assign users to specific projects
5. **Enable 2FA** in production environments

---

## Production Deployment

For production use:

1. Use external PostgreSQL (managed service)
2. Use external Elasticsearch (managed service)
3. Configure SSL/TLS termination (nginx/traefik)
4. Set up automated backups
5. Monitor with Prometheus/Grafana
6. Scale API/UI services horizontally

See: https://reportportal.io/docs/deploy

---

## Resources

- **Documentation**: https://reportportal.io/docs
- **GitHub**: https://github.com/reportportal
- **Slack Community**: https://reportportal.slack.com
- **Demo Instance**: https://demo.reportportal.io

---

## Next Steps

1. ✅ Start ReportPortal: `.\scripts\reportportal-start.ps1`
2. ✅ Complete initial setup (create project, get token)
3. ✅ Configure `reportportal.json`
4. ✅ Run tests: `.\scripts\test-unified-local.ps1`
5. ✅ Upload results: `npm run upload-results -- --all`
6. ✅ View dashboard: http://localhost:8080
7. ✅ Create custom filters and widgets
8. ✅ Enable ML auto-analysis
9. ✅ Set up notification rules

**Questions?** Check [INTEGRATION.md](../test-evidence-framework/docs/INTEGRATION.md) or ask in Slack: `#cds-testing`
