# Functional Test Story 14.14 – Accessibility UI

Trace: story_14_14_accessibility-ui
Tags: @EPIC_14 @CORPORATE_BONDS @ACCESSIBILITY

## Objective
Validate accessibility compliance for bond UI components (forms, tables, charts) to WCAG guidelines.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-14-001 | Keyboard navigation form | E2E | @ACCESSIBILITY |
| FT-14-14-002 | ARIA labels for inputs | E2E | @ACCESSIBILITY |
| FT-14-14-003 | Data table headers semantic | E2E | @ACCESSIBILITY |
| FT-14-14-004 | Contrast ratio compliance | E2E | @ACCESSIBILITY |
| FT-14-14-005 | Unauthorized UI access -> 403 | API | @SECURITY |
| FT-14-14-006 | Drift accessibility baseline | E2E | @DRIFT |
| FT-14-14-007 | Logging redacts user id | API | @SECURITY |
| FT-14-14-008 | Metrics accessibilityIssueCount | API | @METRICS |
| FT-14-14-009 | Performance initial render time | E2E | @PERFORMANCE |
| FT-14-14-010 | Concurrency multi-viewers stability | API | @CONCURRENCY |
| FT-14-14-011 | Edge screen reader reflow | E2E | @EDGE |
| FT-14-14-012 | Contract UI data schema stable | Contract | @CONTRACT |

## Automation Strategy
Playwright + axe-core; contrast check; keyboard traversal; snapshot baseline.
