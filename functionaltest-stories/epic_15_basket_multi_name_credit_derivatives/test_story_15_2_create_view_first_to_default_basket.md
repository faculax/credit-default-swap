# Functional Test Story 15.2 – Create & View First-to-Default Basket UI

Trace: story_15_2_create-view-first-to-default-basket
Tags: @EPIC_15 @BASKET @UI

## Objective
Validate UI flow for creating first-to-default basket and viewing core metrics (fair spread, expected loss, contributors).

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-2-001 | Open basket creation form | E2E | @UI |
| FT-15-2-002 | Submit valid basket form | E2E | @UI |
| FT-15-2-003 | Validation error missing constituent | E2E | @NEGATIVE |
| FT-15-2-004 | Display fair spread metric | E2E | @UI |
| FT-15-2-005 | Display expected loss metric | E2E | @UI |
| FT-15-2-006 | Display contributors panel | E2E | @UI |
| FT-15-2-007 | Unauthorized view -> 403 | API | @SECURITY |
| FT-15-2-008 | Drift UI render baseline | E2E | @DRIFT |
| FT-15-2-009 | Logging redacts internal IDs | API | @SECURITY |
| FT-15-2-010 | Metrics basketCreateLatency | E2E | @METRICS |
| FT-15-2-011 | Performance initial render latency | E2E | @PERFORMANCE |
| FT-15-2-012 | Accessibility form & metrics | E2E | @ACCESSIBILITY |
| FT-15-2-013 | Concurrency multi-creation isolation | API | @CONCURRENCY |
| FT-15-2-014 | Edge large number of constituents | E2E | @EDGE |
| FT-15-2-015 | Export basket detail JSON | API | @EXPORT |
| FT-15-2-016 | Dark mode accessibility contrast | E2E | @ACCESSIBILITY |
| FT-15-2-017 | Rate limit exceeded -> 429 | API | @RESILIENCE |
| FT-15-2-018 | Fair spread numeric formatting | E2E | @FORMAT |
| FT-15-2-019 | Expected loss numeric formatting | E2E | @FORMAT |
| FT-15-2-020 | Contributors sort stability | E2E | @UI |

## Automation Strategy
Playwright UI interactions; baseline fair spread & expected loss values vs fixture; accessibility and performance audits.
