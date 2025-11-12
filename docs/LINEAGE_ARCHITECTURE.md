# Data Lineage Architecture

## Overview

The CDS Platform implements a **two-layer data lineage tracking system** that combines aspect-oriented programming (AOP) with domain-specific lineage schemas to capture comprehensive audit trails.

---

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
│  @TrackLineage annotations (declarative, no code)           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                   AOP Aspect Layer                          │
│  LineageAspect - Automatic data extraction                  │
│  • Intercepts @TrackLineage methods                         │
│  • Extracts @RequestBody fields via reflection              │
│  • Extracts entity IDs from results/@PathVariable           │
│  • Routes to appropriate service method                     │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                  Service Layer (Enrichment)                 │
│  LineageService - Domain-specific lineage schemas           │
│  • Receives extracted data from AOP                         │
│  • Builds comprehensive input/output structures             │
│  • Defines lineage node relationships                       │
│  • Persists to lineage_events table                         │
└─────────────────────────────────────────────────────────────┘
```

---

## Layer Responsibilities

### 1. Controller Layer (Declarative)

**Purpose:** Define WHAT to track, not HOW.

**Example:**
```java
@PostMapping
@TrackLineage(
    operationType = LineageOperationType.TRADE,
    operation = "CREATE",
    entityIdFromResult = "id",
    autoExtractDetails = true
)
public ResponseEntity<CDSTrade> createTrade(@RequestBody CDSTrade trade) {
    return new ResponseEntity<>(cdsTradeService.saveTrade(trade), HttpStatus.CREATED);
}
```

**Key Points:**
- Controllers remain clean, focused on HTTP concerns
- No lineage-specific code in business logic
- Easy to add/remove lineage tracking
- Consistent annotation pattern across all endpoints

---

### 2. AOP Aspect Layer (Extraction)

**Class:** `LineageAspect.java`

**Purpose:** Automatically extract data from controller invocations.

**Responsibilities:**
1. **Intercept annotated methods** after successful execution
2. **Extract entity IDs** from result objects or path variables
3. **Extract field data** from `@RequestBody` parameters via reflection
4. **Route to service** based on `LineageOperationType`

**Key Methods:**
```java
@AfterReturning(
    pointcut = "@annotation(trackLineage)",
    returning = "result"
)
public void trackLineageAfterSuccess(JoinPoint joinPoint, TrackLineage trackLineage, Object result)
```

**Example Data Extraction:**
```java
// Input: CDSTrade object from @RequestBody
// Output: Map with extracted fields
{
  "notionalAmount": 10000000,
  "referenceEntity": "MSFT",
  "maturityDate": "2030-01-08",
  "spread": 377,
  "currency": "AUD",
  "counterparty": "WELLS_FARGO",
  "buySellProtection": "SELL",
  "recoveryRate": 40
}
```

**Why Not Just Store This?**
- Missing **context**: What data sources were consulted?
- Missing **downstream impacts**: What other systems/datasets are affected?
- Missing **semantic meaning**: What does this operation represent in the business domain?

---

### 3. Service Layer (Enrichment)

**Class:** `LineageService.java`

**Purpose:** Define domain-specific lineage schemas that combine extracted data with contextual structure.

**Example: Trade Capture (11-Node Comprehensive Lineage)**

```java
public void trackTradeCapture(Long tradeId, String tradeType, String userName, Map<String, Object> tradeDetails) {
    // INPUT NODES: What data sources were consulted?
    inputs = {
        "ui_trade_entry": {
            source: "user_interface",
            form: "new_single_name_cds",
            user: userName  // from AOP
        },
        "reference_data_lookup": {
            dataset: "issuer_master",
            entity: tradeDetails.get("referenceEntity"),  // from AOP
            purpose: "entity_validation_and_enrichment"
        },
        "market_data_lookup": {
            dataset: "market_quotes",
            spread_bps: tradeDetails.get("spread"),  // from AOP
            purpose: "pricing_reference"
        },
        "position_check": {
            dataset: "portfolio_positions",
            entity: tradeDetails.get("referenceEntity"),  // from AOP
            purpose: "concentration_risk_check"
        }
    }
    
    // OUTPUT NODES: What downstream impacts occurred?
    outputs = {
        "trade_record_created": {
            dataset: "cds_trades",
            trade_id: tradeId,
            notional: tradeDetails.get("notionalAmount"),  // from AOP
            entity: tradeDetails.get("referenceEntity"),    // from AOP
            // ... all trade details from AOP
        },
        "position_updated": {
            dataset: "portfolio_positions",
            action: "increment_notional",
            notional_delta: tradeDetails.get("notionalAmount")  // from AOP
        },
        "valuation_scheduled": {
            dataset: "cds_valuations",
            trade_id: tradeId,
            action: "mark_to_market_required"
        },
        "risk_metrics_triggered": {
            dataset: "risk_metrics",
            action: "recalculate_credit_exposure",
            metrics: ["credit_exposure", "counterparty_risk", "concentration_risk", "jump_to_default"]
        },
        "margin_calculation_triggered": {
            dataset: "margin_requirements",
            action: "recalculate_im_vm",
            methods: ["SIMM", "SA-CCR"]
        },
        "regulatory_reporting_flagged": {
            dataset: "regulatory_reports",
            reports: ["EMIR", "Dodd-Frank", "MiFID_II"],
            action: "include_in_next_submission"
        }
    }
}
```

**This is NOT Hardcoded Duplication - It's Domain Logic:**

| Aspect | Purpose |
|--------|---------|
| **Static Structure** | Defines what a "trade capture" means in terms of data lineage |
| **Dynamic Data** | Filled with actual values extracted by AOP |
| **Domain Semantics** | Captures business knowledge about data flows |
| **Audit Compliance** | Ensures regulatory requirements are met |

---

## Why This Architecture?

### ✅ Benefits

1. **Separation of Concerns**
   - Controllers: HTTP/REST logic
   - AOP: Cross-cutting extraction
   - Service: Domain-specific schemas

2. **Type Safety**
   - IDE autocomplete for lineage schemas
   - Compile-time validation
   - Refactoring support

3. **Testability**
   - Mock AOP aspect for unit tests
   - Test lineage schemas independently
   - Verify extraction logic separately

4. **Maintainability**
   - Lineage schemas in one place
   - Easy to update node structures
   - Clear domain semantics

5. **Flexibility**
   - Different schemas for different operations
   - Easy to add new operation types
   - Progressive enhancement (add fields as needed)

### ❌ Why Not Pure Configuration?

**Could do this:**
```yaml
# lineage-schemas.yml
TRADE_CAPTURE:
  inputs:
    - name: ui_trade_entry
      fields:
        source: user_interface
        form: new_single_name_cds
    - name: reference_data_lookup
      fields:
        dataset: issuer_master
        entity: ${referenceEntity}
  outputs:
    - name: trade_record_created
      fields:
        dataset: cds_trades
        notional: ${notionalAmount}
