# Docker OpenLineage (Marquez) Integration Guide

## Overview

The CDS platform now integrates with **Marquez**, the OpenLineage reference implementation, providing:
- **Visual Lineage UI** - Interactive graph of data flows
- **Centralized Management** - Single source of truth for lineage
- **Job Tracking** - Monitor ETL runs and data pipelines
- **API Access** - Query lineage via REST API

## Architecture

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  CDS Platform   │─────▶│  Marquez API     │◀────▶│  Marquez Web    │
│  (Backend)      │      │  (OpenLineage)   │      │  (Lineage UI)   │
└─────────────────┘      └──────────────────┘      └─────────────────┘
        │                          │                         │
        │                          ▼                         │
        │                  ┌──────────────┐                 │
        │                  │ Marquez DB   │                 │
        │                  │ (PostgreSQL) │                 │
        │                  └──────────────┘                 │
        │                                                    │
        └────────────────────────────────────────────────────┘
                    Lineage Events Flow
```

## Services

| Service | Port | Purpose | UI Access |
|---------|------|---------|-----------|
| **marquez** | 5000 | OpenLineage API | http://localhost:5000 |
| **marquez-web** | 3001 | Lineage Visualization | http://localhost:3001 |
| **marquez-db** | 5433 | PostgreSQL for Marquez | - |
| **backend** | 8080 | CDS Platform API | - |

## Quick Start

### 1. Start All Services

```powershell
# Start full stack including Marquez
docker-compose up -d

# Check status
docker-compose ps

# Wait for services to be healthy
docker-compose logs -f marquez
```

### 2. Verify Marquez is Running

```powershell
# Check Marquez health
Invoke-RestMethod http://localhost:5001/healthcheck

