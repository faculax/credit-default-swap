# Story 4.4 – Physical Settlement Instruction Scaffold

**As an operations user**,  
I want the system to prepare a scaffold for physical settlement instructions  
So that required deliverable obligation data can be captured consistently.

## ✅ Acceptance Criteria
- Applies when credit event recorded AND trade settlement_method = PHYSICAL.
- System creates a placeholder instruction record linked to credit event: status = DRAFT.
- Fields: id, credit_event_id, trade_id, reference_obligation_isin (nullable), proposed_delivery_date (nullable), notes, status, created_at, updated_at.
- No validation of ISIN format yet (reuse existing utility later – backlog cross-link to Epic 2 story 2.3).
- GET endpoint to retrieve scaffold: `/api/cds-trades/{id}/credit-events/{eventId}/physical-instruction`.
- If already exists, returns existing (idempotent creation on event record).

## 🧪 Test Scenarios
1. Credit event with PHYSICAL method → scaffold created automatically.
2. Repeat retrieval → same record.
3. Non-physical event → 404 for physical instruction endpoint.

## 🛠 Implementation Guidance
- Domain entity `PhysicalSettlementInstruction` with unique FK on credit_event_id.
- Created within same transaction as credit event or via async listener.
- Keep minimal fields; enrichment later.

## 📦 Deliverables
- Migration for `cds_physical_settlement_instructions`.
- Entity + repository + retrieval endpoint.
- Creation logic triggered on event persistence.

## 🔮 Backlog Seeds
- Enforce ISIN format (Epic 2 link).
- Matching engine for obligation substitution.

