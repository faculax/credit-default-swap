# Functional Test Story 5.4 – Notional Adjustment & Termination Logic

Trace: story_5_4_notional_adjustment_termination_logic
Tags: @EPIC_05 @STORY_5_4 @LIFECYCLE @NOTIONAL

## Objective
Validate safe notional reductions, partial terminations, and full termination logic with accrual impact.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-5-4-001 | Reduce notional positive delta | API | @CRUD |
| FT-5-4-002 | Reduction exceeding remaining notional rejected | API | @NEGATIVE |
| FT-5-4-003 | Full termination sets status TERMINATED | API | @STATE |
| FT-5-4-004 | Negative resulting notional blocked | API | @NEGATIVE |
| FT-5-4-005 | Accrual stops post termination | Engine | @ACCRUAL |
| FT-5-4-006 | Partial termination adjusts premium proportion | Engine | @CALCULATION |
| FT-5-4-007 | Audit entry includes previous & new notional | API | @AUDIT |
| FT-5-4-008 | Concurrency guard (two reductions) | API | @CONCURRENCY |
| FT-5-4-009 | Idempotent repeat same reduction | API | @IDEMPOTENCY |
| FT-5-4-010 | Undo termination (if allowed) transitions | API | @STATE |
| FT-5-4-011 | Metrics terminationCount increments | API | @METRICS |
| FT-5-4-012 | Permission restricted to role TRADER | API | @SECURITY |
| FT-5-4-013 | Scheduling future-dated termination effective | API | @SCHEDULING |
| FT-5-4-014 | Time zone aware effective timestamp | API | @TIME |
| FT-5-4-015 | Large batch terminations performance | API | @PERFORMANCE |
| FT-5-4-016 | Accrual recompute after partial termination | Engine | @ACCRUAL |
| FT-5-4-017 | Export reflects new notional | API | @EXPORT |
| FT-5-4-018 | Notification dispatch on termination | API | @NOTIFY |

## Automation Strategy
Integration with controlled accrual recalculation; concurrency tested via parallel request submission.
