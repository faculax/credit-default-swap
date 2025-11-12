# 🐛 Lineage System Debug Status

## Current Situation

The lineage graph visualization is working correctly but showing minimal data because **the backend lineage tracking aspects are not capturing data**.

### What's Working ✅

1. **Graph API**: All 4 endpoints functioning correctly
   - GET `/api/lineage/graph/dataset/{name}`
   - GET `/api/lineage/graph/event/{id}`
   - GET `/api/lineage/graph/correlation/{id}`
   - GET `/api/lineage/graph/recent`

2. **Frontend Visualization**: React Flow graph rendering correctly
   - 4 node types supported (dataset, endpoint, service, repository)
   - Interactive controls working
   - Conditional rendering fixed

3. **Database Schema**: lineage_events table exists and storing basic data
   ```sql
   id              | 37cac9f3-6bed-4385-9cd2-842fbdbc2e42
   dataset         | cds_portfolios  
   operation       | CREATE_PORTFOLIO
   inputs          | {"ui_portfolio_entry": {...}, "portfolio_name_check": {...}}
   outputs         | {"portfolio_created": {...}}  -- MISSING CORRELATION METADATA
   created_at      | 2025-11-11 11:26:02.031275+00
   ```

4. **Controller Tracing**: `ControllerTracingAspect` is working
   ```
   2025-11-11T11:26:02.035Z INFO Request completed: Request[POST /api/cds-portfolios] 
   -> 2 services -> 3 repositories -> 4 entities (0 reads, 2 writes) in 17ms
   ```

### What's NOT Working ❌

1. **LineageAspect**: Not being triggered at all
   - No logging from `LineageAspect`
   - No `_tracked_tables_read` or `_tracked_tables_written` in outputs
   - No `_operation_count` in outputs

2. **EnhancedLineageAspect**: Not being triggered at all
   - No logging from `EnhancedLineageAspect`
   - No correlation metadata fields (_correlation_id, _http_method, _endpoint, etc.)
   - No "=== Correlation Context Summary ===" logs

3. **Correlation Metadata Capture**: 0% functional
   - Expected 15+ fields in outputs: `_correlation_id`, `_http_method`, `_endpoint`, `_user_name`, `_duration_ms`, `_controller_class`, `_controller_method`, `_request_dto_type`, `_response_dto_type`, `_path_variables`, `_service_call_chain`, `_service_call_count`, `_repository_call_chain`, `_repository_call_count`, `_entity_operations`
   - Actual: None present

4. **Database Operations Tracking**: Not capturing table operations
   - Expected: `_tracked_tables_read: ["cds_portfolios"]`
   - Expected: `_tracked_tables_written: ["cds_portfolios"]`
   - Actual: Both missing

## Impact on Graph Visualization

The graph shows only 1 node and 0 edges because:

```typescript
Graph Nodes (1): [{"id": "cds_portfolios", "label": "Cds Portfolios", "type": "dataset"}]
Graph Edges (0): []
```

**Why minimal?**
- No correlation metadata → No endpoint/service/repository nodes created
- No `_tracked_tables_read`/`_tracked_tables_written` → No READ/WRITE edges created
- Only the main dataset node is created by default from the `dataset` field

**Expected rich graph:**
```
[Endpoint: POST /api/cds-portfolios] 
    ↓ (calls)
[Service: CdsPortfolioService.createPortfolio]
    ↓ (calls)  
[Repository: CdsPortfolioRepository.findByNameIgnoreCase]
    ↓ (WRITE)
[Dataset: cds_portfolios]
```

## Root Cause Analysis

### Hypothesis 1: @TrackLineage Not Present
**Status**: ❌ DISPROVEN  
Evidence: `grep_search` found 10 matches of `@TrackLineage` in `CdsPortfolioController.java`

### Hypothesis 2: Aspects Not Registered as Spring Beans
**Status**: ✅ LIKELY  
Evidence:
- Both aspects have `@Component` annotation
- Both aspects have `@Aspect` annotation
- But NO aspect logging appears in logs
- ControllerTracingAspect DOES log (proves AOP is working)

