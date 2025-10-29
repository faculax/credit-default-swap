# Functional Test Story 13.3 – Retrieve Portfolio Metrics

Trace: story_13_3_retrieve-portfolio-metrics
Tags: @EPIC_13 @SIMULATION @METRICS

## Objective
Validate retrieval and correctness of aggregated simulation portfolio metrics (loss statistics, percentiles, PFE, EE).

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-3-001 | Fetch metrics post-completion | API | @API |
| FT-13-3-002 | Percentile calculation correctness | Engine | @NUMERIC |
| FT-13-3-003 | Mean/median loss values | Engine | @NUMERIC |
| FT-13-3-004 | PFE calculation vs definition | Engine | @NUMERIC |
| FT-13-3-005 | EE calculation vs definition | Engine | @NUMERIC |
| FT-13-3-006 | Concurrency fetch stable | API | @CONCURRENCY |
| FT-13-3-007 | Deterministic seed metrics reproducibility | Engine | @DETERMINISM |
| FT-13-3-008 | Export metrics JSON | API | @EXPORT |
| FT-13-3-009 | Performance fetch latency < threshold | API | @PERFORMANCE |
| FT-13-3-010 | Unauthorized metrics access -> 403 | API | @SECURITY |
| FT-13-3-011 | Drift baseline metrics | Engine | @DRIFT |
| FT-13-3-012 | Logging redacts portfolio IDs | API | @SECURITY |
| FT-13-3-013 | Invalid runId -> 404 | API | @NEGATIVE |
| FT-13-3-014 | Accessibility metrics UI table | E2E | @ACCESSIBILITY |
| FT-13-3-015 | Metrics emission (lossMean) | Engine | @METRICS |
| FT-13-3-016 | Time zone normalized timestamps | API | @TIME |
| FT-13-3-017 | High path count metrics integrity | Engine | @SCALING |
| FT-13-3-018 | Percentile interpolation correctness | Engine | @NUMERIC |
| FT-13-3-019 | Negative losses (gains) handled | Engine | @EDGE |
| FT-13-3-020 | API contract stable | Contract | @CONTRACT |

## Automation Strategy
Engine statistical verification vs pre-computed baseline; API schema snapshot.
