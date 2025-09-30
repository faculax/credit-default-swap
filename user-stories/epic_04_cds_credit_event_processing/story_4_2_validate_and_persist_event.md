# Story 4.2 – Validate & Persist Credit Event

**As the system**,  
I need to validate an incoming credit event against trade eligibility rules  
So that only legitimate lifecycle events are recorded.

## ✅ Acceptance Criteria
- Endpoint validates that trade exists and is ACTIVE.
- Reject if a prior terminal credit event already recorded (idempotency guard).
- Valid event types enforced via enum / lookup.
- Persistent storage in `cds_credit_events` with: id (UUID), trade_id, event_type, event_date, notice_date, settlement_method, comments, created_at.
- Duplicate (same trade_id + event_type + event_date) returns 200 with existing resource (graceful idempotency) – no new row.
- On success, trade status transitions to CREDIT_EVENT_RECORDED (intermediate) unless auto-calculation moves it further.

## 🧪 Test Scenarios
1. Unknown trade id → 404.
2. Trade not ACTIVE → 422 (validation error code schema TBD).
3. Duplicate event (same key fields) → existing event returned.
4. Invalid event type → 400.
5. Success persists row; trade status updated.

## 🛠 Implementation Guidance
- Service layer method: `recordCreditEvent(tradeId, dto)` returning event entity.
- Database uniqueness constraint: (trade_id, event_type, event_date).
- Map DTO → entity; leverage JSR-303 for basic field validation.
- Wrap in transaction; on duplicate detection query existing.

## 📦 Deliverables
- Domain entity + JPA repository for credit event.
- Flyway migration creating `cds_credit_events`.
- Service + controller logic with idempotency.
- Unit + integration tests.

## 🔮 Backlog Seeds
- Pluggable validation rule engine.
- Audit trail enhancements (see Story 4.6).

