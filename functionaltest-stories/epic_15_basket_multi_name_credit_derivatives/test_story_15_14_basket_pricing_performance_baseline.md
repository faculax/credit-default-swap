# Story 15.14 - Basket Pricing Performance Baseline

## Objective
Establish and validate baseline performance metrics for basket pricing engine: latency distributions, memory usage ceiling, path reuse hit ratio, and scaling behavior as constituents increase; create artifacts for future drift comparisons.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-14-001 | Price basket with 5 names: capture latency & memory | Domain/API | @PERFORMANCE @BASELINE |
| FT-15-14-002 | Scale to 20 names: latency growth within expected factor | Domain | @SCALING @NUMERIC |
| FT-15-14-003 | Path reuse hit ratio > configured minimum | Domain | @OPTIMIZATION @PATH_REUSE |
| FT-15-14-004 | Memory footprint peak below threshold | Domain | @PERFORMANCE @MEMORY |
| FT-15-14-005 | Deterministic seed produces identical fair spread | Domain | @DETERMINISM @REPRODUCIBILITY |
| FT-15-14-006 | Unauthorized pricing attempt returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-14-007 | Rate limit engaged for burst pricing requests | API | @SECURITY @RATE_LIMIT |
| FT-15-14-008 | JSON schema stable for pricing response | API | @SCHEMA @STABILITY |
| FT-15-14-009 | Drift artifacts stored (latency distribution JSON) | Domain | @DRIFT @ARTIFACT |
| FT-15-14-010 | Accessibility: performance chart has proper roles | UI | @ACCESSIBILITY @A11Y |
| FT-15-14-011 | Error handling: invalid basket ID returns 404 structured error | API | @ERROR @NEGATIVE |
| FT-15-14-012 | Correlation bump scenario performance recorded | Domain | @CORRELATION @PERFORMANCE |

## Automation Strategy
1. Execute pricing with basket sizes [5, 10, 20]; record latency metrics (time start/end) & collect memory (heap snapshot or API metric).
2. Record path reuse hit ratio from metrics endpoint.
3. Store baseline artifact JSON (latency histogram, memory peak) in fixtures.
4. Re-run with fixed seed for determinism fairness spread check.
5. Negative tests (invalid ID 404, unauthorized 403, rate limit 429).
6. Validate response schema hash stable.
7. UI: open performance dashboard; verify chart roles & aria attributes via axe.
8. Correlation bump: execute scenario with +10% correlation; capture comparative latency.
9. Ensure scaling growth factor (latency_20 / latency_5) <= expected (e.g., 4x).

## Metrics
- basketPricingLatency
- basketPricingMemoryPeak
- basketPathReuseHitRatio

## Thresholds
- p95 latency (5 names) < 500ms; path reuse hit ratio > 0.60; memory peak < 250MB

## Fixtures
- `fixtures/basket/perf/FT-15-14-baseline-5.json`
- `fixtures/basket/perf/FT-15-14-baseline-20.json`

## Exit Criteria
Baseline artifacts created & stored; metrics within defined thresholds; deterministic pricing validated.
