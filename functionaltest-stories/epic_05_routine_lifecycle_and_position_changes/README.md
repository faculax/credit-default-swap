# Test Epic 05 – Routine Lifecycle & Position Changes

## Scope
Automated lifecycle events (coupon accrual, IMM schedule generation), economic amendments, notional adjustments/terminations, novations, compression ingestion & execution.

## Systems / Components
- Scheduler / cron triggers
- Lifecycle engine (accrual calc, event generation)
- Trade service amendments
- Novation workflow endpoints
- Compression proposal ingestion & execution

## Objectives
- Correct lifecycle event generation timings & amounts
- Accurate accrual and net cash posting
- Safe economic amendment workflows with audit
- Proper notional adjustment & termination logic
- Novation party role transitions validated
- Compression proposals applied consistently

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Missed IMM schedule generation | Time-travel tests | FT-5-1-003 |
| Incorrect accrual breakage | Accrual formula tests | FT-5-2-005..010 |
| Incomplete amendment audit | Audit assertions | FT-5-3-012 |
| Notional negative after adjustment | Validation test | FT-5-4-004 |
| Novation leaves orphan references | Consistency checks | FT-5-5-010 |
| Compression partial apply | Transaction rollback | FT-5-6-015 |

## Scenario Taxonomy
| Category | Stories |
|----------|---------|
| Scheduling & Events | 5.1 |
| Accrual & Cash Posting | 5.2 |
| Economic Amend Workflow | 5.3 |
| Notional Adjustment & Termination | 5.4 |
| Novation Workflow | 5.5 |
| Compression Ingestion & Execution | 5.6 |

## Tooling Matrix
| Layer | Tool |
|-------|------|
| Scheduler | Time-freeze / clock injection |
| API | REST-assured |
| DB | Testcontainers |
| E2E | Playwright (novations UI if present) |

## Non-Functional Hooks
- Accrual batch run p95 latency < target
- Compression execution < 2s for 100 trades sample

## Exit Criteria
All FT-5-* scenarios automated; no accrual drift beyond tolerance.
