# Functional Test Story 13.24 – Reporting Export Integrity

Trace: story_13_24_reporting-export-integrity
Tags: @EPIC_13 @SIMULATION @REPORTING

## Objective
Validate integrity of exported reporting artifacts (CSV summary, JSON detail, PDF overview).

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-24-001 | Export CSV summary | API | @EXPORT |
| FT-13-24-002 | Export JSON detail | API | @EXPORT |
| FT-13-24-003 | Export PDF overview | API | @EXPORT |
| FT-13-24-004 | Unauthorized export -> 403 | API | @SECURITY |
| FT-13-24-005 | Drift export hash baseline | API | @DRIFT |
| FT-13-24-006 | Logging redacts file paths | API | @SECURITY |
| FT-13-24-007 | Metrics exportCount | API | @METRICS |
| FT-13-24-008 | API contract export schema stable | Contract | @CONTRACT |
| FT-13-24-009 | Performance export latency | API | @PERFORMANCE |
| FT-13-24-010 | Accessibility export buttons | E2E | @ACCESSIBILITY |
| FT-13-24-011 | Concurrency multiple exports stable | API | @CONCURRENCY |
| FT-13-24-012 | Edge large file export success | API | @SCALING |

## Automation Strategy
Download + hash; schema comparison; concurrency stress; baseline artifact hashes.
