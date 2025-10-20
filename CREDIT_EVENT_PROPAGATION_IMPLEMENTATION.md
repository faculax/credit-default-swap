# Credit Event Propagation Implementation

## Overview
This document describes the implementation of credit event propagation for CDS contracts, ensuring that when a BANKRUPTCY or RESTRUCTURING event is recorded for one CDS, it automatically propagates to all other ACTIVE CDS contracts for the same reference entity.

## Business Rationale
When a reference entity experiences a credit event such as bankruptcy or restructuring, it affects **all** CDS contracts that reference that entity, not just a single contract. This implementation ensures that the credit event is automatically recorded across all active positions, triggering appropriate payouts and settlement processes.

## Implementation Details

### Modified Service
**File**: `/backend/src/main/java/com/creditdefaultswap/platform/service/CreditEventService.java`

#### Key Changes

1. **Enhanced `recordCreditEvent` method**
   - After recording the credit event for the originating trade
   - After creating the automatic payout event
   - Now calls `propagateCreditEventToReferenceEntity()` for BANKRUPTCY and RESTRUCTURING events

2. **New `propagateCreditEventToReferenceEntity` method**
   - Finds all ACTIVE CDS trades for the same reference entity (excluding the originating trade)
   - For each affected trade:
     - Creates an identical credit event with propagation metadata
     - Updates trade status to CREDIT_EVENT_RECORDED
     - Processes settlement based on settlement method (CASH or PHYSICAL)
     - Creates automatic PAYOUT event
     - Updates final trade status to SETTLED_CASH or SETTLED_PHYSICAL
     - Logs comprehensive audit trail
   - Includes error handling to continue processing remaining trades if one fails

#### Logic Flow

```
1. User records credit event on Trade A (e.g., BANKRUPTCY for "ACME Corp")
   ↓
2. System validates and creates credit event for Trade A
   ↓
3. System updates Trade A status to CREDIT_EVENT_RECORDED
   ↓
4. System processes settlement for Trade A
   ↓
5. System creates PAYOUT event for Trade A
   ↓
6. System queries for all other ACTIVE trades with "ACME Corp" as reference entity
   ↓
7. For each affected trade (Trade B, Trade C, etc.):
   a. Create credit event with same details
   b. Update status to CREDIT_EVENT_RECORDED
   c. Process settlement
   d. Create PAYOUT event
   e. Update final status to SETTLED_CASH/SETTLED_PHYSICAL
   ↓
8. Return original credit event to caller
```

#### Event Types That Trigger Propagation
- **BANKRUPTCY** ✅
- **RESTRUCTURING** ✅
- FAILURE_TO_PAY ❌ (no propagation)
- OBLIGATION_DEFAULT ❌ (no propagation)
- REPUDIATION_MORATORIUM ❌ (no propagation)

## Testing

### Test Coverage
**File**: `/backend/src/test/java/com/creditdefaultswap/platform/service/CreditEventServiceTest.java`

#### New Test Cases

1. **`recordCreditEvent_BankruptcyEvent_PropagatesOtherActiveTrades`**
   - Verifies that BANKRUPTCY events propagate to multiple active trades
   - Confirms credit events and payouts are created for all affected trades
   - Validates cash settlement calculations occur for all trades

2. **`recordCreditEvent_RestructuringEvent_PropagatesOtherActiveTrades`**
   - Verifies that RESTRUCTURING events propagate to other active trades
   - Ensures proper status transitions for all affected trades

3. **`recordCreditEvent_NonTerminalEvent_DoesNotPropagate`**
   - Confirms that non-terminal events (e.g., FAILURE_TO_PAY) do NOT propagate
   - Ensures only the original trade is affected

#### Test Results
```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

## Audit Trail

The implementation provides comprehensive audit logging:
- Each propagated credit event records the originating trade ID in the comments
- Audit entries are created for each credit event creation
- Trade status transitions are logged for all affected trades
- Errors during propagation are logged but don't halt processing

## Example Scenario

### Before Implementation
- Trade 1: ACME Corp, $1M, ACTIVE
- Trade 2: ACME Corp, $500K, ACTIVE
- Trade 3: ACME Corp, $750K, ACTIVE

**Action**: Record BANKRUPTCY for Trade 1
**Result**: Only Trade 1 is affected

### After Implementation
- Trade 1: ACME Corp, $1M, ACTIVE
- Trade 2: ACME Corp, $500K, ACTIVE
- Trade 3: ACME Corp, $750K, ACTIVE

**Action**: Record BANKRUPTCY for Trade 1
**Result**: 
- Trade 1 → SETTLED_CASH with BANKRUPTCY + PAYOUT events
- Trade 2 → SETTLED_CASH with propagated BANKRUPTCY + PAYOUT events
- Trade 3 → SETTLED_CASH with propagated BANKRUPTCY + PAYOUT events

All three trades automatically processed with payouts generated.

## API Behavior

The existing API endpoint remains unchanged:
```
POST /api/cds-trades/{tradeId}/credit-events
```

The propagation happens transparently within the service layer. The response returns the original credit event for the specified trade, but the propagation occurs in the background as part of the same transaction.

## Database Impact

- Multiple `cds_credit_events` records created in a single transaction
- Multiple trade status updates in a single transaction
- Multiple audit log entries
- Transaction is rolled back entirely if any critical error occurs

## Future Enhancements

Consider implementing:
1. Batch notifications to users about propagated credit events
2. Dashboard view showing all affected trades for a reference entity
3. UI warnings when recording credit events that will affect multiple trades
4. Configurable propagation rules per event type
5. Reference entity group/family management for related entities

## Dependencies

This implementation relies on:
- `CDSTradeRepository.findByReferenceEntityOrderByCreatedAtDesc()`
- `CreditEventRepository.save()`
- `TradeRepository.save()`
- `CashSettlementService.calculateCashSettlement()`
- `AuditService` logging methods

## Configuration

No additional configuration required. The propagation is enabled by default for BANKRUPTCY and RESTRUCTURING event types.
