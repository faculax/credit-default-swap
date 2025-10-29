# Functional Test Story 14.13 – Frontend Portfolio Bond Metrics Columns

Trace: story_14_13_frontend-portfolio-bond-metrics-columns
Tags: @EPIC_14 @CREDIT_BONDS @UI

## Objective
Validate portfolio UI displays metrics columns (PV, Yield, Spread, DV01, CS01, JtD) with correct formatting and sorting.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-13-001 | Metrics columns present | E2E | @UI |
| FT-14-13-002 | Sorting PV ascending | E2E | @UI |
| FT-14-13-003 | Sorting yield descending | E2E | @UI |
| FT-14-13-004 | Filter by spread range | E2E | @FILTER |
| FT-14-13-005 | Unauthorized portfolio view -> 403 | API | @SECURITY |
| FT-14-13-006 | Drift metrics column baseline | E2E | @DRIFT |
| FT-14-13-007 | Logging redacts internal IDs | API | @SECURITY |
| FT-14-13-008 | Metrics metricsColumnAccuracyPct | E2E | @METRICS |
| FT-14-13-009 | Performance render latency | E2E | @PERFORMANCE |
| FT-14-13-010 | Accessibility metrics table semantics | E2E | @ACCESSIBILITY |
| FT-14-13-011 | Concurrency multi-viewers stable | API | @CONCURRENCY |
| FT-14-13-012 | Edge extremely large DV01 formatting | E2E | @EDGE |
| FT-14-13-013 | Edge negative spread formatting | E2E | @EDGE |
| FT-14-13-014 | Export table CSV | API | @EXPORT |

## Automation Strategy
Playwright table interactions; sorting/filtering; snapshot baseline; CSV export hash.
