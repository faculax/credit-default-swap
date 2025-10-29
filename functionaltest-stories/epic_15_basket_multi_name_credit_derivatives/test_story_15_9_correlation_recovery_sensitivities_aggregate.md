# Functional Test Story 15.9 – Correlation & Recovery Sensitivities Aggregate

Trace: story_15_9_correlation-recovery-sensitivities-aggregate
Tags: @EPIC_15 @BASKET @SENSITIVITY

## Objective
Validate aggregation of correlation and recovery sensitivities across basket constituents.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-9-001 | Aggregate correlation sensitivities | Engine | @NUMERIC |
| FT-15-9-002 | Aggregate recovery sensitivities | Engine | @NUMERIC |
| FT-15-9-003 | Unauthorized sensitivity aggregate -> 403 | API | @SECURITY |
| FT-15-9-004 | Drift sensitivity aggregate baseline | Engine | @DRIFT |
| FT-15-9-005 | Logging redacts raw sensitivity details | API | @SECURITY |
| FT-15-9-006 | Metrics corrSensCalcLatency | Engine | @METRICS |
| FT-15-9-007 | Performance p95 aggregate latency | Engine | @PERFORMANCE |
| FT-15-9-008 | Contract sensitivity aggregate schema stable | Contract | @CONTRACT |
| FT-15-9-009 | Concurrency multi-aggregate isolation | Engine | @CONCURRENCY |
| FT-15-9-010 | Edge degenerate zero correlation | Engine | @EDGE |
| FT-15-9-011 | Edge perfect correlation | Engine | @EDGE |
| FT-15-9-012 | Export sensitivity aggregate JSON | API | @EXPORT |
| FT-15-9-013 | Parameter validation errors aggregated | API | @NEGATIVE |
| FT-15-9-014 | Large constituent count scaling | Engine | @SCALING |
| FT-15-9-015 | Deterministic seed reproducibility | Engine | @DETERMINISM |
| FT-15-9-016 | Negative recovery override classification | Engine | @NEGATIVE |

## Automation Strategy
Aggregation harness baseline compare; edge correlation extremes; scaling test; export JSON.