```

**Problems:**
- Lose type safety
- Complex templating engine needed
- Harder to debug
- No IDE support
- Over-engineering for current needs

---

## Data Flow Example

### Request
```json
POST /api/cds-trades
{
  "referenceEntity": "MSFT",
  "notionalAmount": 10000000,
  "maturityDate": "2030-01-08",
  "spread": 377,
  "currency": "AUD",
  "counterparty": "WELLS_FARGO",
  "buySellProtection": "SELL",
  "recoveryRate": 40
}
```

### Step 1: Controller Execution
```java
@PostMapping
@TrackLineage(operationType = TRADE, operation = "CREATE", ...)
public ResponseEntity<CDSTrade> createTrade(@RequestBody CDSTrade trade) {
    CDSTrade saved = service.saveTrade(trade);  // Returns trade with ID=9
    return ResponseEntity.ok(saved);
}
```

### Step 2: AOP Extraction
```java
// After successful return
details = {
  "referenceEntity": "MSFT",
  "notionalAmount": 10000000,
  "maturityDate": "2030-01-08",
  "spread": 377,
  "currency": "AUD",
  "counterparty": "WELLS_FARGO",
  "buySellProtection": "SELL",
  "recoveryRate": 40
}
entityId = "9"
```

### Step 3: Service Enrichment
```java
lineageService.trackTradeCapture(9L, "CREATE", "system", details)
// Builds 11-node structure with actual values
```

### Step 4: Database Persistence
```json
{
  "dataset": "cds_trades",
  "operation": "TRADE_CAPTURE_CREATE",
  "inputs": {
    "ui_trade_entry": {...},
    "reference_data_lookup": {"entity": "MSFT", ...},
    "market_data_lookup": {"spread_bps": 377, ...},
    "position_check": {...}
  },
  "outputs": {
    "trade_record_created": {
      "trade_id": 9,
      "notional": 10000000,
      "entity": "MSFT",
      "maturity": "2030-01-08",
      "spread_bps": 377,
      "currency": "AUD",
      "counterparty": "WELLS_FARGO",
      "buy_sell_protection": "SELL",
      "recovery_rate": 40
    },
    "position_updated": {...},
    "valuation_scheduled": {...},
    "risk_metrics_triggered": {...},
    "margin_calculation_triggered": {...},
    "regulatory_reporting_flagged": {...}
  },
  "user_name": "system",
  "timestamp": "2025-11-11T09:45:00Z"
}
```

---

## Operation Types & Schemas

| Operation Type | Service Method | Node Count | Purpose |
|---------------|---------------|------------|---------|
| `TRADE` | `trackTradeCapture()` | 11 nodes | Comprehensive trade lifecycle tracking |
| `BOND` | `trackBondOperation()` | 6 nodes | Bond creation/update with valuation triggers |
| `PORTFOLIO` | `trackPortfolioOperation()` | 5 nodes | Portfolio aggregation with risk metrics |
| `BASKET` | `trackBasketOperation()` | 4 nodes | Index/basket composition tracking |
| `MARGIN` | `trackMarginOperation()` | 6 nodes | SIMM/SA-CCR margin calculations |
| `LIFECYCLE` | `trackLifecycleOperation()` | 4 nodes | Coupon payments, maturities |
| `NOVATION` | `trackNovationOperation()` | 5 nodes | Trade novation with counterparty changes |
| `PRICING` | `trackPricingCalculation()` | 3 nodes | Mark-to-market valuation |
| `CREDIT_EVENT` | `trackCreditEvent()` | 3 nodes | Default, restructuring events |

---

## Field Name Mapping

**Critical:** AOP extracts fields using **exact field names** from DTOs/entities via reflection.

### Common Mappings

| DTO Field Name | Lineage Key | Service Method Lookup |
|----------------|-------------|----------------------|
| `notionalAmount` | `notional` | `details.getOrDefault("notionalAmount", ...)` |
| `referenceEntity` | `entity` | `details.getOrDefault("referenceEntity", ...)` |
| `entityName` | `entity` | `details.getOrDefault("entityName", ...)` |
| `maturityDate` | `maturity` | `details.getOrDefault("maturityDate", "")` |
| `tradeCount` | `trade_count` | `details.getOrDefault("tradeCount", 0)` |
| `portfolioId` | `portfolio_id` | `details.get("portfolioId")` |

**Important:** Always use fallback chains:
```java
// Handle different field names from different DTOs
details.getOrDefault("notionalAmount", details.getOrDefault("notional", 0))
details.getOrDefault("entityName", details.getOrDefault("referenceEntity", "UNKNOWN"))
```

---

## Adding New Operation Types

### 1. Define Annotation Enum
```java
// LineageOperationType.java
public enum LineageOperationType {
    TRADE, BOND, PORTFOLIO, BASKET, MARGIN,
    LIFECYCLE, NOVATION, PRICING, CREDIT_EVENT,
    YOUR_NEW_TYPE  // Add here
}
```

### 2. Create Service Method
```java
// LineageService.java
public void trackYourNewOperation(Long entityId, String operation, String userName, Map<String, Object> details) {
    Map<String, Object> inputs = new HashMap<>();
    // Define input nodes with actual data from details map
    
    Map<String, Object> outputs = new HashMap<>();
    // Define output nodes with actual data from details map
    
    trackTransformation("your_dataset", operation, inputs, outputs, userName, "run-id-" + entityId);
}
```

### 3. Add Routing in Aspect
```java
// LineageAspect.java, routeToTracker() method
case YOUR_NEW_TYPE:
    lineageService.trackYourNewOperation(Long.parseLong(entityId), operation, actor, details);
    break;
