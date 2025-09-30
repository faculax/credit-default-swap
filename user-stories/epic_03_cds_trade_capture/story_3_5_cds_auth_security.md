# Story 3.5 – Authorization & Security Guardrails

**As the platform**,  
I want to restrict CDS trade booking to authenticated and authorized users  
So that only permitted actors can introduce contractual risk into the system.

## ✅ Acceptance Criteria
- Booking endpoint requires an auth context (interim: custom header `X-User` accepted in dev environments).
- If missing/invalid auth context → HTTP 401.
- If user lacks `BOOK_CDS_TRADE` role/permission → HTTP 403.
- All successful bookings record `bookedBy` (username/principal) and timestamp.
- Security model pluggable: placeholder implementation can be replaced by real IdP without rewriting controller.
- Logging: each POST success logs `tradeId`, `bookedBy` at INFO; failures log reason at WARN.
- (Optional) Rate limit placeholder (document only) for future DoS protection.

## 🧪 Test Scenarios
1. No header → 401.
2. Header present but no permission → 403.
3. Valid header + permission → 201.
4. Booking response includes `bookedBy` (if included in payload) OR appears in audit log.

## 🛠 Implementation Guidance
- Introduce a `SecurityContextResolver` component.
- For now: read `X-User` header; if absent and not dev profile → reject.
- Add `bookedBy` column future migration (or reuse existing metadata table if added later) – if not part of current schema, document as backlog.
- Provide a feature flag `AUTH_ENABLED` (env) to bypass in local sandbox.

## 📦 Deliverables
- Security hook in controller (pre-validation).
- Unit tests for auth decision logic.
- Documentation snippet added to epic README (optional note section).

## ⏭ Dependencies / Links
- Enhances Story 3.3 endpoint.
- Completes acceptance criterion on authorization.

## 🔮 Backlog Seeds
- OAuth2 / OIDC integration.
- Fine-grained entitlements (view vs book vs amend vs cancel).
- Audit event outbox publishing.

