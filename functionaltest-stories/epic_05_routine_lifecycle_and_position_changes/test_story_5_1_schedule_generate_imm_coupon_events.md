# Functional Test Story 5.1 – Schedule & IMM Coupon Event Generation

Trace: story_5_1_schedule_generate_imm_coupon_events
Tags: @EPIC_05 @STORY_5_1 @LIFECYCLE @SCHEDULER

## Objective
Validate scheduled generation of coupon events aligned with IMM dates and custom schedules.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-5-1-001 | Scheduler triggers on configured cron | Engine | @CRON |
| FT-5-1-002 | IMM date detection logic | Engine | @DATE |
| FT-5-1-003 | Time-travel missed run triggers catch-up | Engine | @RECOVERY |
| FT-5-1-004 | Generated coupon events count matches trades | Engine | @SCALING |
| FT-5-1-005 | Event amount formula correct | Engine | @CALCULATION |
| FT-5-1-006 | Prorated first stub period | Engine | @CALCULATION |
| FT-5-1-007 | Leap year accrual handled | Engine | @DATE |
| FT-5-1-008 | Duplicate generation prevented | Engine | @IDEMPOTENCY |
| FT-5-1-009 | Audit entries per event | API | @AUDIT |
| FT-5-1-010 | Unauthorized schedule trigger blocked | API | @SECURITY |
| FT-5-1-011 | Manual re-run dry-run flag | API | @DRYRUN |
| FT-5-1-012 | Performance batch < threshold (1000 trades) | Engine | @PERFORMANCE |
| FT-5-1-013 | Partial failure rollback none persisted | Engine | @TRANSACTION |
| FT-5-1-014 | Logging summary includes counts | Engine | @OBSERVABILITY |
| FT-5-1-015 | Rate limit manual trigger | API | @RESILIENCE |
| FT-5-1-016 | Disabled flag prevents generation | Engine | @CONFIG |
| FT-5-1-017 | Metrics emitted (eventsGenerated) | Engine | @METRICS |

## Automation Strategy
Inject fixed clock; simulate missed window by advancing clock and running catch-up routine.
