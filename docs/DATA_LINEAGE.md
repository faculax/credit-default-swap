# Data Lineage Implementation

## Overview

Data lineage tracking has been integrated into the credit default swap platform to provide full audit trails of data transformations, meet compliance requirements, and enable impact analysis.

## Implementation Status

✅ **Database Schema** - `lineage_events` table created via Flyway migration V55  
✅ **Backend Service** - `LineageService` with helper methods for common operations  
✅ **REST API** - `LineageController` with POST/GET endpoints  
✅ **OpenLineage Integration** - Full OpenLineage 1.0.5 spec compatibility  
✅ **Integration** - Trade capture and credit event processing instrumented  
✅ **Test Scripts** - PowerShell and Bash scripts for testing lineage API

## Architecture

### Database Layer

**Table:** `lineage_events`

```sql
CREATE TABLE lineage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset VARCHAR(255) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    inputs JSONB,
    outputs JSONB,
    user_name VARCHAR(100),
    run_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for query performance
CREATE INDEX idx_lineage_dataset ON lineage_events(dataset);
CREATE INDEX idx_lineage_created_at ON lineage_events(created_at);
CREATE INDEX idx_lineage_run_id ON lineage_events(run_id);
```

### Service Layer

**LineageService** (`backend/src/main/java/com/creditdefaultswap/platform/service/LineageService.java`)

Provides helper methods for tracking common operations:
- `trackTradeCapture(tradeId, tradeType, userName)` - CDS trade capture
- `trackCreditEvent(tradeId, eventType, eventId, userName)` - Credit event processing
- `trackLifecycleEvent(tradeId, eventType, userName)` - Lifecycle events (coupon, maturity)
- `trackPortfolioAggregation(portfolioId, tradeCount, userName)` - Portfolio creation
- `trackPricingCalculation(entityType, entityId, pricingMethod, userName)` - Pricing
- `trackMarginCalculation(marginType, accountId, marginAmount, userName)` - Margin
- `trackSchemaMigration(version, description, userName)` - Schema migrations
- `trackBatchProcess(batchType, recordsProcessed, inputSource, outputDestination, userName)` - Batch jobs

### REST API

**Endpoints:**

```
POST   /api/lineage                      - Ingest lineage event (internal format)
GET    /api/lineage?dataset=...          - Query by dataset name
GET    /api/lineage/run/{runId}          - Query by run ID
POST   /api/lineage/openlineage          - Ingest OpenLineage-formatted event
GET    /api/lineage/openlineage?dataset= - Query in OpenLineage format by dataset
GET    /api/lineage/openlineage/run/{id} - Query in OpenLineage format by run ID
```

**Example POST Request:**

```json
{
  "dataset": "cds_trades_cleaned",
  "operation": "ETL_TRANSFORM",
  "inputs": {
    "raw_trades": "cds_trades_raw",
    "reference_data": "issuer_master"
  },
  "outputs": {
    "cleaned_trades": "cds_trades_cleaned",
    "rejected_trades": "cds_trades_rejected"
  },
  "userName": "etl-pipeline",
  "runId": "run-2025-11-10-001"
}
```

**Example OpenLineage Request:**

```json
{
  "eventType": "COMPLETE",
  "eventTime": "2025-11-10T19:53:27.619Z",
  "run": {
    "runId": "ol-run-2025-11-10-001"
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
  ],
  "producer": "credit-default-swap-platform/1.0",
  "schemaURL": "https://openlineage.io/spec/1-0-5/OpenLineage.json"
}
```

## Current Integrations

### 1. Trade Capture
**Location:** `CDSTradeController.createTrade()`

Tracks when a new CDS trade is captured:
```java
lineageService.trackTradeCapture(savedTrade.getId(), "CDS", "system");
```

### 2. Credit Event Processing
**Location:** `CreditEventController.recordCreditEvent()`

Tracks credit events (default, bankruptcy, restructuring):
```java
lineageService.trackCreditEvent(tradeId, eventType, eventId, "system");
```

## Testing

### Local Testing

**PowerShell:**
```powershell
# Start backend service
docker-compose up -d backend

# Run test script
.\scripts\test-data-lineage.ps1
```

**Bash:**
```bash
# Start backend service
docker-compose up -d backend

# Run test script
./scripts/test-data-lineage.sh
```

### Manual Testing

```powershell
# Create a trade and check lineage
$trade = @{
    referenceEntity = "ACME Corp"
    notionalAmount = 1000000
    spread = 250
} | ConvertTo-Json

$result = Invoke-RestMethod -Uri "http://localhost:8080/api/cds-trades" -Method Post -Body $trade -ContentType "application/json"

# Query lineage for cds_trades
Invoke-RestMethod -Uri "http://localhost:8080/api/lineage?dataset=cds_trades"
```

