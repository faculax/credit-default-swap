# Story 15.11 - Portfolio Aggregation Integration

## Objective
Validate that basket derivative positions (first-to-default, n-th, and tranche placeholders) appear correctly in portfolio aggregation views: position listing, exposure metrics, risk sensitivity roll-ups, correlation contribution, and valuation summaries; ensure tagging, filtering, and drill-down preserve consistency and determinism.

## Scope & Layers
- UI: Portfolio list, position detail drawer, aggregated metrics panel
- API: /portfolio/positions, /portfolio/metrics, /basket/{id}, /risk/aggregate
- Domain: Aggregation calculators, sensitivity merging, correlation matrix integration
- Data: Persisted basket/tranche position rows, cached metrics snapshots

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-11-001 | Basket position appears in portfolio list with correct type badge | UI | @UI @PORTFOLIO @BASKET |
| FT-15-11-002 | Drill-down loads basket constituents with weights | UI/API | @UI @DETAIL @WEIGHTS |
| FT-15-11-003 | Aggregated notional sums include basket notional once (no double-count) | Domain/API | @NUMERIC @AGGREGATION @INTEGRITY |
| FT-15-11-004 | DV01 roll-up includes basket path sensitivities merged | Domain | @RISK @SENSITIVITY |
| FT-15-11-005 | Correlation contribution panel shows basket correlation impact | UI/Domain | @CORRELATION @UI |
| FT-15-11-006 | Basket excluded via type filter removes its metrics from totals | UI | @FILTER @UI @AGGREGATION |
| FT-15-11-007 | Unauthorized user cannot view basket details (403) | API | @SECURITY @NEGATIVE |
| FT-15-11-008 | Export portfolio CSV includes basket position line | API | @EXPORT @CSV @CONTRACT |
| FT-15-11-009 | JSON export stable schema (hash unchanged) | API | @SCHEMA @STABILITY @DRIFT |
| FT-15-11-010 | Performance baseline: portfolio metrics endpoint p95 < threshold | API | @PERFORMANCE @LATENCY |
| FT-15-11-011 | Deterministic seed produces identical aggregated sensitivities snapshot | Domain | @DETERMINISM @SNAPSHOT |
| FT-15-11-012 | Constituents weight edit propagates to aggregation after refresh | UI/API | @WEIGHTS @REFRESH |
| FT-15-11-013 | Rate-limited user gets 429 for rapid metrics polling | API | @SECURITY @RATE_LIMIT |
| FT-15-11-014 | Accessibility: basket badge announces role via aria-label | UI | @ACCESSIBILITY @A11Y |
| FT-15-11-015 | Error handling: broken constituent reference logged & surfaced gracefully | Domain/API | @ERROR @RESILIENCE |

## Automation Strategy
1. Seed fixture: create basket with 5 constituents + sensitivities baseline JSON.
2. Use Playwright to navigate portfolio list; assert badges, drill-down detail weights.
3. Fetch metrics before and after filter toggles; compute totals locally to ensure inclusion/exclusion logic.
4. Validate export artifacts (CSV row presence; JSON schema hash).
5. Performance: measure latency with repeated calls (warm cache vs cold) capturing p50/p95.
6. Determinism: re-run aggregation with same seed and compare JSON snapshots (deep equality).
7. Security: simulate unauthorized API token; expect 403; rate limit test with burst requests expecting 429.
8. Accessibility: axe-core validation for badge element & aria-label semantics.
9. Negative/resilience: inject invalid constituent ID; expect graceful error object and log entry (mock/spy).

## Test Data
- Basket constituents: ISIN set BSK-SET-15-11 (5 names, varied notionals)
- Baseline sensitivity snapshot file: `fixtures/basket/FT-15-11-sens-baseline.json`
- Thresholds: p95 latency < 450ms, schema hash stable across runs

## Metrics & Observability
- portfolioAggregationLatency
- basketSensitivityMergeCount
- basketAggregationErrorCount

## Exit Criteria
All assertions pass; no drift detected; performance within threshold; accessibility audit passes with zero critical issues.
