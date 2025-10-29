# Functional Test Story 13.18 – Distribution Chart UI

Trace: story_13_18_distribution-chart-ui
Tags: @EPIC_13 @SIMULATION @UI

## Objective
Validate interactive distribution chart (hover, percentile markers, zoom) renders correctly and is accessible.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-18-001 | Chart canvas renders | E2E | @UI |
| FT-13-18-002 | Hover tooltip shows loss value | E2E | @UI |
| FT-13-18-003 | Percentile markers correct positions | Engine | @NUMERIC |
| FT-13-18-004 | Zoom in/out resets correctly | E2E | @UI |
| FT-13-18-005 | Deterministic seed consistent chart | Engine | @DETERMINISM |
| FT-13-18-006 | Drift percentile marker baseline | Engine | @DRIFT |
| FT-13-18-007 | Accessibility ARIA roles present | E2E | @ACCESSIBILITY |
| FT-13-18-008 | Performance initial render time | E2E | @PERFORMANCE |
| FT-13-18-009 | Unauthorized data fetch -> 403 | API | @SECURITY |
| FT-13-18-010 | Logging redacts internal arrays | API | @SECURITY |
| FT-13-18-011 | Export image snapshot | E2E | @EXPORT |
| FT-13-18-012 | Concurrency multiple viewers stable | API | @CONCURRENCY |
| FT-13-18-013 | API contract data schema stable | Contract | @CONTRACT |

## Automation Strategy
Playwright UI actions; snapshot image comparison; numeric percentile asserts.
