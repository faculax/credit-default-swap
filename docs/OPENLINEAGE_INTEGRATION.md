# OpenLineage Integration Guide

## Overview

The CDS platform is fully compatible with the OpenLineage 1.0.5 specification, enabling standardized data lineage collection and integration with the broader OpenLineage ecosystem.

## Quick Start

### 1. Ingest OpenLineage Events

**Endpoint:** `POST /api/lineage/openlineage`

**Example:**
```bash
curl -X POST http://localhost:8080/api/lineage/openlineage \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "COMPLETE",
    "eventTime": "2025-11-10T20:00:00Z",
    "run": {
      "runId": "daily-etl-2025-11-10"
    },
    "job": {
      "namespace": "credit-default-swap",
      "name": "cds_etl_pipeline"
    },
    "inputs": [
      {
        "namespace": "postgres://cds_platform",
        "name": "raw_cds_trades"
      }
    ],
    "outputs": [
      {
        "namespace": "postgres://cds_platform",
        "name": "cds_trades"
      }
    ]
  }'
```

### 2. Query Lineage in OpenLineage Format

**By Dataset:**
```bash
curl "http://localhost:8080/api/lineage/openlineage?dataset=cds_trades"
```

**By Run ID:**
```bash
curl "http://localhost:8080/api/lineage/openlineage/run/daily-etl-2025-11-10"
```

## OpenLineage Event Structure

### Event Types
- `START` - Job execution started
- `COMPLETE` - Job completed successfully
- `ABORT` - Job was aborted
- `FAIL` - Job failed with errors

### Core Components

```json
{
  "eventType": "COMPLETE",           // Required: Event lifecycle state
  "eventTime": "2025-11-10T20:00Z",  // Required: ISO 8601 timestamp
  "run": {                            // Required: Run information
    "runId": "unique-run-id"          // Required: Unique identifier for this run
  },
  "job": {                            // Required: Job information
    "namespace": "my-namespace",      // Required: Job namespace
    "name": "my-job-name"             // Required: Job name
  },
  "inputs": [],                       // Optional: Input datasets
  "outputs": [],                      // Optional: Output datasets
  "producer": "my-app/1.0",          // Optional: Application identifier
  "schemaURL": "https://..."         // Optional: Schema URL
}
```

## Integration Patterns

### Pattern 1: ETL Pipeline Tracking

```python
import requests
from datetime import datetime

def track_etl_run(run_id, job_name, inputs, outputs):
    event = {
        "eventType": "COMPLETE",
        "eventTime": datetime.utcnow().isoformat() + "Z",
        "run": {"runId": run_id},
        "job": {
            "namespace": "credit-default-swap",
            "name": job_name
        },
        "inputs": [{"namespace": "postgres://cds_platform", "name": ds} for ds in inputs],
        "outputs": [{"namespace": "postgres://cds_platform", "name": ds} for ds in outputs]
    }
    
    response = requests.post(
        "http://localhost:8080/api/lineage/openlineage",
        json=event
    )
    return response.json()

# Usage
track_etl_run(
    run_id="etl-2025-11-10-001",
    job_name="daily_cds_import",
    inputs=["raw_cds_trades", "issuer_master"],
    outputs=["cds_trades", "cds_trades_rejected"]
)
```

### Pattern 2: Apache Airflow Integration

```python
from airflow import DAG
from airflow.operators.python import PythonOperator
from openlineage.airflow import OpenLineagePlugin

# Configure OpenLineage to send to CDS platform
OPENLINEAGE_URL = "http://localhost:8080/api/lineage/openlineage"
OPENLINEAGE_NAMESPACE = "credit-default-swap"

dag = DAG(
    'cds_daily_processing',
    default_args={'openlineage_url': OPENLINEAGE_URL}
)

# Airflow automatically tracks lineage for each task
task = PythonOperator(
    task_id='process_cds_trades',
    python_callable=process_trades,
    dag=dag
)
```

### Pattern 3: dbt Integration

```yaml
# dbt_project.yml
models:
  credit_default_swap:
    +meta:
      openlineage:
        url: http://localhost:8080/api/lineage/openlineage
        namespace: credit-default-swap
```

### Pattern 4: Batch Job Tracking

