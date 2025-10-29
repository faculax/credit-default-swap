# Story 15.17 - Batch Pricing UI Phase B

## Objective
Extend batch pricing UI (Phase B) with advanced controls: correlation bump sets, recovery override toggles, seed reproducibility checkbox, and progress segmentation; validate UI -> API payload mapping & progress streaming integrity.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-17-001 | Correlation bump set selection updates payload | UI/API | @CORRELATION @UI |
| FT-15-17-002 | Recovery override toggle adds override field | UI/API | @RECOVERY @UI |
| FT-15-17-003 | Seed reproducibility checkbox fixes seed value | UI | @DETERMINISM @UI |
| FT-15-17-004 | Progress segmented by batches displayed | UI | @UI @PROGRESS |
| FT-15-17-005 | Streaming events ordered by timestamp | API | @STREAM @ORDER |
| FT-15-17-006 | Unauthorized subscription blocked (403) | API | @SECURITY @NEGATIVE |
| FT-15-17-007 | Rate limit on starting multiple batches rapidly | API | @SECURITY @RATE_LIMIT |
| FT-15-17-008 | Accessibility: progress region announces updates politely | UI | @ACCESSIBILITY @A11Y |
| FT-15-17-009 | Deterministic seed yields identical result summary JSON | Domain | @DETERMINISM @SNAPSHOT |
| FT-15-17-010 | Performance: stream first event latency < threshold | API | @PERFORMANCE @LATENCY |
| FT-15-17-011 | Drift detection: batch pricing summary schema stable | API | @SCHEMA @STABILITY |
| FT-15-17-012 | Error handling: invalid bump set triggers validation error | API | @VALIDATION @NEGATIVE |

## Automation Strategy
1. Configure UI controls; intercept outgoing request; assert payload fields (correlationBumps, recoveryOverride, seedFixed).
2. Start batch pricing; subscribe to progress; verify incremental segmentation markers.
3. Collect event timestamps; assert ordering & monotonicity.
4. Run deterministic seed twice; compare summary JSON (hash equality).
5. Negative tests (invalid bump set; unauthorized; rate limit).
6. Accessibility audit for progress live region (aria-live polite, role status).
7. Measure first event latency.
8. Schema hash stability check.

## Metrics
- batchPricingFirstEventLatency
- batchPricingSeedDeterminismFailures

## Exit Criteria
Advanced controls functional; streaming consistent; determinism & accessibility validated.
