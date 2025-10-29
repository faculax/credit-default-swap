# Functional Test Story 13.27 – Contract Schema Stability

Trace: story_13_27_contract-schema-stability
Tags: @EPIC_13 @SIMULATION @CONTRACT

## Objective
Validate stability of public API schemas (submission, progress, metrics, export) via snapshot comparison.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-27-001 | Submission schema snapshot match | Contract | @CONTRACT |
| FT-13-27-002 | Progress frame schema snapshot match | Contract | @CONTRACT |
| FT-13-27-003 | Metrics schema snapshot match | Contract | @CONTRACT |
| FT-13-27-004 | Export schema snapshot match | Contract | @CONTRACT |
| FT-13-27-005 | Unauthorized contract schema fetch -> 403 | API | @SECURITY |
| FT-13-27-006 | Drift schema hash baseline | Contract | @DRIFT |
| FT-13-27-007 | Logging redacts internal field names | API | @SECURITY |
| FT-13-27-008 | Metrics contractChangeDetectedCount | API | @METRICS |
| FT-13-27-009 | Performance schema fetch latency | API | @PERFORMANCE |
| FT-13-27-010 | Accessibility contract docs UI | E2E | @ACCESSIBILITY |
| FT-13-27-011 | Concurrency multiple schema fetch stable | API | @CONCURRENCY |
| FT-13-27-012 | Edge missing field triggers failure | Contract | @NEGATIVE |

## Automation Strategy
JSON schema snapshots; hash comparison; negative altered field test.
