# Story 15.19 - Sensitivities Toggle & Display

## Objective
Validate UI and API behavior when toggling sensitivities visibility for basket & tranche positions; ensure numeric formatting, performance, accessibility and caching integrity.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-19-001 | Sensitivities hidden by default until toggle on | UI | @UI @DEFAULTS |
| FT-15-19-002 | Toggle exposes DV01, CS01 columns | UI | @UI @RISK |
| FT-15-19-003 | Numeric formatting consistent (thousand separators) | UI | @NUMERIC @FORMAT |
| FT-15-19-004 | Performance: toggle latency < threshold | UI | @PERFORMANCE @LATENCY |
| FT-15-19-005 | Cache: second toggle on uses cached data (no API call) | UI/API | @CACHE @OPTIMIZATION |
| FT-15-19-006 | Accessibility: toggle button has aria-pressed state | UI | @ACCESSIBILITY @A11Y |
| FT-15-19-007 | Deterministic values stable with fixed seed | Domain | @DETERMINISM @SNAPSHOT |
| FT-15-19-008 | Drift detection: sensitivities hash stable | Domain | @DRIFT @NUMERIC |
| FT-15-19-009 | Unauthorized risk data fetch returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-19-010 | Rate limit engaged for rapid toggles causing fetches | API | @SECURITY @RATE_LIMIT |
| FT-15-19-011 | Error handling: API failure shows fallback column placeholder | UI | @ERROR @RESILIENCE |

## Automation Strategy
1. Load page; confirm sensitivities columns absent initially.
2. Click toggle; assert columns appear & numeric formatting (regex).
3. Capture network requests; confirm cache usage on second toggle cycle.
4. Measure toggle latency event -> columns visible.
5. Deterministic run with fixed seed; compare DV01 snapshot.
6. Drift: hash DV01/CS01 array vs baseline fixture.
7. Accessibility: check aria-pressed true/false states.
8. Negative tests (403 unauthorized token; rate limit with rapid toggles).
9. Inject API failure; confirm placeholder rendering.

## Metrics
- sensitivitiesToggleLatency
- sensitivitiesCacheHitRatio

## Exit Criteria
Toggle works, cached efficiently, deterministic & accessible, numeric formatting correct.
