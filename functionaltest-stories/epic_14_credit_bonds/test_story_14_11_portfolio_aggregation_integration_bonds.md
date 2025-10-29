# Functional Test Story 14.11 – Portfolio Aggregation Integration (Bonds)

Trace: story_14_11_portfolio-aggregation-integration-bonds
Tags: @EPIC_14 @CREDIT_BONDS @AGGREGATION

## Objective
Validate bond portfolio aggregation integration returns correct aggregated metrics (total PV, avg yield, spread distribution).

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-11-001 | Aggregate PV across portfolio | Engine | @NUMERIC |
| FT-14-11-002 | Compute average yield | Engine | @NUMERIC |
| FT-14-11-003 | Spread bucket distribution | Engine | @NUMERIC |
| FT-14-11-004 | Unauthorized aggregation -> 403 | API | @SECURITY |
| FT-14-11-005 | Drift aggregation baseline | Engine | @DRIFT |
| FT-14-11-006 | Logging redacts instrument IDs | API | @SECURITY |
| FT-14-11-007 | Metrics aggregationLatency | Engine | @METRICS |
| FT-14-11-008 | Performance p95 aggregation latency | Engine | @PERFORMANCE |
| FT-14-11-009 | Contract aggregation schema stable | Contract | @CONTRACT |
| FT-14-11-010 | Accessibility aggregation UI | E2E | @ACCESSIBILITY |
| FT-14-11-011 | Concurrency multi-aggregation isolation | Engine | @CONCURRENCY |
| FT-14-11-012 | Edge empty portfolio | Engine | @EDGE |
| FT-14-11-013 | Edge single bond portfolio | Engine | @EDGE |
| FT-14-11-014 | Export aggregation metrics JSON | API | @EXPORT |
| FT-14-11-015 | Large portfolio scaling | Engine | @SCALING |
| FT-14-11-016 | Filtered aggregation subset | API | @FILTER |

## Automation Strategy
Aggregation harness; baseline metrics compare; scaling test; filtered subset validation.
