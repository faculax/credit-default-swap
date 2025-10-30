# Functional Test Story 16.15 – Performance Batch Pricing Preparation

Trace: story_16_15_performance-batch-pricing-preparation
Tags: @EPIC_16 @CREDIT_BONDS @PERFORMANCE

## Objective
Validate preparatory steps for batch pricing (preloading curves, caching schedules) improve performance metrics.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-15-001 | Preload curves reduces latency | Engine | @PERFORMANCE |
| FT-16-15-002 | Cache schedules reduces latency | Engine | @PERFORMANCE |
| FT-16-15-003 | Unauthorized preload -> 403 | API | @SECURITY |
| FT-16-15-004 | Drift performance baseline | Engine | @DRIFT |
| FT-16-15-005 | Logging redacts cache keys | API | @SECURITY |
| FT-16-15-006 | Metrics batchPricingLatency | Engine | @METRICS |
| FT-16-15-007 | Performance p95 after optimization | Engine | @PERFORMANCE |
| FT-16-15-008 | Concurrency multi-preload stability | Engine | @CONCURRENCY |
| FT-16-15-009 | Edge large batch still optimized | Engine | @SCALING |
| FT-16-15-010 | Contract batch pricing schema stable | Contract | @CONTRACT |
| FT-16-15-011 | Export optimization report JSON | API | @EXPORT |
| FT-16-15-012 | Fallback on cache miss | Engine | @RESILIENCE |

## Automation Strategy
Run batch with and without optimizations; compare latency; export report.
