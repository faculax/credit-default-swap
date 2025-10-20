# UI Auto-Refresh for Propagated Credit Events - Implementation

## Problem
When recording a credit event (BANKRUPTCY or RESTRUCTURING) for one CDS contract, the event propagates to all other ACTIVE CDS contracts for the same reference entity. However, the UI blotter didn't automatically refresh to show the updated status of the affected trades - users had to manually refresh the page.

## Solution
Implemented a callback-based refresh mechanism that automatically updates the blotter when credit events are propagated across multiple trades.

## Changes Made

### Backend

1. **New DTO: `CreditEventResponse.java`**
   - Wraps the credit event with a list of affected trade IDs
   - Allows the frontend to know which trades were impacted

2. **Updated `CreditEventService.java`**
   - Changed `recordCreditEvent()` to return `CreditEventResponse` instead of `CreditEvent`
   - Modified `propagateCreditEventToReferenceEntity()` to return `List<Long>` of affected trade IDs
   - Collects all affected trade IDs (original + propagated) and includes them in the response

3. **Updated `CreditEventController.java`**
   - Changed endpoint to return `CreditEventResponse` instead of `CreditEvent`
   - Response includes both the credit event and all affected trade IDs

4. **Updated `DemoCreditEventService.java`**
   - Adapted to extract credit event from the new response structure

5. **Updated Tests**
   - Fixed `CreditEventServiceTest.java` to handle the new response type
   - All tests passing ✅

### Frontend

1. **Updated `creditEventService.ts`**
   - Added `CreditEventResponse` interface matching backend DTO
   - Changed `recordCreditEvent()` to return `CreditEventResponse`

2. **Updated `CDSBlotter.tsx`**
   - Converted to use `forwardRef` and `useImperativeHandle`
   - Exposed `refreshTrades()` method via ref
   - Exported `CDSBlotterRef` interface for type safety

3. **Updated `TradeDetailModal.tsx`**
   - Added `onTradesUpdated` callback prop
   - Added `successMessage` state to store dynamic success messages
   - Enhanced `handleRecordCreditEvent()` to:
     - Extract affected trade IDs from response
     - Call `onTradesUpdated()` if multiple trades affected
     - Set custom success message based on propagation count
     - Display success notification (green toast) with propagation details
   - Updated success notification to display dynamic message with proper styling
   - Increased timeout to 5 seconds for propagation messages (vs 3 seconds for normal)

4. **Updated `App.tsx`**
   - Added ref to `CDSBlotter` component
   - Created `handleTradesUpdated()` callback
   - Callback triggers blotter refresh via ref

## User Experience Flow

**Before:**
1. User records BANKRUPTCY event on Trade CDS-1152
2. Event propagates to Trade CDS-1151 in backend
3. Trade CDS-1151 still shows as "ACTIVE" in blotter
4. User must manually refresh page to see updated status

**After:**
1. User records BANKRUPTCY event on Trade CDS-1152
2. Event propagates to Trade CDS-1151 in backend
3. Backend response includes: `affectedTradeIds: [1152, 1151]`
4. Frontend receives response
5. Green success notification appears (top-right) with message: "This BANKRUPTCY event has been propagated to 1 other active CDS contract(s) for the same reference entity. All affected trades have been settled."
6. TradeDetailModal calls `onTradesUpdated([1152, 1151])`
7. App.tsx triggers `blotterRef.current.refreshTrades()`
8. Blotter automatically reloads all trades
9. Both CDS-1152 and CDS-1151 now show as "SETTLED_CASH"
10. Success notification auto-dismisses after 5 seconds

## API Response Example

**Before:**
```json
{
  "id": "...",
  "tradeId": 1152,
  "eventType": "BANKRUPTCY",
  ...
}
```

**After:**
```json
{
  "creditEvent": {
    "id": "...",
    "tradeId": 1152,
    "eventType": "BANKRUPTCY",
    ...
  },
  "affectedTradeIds": [1152, 1151]
}
```

## Technical Details

### Ref-Based Communication
- Uses React `forwardRef` and `useImperativeHandle` pattern
- Allows parent component to trigger child component methods
- Type-safe with `CDSBlotterRef` interface

### Callback Propagation
```
TradeDetailModal.handleRecordCreditEvent()
  → API returns affectedTradeIds
  → onTradesUpdated(affectedTradeIds)
  → App.handleTradesUpdated()
  → blotterRef.current.refreshTrades()
  → CDSBlotter.loadTrades()
```

## Testing

1. **Backend Tests**: All 10 tests passing
2. **Frontend Build**: Compiles successfully with no errors
3. **Manual Testing Required**:
   - Create multiple ACTIVE CDS for same reference entity (e.g., "BAC")
   - Record BANKRUPTCY or RESTRUCTURING on one trade
   - Verify blotter automatically refreshes
   - Verify all affected trades show correct status
   - Verify alert displays propagation count

## Files Modified

### Backend
- `CreditEventResponse.java` (new)
- `CreditEventService.java`
- `CreditEventController.java`
- `DemoCreditEventService.java`
- `CreditEventServiceTest.java`

### Frontend
- `App.tsx`
- `CDSBlotter.tsx`
- `TradeDetailModal.tsx`
- `creditEventService.ts`

## Services to Restart

**Backend only** - you need to bounce the backend service.

The frontend is running with `npm start` and will hot-reload automatically.
