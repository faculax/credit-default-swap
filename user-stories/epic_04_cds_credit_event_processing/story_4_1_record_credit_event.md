# Story 4.1 – Record Credit Event Action & Form

**As an operations / risk user**,  
I want to initiate recording of a credit event for an active CDS trade  
So that the trade lifecycle properly reflects the trigger condition.

## ✅ Acceptance Criteria
- UI exposes a "Record Credit Event" action only for trades in ACTIVE state.
- Form fields: Event Type (dropdown), Event Date, Notice Date, Description/Comments, Supporting Document (optional upload placeholder), Settlement Method (Cash | Physical) pre-filled from trade if stored.
- Event Type options: BANKRUPTCY, FAILURE_TO_PAY, RESTRUCTURING, OBLIGATION_DEFAULT, REPUDIATION_MORATORIUM (extensible).
- Required: Event Type, Event Date, Notice Date.
- Validation: Event Date <= today; Notice Date >= Event Date; cannot submit if invalid.
- On submit, client calls `POST /api/cds-trades/{id}/credit-events`.
- Pending/in-flight request disables submit button and shows progress state.

## 🧪 Test Scenarios
1. Non-active trade → action hidden/disabled.
2. Missing required field → inline error.
3. Event Date in future → error.
4. Notice Date < Event Date → error.
5. Successful submit returns 201 with event payload (id + timestamps).

## 🛠 Implementation Guidance
- Introduce new route/section in UI or modal overlay from trade detail.
- Backend controller: `POST /api/cds-trades/{id}/credit-events` -> delegates to service.
- Persist event in `cds_credit_events` table (future migration) with foreign key to trade.
- Keep upload placeholder as metadata only (no binary storage yet) – document backlog.

## 📦 Deliverables
- UI action + modal/form.
- Backend endpoint + basic persistence stub.
- Validation tests.

## 🔮 Backlog Seeds
- File storage (S3 / object store) for attachments.
- Real-time notification to downstream systems.