```java
// Java batch job
OpenLineageClient client = new OpenLineageClient("http://localhost:8080/api/lineage/openlineage");

String runId = UUID.randomUUID().toString();

// Start event
client.emit(OpenLineageEvent.builder()
    .eventType("START")
    .eventTime(ZonedDateTime.now())
    .run(new Run(runId))
    .job(new Job("credit-default-swap", "margin_calculation_batch"))
    .build());

try {
    // Process batch
    processBatch();
    
    // Complete event
    client.emit(OpenLineageEvent.builder()
        .eventType("COMPLETE")
        .eventTime(ZonedDateTime.now())
        .run(new Run(runId))
        .job(new Job("credit-default-swap", "margin_calculation_batch"))
        .inputs(List.of(new Dataset("postgres://cds_platform", "cds_trades")))
        .outputs(List.of(new Dataset("postgres://cds_platform", "margin_accounts")))
        .build());
} catch (Exception e) {
    // Fail event
    client.emit(OpenLineageEvent.builder()
        .eventType("FAIL")
        .eventTime(ZonedDateTime.now())
        .run(new Run(runId))
        .job(new Job("credit-default-swap", "margin_calculation_batch"))
        .build());
}
```

## Integration with OpenLineage Ecosystem

### Marquez (OpenLineage Reference Implementation)

Marquez can consume lineage from the CDS platform:

```bash
# Configure Marquez to read from CDS platform
# CDS platform can forward events to Marquez
curl -X POST http://localhost:8080/api/lineage/openlineage \
  -H "Content-Type: application/json" \
  -d @event.json

# Events are stored locally and can be queried
curl "http://localhost:8080/api/lineage/openlineage?dataset=cds_trades"
```

### Apache Atlas

Forward lineage to Atlas via OpenLineage:

```python
# Bridge pattern: CDS → Atlas
def forward_to_atlas(cds_event):
    # Query CDS OpenLineage endpoint
    lineage = requests.get(
        "http://localhost:8080/api/lineage/openlineage?dataset=cds_trades"
    ).json()
    
    # Transform and send to Atlas
    for event in lineage:
        atlas_client.send_lineage(transform_to_atlas(event))
```

### DataHub

Integrate with DataHub's lineage system:

```yaml
# DataHub ingestion config
source:
  type: openlineage
  config:
    url: http://localhost:8080/api/lineage/openlineage
    namespace: credit-default-swap
```

## Testing OpenLineage Integration

```powershell
# Run comprehensive tests
.\scripts\test-data-lineage.ps1

# Tests include:
# - Internal format ingestion
# - OpenLineage format ingestion
# - Querying in both formats
# - Format conversion validation
```

## Best Practices

### 1. Use Consistent Namespaces
```json
{
  "job": {
    "namespace": "credit-default-swap",  // Use consistent namespace
    "name": "specific-job-name"
  }
}
```

### 2. Include Meaningful Run IDs
```json
{
  "run": {
    "runId": "etl-2025-11-10-001"  // Include date/sequence for traceability
  }
}
```

### 3. Specify Dataset Namespaces
```json
{
  "inputs": [
    {
      "namespace": "postgres://cds_platform",  // Explicit source
      "name": "cds_trades"
    }
  ]
}
```

### 4. Track All Event Lifecycle States
```
START → COMPLETE  (success)
START → FAIL      (error)
START → ABORT     (cancelled)
```

### 5. Add Facets for Rich Metadata
```json
{
  "run": {
    "runId": "etl-2025-11-10-001",
    "facets": {
      "processingTime": {"durationMs": 12345},
      "dataQuality": {"recordsProcessed": 1000}
    }
  }
}
```

## Troubleshooting

### Event Not Appearing

1. Check endpoint: `POST /api/lineage/openlineage`
2. Verify JSON structure matches OpenLineage spec
3. Check logs: `docker logs <backend-container>`

### 404 on Query

Ensure backend is running:
```powershell
docker-compose ps backend
docker-compose logs backend
```

### Format Conversion Issues

Use test script to validate:
```powershell
.\scripts\test-data-lineage.ps1
```

## Resources

- **OpenLineage Spec:** https://openlineage.io/spec/
- **OpenLineage GitHub:** https://github.com/OpenLineage/OpenLineage
- **CDS Lineage Docs:** `docs/DATA_LINEAGE.md`
- **Integration Summary:** `docs/LINEAGE_INTEGRATION_SUMMARY.md`

---

**Last Updated:** 2025-11-10  
**OpenLineage Version:** 1.0.5  
**CDS Platform:** Release Chain Hardening - Story 06
