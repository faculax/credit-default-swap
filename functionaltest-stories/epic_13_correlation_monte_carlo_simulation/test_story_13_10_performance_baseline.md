# Functional Test Story 13.10 – Performance Baseline

Trace: story_13_10_performance-baseline
Tags: @EPIC_13 @SIMULATION @PERFORMANCE

## Objective
Establish and monitor performance baselines for simulation runs at various path counts.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-10-001 | Baseline 10k paths time capture | Engine | @BASELINE |
| FT-13-10-002 | Baseline 50k paths time capture | Engine | @BASELINE |
| FT-13-10-003 | Baseline 100k paths time capture | Engine | @BASELINE |
| FT-13-10-004 | p50 latency threshold | Engine | @PERFORMANCE |
| FT-13-10-005 | p95 latency threshold | Engine | @PERFORMANCE |
| FT-13-10-006 | Memory usage profile | Engine | @PERFORMANCE |
| FT-13-10-007 | CPU utilization profile | Engine | @PERFORMANCE |
| FT-13-10-008 | GC pause time | Engine | @PERFORMANCE |
| FT-13-10-009 | Concurrency scaling factor | Engine | @SCALING |
| FT-13-10-010 | Drift baseline comparison | Engine | @DRIFT |
| FT-13-10-011 | Metrics performanceRegressionCount | Engine | @METRICS |
| FT-13-10-012 | Reporting baseline artifact JSON | Engine | @REPORT |
| FT-13-10-013 | Logging redacts hostnames | Engine | @SECURITY |
| FT-13-10-014 | SLA p95 < threshold enforced | Engine | @PERFORMANCE |
| FT-13-10-015 | Warmup excluded from metrics | Engine | @SETUP |
| FT-13-10-016 | Accessibility performance dashboard UI | E2E | @ACCESSIBILITY |

## Automation Strategy
Timed harness; baseline JSON stored; compare new runs; fail on regression.