### Hypothesis 3: Aspect Order Issue
**Status**: ⏳ POSSIBLE  
Evidence:
- EnhancedLineageAspect: `@Order(0)`
- LineageAspect: `@Order(5)`
- Should execute in order, but neither is executing

### Hypothesis 4: Pointcut Expression Mismatch
**Status**: ⏳ POSSIBLE  
Code:
```java
@Around("@annotation(trackLineage)")
public Object enrichWithCorrelationData(ProceedingJoinPoint joinPoint, TrackLineage trackLineage) throws Throwable
```
This should match any method annotated with `@TrackLineage`

### Hypothesis 5: AspectJ Weaving Not Enabled
**Status**: ⏳ MOST LIKELY  
Evidence:
- Spring Boot may need explicit AspectJ configuration
- `@EnableAspectJAutoProxy` may be missing
- Aspects may need different pointcut syntax

## Next Steps to Fix

### Step 1: Verify AspectJ Configuration ⏳
Check `Application.java` for `@EnableAspectJAutoProxy`:
```java
@SpringBootApplication
@EnableAspectJAutoProxy  // ← May be missing
public class Application { ... }
```

### Step 2: Add Debug Logging to Aspects ⏳
Temporarily change all aspect loggers to `ERROR` level to force visibility:
```java
logger.error("LINEAGE ASPECT TRIGGERED!"); // Instead of logger.debug/info
```

### Step 3: Verify Aspect Scanning ⏳
Check if aspects are in correct package for component scanning:
```
com.creditdefaultswap.platform.aspect.EnhancedLineageAspect
com.creditdefaultswap.platform.aspect.LineageAspect
```
Should be scanned if base package is `com.creditdefaultswap.platform`

### Step 4: Check Maven Dependencies ⏳
Verify AspectJ dependencies in `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### Step 5: Alternative Pointcut Syntax ⏳
Try execution-based pointcut instead of annotation-based:
```java
@Around("execution(* com.creditdefaultswap.platform.service..*(..)) && @annotation(trackLineage)")
```

## Code Status

### Implementation: 100% Complete ✅
- EnhancedLineageAspect: Full correlation metadata injection logic
- LineageAspect: Database operations merging logic
- DatabaseOperationTracker: ThreadLocal storage for correlation data
- Graph API: 4 endpoints with full graph building
- Frontend: 4 view modes with interactive visualization

### Runtime Behavior: 0% Functional ❌
- Aspects not executing
- No data being captured
- Empty correlation metadata
- Minimal graph visualization

## Workaround: Manual Lineage Entry (Temporary)

Until aspects are fixed, you can manually verify the graph visualization works by inserting test data:

```sql
INSERT INTO lineage_events (id, dataset, operation, outputs, created_at)
VALUES (
    gen_random_uuid(),
    'cds_portfolios',
    'CREATE_PORTFOLIO',
    '{
        "portfolio_created": {"name": "Test Portfolio", "portfolio_id": 999},
        "_correlation_id": "test-123",
        "_http_method": "POST",
        "_endpoint": "/api/cds-portfolios",
        "_service_call_chain": ["CdsPortfolioService.createPortfolio"],
        "_repository_call_chain": ["CdsPortfolioRepository.save"],
        "_tracked_tables_read": [],
        "_tracked_tables_written": ["cds_portfolios"]
    }'::jsonb,
    NOW()
);
```

This would produce a graph with:
- 1 endpoint node (POST /api/cds-portfolios)
- 1 service node (CdsPortfolioService.createPortfolio)
- 1 repository node (CdsPortfolioRepository.save)
- 1 dataset node (cds_portfolios)
- 3 edges connecting them

## Summary

**Graph Visualization**: ✅ Working perfectly  
**Lineage Data Capture**: ❌ Not working at all  
**Root Cause**: Aspects not being triggered (configuration issue)  
**Priority**: HIGH - System incomplete without data capture  
**Complexity**: Medium - AspectJ configuration troubleshooting required

---

*Last Updated: 2025-11-11 11:38 UTC*  
*Status: Investigation Phase - Awaiting aspect configuration fix*
