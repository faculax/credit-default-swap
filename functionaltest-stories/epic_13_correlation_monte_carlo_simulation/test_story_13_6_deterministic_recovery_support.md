# Functional Test Story 13.6 – Deterministic Recovery Support

Trace: story_13_6_deterministic-recovery-support
Tags: @EPIC_13 @SIMULATION @RECOVERY

## Objective
Validate deterministic recovery modeling producing reproducible outcomes across runs when enabled.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-6-001 | Enable deterministic recovery flag | API | @CONFIG |
| FT-13-6-002 | Repeated run identical recovery values | Engine | @DETERMINISM |
| FT-13-6-003 | Disable flag reverts to stochastic | Engine | @CONFIG |
| FT-13-6-004 | Mixed mode rejection | Engine | @NEGATIVE |
| FT-13-6-005 | Export deterministic recovery series | API | @EXPORT |
| FT-13-6-006 | Unauthorized toggle -> 403 | API | @SECURITY |
| FT-13-6-007 | Drift baseline deterministic outputs | Engine | @DRIFT |
| FT-13-6-008 | Logging redacts seed | Engine | @SECURITY |
| FT-13-6-009 | Performance overhead minimal | Engine | @PERFORMANCE |
| FT-13-6-010 | API contract stable | Contract | @CONTRACT |
| FT-13-6-011 | Metrics deterministicRecoveryEnabled | API | @METRICS |
| FT-13-6-012 | Time zone normalized timestamps | API | @TIME |
| FT-13-6-013 | Concurrency consistent deterministic runs | Engine | @CONCURRENCY |
| FT-13-6-014 | Edge high recovery value 1.0 | Engine | @EDGE |
| FT-13-6-015 | Edge zero recovery value | Engine | @EDGE |
| FT-13-6-016 | Accessibility recovery config UI | E2E | @ACCESSIBILITY |

## Automation Strategy
Seed injection harness; baseline JSON for recovery path values.
