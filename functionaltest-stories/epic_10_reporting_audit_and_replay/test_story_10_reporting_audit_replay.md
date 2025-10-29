# Functional Test Story 10.0 – Reporting, Audit & Replay

Trace: epic_10_reporting_audit_and_replay/README.md
Tags: @EPIC_10 @REPORTING @AUDIT

## Objective
Validate reporting outputs, audit log completeness, and deterministic replay of trade & event history.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-10-0-001 | Generate daily trade report CSV | API | @REPORT |
| FT-10-0-002 | Generate risk summary PDF | API | @REPORT |
| FT-10-0-003 | Export audit log filtered by date range | API | @AUDIT |
| FT-10-0-004 | Audit entry for trade create | API | @AUDIT |
| FT-10-0-005 | Audit entry for credit event | API | @AUDIT |
| FT-10-0-006 | Report schema matches snapshot | API | @CONTRACT |
| FT-10-0-007 | Unauthorized report request -> 403 | API | @SECURITY |
| FT-10-0-008 | Pagination stable ordering audit log | API | @PAGINATION |
| FT-10-0-009 | Retention job trims old audit entries | API | @RETENTION |
| FT-10-0-010 | Replay reconstructs trade state | Engine | @REPLAY |
| FT-10-0-011 | Replay reconstructs risk metrics | Engine | @REPLAY |
| FT-10-0-012 | Deterministic replay hash matches baseline | Engine | @DETERMINISM |
| FT-10-0-013 | Replay partial missing event -> error | Engine | @NEGATIVE |
| FT-10-0-014 | Performance report generation < threshold | API | @PERFORMANCE |
| FT-10-0-015 | Logging redacts sensitive fields | API | @SECURITY |
| FT-10-0-016 | Metrics reportGenerationLatency | API | @METRICS |
| FT-10-0-017 | Correlation id available in report logs | API | @OBSERVABILITY |
| FT-10-0-018 | Concurrent report requests isolation | API | @CONCURRENCY |
| FT-10-0-019 | CSV injection prevention | API | @SECURITY |
| FT-10-0-020 | Accessibility report download UI | E2E | @ACCESSIBILITY |
| FT-10-0-021 | Rate limit on heavy reports | API | @RESILIENCE |
| FT-10-0-022 | Drift detection risk summary metrics | Engine | @DRIFT |
| FT-10-0-023 | Export JSON variant of report | API | @EXPORT |
| FT-10-0-024 | Time zone normalization in timestamps | API | @TIME |

## Automation Strategy
Report generation integration tests capturing file outputs; replay harness replays event stream into ephemeral DB and compares hashed snapshots.
