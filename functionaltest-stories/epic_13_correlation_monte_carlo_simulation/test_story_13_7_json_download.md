# Functional Test Story 13.7 – JSON Download

Trace: story_13_7_json-download
Tags: @EPIC_13 @SIMULATION @EXPORT

## Objective
Validate JSON export of simulation results (loss distribution, metrics, configuration).

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-7-001 | Download JSON results file | API | @EXPORT |
| FT-13-7-002 | Content contains distribution array | API | @CONTENT |
| FT-13-7-003 | Content contains configuration block | API | @CONTENT |
| FT-13-7-004 | Unauthorized download -> 403 | API | @SECURITY |
| FT-13-7-005 | Invalid runId -> 404 | API | @NEGATIVE |
| FT-13-7-006 | Performance download latency | API | @PERFORMANCE |
| FT-13-7-007 | Drift distribution hash check | Engine | @DRIFT |
| FT-13-7-008 | Logging redacts seed | API | @SECURITY |
| FT-13-7-009 | API contract stable | Contract | @CONTRACT |
| FT-13-7-010 | Accessibility download button | E2E | @ACCESSIBILITY |
| FT-13-7-011 | Compression (.gz) optional | API | @EXPORT |
| FT-13-7-012 | Time zone normalized timestamps | API | @TIME |

## Automation Strategy
Binary vs JSON validation; hash baseline of distribution subset; Playwright download.
