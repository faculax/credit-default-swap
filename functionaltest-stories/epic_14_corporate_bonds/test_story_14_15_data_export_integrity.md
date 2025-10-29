# Functional Test Story 14.15 – Data Export Integrity

Trace: story_14_15_data-export-integrity
Tags: @EPIC_14 @CORPORATE_BONDS @EXPORT

## Objective
Validate exported data artifacts (pricing CSV, risk JSON, schedule PDF) for completeness and hash stability.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-15-001 | Export pricing CSV | API | @EXPORT |
| FT-14-15-002 | Export risk sensitivities JSON | API | @EXPORT |
| FT-14-15-003 | Export schedule PDF | API | @EXPORT |
| FT-14-15-004 | Unauthorized export -> 403 | API | @SECURITY |
| FT-14-15-005 | Drift export hash baseline | API | @DRIFT |
| FT-14-15-006 | Logging redacts file path | API | @SECURITY |
| FT-14-15-007 | Metrics exportCount | API | @METRICS |
| FT-14-15-008 | Performance export latency | API | @PERFORMANCE |
| FT-14-15-009 | Accessibility export UI buttons | E2E | @ACCESSIBILITY |
| FT-14-15-010 | Concurrency multiple exports stability | API | @CONCURRENCY |
| FT-14-15-011 | Edge large file export | API | @SCALING |
| FT-14-15-012 | Contract export schema stable | Contract | @CONTRACT |

## Automation Strategy
Download artifacts; hash compare; negative unauthorized; large file stress.
