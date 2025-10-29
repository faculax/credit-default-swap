# Functional Test Story 13.26 – Time Zone Normalization

Trace: story_13_26_time-zone-normalization
Tags: @EPIC_13 @SIMULATION @TIME

## Objective
Validate all timestamps in simulation artifacts are normalized to UTC and consistent.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-26-001 | Submission timestamp UTC | API | @TIME |
| FT-13-26-002 | Progress frame timestamps UTC | API | @TIME |
| FT-13-26-003 | Completion timestamp UTC | API | @TIME |
| FT-13-26-004 | Export metadata timestamps UTC | API | @TIME |
| FT-13-26-005 | Unauthorized timestamp read -> 403 | API | @SECURITY |
| FT-13-26-006 | Drift timestamp format baseline | API | @DRIFT |
| FT-13-26-007 | Logging redacts user tz | API | @SECURITY |
| FT-13-26-008 | Metrics utcTimestampCount | API | @METRICS |
| FT-13-26-009 | API contract timestamp schema | Contract | @CONTRACT |
| FT-13-26-010 | Accessibility timestamp UI formatting | E2E | @ACCESSIBILITY |
| FT-13-26-011 | Concurrency multiple runs timestamps consistent | API | @CONCURRENCY |
| FT-13-26-012 | Edge daylight saving unaffected | API | @EDGE |

## Automation Strategy
Parse timestamps; assert 'Z' suffix or offset; compare formats; baseline sample.
