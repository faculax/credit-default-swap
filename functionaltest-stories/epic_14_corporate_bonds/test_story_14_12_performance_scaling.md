# Functional Test Story 14.12 – Performance & Scaling

Trace: story_14_12_performance-scaling
Tags: @EPIC_14 @CORPORATE_BONDS @PERFORMANCE

## Objective
Validate performance and scaling characteristics for batch pricing and risk calculations across large bond portfolios.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-12-001 | Baseline batch pricing 100 bonds | Engine | @BASELINE |
| FT-14-12-002 | Batch pricing 500 bonds | Engine | @SCALING |
| FT-14-12-003 | Batch pricing 1000 bonds | Engine | @SCALING |
| FT-14-12-004 | Unauthorized batch request -> 403 | API | @SECURITY |
| FT-14-12-005 | Drift batch runtime baseline | Engine | @DRIFT |
| FT-14-12-006 | Logging redacts instrument list | API | @SECURITY |
| FT-14-12-007 | Metrics batchPricingLatency | Engine | @METRICS |
| FT-14-12-008 | Performance p95 batch latency | Engine | @PERFORMANCE |
| FT-14-12-009 | Memory usage within bounds | Engine | @PERFORMANCE |
| FT-14-12-010 | Accessibility batch UI | E2E | @ACCESSIBILITY |
| FT-14-12-011 | Concurrency multiple batches isolation | Engine | @CONCURRENCY |
| FT-14-12-012 | Edge extremely large batch rejection | Engine | @NEGATIVE |

## Automation Strategy
Batch pricing harness; resource profiling; baseline and drift comparison.
