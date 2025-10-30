# Functional Test Story 16.12 – Frontend Bond Creation & Detail View

Trace: story_16_12_frontend-bond-creation-detail-view
Tags: @EPIC_16 @CREDIT_BONDS @UI

## Objective
Validate UI flow for creating a bond and viewing its detail with pricing, risk, and schedule panels.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-12-001 | Open bond creation form | E2E | @UI |
| FT-16-12-002 | Submit valid bond form | E2E | @UI |
| FT-16-12-003 | Validation error display | E2E | @NEGATIVE |
| FT-16-12-004 | Detail view loads pricing panel | E2E | @UI |
| FT-16-12-005 | Detail view loads risk panel | E2E | @UI |
| FT-16-12-006 | Detail view loads schedule panel | E2E | @UI |
| FT-16-12-007 | Unauthorized detail access -> 403 | API | @SECURITY |
| FT-16-12-008 | Drift UI render baseline | E2E | @DRIFT |
| FT-16-12-009 | Logging redacts internal IDs | API | @SECURITY |
| FT-16-12-010 | Metrics uiRenderTime | E2E | @METRICS |
| FT-16-12-011 | Performance initial render latency | E2E | @PERFORMANCE |
| FT-16-12-012 | Accessibility form & detail panels | E2E | @ACCESSIBILITY |
| FT-16-12-013 | Concurrency multi-detail views | API | @CONCURRENCY |
| FT-16-12-014 | Edge long bond name truncation | E2E | @EDGE |
| FT-16-12-015 | Edge extremely high coupon formatting | E2E | @EDGE |
| FT-16-12-016 | Export detail view JSON snapshot | API | @EXPORT |
| FT-16-12-017 | Dark mode accessibility contrast | E2E | @ACCESSIBILITY |
| FT-16-12-018 | Rate limit exceeded -> 429 | API | @RESILIENCE |

## Automation Strategy
Playwright UI interactions; form validations; panel loads; performance & accessibility audit.
