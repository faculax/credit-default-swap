# Functional Test Story 14.15 – Performance Batch Pricing Preparation

Trace: story_14_15_performance-batch-pricing-preparation
Tags: @EPIC_14 @CREDIT_BONDS @PERFORMANCE

## Objective
Validate preparatory steps for batch pricing (preloading curves, caching schedules) improve performance metrics.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-15-001 | Preload curves reduces latency | Engine | @PERFORMANCE |
| FT-14-15-002 | Cache schedules reduces latency | Engine | @PERFORMANCE |
| FT-14-15-003 | Unauthorized preload -> 403 | API | @SECURITY |
| FT-14-15-004 | Drift performance baseline | Engine | @DRIFT |
| FT-14-15-005 | Logging redacts cache keys | API | @SECURITY |
| FT-14-15-006 | Metrics batchPricingLatency | Engine | @METRICS |
| FT-14-15-007 | Performance p95 after optimization | Engine | @PERFORMANCE |
| FT-14-15-008 | Concurrency multi-preload stability | Engine | @CONCURRENCY |
| FT-14-15-009 | Edge large batch still optimized | Engine | @SCALING |
| FT-14-15-010 | Contract batch pricing schema stable | Contract | @CONTRACT |
| FT-14-15-011 | Export optimization report JSON | API | @EXPORT |
| FT-14-15-012 | Fallback on cache miss | Engine | @RESILIENCE |

## Automation Strategy
Run batch with and without optimizations; compare latency; export report.
