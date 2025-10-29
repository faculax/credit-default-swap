# Story 15.15 - Diagnostics & Metrics Documentation

## Objective
Ensure basket & tranche diagnostics and emitted metrics are fully documented, discoverable, and test that documentation examples match live API / metrics payloads; validate no stale or missing fields.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-15-001 | Metrics endpoint lists basketPricingLatency with units | API | @METRICS @DOCUMENTATION |
| FT-15-15-002 | Diagnostics endpoint returns solver iterations field documented | API | @DIAGNOSTICS @CONSISTENCY |
| FT-15-15-003 | Documentation page includes correlation matrix example matching live response | UI/API | @DOCS @CORRELATION |
| FT-15-15-004 | Missing field detection: all live metrics present in docs table | API | @DOCS @VALIDATION |
| FT-15-15-005 | Stale doc detection: removed metric triggers doc mismatch alert | API | @DOCS @NEGATIVE |
| FT-15-15-006 | Accessibility: documentation tables have headers role semantics | UI | @ACCESSIBILITY @A11Y |
| FT-15-15-007 | Unauthorized access to diagnostics returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-15-008 | Rate limit metrics polling yields 429 after threshold | API | @SECURITY @RATE_LIMIT |
| FT-15-15-009 | Deterministic metrics snapshot (fixed seed) matches documented example values (range tolerance) | Domain/API | @DETERMINISM @SNAPSHOT |
| FT-15-15-010 | Schema stability: diagnostics payload hash unchanged | API | @SCHEMA @STABILITY |
| FT-15-15-011 | Error logging: documentation mismatch recorded with specific code DOC_MISMATCH | Domain | @LOGGING @ERROR |
| FT-15-15-012 | Performance: metrics endpoint latency baseline < threshold | API | @PERFORMANCE @LATENCY |

## Automation Strategy
1. Fetch metrics endpoint; assert presence & units for basketPricingLatency.
2. Fetch diagnostics; confirm solverIterations present.
3. Load documentation page (Playwright); extract correlation matrix example; compare keys & shape to live endpoint.
4. Build set comparisons: live metrics vs documented metrics -> expect equality.
5. Simulate stale doc by mocking removed metric; assert mismatch alert and log code.
6. Accessibility audit for docs tables (thead/th roles semantics, contrast).
7. Security tests (403 unauthorized; 429 rate limit on polling).
8. Deterministic snapshot: run with fixed seed; compare documented example tolerance (e.g., +/-5%).
9. Schema hash stability validation.
10. Performance latency measurement for metrics call.

## Metrics
- diagnosticsEndpointLatency
- documentationMismatchCount
- metricsPollingRateLimitCount

## Fixtures
- `fixtures/docs/FT-15-15-metrics-example.json`
- `fixtures/docs/FT-15-15-diagnostics-example.json`

## Thresholds
- Metrics latency p95 < 200ms
- Documentation mismatchCount must remain 0 (except intentional negative test)

## Exit Criteria
All live metrics & diagnostics fields documented; no mismatches; accessibility & performance pass; schema stable.
