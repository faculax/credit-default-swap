# Functional Test Story 15.6 – Basket Builder UI Phase A

Trace: story_15_6_basket-builder-ui-phase-a
Tags: @EPIC_15 @BASKET @UI

## Objective
Validate initial basket builder UI (Phase A) for constituent selection, weight entry, correlation matrix reference selection.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-6-001 | Open builder UI | E2E | @UI |
| FT-15-6-002 | Add constituent row | E2E | @UI |
| FT-15-6-003 | Remove constituent row | E2E | @UI |
| FT-15-6-004 | Enter weights sum to 1 validation | E2E | @VALIDATION |
| FT-15-6-005 | Select correlation matrix reference | E2E | @UI |
| FT-15-6-006 | Unauthorized builder access -> 403 | API | @SECURITY |
| FT-15-6-007 | Drift builder render baseline | E2E | @DRIFT |
| FT-15-6-008 | Logging redacts internal IDs | API | @SECURITY |
| FT-15-6-009 | Metrics builderRenderLatency | E2E | @METRICS |
| FT-15-6-010 | Performance initial render latency | E2E | @PERFORMANCE |
| FT-15-6-011 | Accessibility form elements | E2E | @ACCESSIBILITY |
| FT-15-6-012 | Concurrency multi-builders isolation | API | @CONCURRENCY |
| FT-15-6-013 | Edge large constituent count | E2E | @EDGE |
| FT-15-6-014 | Edge zero-weight rejection | E2E | @NEGATIVE |
| FT-15-6-015 | Export builder configuration JSON | API | @EXPORT |
| FT-15-6-016 | Dark mode accessibility contrast | E2E | @ACCESSIBILITY |
| FT-15-6-017 | Weight normalization auto-fix | E2E | @UI |
| FT-15-6-018 | Rate limit exceeded -> 429 | API | @RESILIENCE |

## Automation Strategy
Playwright interactions; weight validation logic; accessibility audit; export configuration snapshot.
