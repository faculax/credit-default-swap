# Functional Test Story 15.10 – Batch Pricing Path Reuse

Trace: story_15_10_batch-pricing-path-reuse
Tags: @EPIC_15 @BASKET @PERFORMANCE

## Objective
Validate batch pricing path reuse mechanism improves performance (hit ratio, latency) without altering results.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-10-001 | Baseline batch pricing runtime | Engine | @BASELINE |
| FT-15-10-002 | Path reuse enabled runtime | Engine | @PERFORMANCE |
| FT-15-10-003 | Unauthorized path reuse toggle -> 403 | API | @SECURITY |
| FT-15-10-004 | Drift performance baseline | Engine | @DRIFT |
| FT-15-10-005 | Logging redacts cache keys | API | @SECURITY |
| FT-15-10-006 | Metrics pathReuseHitRatio | Engine | @METRICS |
| FT-15-10-007 | Performance p95 latency improvement | Engine | @PERFORMANCE |
| FT-15-10-008 | Contract batch pricing schema stable | Contract | @CONTRACT |
| FT-15-10-009 | Concurrency multi-batch isolation | Engine | @CONCURRENCY |
| FT-15-10-010 | Edge large batch scaling | Engine | @SCALING |
| FT-15-10-011 | Export performance comparison report | API | @EXPORT |
| FT-15-10-012 | Cache invalidation correctness | Engine | @RESILIENCE |
| FT-15-10-013 | Path reuse disabled fallback | Engine | @RESILIENCE |
| FT-15-10-014 | Numeric result parity reuse vs baseline | Engine | @PARITY |

## Automation Strategy
Benchmark runs with/without reuse; compare latency & parity; export performance report JSON.
