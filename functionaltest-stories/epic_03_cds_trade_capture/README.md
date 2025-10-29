# Test Epic 03 – CDS Trade Capture

Mirrors user stories in `epic_03_cds_trade_capture` validating full trade booking lifecycle up to confirmation issuance.

## Scope
From UI ticket initiation through server validation, persistence, confirmation artifact generation, and security enforcement.

## Systems / Components
- Frontend trade ticket & blotter components
- Gateway API routes `/api/trades/*`
- Backend trade service (validation, persistence, confirmation generator)
- AuthN/Z layer (JWT middleware)
- Audit logging subsystem

## Out of Scope
- Post-trade lifecycle events (Epic 05)
- Credit events (Epic 04)

## Objectives
- Ensure correctness & completeness of captured trade data
- Enforce validation & business rules consistently (client & server)
- Guarantee idempotent booking and versioning semantics
- Confirm authorization boundaries for trade creation & viewing
- Validate confirmation generation integrity & hash stability

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Silent validation mismatch UI vs API | Cross-check negative matrix | FT-3-2-* |
| Duplicate trades via rapid submit | Idempotency key tests | FT-3-3-010/011 |
| Unauthorized access to booking | RBAC scenarios | FT-3-5-001..008 |
| Confirmation drift | Hash baseline comparison | FT-3-4-010 |
| Partial persistence on failure | Transaction rollback tests | FT-3-3-006 |

## Scenario Taxonomy
| Category | Stories | Notes |
|----------|---------|-------|
| UI Capture | 3.1 | Form & UX validations |
| Validation Matrix | 3.2 | Server-centric rules |
| Persistence & Versioning | 3.3 | DB, idempotency |
| Confirmation | 3.4 | Artifact, hash stability |
| Security & Auth | 3.5 | RBAC, tokens |

## Tooling Matrix
| Layer | Tool | Notes |
|-------|------|-------|
| FE E2E | Playwright | Tags @EPIC_03 |
| API Integration | REST-assured + Testcontainers | Postgres isolation |
| Contract | JSON Schema snapshot | Backward compatibility guard |
| DB | Testcontainers Postgres | Clean schema per test |
| Security | Playwright + mocked tokens | Role permutations |

## Non-Functional Hooks
- p50 trade create < 300ms (local CI baseline)
- Confirmation PDF hash stable except timestamp metadata
- Accessibility: axe no critical violations on ticket modal

## Exit Criteria
- All FT-3-* automated & green
- No open Sev1/2 defects
- Drift monitor stable 7 consecutive runs

## Data Strategy
Synthetic reference entities & deterministic ISINs enumerated in shared fixture JSON.
