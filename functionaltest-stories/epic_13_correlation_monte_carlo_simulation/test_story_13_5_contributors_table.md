# Functional Test Story 13.5 – Contributors Table

Trace: story_13_5_contributors-table
Tags: @EPIC_13 @SIMULATION @UI

## Objective
Validate contributors table ranking entities by loss contribution and displaying recovery stats.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-5-001 | Contributors list populated | API | @API |
| FT-13-5-002 | Sorting by contribution descending | E2E | @UI |
| FT-13-5-003 | Filtering by sector | E2E | @FILTER |
| FT-13-5-004 | Pagination stable order | API | @PAGINATION |
| FT-13-5-005 | Deterministic ordering same seed | Engine | @DETERMINISM |
| FT-13-5-006 | Negative contribution handling | Engine | @EDGE |
| FT-13-5-007 | Export contributors CSV | API | @EXPORT |
| FT-13-5-008 | Unauthorized access -> 403 | API | @SECURITY |
| FT-13-5-009 | Performance load latency < threshold | API | @PERFORMANCE |
| FT-13-5-010 | Drift baseline top 10 list check | Engine | @DRIFT |
| FT-13-5-011 | Recovery rate column formatting | E2E | @FORMAT |
| FT-13-5-012 | Accessibility table semantics | E2E | @ACCESSIBILITY |
| FT-13-5-013 | Metrics contributorsCount | API | @METRICS |
| FT-13-5-014 | Logging redacts internal IDs | API | @SECURITY |
| FT-13-5-015 | Concurrency multi-fetch isolation | API | @CONCURRENCY |
| FT-13-5-016 | API contract stable | Contract | @CONTRACT |
| FT-13-5-017 | Time zone normalized timestamps | API | @TIME |
| FT-13-5-018 | Large dataset scaling | API | @SCALING |

## Automation Strategy
Playwright UI sorting/filtering; API contract snapshot; baseline top list JSON.
