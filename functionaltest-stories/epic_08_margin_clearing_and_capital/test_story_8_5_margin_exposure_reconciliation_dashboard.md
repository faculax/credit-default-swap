# Functional Test Story 8.5 – Margin Exposure Reconciliation Dashboard

Trace: story_8_5_margin_exposure_reconciliation_dashboard
Tags: @EPIC_08 @STORY_8_5 @MARGIN @DASHBOARD

## Objective
Validate reconciliation dashboard ingestion of internal vs external margin/exposure figures, break identification, filtering, and resolution workflow.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-8-5-001 | Load dashboard baseline no breaks | E2E | @UI |
| FT-8-5-002 | Display break when internal != external VM | E2E | @UI @BREAK |
| FT-8-5-003 | Display break when IM difference > threshold | E2E | @UI @BREAK |
| FT-8-5-004 | Filter by counterparty | E2E | @FILTER |
| FT-8-5-005 | Sort by largest difference | E2E | @UI |
| FT-8-5-006 | Mark break as acknowledged | API | @WORKFLOW |
| FT-8-5-007 | Break resolution removes from active list | API | @WORKFLOW |
| FT-8-5-008 | Export breaks CSV | API | @EXPORT |
| FT-8-5-009 | Metrics breakCount | API | @METRICS |
| FT-8-5-010 | Injected synthetic break detected | API | @QUALITY |
| FT-8-5-011 | Unauthorized access -> 403 | API | @SECURITY |
| FT-8-5-012 | Pagination stable ordering | API | @PAGINATION |
| FT-8-5-013 | Concurrency resolve race single winner | API | @CONCURRENCY |
| FT-8-5-014 | Drift baseline (total diff) stored | Engine | @DRIFT |
| FT-8-5-015 | Performance load < threshold | E2E | @PERFORMANCE |
| FT-8-5-016 | Accessibility table axe clean | E2E | @ACCESSIBILITY |
| FT-8-5-017 | Logging redacts counterparty IDs | API | @SECURITY |
| FT-8-5-018 | Time zone consistent timestamps | API | @TIME |

## Automation Strategy
Playwright UI spec for filtering/sorting & accessibility; integration tests for break detection & workflow endpoints.