```

### 4. Annotate Controller
```java
@PostMapping
@TrackLineage(
    operationType = LineageOperationType.YOUR_NEW_TYPE,
    operation = "CREATE",
    entityIdFromResult = "id",
    autoExtractDetails = true
)
public ResponseEntity<YourEntity> createEntity(@RequestBody YourEntity entity) {
    return ResponseEntity.ok(service.save(entity));
}
```

---

## Testing Strategy

### Unit Tests
```java
@Test
void testTradeLineageExtraction() {
    // Test AOP extraction
    Map<String, Object> details = aspect.extractFields(tradeRequest);
    assertThat(details).containsEntry("notionalAmount", 10000000);
}

@Test
void testTradeLineageEnrichment() {
    // Test service enrichment
    Map<String, Object> details = Map.of("notionalAmount", 10000000, "referenceEntity", "MSFT");
    lineageService.trackTradeCapture(1L, "CREATE", "test", details);
    
    LineageEvent event = repository.findLatest();
    assertThat(event.getOutputs()).containsKey("trade_record_created");
    assertThat(event.getInputs()).containsKey("reference_data_lookup");
}
```

### Integration Tests
```java
@Test
void testEndToEndLineageCapture() {
    // POST trade
    ResponseEntity<CDSTrade> response = restTemplate.postForEntity("/api/cds-trades", tradeRequest, CDSTrade.class);
    
    // Verify lineage event created
    List<LineageEvent> events = lineageRepository.findByDataset("cds_trades");
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getOperation()).isEqualTo("TRADE_CAPTURE_CREATE");
}
```

---

## Performance Considerations

1. **Async Lineage Tracking** (Future Enhancement)
   - Currently synchronous in transaction
   - Could use `@Async` for non-blocking tracking
   - Trade-off: May lose lineage if async fails

2. **Batch Operations**
   - For bulk imports, use `trackBatchProcess()`
   - Avoid tracking individual items in loops

3. **Storage**
   - JSONB columns in PostgreSQL for efficient querying
   - Index on `dataset`, `operation`, `timestamp`
   - Archive old events periodically

---

## Compliance & Audit

### Regulatory Requirements Met

✅ **EMIR** - Trade lifecycle tracking  
✅ **Dodd-Frank** - Comprehensive audit trail  
✅ **MiFID II** - Transaction reporting  
✅ **BCBS 239** - Risk data aggregation  

### Audit Queries

```sql
-- Find all data sources used for a specific trade
SELECT inputs FROM lineage_events 
WHERE dataset = 'cds_trades' AND outputs->>'trade_id' = '9';

-- Trace downstream impacts of a trade
SELECT outputs FROM lineage_events 
WHERE dataset = 'cds_trades' AND outputs->>'trade_id' = '9';

-- Find all operations by a user
SELECT * FROM lineage_events 
WHERE user_name = 'trader@bank.com' 
ORDER BY timestamp DESC;
```

---

## Summary

The two-layer lineage architecture provides:

1. **Clean Controllers** - Declarative annotations only
2. **Automatic Extraction** - AOP handles cross-cutting concerns
3. **Domain Semantics** - Service layer defines business meaning
4. **Comprehensive Audit** - Full input/output tracking
5. **Type Safety** - Compile-time validation
6. **Maintainability** - Clear separation of concerns

This design balances **automation** (via AOP) with **domain knowledge** (via service schemas) to create a robust, compliant, and maintainable lineage system.

---

**Last Updated:** November 11, 2025  
**Related Docs:** 
- [AOP_LINEAGE_IMPLEMENTATION.md](./AOP_LINEAGE_IMPLEMENTATION.md)
- [DATA_LINEAGE.md](./DATA_LINEAGE.md)
- [OPENLINEAGE_INTEGRATION.md](./OPENLINEAGE_INTEGRATION.md)
