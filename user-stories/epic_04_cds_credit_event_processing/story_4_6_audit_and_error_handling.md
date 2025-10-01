# Story 4.6 – Audit Logging & Error Handling

**As a compliance stakeholder**,  
I want credit event and settlement changes auditable with clear error signaling  
So that regulatory and investigative needs are met.

## ✅ Acceptance Criteria
- All credit event creations + settlement creations write an audit row: id, entity_type, entity_id, action, timestamp, actor (user/system), summary.
- Errors return structured problem+json: type, title, status, detail, timestamp, correlation_id.
- Correlation ID generated per request (UUID) and returned in header `X-Correlation-Id` and error payload.
- Audit repository persisted in `cds_audit_log` table (append-only).
- On transaction rollback, no audit rows persist (except for dedicated error audit entries if later adopted – out of scope now).
- Basic query endpoint (optional backlog) NOT required now.

## 🧪 Test Scenarios
1. Successful credit event → audit row present.
2. Duplicate event (idempotent) → single audit row only (creation). No second row.
3. Validation failure → structured error payload with correlation_id.
4. Settlement calculation success → audit row.

## 🛠 Implementation Guidance
- Use Spring `HandlerInterceptor` or filter to inject correlation id into MDC.
- Service layer publishes audit helper method inside same transaction.
- Flyway migration for `cds_audit_log` (no updates/deletes, only inserts).
- Provide centralized `@ControllerAdvice` for exception mapping to problem+json.

## 📦 Deliverables
- Audit table migration + entity.
- Correlation ID filter/interceptor.
- Controller advice for errors.
- Tests verifying audit persistence + error payload schema.

## 🔮 Backlog Seeds
- Search/reporting endpoint.
- Integration with external observability platform.

