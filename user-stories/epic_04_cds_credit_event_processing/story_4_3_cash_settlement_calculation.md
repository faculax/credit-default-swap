# Story 4.3 – Cash Settlement Calculation

**As a risk or operations user**,  
I want the system to calculate the cash settlement amount for a credit event  
So that payouts are consistent and transparent.

## ✅ Acceptance Criteria
- Triggered automatically after recording a credit event when settlement_method = CASH.
- Calculation formula documented: `Payout = Notional * (1 - RecoveryRate)`.
- RecoveryRate source order: (a) provided override in request (future) (b) trade default recovery (c) system default (e.g., 40%).
- Result persisted in `cds_cash_settlements` table: id, credit_event_id (FK), trade_id, notional, recovery_rate, payout_amount, calculated_at.
- Rounding: 2 decimal places, HALF_UP.
- Idempotent: recalculation does not create duplicate rows; returns existing.
- Exposed via GET `/api/cds-trades/{id}/credit-events/{eventId}/cash-settlement`.

## 🧪 Test Scenarios
1. Event with CASH method triggers calculation → row created.
2. Re-post same event (idempotent) → reused calculation.
3. Missing recovery defaults to system default.
4. Precision check: notional 1000000 & recovery 0.37 → payout 630000.00.

## 🛠 Implementation Guidance
- Service method invoked post event persistence (domain event or inline call).
- Configurable default recovery via env var: `CDS_DEFAULT_RECOVERY=0.40`.
- Add Flyway migration for `cds_cash_settlements` with unique credit_event_id.
- Expose DTO with calculation inputs & result.

## 📦 Deliverables
- Migration, entity, repository, service logic.
- REST endpoint for retrieval.
- Unit tests for formula & rounding edge cases.

## 🔮 Backlog Seeds
- Historical recovery curves integration.
- Support partial notional unwind.

