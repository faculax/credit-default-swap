# Functional Test Story 15.7 – Basket Detail Valuation Panel

Trace: story_15_7_basket-detail-valuation-panel
Tags: @EPIC_15 @BASKET @UI

## Objective
Validate valuation panel displays fair spread, expected loss, PV, contributors, and refreshes on updates.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-7-001 | Panel loads with metrics | E2E | @UI |
| FT-15-7-002 | Fair spread displayed | E2E | @UI |
| FT-15-7-003 | Expected loss displayed | E2E | @UI |
| FT-15-7-004 | PV displayed | E2E | @UI |
| FT-15-7-005 | Contributors list displayed | E2E | @UI |
| FT-15-7-006 | Unauthorized panel access -> 403 | API | @SECURITY |
| FT-15-7-007 | Drift panel metrics baseline | E2E | @DRIFT |
| FT-15-7-008 | Logging redacts internal IDs | API | @SECURITY |
| FT-15-7-009 | Metrics valuationLatency | E2E | @METRICS |
| FT-15-7-010 | Performance render latency | E2E | @PERFORMANCE |
| FT-15-7-011 | Accessibility ARIA roles | E2E | @ACCESSIBILITY |
| FT-15-7-012 | Concurrency multi-panels isolation | API | @CONCURRENCY |
| FT-15-7-013 | Edge very large PV formatting | E2E | @EDGE |
| FT-15-7-014 | Refresh cycle updates values | E2E | @UI |
| FT-15-7-015 | Export valuation snapshot JSON | API | @EXPORT |
| FT-15-7-016 | Negative PV formatting | E2E | @EDGE |
| FT-15-7-017 | Dark mode accessibility contrast | E2E | @ACCESSIBILITY |
| FT-15-7-018 | Rate limit exceeded -> 429 | API | @RESILIENCE |

## Automation Strategy
Playwright panel rendering & refresh; baseline metrics snapshot; accessibility audit.
