# Functional Test Story 16.11 – Portfolio Aggregation Integration (Bonds)

Trace: story_16_11_portfolio-aggregation-integration-bonds
Tags: @EPIC_16 @CREDIT_BONDS @AGGREGATION

## Objective
Validate bond portfolio aggregation integration returns correct aggregated metrics (total PV, avg yield, spread distribution).

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-11-001 | Aggregate PV across portfolio | Engine | @NUMERIC |
| FT-16-11-002 | Compute average yield | Engine | @NUMERIC |
| FT-16-11-003 | Spread bucket distribution | Engine | @NUMERIC |
| FT-16-11-004 | Unauthorized aggregation -> 403 | API | @SECURITY |
| FT-16-11-005 | Drift aggregation baseline | Engine | @DRIFT |
| FT-16-11-006 | Logging redacts instrument IDs | API | @SECURITY |
| FT-16-11-007 | Metrics aggregationLatency | Engine | @METRICS |
| FT-16-11-008 | Performance p95 aggregation latency | Engine | @PERFORMANCE |
| FT-16-11-009 | Contract aggregation schema stable | Contract | @CONTRACT |
| FT-16-11-010 | Accessibility aggregation UI | E2E | @ACCESSIBILITY |
| FT-16-11-011 | Concurrency multi-aggregation isolation | Engine | @CONCURRENCY |
| FT-16-11-012 | Edge empty portfolio | Engine | @EDGE |
| FT-16-11-013 | Edge single bond portfolio | Engine | @EDGE |
| FT-16-11-014 | Export aggregation metrics JSON | API | @EXPORT |
| FT-16-11-015 | Large portfolio scaling | Engine | @SCALING |
| FT-16-11-016 | Filtered aggregation subset | API | @FILTER |

## Automation Strategy
Aggregation harness; baseline metrics compare; scaling test; filtered subset validation.