## Next Steps

### Pending Instrumentation

1. **Lifecycle Events** - Coupon payments, maturity processing, notional adjustments
2. **Portfolio Operations** - Portfolio creation, constituent updates, aggregations
3. **Pricing Operations** - Price calculations, batch pricing
4. **Margin Calculations** - SIMM, SA-CCR, automated margin calls
5. **Batch Processing** - CSV imports, EOD processes, statement generation
6. **Schema Migrations** - Flyway callback to track schema changes

### Enhancement Opportunities

1. **Async Processing** - Use `@Async` to avoid blocking main operations
2. **Lineage Visualization** - Build UI to display lineage graphs
3. **Retention Policies** - Implement automated cleanup of old lineage records
4. **Alerting** - Monitor for unexpected data transformations
5. **Compliance Reports** - Generate audit reports from lineage data

## Acceptance Criteria

- [x] Database table created with indexes
- [x] Service layer with helper methods
- [x] REST API for ingestion and queries
- [x] OpenLineage 1.0.5 specification compliance
- [x] Trade capture instrumented
- [x] Credit event processing instrumented
- [x] Test scripts provided
- [ ] All lifecycle operations instrumented
- [ ] Batch processing instrumented
- [ ] Documentation complete

## Usage Examples

### Query Lineage for a Dataset

```bash
# All events for cds_trades
curl "http://localhost:8080/api/lineage?dataset=cds_trades"

# All events from a specific ETL run
curl "http://localhost:8080/api/lineage/run/run-2025-11-10-001"
```

### Ingest Custom Lineage Event

```bash
curl -X POST http://localhost:8080/api/lineage \
  -H "Content-Type: application/json" \
  -d '{
    "dataset": "custom_report",
    "operation": "REPORT_GENERATION",
    "inputs": {"source": "cds_trades", "filters": "maturity < 2026"},
    "outputs": {"report": "q4_2025_exposure_report.pdf"},
    "userName": "reporting-service",
    "runId": "report-q4-2025"
  }'
```

## Compliance & Audit

Data lineage supports:
- **SOX Compliance** - Audit trail of financial data transformations
- **MiFID II** - Trade lifecycle tracking
- **BCBS 239** - Risk data aggregation and reporting lineage
- **Internal Audit** - Impact analysis and root cause investigation
- **OpenLineage Standard** - Industry-standard lineage format for interoperability

## OpenLineage Integration

### Why OpenLineage?

OpenLineage is an open standard for data lineage collection and analysis. By supporting OpenLineage, the CDS platform gains:

1. **Interoperability** - Works with tools like Marquez, Amundsen, DataHub, Apache Atlas
2. **Standardization** - Industry-standard event format
3. **Ecosystem** - Access to OpenLineage tooling and integrations
4. **Future-proofing** - Standard evolves with community input

### Supported Features

- ✅ **Event Types**: START, COMPLETE, ABORT, FAIL
- ✅ **Run Tracking**: Unique run IDs with facets
- ✅ **Job Metadata**: Namespace, name, facets
- ✅ **Dataset Lineage**: Input/output datasets with namespaces
- ✅ **Bi-directional Conversion**: Internal ↔ OpenLineage format

### Usage Examples

**Ingest OpenLineage Event:**
```bash
curl -X POST http://localhost:8080/api/lineage/openlineage \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "COMPLETE",
    "eventTime": "2025-11-10T20:00:00Z",
    "run": {"runId": "etl-2025-11-10"},
    "job": {"namespace": "credit-default-swap", "name": "daily_etl"},
    "inputs": [{"namespace": "postgres://cds_platform", "name": "raw_trades"}],
    "outputs": [{"namespace": "postgres://cds_platform", "name": "cds_trades"}]
  }'
```

**Query in OpenLineage Format:**
```bash
curl "http://localhost:8080/api/lineage/openlineage?dataset=cds_trades"
curl "http://localhost:8080/api/lineage/openlineage/run/etl-2025-11-10"
```

### Integration with OpenLineage Tools

**Marquez (OpenLineage Reference Implementation):**
```bash
# Configure Marquez backend (optional)
# Point external tools to: http://localhost:8080/api/lineage/openlineage
```

**Apache Airflow OpenLineage Plugin:**
```python
# Airflow DAG automatically sends lineage to our endpoint
OPENLINEAGE_URL = "http://localhost:8080/api/lineage/openlineage"
```

**dbt OpenLineage Integration:**
```yaml
# dbt profile configuration
models:
  openlineage:
    url: http://localhost:8080/api/lineage/openlineage
```

---

**Last Updated:** 2025-11-10  
**Implemented By:** Release Chain Hardening Epic - Story 06
