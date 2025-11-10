# Data Lineage Integration Summary

## ✅ What's Been Integrated

### 1. Database Layer
- **Migration:** `V55__create_lineage_events_table.sql`
- **Location:** `backend/src/main/resources/db/migration/`
- **Table:** `lineage_events` with UUID id, dataset, operation, JSONB inputs/outputs, user_name, run_id, created_at
- **Indexes:** dataset, created_at, run_id

### 2. Data Layer
- **Entity:** `LineageEvent.java` in `backend/.../model/`
- **Repository:** `LineageEventRepository.java` in `backend/.../repository/`
- **Methods:** `findByDatasetOrderByCreatedAtDesc()`, `findByRunIdOrderByCreatedAtDesc()`

### 3. Service Layer
- **Service:** `LineageService.java` in `backend/.../service/`
- **Helper Methods:**
  - `trackTradeCapture()` - CDS trade ingestion
  - `trackCreditEvent()` - Credit event processing  
  - `trackLifecycleEvent()` - Coupon/maturity processing
  - `trackPortfolioAggregation()` - Portfolio creation
  - `trackPricingCalculation()` - Price calculations
  - `trackMarginCalculation()` - Margin computations
  - `trackSchemaMigration()` - Schema changes
  - `trackBatchProcess()` - Batch/ETL jobs

### 4. REST API
- **Controller:** `LineageController.java` in `backend/.../controller/`
- **Endpoints:**
  - `POST /api/lineage` - Ingest lineage event
  - `GET /api/lineage?dataset={name}` - Query by dataset
  - `GET /api/lineage/run/{runId}` - Query by run ID
  - `POST /api/lineage/openlineage` - Ingest OpenLineage-formatted event
  - `GET /api/lineage/openlineage?dataset={name}` - Query in OpenLineage format
  - `GET /api/lineage/openlineage/run/{runId}` - Query by run in OpenLineage format

### 5. Instrumented Controllers
- **CDSTradeController**
  - `createTrade()` → calls `lineageService.trackTradeCapture()`
  - Tracks: trade ID, trade type, user
  
- **CreditEventController**
  - `recordCreditEvent()` → calls `lineageService.trackCreditEvent()`
  - Tracks: trade ID, event type, event UUID, user

### 6. Test Scripts
- **PowerShell:** `scripts/test-data-lineage.ps1`
- **Bash:** `scripts/test-data-lineage.sh`
- **Features:** Creates sample events, queries by dataset/run, validates API, tests OpenLineage integration

### 7. OpenLineage Integration
- **DTO:** `OpenLineageEvent.java` - OpenLineage-compatible event structure
- **Adapter:** `OpenLineageAdapter.java` - Converts between OpenLineage and internal format
- **Spec:** Compliant with OpenLineage 1.0.5 specification
- **Features:**
  - Ingest events in OpenLineage format
  - Query lineage in OpenLineage format
  - Automatic conversion between formats
  - Compatible with OpenLineage ecosystem tools

## 🧪 How to Test

### Quick Test
```powershell
# 1. Start backend
docker-compose up -d backend

# 2. Wait for startup
Start-Sleep -Seconds 10

# 3. Run tests
.\scripts\test-data-lineage.ps1
```

### Manual Test
```powershell
# Create a trade (triggers lineage)
$trade = @{
    referenceEntity = "Test Corp"
    notionalAmount = 1000000
    spread = 200
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/cds-trades" `
  -Method Post -Body $trade -ContentType "application/json"

# Query lineage
Invoke-RestMethod -Uri "http://localhost:8080/api/lineage?dataset=cds_trades"

# Query in OpenLineage format
Invoke-RestMethod -Uri "http://localhost:8080/api/lineage/openlineage?dataset=cds_trades"
```

### Database Verification
```bash
docker exec -it <backend-db-container> psql -U postgres -d cds_platform

SELECT dataset, operation, user_name, run_id, created_at 
FROM lineage_events 
ORDER BY created_at DESC 
LIMIT 10;
```

## 📊 What Gets Tracked

### Currently Instrumented
✅ CDS trade creation  
✅ Credit event processing (default, bankruptcy, restructuring)

### Ready for Instrumentation (helpers exist)
- Lifecycle events (coupon payments, maturity)
- Portfolio operations (creation, updates, aggregations)
- Pricing calculations
- Margin calculations (SIMM, SA-CCR)
- Batch processing (CSV imports, EOD runs)
- Schema migrations

## 🔧 How to Add Lineage to New Operations

### Step 1: Inject LineageService
```java
private final LineageService lineageService;

public MyController(MyService myService, LineageService lineageService) {
    this.myService = myService;
    this.lineageService = lineageService;
}
```

### Step 2: Call Helper Method
```java
@PostMapping
public ResponseEntity<Trade> createTrade(@RequestBody Trade trade) {
    Trade saved = myService.save(trade);
    
    // Track lineage
    lineageService.trackTradeCapture(saved.getId(), "CDS", "system");
    
    return ResponseEntity.ok(saved);
}
```

### Step 3: Or Use Generic Method
```java
Map<String, Object> inputs = Map.of("source", "csv_import", "file", "trades.csv");
Map<String, Object> outputs = Map.of("records_created", 150);

lineageService.trackTransformation("cds_trades", "CSV_IMPORT", 
    inputs, outputs, "batch-service", "import-" + UUID.randomUUID());
```

## 📖 Documentation

- **Full Docs:** `docs/DATA_LINEAGE.md`
- **Epic:** `epics/release-chain-hardening/story-06-data-lineage.md`
- **Migration:** `backend/src/main/resources/db/migration/V55__create_lineage_events_table.sql`
- **OpenLineage Spec:** https://openlineage.io/spec/

## 🌐 OpenLineage Compatibility

### Why OpenLineage?
OpenLineage is an open standard for data lineage, enabling interoperability across different tools and platforms. By supporting OpenLineage format, the CDS platform can:
- Integrate with existing OpenLineage-compatible tools (Marquez, Amundsen, DataHub)
- Export lineage data to centralized lineage platforms
- Import lineage from external ETL tools
- Follow industry-standard lineage practices

### OpenLineage Event Structure
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

### Usage
```bash
# Ingest OpenLineage event
curl -X POST http://localhost:8080/api/lineage/openlineage \
  -H "Content-Type: application/json" \
  -d @openlineage-event.json

# Query in OpenLineage format
curl "http://localhost:8080/api/lineage/openlineage?dataset=cds_etl_pipeline"
```

## 🎯 Next Steps

1. **Instrument more operations** - Lifecycle, portfolio, pricing, margin
2. **Add async processing** - Use `@Async` to avoid blocking
3. **Build visualization** - UI to display lineage graphs
4. **Compliance reports** - Generate audit trails
5. **Retention policies** - Archive/delete old lineage records

---

**Status:** ✅ Core infrastructure complete, ready for broader instrumentation  
**Epic:** Release Chain Hardening - Story 06  
**Date:** 2025-11-10
