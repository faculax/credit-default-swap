# Story 3.3 – Persist & Book Trade

**As a trader / system**,  
I want a booked CDS trade to be stored durably with identifiers and timestamps  
So that it can be referenced, audited, and downstream processes can consume it.

## ✅ Acceptance Criteria
- Endpoint: `POST /api/cds-trades` (JSON) persists a trade.
- Response: full trade + `id`, `createdAt`, optionally `updatedAt` (null or = createdAt initially).
- Stored fields match form input + system managed fields.
- Trade status defaults to `PENDING` unless provided (future state machine extension).
- Idempotency guidance placeholder: (Future) optionally accept `Idempotency-Key` header.
- Transactional integrity: either all fields persist or none (rollback on failure).
- DB schema: `cds_trades` (already defined) columns align with entity.
- Indexes used from migration (reference_entity, counterparty, trade_status, trade_date, created_at).

## 🧪 Test Scenarios
1. Valid trade returns 201 + body with id.
2. Duplicate POST (same payload) currently allowed (document) – future story may address.
3. DB failure (simulate) → 500 with generic error (no partial record).
4. Missing field handled by Story 3.2 (should not reach persistence).

## 🛠 Implementation Guidance
- Reuse existing `CDSTrade` entity & repository.
- Add service method `saveTrade` already present; ensure mapping consistent.
- Ensure timestamps auto-populate (DB default or application-level if needed).
- Consider returning a simplified DTO externally (future optimization).

## 📦 Deliverables
- Confirmed repository + controller logic.
- Integration test hitting real (test) DB verifying persistence + index usage (optional explain plan).

## ⏭ Dependencies / Links
- Relies on validation (Story 3.2).
- Enables confirmation UI (Story 3.4).

