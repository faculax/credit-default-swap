# Test Epic 04 – Credit Event Processing

## Scope
End-to-end credit event lifecycle: recording, validation, settlement calculation (cash & scaffold for physical), instructions persistence, audit & error handling.

## Systems / Components
- Frontend trade detail / credit event modal
- Gateway endpoints `/api/credit-events/*`
- Backend credit event service (validation, settlement engine integration)
- Trade service (status transitions)
- Audit & notification subsystem

## Objectives
- Accurate recording & prevention of duplicates
- Correct payout calculation (cash path)
- Propagation across correlated trades atomically
- Robust validation & error semantics
- Full audit trail integrity

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Duplicate events | Idempotent checks | FT-4-1-019/020 |
| Partial propagation | Transaction / rollback tests | FT-4-1-011 |
| Incorrect payout formula | Calculation verification | FT-4-3-005..010 |
| Settlement instruction loss | Persistence integrity | FT-4-5-006 |
| Audit gaps | Audit presence tests | FT-4-6-001..005 |

## Scenario Taxonomy
| Category | Stories |
|----------|---------|
| Record & UI | 4.1 |
| Validation & Persist | 4.2 |
| Cash Settlement | 4.3 |
| Physical Settlement Scaffold | 4.4 |
| Instructions Persistence | 4.5 |
| Audit & Error Handling | 4.6 |

## Tooling Matrix
| Layer | Tool |
|-------|------|
| FE E2E | Playwright |
| API | REST-assured |
| Calculation | JUnit deterministic fixtures |
| Audit | Integration DB assertions |

## Non-Functional Hooks
- Payout calc latency p95 < 500ms
- Propagation batch (<50 trades) < 1.2s

## Exit Criteria
All FT-4-* green; no Sev1/2 defects; payout drift < tolerance.
