# 📊 Multi-Table Lineage Tracking

## Overview

Enhanced the data lineage tracking system to capture **ALL tables touched** during business operations, not just the primary table. This ensures complete audit trails for complex operations like credit events that cascade across multiple database tables.

---

## 🎯 Problem Solved

### Before
When recording a credit event, the lineage only showed:
- ✅ `credit_events` table (primary)
- ❌ Missing: `cds_trades` (status updated)
- ❌ Missing: `cash_settlements` (settlement calculated)
- ❌ Missing: `physical_settlement_instructions` (instructions created)
- ❌ Missing: `audit_logs` (compliance trail)

### After
The lineage now captures **every table** modified during the operation with:
- ✅ Table name
- ✅ Operation type (READ/WRITE)
- ✅ Business context (why was it touched?)
- ✅ Purpose description (what was done?)

---

## 🏗️ Architecture

### 1. **Automatic Database Operation Tracking**

`DatabaseOperationTracker.java` (Thread-local tracking):
```java
// Automatically tracks EVERY database operation
DatabaseOperationTracker.recordRead("cds_trades", tradeId);
DatabaseOperationTracker.recordWrite("credit_events", eventId, INSERT);
DatabaseOperationTracker.recordWrite("cds_trades", tradeId, UPDATE);
DatabaseOperationTracker.recordWrite("cash_settlements", settlementId, INSERT);
```

**Key Features:**
- Thread-safe with `ThreadLocal` storage
- Enabled automatically by `LineageAspect` (Order=5)
- Captures table names, entity IDs, timestamps
- Groups operations by table for analysis

### 2. **Enhanced Lineage Service**

`LineageService.trackCreditEventWithDetails()`:
```java
// Extracts tracked read operations → inputs
if (details.containsKey("_tracked_tables_read")) {
    List<String> trackedReads = details.get("_tracked_tables_read");
    for (String tableName : trackedReads) {
        inputs.put(tableName + "_read", tableName);
    }
}

// Extracts tracked write operations → transformations
if (details.containsKey("_tracked_tables_written")) {
    List<String> trackedWrites = details.get("_tracked_tables_written");
    
    // Create structured transformation records
    for (String tableName : trackedWrites) {
        Map<String, Object> tableTransform = new HashMap<>();
        tableTransform.put("table", tableName);
        tableTransform.put("operation", "WRITE");
        tableTransform.put("event_context", "credit_event_" + eventType);
        
        // Add context for known tables
        switch (tableName) {
            case "credit_events":
                tableTransform.put("description", "Primary credit event record");
                tableTransform.put("purpose", "Event recorded for trade " + tradeId);
                break;
            case "cds_trades":
                tableTransform.put("description", "Trade status updated");
                tableTransform.put("purpose", "Status changed to CREDIT_EVENT_RECORDED or SETTLED");
                break;
            // ... more cases
        }
    }
}
```

### 3. **Frontend Intelligence Display**

`LineagePage.tsx` - **Transformations Tab Enhancement**:
```tsx
{/* Shows ALL tables touched with rich context */}
{transform.table && (
  <code className="text-fd-green font-semibold bg-fd-dark/50 px-2 py-1 rounded">
    {transform.table}
  </code>
)}
{transform.description && (
  <p className="text-fd-text-muted text-sm mb-2">{transform.description}</p>
)}
{transform.purpose && (
  <p className="text-fd-text text-sm">
    <span className="text-fd-text-muted">Purpose:</span>
    <span>{transform.purpose}</span>
  </p>
)}
{transform.event_context && (
  <code className="text-fd-cyan text-xs">{transform.event_context}</code>
)}
```

---

## 🔄 Credit Event Flow Example

### User Action
```
POST /api/cds-trades/1001/credit-events
{
  "eventType": "BANKRUPTCY",
  "eventDate": "2024-03-15",
  "settlementMethod": "CASH"
}
```

### Backend Processing (CreditEventService)
```java
1. creditEventRepository.save(creditEvent);           // ✅ credit_events
2. trade.setTradeStatus(CREDIT_EVENT_RECORDED);       
   tradeRepository.save(trade);                       // ✅ cds_trades
3. cashSettlementService.calculateCashSettlement();   // ✅ cash_settlements
4. auditService.logCreditEventCreation();             // ✅ audit_logs
5. createPayoutEvent() → payoutEvent.save();          // ✅ credit_events (again)
   trade.setTradeStatus(SETTLED_CASH);
   tradeRepository.save(trade);                       // ✅ cds_trades (again)
```