# Check CDS backend can reach Marquez
Invoke-RestMethod http://localhost:8080/api/lineage/marquez/health
```

### 3. Access Marquez UI

Open browser to: **http://localhost:3001**

You'll see the Marquez web interface showing:
- Namespaces
- Jobs
- Datasets
- Lineage graphs

## How It Works

### Automatic Forwarding

Every lineage event ingested by the CDS platform is **automatically forwarded to Marquez**:

1. **Trade Capture** → CDS Backend → Marquez
2. **Credit Event** → CDS Backend → Marquez
3. **Custom Event** → CDS Backend → Marquez

### Example Flow

```powershell
# 1. Create a trade (triggers lineage)
$trade = @{
    referenceEntity = "ACME Corp"
    notionalAmount = 1000000
    spread = 250
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/cds-trades" `
  -Method Post -Body $trade -ContentType "application/json"

# 2. Lineage is automatically:
#    - Stored in CDS backend database
#    - Forwarded to Marquez

# 3. View in Marquez UI
#    Open http://localhost:3001
#    Navigate to: credit-default-swap namespace
#    See lineage graph
```

## Configuration

### Enable/Disable Marquez

**Backend Configuration** (`application-marquez.properties`):

```properties
# Enable Marquez forwarding (default: false)
marquez.enabled=true

# Marquez URL (default: http://marquez:5000)
marquez.url=http://marquez:5000
```

**Activate Profile:**

```yaml
# docker-compose.yml
backend:
  environment:
    SPRING_PROFILES_ACTIVE: dev,marquez  # Add marquez profile
```

Or via command line:

```bash
java -jar backend.jar --spring.profiles.active=dev,marquez
```

### Local Development (Outside Docker)

If running backend locally but Marquez in Docker:

```properties
marquez.enabled=true
marquez.url=http://localhost:5000
```

## Using Marquez UI

### 1. Navigate to Namespaces

http://localhost:3001 → **credit-default-swap**

### 2. View Jobs

See all data jobs/pipelines:
- `cds_trades` - Trade capture
- `credit_events` - Credit event processing
- `cds_etl_pipeline` - Custom ETL jobs

### 3. Explore Lineage Graph

Click on any job to see:
- **Upstream dependencies** - Input datasets
- **Downstream consumers** - Output datasets
- **Run history** - Execution timeline
- **Dataset schema** - Column-level lineage

### 4. Search Datasets

Search for datasets:
- `cds_trades`
- `credit_events`
- `margin_accounts`

Click to see:
- Which jobs read this dataset
- Which jobs write to this dataset
- Data flow graph

## API Usage

### Query Marquez Directly

```powershell
# Get all namespaces
Invoke-RestMethod http://localhost:5000/api/v1/namespaces

# Get jobs in namespace
Invoke-RestMethod "http://localhost:5000/api/v1/namespaces/credit-default-swap/jobs"

# Get specific job
Invoke-RestMethod "http://localhost:5000/api/v1/namespaces/credit-default-swap/jobs/cds_etl_pipeline"

# Get lineage for dataset
Invoke-RestMethod "http://localhost:5000/api/v1/lineage?dataset=cds_trades"
```

### Send Events to Marquez via CDS Backend

```powershell
# Ingest lineage (automatically forwarded to Marquez)
$lineageEvent = @{
    eventType = "COMPLETE"
    eventTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    run = @{ runId = "etl-2025-11-10-001" }
    job = @{
        namespace = "credit-default-swap"
        name = "daily_cds_import"
    }
    inputs = @(
        @{ namespace = "postgres://cds_platform"; name = "raw_cds_trades" }
    )
    outputs = @(
        @{ namespace = "postgres://cds_platform"; name = "cds_trades" }
    )
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Uri "http://localhost:8080/api/lineage/openlineage" `
  -Method Post -Body $lineageEvent -ContentType "application/json"
```

## Advanced Features

### 1. Job Versioning

Marquez tracks job versions automatically:

```json
{
  "job": {
    "namespace": "credit-default-swap",
    "name": "cds_etl_pipeline",
    "facets": {
      "sourceCode": {
        "language": "java",
        "sourceCodeLocation": "https://github.com/..."
      }
    }
  }
}
```

### 2. Dataset Schema Tracking

Add schema facets to datasets:

```json
{
  "outputs": [{
    "namespace": "postgres://cds_platform",
    "name": "cds_trades",
    "facets": {
      "schema": {
        "fields": [
          {"name": "id", "type": "BIGINT"},
          {"name": "reference_entity", "type": "VARCHAR"},
          {"name": "notional_amount", "type": "DECIMAL"}
        ]
      }
    }
  }]
}
```

### 3. Column-Level Lineage

Track field transformations:

```json
{
  "job": {
    "facets": {
      "columnLineage": {
        "fields": {
          "output.total_exposure": {
            "inputFields": [
              "input.notional_amount",
              "input.recovery_rate"
            ]
          }
        }
      }
    }
  }
}
```

### 4. Data Quality Metrics

Add quality facets:

```json
{
  "outputs": [{
    "facets": {
      "dataQuality": {
        "rowCount": 1000,
        "bytes": 524288,
        "columnMetrics": {
          "spread": {
            "nullCount": 0,
            "distinctCount": 450,
            "min": 50,
            "max": 800
          }
        }
      }
    }
  }]
}
```

## Troubleshooting

### Marquez Not Starting

```powershell
# Check logs
docker-compose logs marquez

# Common issues:
# - Database not ready: Wait for marquez-db health check
# - Port conflict: Check if 5000/5001/3001 are available
```

### CDS Backend Can't Reach Marquez

```powershell
# Test connectivity from backend container
docker-compose exec backend curl http://marquez:5000/api/v1/namespaces

# If fails, check:
# - Marquez is running: docker-compose ps marquez
# - Network connectivity: docker network inspect credit-default-swap_default
```

### Events Not Appearing in Marquez

1. **Check Marquez is enabled:**
   ```properties
   marquez.enabled=true
   ```

2. **Check backend logs:**
   ```powershell
   docker-compose logs backend | Select-String "Marquez"
   ```

3. **Test Marquez health:**
   ```powershell
   Invoke-RestMethod http://localhost:8080/api/lineage/marquez/health
   ```

4. **Check Marquez logs:**
   ```powershell
   docker-compose logs marquez | Select-String "POST"
   ```

### UI Not Loading

```powershell
# Restart marquez-web
docker-compose restart marquez-web

# Check marquez-web logs
docker-compose logs marquez-web

# Access UI directly
Start-Process http://localhost:3001
```

## Performance Considerations

### 1. Async Forwarding

Marquez forwarding is **non-blocking**:
- Events are forwarded asynchronously
- Failures don't affect main operations
- Logged for debugging

### 2. Batching (Future Enhancement)

For high-volume scenarios, consider:
- Batch forwarding (collect and send in groups)
- Message queue (Kafka/RabbitMQ)
- Retry logic with exponential backoff

### 3. Resource Limits

Marquez containers use default resources. For production:

```yaml
marquez:
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 4G
      reservations:
        cpus: '1'
        memory: 2G
```

## Backup and Recovery

### Backup Marquez Data

```powershell
# Backup Marquez database
docker-compose exec marquez-db pg_dump -U marquez marquez > marquez-backup.sql

# Backup Docker volume
docker run --rm -v credit-default-swap_marquez_data:/data -v ${PWD}:/backup alpine tar czf /backup/marquez-data.tar.gz /data
```

### Restore Marquez Data

```powershell
# Restore from SQL dump
Get-Content marquez-backup.sql | docker-compose exec -T marquez-db psql -U marquez marquez

# Restore volume
docker run --rm -v credit-default-swap_marquez_data:/data -v ${PWD}:/backup alpine tar xzf /backup/marquez-data.tar.gz -C /
```

## Integration Examples

### Airflow DAG

```python
from airflow import DAG
from openlineage.airflow import OpenLineagePlugin

OPENLINEAGE_URL = "http://localhost:8080/api/lineage/openlineage"

with DAG('cds_daily_processing') as dag:
    # Airflow automatically sends lineage to our backend
    # which forwards to Marquez
    task = BashOperator(
        task_id='process_cds',
        bash_command='python process.py'
    )
```

### dbt Project

```yaml
# profiles.yml
credit_default_swap:
  outputs:
    dev:
      type: postgres
      openlineage:
        url: http://localhost:8080/api/lineage/openlineage
        namespace: credit-default-swap
```

## Next Steps

1. ✅ Marquez running in Docker
2. ✅ Automatic event forwarding
3. ✅ Web UI accessible
4. 🔜 Add column-level lineage
5. 🔜 Integrate with CI/CD pipelines
6. 🔜 Set up retention policies
7. 🔜 Configure alerting on lineage changes

---

**Services:**
- **Marquez UI:** http://localhost:3001
- **Marquez API:** http://localhost:5000
- **CDS Backend:** http://localhost:8080
- **Health Check:** http://localhost:8080/api/lineage/marquez/health

**Documentation:**
- `docs/DATA_LINEAGE.md`
- `docs/OPENLINEAGE_INTEGRATION.md`
- `docs/LINEAGE_INTEGRATION_SUMMARY.md`

**Last Updated:** 2025-11-10
