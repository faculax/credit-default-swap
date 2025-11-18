# ReportPortal Setup Guide

## 🎯 Quick Start

### Step 1: Get Your API Token

1. **Login to ReportPortal**: http://localhost:8080/ui
   - Username: `superadmin`
   - Password: `erebus`

2. **Get API Token** (Project will be created automatically):
   - Click your username (superadmin) in top-right corner
   - Select "Profile" → "API Keys" tab
   - Click "Generate API Key"
   - Copy the generated token

> **Note:** The uploader automatically creates projects if they don't exist, so you can skip manual project creation in the UI.

### Step 2: Configure ReportPortal

Edit `reportportal.json` and replace `PASTE_YOUR_API_TOKEN_HERE` with your actual token:

```json
{
  "endpoint": "http://localhost:8080",
  "token": "YOUR_ACTUAL_TOKEN_HERE",
  "project": "cds-platform",
  ...
}
```

### Step 3: Upload Test Results

#### Upload Single Test Report

If you have a validation report (e.g., `test-validation-report.json`):

```powershell
npm run upload-reportportal -- --config reportportal.json --report test-validation-report.json
```

#### Upload All Test Results (Batch Mode)

To upload all test results from a directory:

```powershell
npm run upload-reportportal -- --batch --dir ./generated-tests --pattern "**/*.json" --config reportportal.json
```

#### Upload with Command-Line Config (No Config File)

```powershell
npm run upload-reportportal -- `
  --report test-validation-report.json `
  --endpoint http://localhost:8080 `
  --token YOUR_TOKEN `
  --project cds-platform `
  --launch-name "CDS Platform Tests"
```

### Step 4: View Results in ReportPortal

After upload completes:

1. Navigate to http://localhost:8080/ui
2. Select "cds-platform" project
3. Click "Launches" to see your test runs
4. Click on a launch to view detailed test results

## 📊 Understanding the Dashboard

- **Launches**: Each test run appears as a launch
- **Statistics**: Pass/Fail/Skip counts with historical trends
- **Filters**: Filter by status, tags, time range
- **Trends**: Track test stability over time
- **Defects**: Link test failures to defect tracking

## 🔄 Continuous Integration

Add to your CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Upload to ReportPortal
  run: |
    npm run upload-reportportal -- \
      --batch \
      --dir ./test-results \
      --config reportportal.json
  env:
    RP_TOKEN: ${{ secrets.RP_TOKEN }}
```

## 🛠️ Advanced Configuration

### Launch Attributes (Tags)

Add tags to organize launches:

```json
"launchAttributes": [
  { "key": "environment", "value": "staging" },
  { "key": "browser", "value": "chrome" },
  { "key": "version", "value": "1.2.3" }
]
```

### Debug Mode

Enable debug logging:

```json
"mode": "DEBUG",
"debug": true
```

### Attachments

Configure attachment upload:

```json
"uploadAttachments": true,
"maxAttachmentSize": 10485760  // 10MB in bytes
```

## 📝 Troubleshooting

### Connection Issues

If upload fails with connection error:

```powershell
# Check if ReportPortal is running
docker ps | Select-String "reportportal"

# Check specific service health
docker ps --filter "name=uat|api|gateway" --format "table {{.Names}}\t{{.Status}}"
```

### Authentication Issues

- Verify token is correct (copy-paste from ReportPortal UI)
- Ensure project name matches exactly (case-sensitive)
- Check token hasn't expired

### Project Not Found

- Create project in ReportPortal UI first
- Ensure project name in config matches exactly

## 🚀 Managing ReportPortal

### Start ReportPortal

```powershell
docker compose -f docker-compose.reportportal.yml up -d
```

### Stop ReportPortal

```powershell
docker compose -f docker-compose.reportportal.yml down
```

### Check Status

```powershell
.\scripts\reportportal-start.ps1 -Status
```

### View Logs

```powershell
docker logs credit-default-swap-api-1 --tail 50
docker logs credit-default-swap-ui-1 --tail 50
```

## 📚 Additional Resources

- **ReportPortal Documentation**: Full integration guide in `docs/INTEGRATION.md`
- **User Guide**: Daily workflow in `docs/USER_GUIDE.md`
- **Getting Started**: Quick start in `docs/GETTING_STARTED.md`