### Tracked Operations
```
DatabaseOperationTracker captures:
- READ: cds_trades (id=1001)
- WRITE: credit_events (id=uuid-123, INSERT)
- WRITE: cds_trades (id=1001, UPDATE)
- WRITE: cash_settlements (id=uuid-456, INSERT)
- WRITE: audit_logs (id=uuid-789, INSERT)
- WRITE: credit_events (id=uuid-abc, INSERT)
- WRITE: cds_trades (id=1001, UPDATE)
```

### Lineage Event Created
```json
{
  "dataset": "credit_events",
  "operation": "CREDIT_EVENT_BANKRUPTCY",
  "inputs": {
    "trade_id": 1001,
    "event_type": "BANKRUPTCY",
    "cds_trades_read": "cds_trades"
  },
  "outputs": {
    "credit_event_id": "uuid-123",
    "affected_tables": [
      "credit_events",
      "cds_trades", 
      "cash_settlements",
      "audit_logs"
    ],
    "table_transformations": [
      {
        "table": "credit_events",
        "operation": "WRITE",
        "description": "Primary credit event record",
        "purpose": "Event recorded for trade 1001",
        "event_context": "credit_event_bankruptcy"
      },
      {
        "table": "cds_trades",
        "operation": "WRITE",
        "description": "Trade status updated",
        "purpose": "Status changed to CREDIT_EVENT_RECORDED or SETTLED",
        "event_context": "credit_event_bankruptcy"
      },
      {
        "table": "cash_settlements",
        "operation": "WRITE",
        "description": "Cash settlement calculated",
        "purpose": "Recovery rate applied for cash settlement",
        "event_context": "credit_event_bankruptcy"
      },
      {
        "table": "audit_logs",
        "operation": "WRITE",
        "description": "Audit trail recorded",
        "purpose": "Compliance and regulatory tracking",
        "event_context": "credit_event_bankruptcy"
      }
    ],
    "_tracked_tables_read": ["cds_trades"],
    "_tracked_tables_written": [
      "credit_events", 
      "cds_trades", 
      "cash_settlements", 
      "audit_logs"
    ]
  }
}
```

---

## 🎨 Frontend Display

### Intelligence Panel → Transformations Tab
```
┌─────────────────────────────────────────────────────┐
│ 🔄 Transformations                                  │
├─────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────┐ │
│ │ [WRITE] credit_events                           │ │
│ │ Primary credit event record                     │ │
│ │ Purpose: Event recorded for trade 1001          │ │
│ │ Context: credit_event_bankruptcy                │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ ┌─────────────────────────────────────────────────┐ │
│ │ [WRITE] cds_trades                              │ │
│ │ Trade status updated                            │ │
│ │ Purpose: Status changed to SETTLED_CASH         │ │
│ │ Context: credit_event_bankruptcy                │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ ┌─────────────────────────────────────────────────┐ │
│ │ [WRITE] cash_settlements                        │ │
│ │ Cash settlement calculated                      │ │
│ │ Purpose: Recovery rate applied                  │ │
│ │ Context: credit_event_bankruptcy                │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ ┌─────────────────────────────────────────────────┐ │
│ │ [WRITE] audit_logs                              │ │
│ │ Audit trail recorded                            │ │
│ │ Purpose: Compliance tracking                    │ │
│ │ Context: credit_event_bankruptcy                │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## 🔧 Applying to Other Operations

This pattern is now available for **ALL business operations**:

### 1. Trade Lifecycle Events
```java
@TrackLineage(operationType = LIFECYCLE, operation = "COUPON_PAYMENT")
public void processCouponPayment(Long tradeId) {
    // Tables touched: cds_trades, cash_flows, margin_accounts
    // All automatically tracked!
}
```

### 2. Margin Calculations
```java
@TrackLineage(operationType = MARGIN, operation = "SIMM_CALCULATION")
public SimmResult calculateSimm(String portfolioId) {
    // Tables touched: simm_calculations, risk_sensitivities, 
    //                 netting_sets, margin_requirements
    // All automatically tracked!
}
```

### 3. Portfolio Operations
```java
@TrackLineage(operationType = PORTFOLIO, operation = "ADD_TRADES")
public void addTradesToPortfolio(Long portfolioId, List<Long> tradeIds) {
    // Tables touched: cds_portfolios, portfolio_trades, cds_trades
    // All automatically tracked!
}
```

---

## 🎯 Key Benefits

### 1. **Complete Audit Trail**
- Regulatory compliance: prove which tables were modified
- Forensic analysis: trace every data change
- Impact analysis: understand cascading effects

### 2. **Automatic Tracking**
- No manual instrumentation needed
- Works across all `@TrackLineage` endpoints
- Thread-safe and performant

### 3. **Rich Context**
- Not just table names, but WHY they were touched
- Business purpose for each modification
- Event context linking operations together

### 4. **Frontend Visibility**
- Users can see complete data flow
- Transformations tab shows all affected tables
- Filterable, searchable event history

---

## 🧪 Testing

### Test Credit Event Lineage
```bash
# 1. Clear existing lineage
docker exec -it credit-default-swap-db-1 psql -U cdsuser -d cdsplatform -c "DELETE FROM lineage_events;"

