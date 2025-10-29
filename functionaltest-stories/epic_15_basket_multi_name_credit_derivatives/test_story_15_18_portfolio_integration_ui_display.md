# Story 15.18 - Portfolio Integration UI Display

## Objective
Validate front-end integration displaying basket & tranche positions in aggregated portfolio pages: correct icons, tooltips, metric formatting, grouping toggles and dark mode accessibility.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-18-001 | Basket icon displayed with tooltip explaining structure | UI | @UI @TOOLTIP |
| FT-15-18-002 | Tranche position shows attachment-detachment range | UI | @UI @TRANCHE |
| FT-15-18-003 | Grouping toggle merges basket constituents count display | UI | @UI @GROUPING |
| FT-15-18-004 | Metric formatting (DV01, CS01) uses consistent decimals | UI | @UI @NUMERIC |
| FT-15-18-005 | Dark mode contrast meets WCAG AA for icons/text | UI | @ACCESSIBILITY @DARKMODE |
| FT-15-18-006 | Keyboard navigation reaches basket & tranche rows | UI | @ACCESSIBILITY @KEYBOARD |
| FT-15-18-007 | Screen reader announces tranche interval properly | UI | @ACCESSIBILITY @A11Y |
| FT-15-18-008 | Performance: initial render latency under threshold | UI | @PERFORMANCE @LATENCY |
| FT-15-18-009 | Error placeholder shown if metrics API fails | UI | @ERROR @RESILIENCE |
| FT-15-18-010 | Deterministic snapshot: render tree structure stable | UI | @DETERMINISM @SNAPSHOT |
| FT-15-18-011 | Drift: tooltip text hash stable | UI | @DRIFT @TEXT |

## Automation Strategy
1. Load portfolio page with seeded basket/tranche positions.
2. Assert icon existence & tooltip content via hover.
3. Verify tranche interval text (e.g., 3%-7%).
4. Toggle grouping; verify constituent count merges.
5. Capture metrics cell text; assert decimal formatting (e.g., 2 decimals).
6. Switch to dark mode; run contrast checks (axe/core). Ensure pass.
7. Keyboard tab sequence includes new rows; assert focus outline & aria-label announcements.
8. Measure time from navigation to first meaningful paint for performance.
9. Simulate metrics API failure; assert placeholder & retry button.
10. Snapshot DOM subtree for determinism; compare hashed structure.
11. Tooltip text hash stability check.

## Metrics
- portfolioPageRenderLatency
- accessibilityA11yViolationsCount

## Exit Criteria
UI integrates positions with proper formatting/accessibility; performance within bounds; determinism & drift stable.
