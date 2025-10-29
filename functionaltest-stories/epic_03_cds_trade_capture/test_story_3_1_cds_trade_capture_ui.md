# Functional Test Story 3.1 – Trade Capture UI

Trace: story_3_1_cds_trade_capture_ui
Tags: @EPIC_03 @STORY_3_1 @TRADE @CRITPATH @ACCESSIBILITY

## Objective
Validate user can capture a new CDS trade with complete, correctly validated field set, and receive visual confirmation of success.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-3-1-001 | Open trade ticket from blotter | E2E | @UI |
| FT-3-1-002 | Mandatory field indicators present | E2E | @UI @VALIDATION |
| FT-3-1-003 | ISIN auto-resolves reference entity | E2E | @REFDATA |
| FT-3-1-004 | Numeric formatting (grouping, bp) | E2E | @FORMAT |
| FT-3-1-005 | Validation: Notional > 0 enforced | E2E | @NEGATIVE |
| FT-3-1-006 | Validation: Spread bounds | E2E | @NEGATIVE |
| FT-3-1-007 | Date ordering (Trade ≤ Effective ≤ Maturity) | E2E | @NEGATIVE |
| FT-3-1-008 | Restructuring clause enum mapping displays labels | E2E | @UI |
| FT-3-1-009 | Save disabled until all validations pass | E2E | @UX |
| FT-3-1-010 | Successful submit closes modal & toast appears | E2E | @CRITPATH |
| FT-3-1-011 | Server 400 maps inline to fields | E2E | @NEGATIVE |
| FT-3-1-012 | Transient 502 retried with backoff | E2E | @RESILIENCE |
| FT-3-1-013 | Expired token triggers refresh & resume | E2E | @SECURITY |
| FT-3-1-014 | Role without permission hides New Trade button | E2E | @SECURITY |
| FT-3-1-015 | Unsaved changes navigation guard | E2E | @UX |
| FT-3-1-016 | Accessibility axe: no critical violations | E2E | @ACCESSIBILITY |
| FT-3-1-017 | Performance: form load <1s cold | E2E | @PERFORMANCE |
| FT-3-1-018 | Localization fallback (US date) | E2E | @I18N |

## Detailed Scenario Example
### FT-3-1-005 Validation Notional > 0
Preconditions: User authenticated role=TRADER.
Steps:
1. Open trade ticket.
2. Enter notional 0.
3. Attempt submit.
Assertions:
- Submit disabled OR inline error "Must be > 0".
- No network call executed.

(Other scenarios follow same pattern.)

## Negative / Edge Additional
- FT-3-1-N01 Extremely large notional 1e13 rejected (server 400)
- FT-3-1-N02 Spread precision >3 decimals rounds client-side
- FT-3-1-N03 Double-click submit -> single POST (idempotency key)

## Test Data
| Label | ISIN | Entity | Currency | Recovery |
|-------|------|--------|----------|----------|
| T1 | US0000000001 | ACME CORP | USD | 0.40 |
| T2 | GB0000000002 | BETA PLC | GBP | 0.35 |

## Automation Strategy
Playwright spec: `frontend/tests/e2e/epic_03/tradeCapture.spec.ts`
- Use route interception to simulate 400, 502, token refresh.
- Accessibility via `@axe-core/playwright`.
- Performance measure using `page.metrics()` & timestamps.

## Metrics & Instrumentation
- Capture form load time (navigation start -> first paint of modal root).
- Count validation error occurrences.

## Open Questions
- Confirm allowable spread bounds config source (env vs API).