# 2. Create a trade
curl -X POST http://localhost:8080/api/cds-trades \
  -H "Content-Type: application/json" \
  -d '{
    "referenceEntity": "ACME Corp",
    "notionalAmount": 10000000,
    "spread": 250,
    "maturityDate": "2025-12-31"
  }'

# 3. Record credit event
curl -X POST http://localhost:8080/api/cds-trades/1/credit-events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "BANKRUPTCY",
    "eventDate": "2024-03-15",
    "noticeDate": "2024-03-15",
    "settlementMethod": "CASH"
  }'

# 4. Check lineage
curl http://localhost:8080/api/lineage/events | jq '.[-1].outputs.table_transformations'
```

Expected Output:
```json
[
  {
    "table": "credit_events",
    "operation": "WRITE",
    "description": "Primary credit event record",
    "purpose": "Event recorded for trade 1",
    "event_context": "credit_event_bankruptcy"
  },
  {
    "table": "cds_trades",
    "operation": "WRITE",
    "description": "Trade status updated",
    "purpose": "Status changed to CREDIT_EVENT_RECORDED or SETTLED",
    "event_context": "credit_event_bankruptcy"
  },
  {
    "table": "cash_settlements",
    "operation": "WRITE",
    "description": "Cash settlement calculated",
    "purpose": "Recovery rate applied for cash settlement",
    "event_context": "credit_event_bankruptcy"
  },
  {
    "table": "audit_logs",
    "operation": "WRITE",
    "description": "Audit trail recorded",
    "purpose": "Compliance and regulatory tracking",
    "event_context": "credit_event_bankruptcy"
  }
]
```

---

## 📝 Implementation Checklist

✅ **Backend:**
- [x] `DatabaseOperationTracker` captures all read/write operations
- [x] `LineageAspect` enables tracking and merges metadata
- [x] `LineageService.trackCreditEventWithDetails()` extracts and structures table operations
- [x] `extractTransformations()` includes table_transformations in intelligence
- [x] Context descriptions for known tables (credit_events, cds_trades, etc.)

✅ **Frontend:**
- [x] Transformations tab displays table name, description, purpose, context
- [x] Event filtering by dataset, operation, search
- [x] Dynamic intelligence updates when clicking events
- [x] "Affected Tables" section in intelligence panel

---

## 🚀 Future Enhancements

1. **Entity-Level Tracking**
   - Track specific entity IDs modified (not just tables)
   - Show before/after values for critical fields

2. **Performance Metrics**
   - Duration per table operation
   - Query optimization insights

3. **Visual Data Flow**
   - Graph visualization showing table relationships
   - Dependency analysis for complex operations

4. **Alerting**
   - Notify when unexpected tables are touched
   - Detect anomalous data access patterns

---

## 📚 Related Documentation

- [COMPREHENSIVE_LINEAGE_COMPLETION.md](./COMPREHENSIVE_LINEAGE_COMPLETION.md) - Full lineage implementation
- [LINEAGE_ASPECT_CLEANUP.md](./LINEAGE_ASPECT_CLEANUP.md) - Aspect architecture cleanup
- [AOP_LINEAGE_IMPLEMENTATION.md](./AOP_LINEAGE_IMPLEMENTATION.md) - Original AOP design
- [LINEAGE_USER_GUIDE.md](./LINEAGE_USER_GUIDE.md) - User guide for lineage features

---

**Status:** ✅ Complete and Production-Ready  
**Last Updated:** November 11, 2025  
**Feature Flag:** `lineage.auto-tracking.enabled=true` (default)
