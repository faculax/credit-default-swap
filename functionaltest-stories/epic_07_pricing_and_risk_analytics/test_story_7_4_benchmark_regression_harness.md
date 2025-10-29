# Functional Test Story 7.4 – Benchmark Regression Harness

Trace: story_7_4_benchmark-regression-harness
Tags: @EPIC_07 @STORY_7_4 @RISK @REGRESSION

## Objective
Detect performance & numerical drift across risk engine updates using benchmark baselines.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-7-4-001 | Load performance baseline | Engine | @SETUP |
| FT-7-4-002 | Measure current run timings | Engine | @PERFORMANCE |
| FT-7-4-003 | Compare p50 latency within threshold | Engine | @PERFORMANCE |
| FT-7-4-004 | Compare p95 latency within threshold | Engine | @PERFORMANCE |
| FT-7-4-005 | Numerical drift PV outside tolerance fails | Engine | @DRIFT |
| FT-7-4-006 | Numerical drift CS01 outside tolerance fails | Engine | @DRIFT |
| FT-7-4-007 | Accept annotated expected drift | Engine | @WAIVER |
| FT-7-4-008 | Record new baseline on approval flag | Engine | @BASELINE |
| FT-7-4-009 | Memory allocation profile stable | Engine | @MEMORY |
| FT-7-4-010 | GC pauses within limit | Engine | @MEMORY |
| FT-7-4-011 | CPU utilization below ceiling | Engine | @PERFORMANCE |
| FT-7-4-012 | Concurrency scaling linearity check | Engine | @SCALING |
| FT-7-4-013 | Regression report artifact stored | Engine | @REPORT |
| FT-7-4-014 | Slack/notification on regression fail | Engine | @NOTIFY |
| FT-7-4-015 | Baseline version tagging semantic | Engine | @VERSIONING |
| FT-7-4-016 | Historical baseline retrieval | Engine | @HISTORY |
| FT-7-4-017 | Warmup runs excluded | Engine | @SETUP |
| FT-7-4-018 | Outlier detection (3σ rule) | Engine | @ANALYTICS |
| FT-7-4-019 | Logging redacts machine hostnames | Engine | @SECURITY |
| FT-7-4-020 | Metrics regressionFailureCount | Engine | @METRICS |

## Automation Strategy
Benchmark harness with JMH-like simple wrapper; store JSON stats; compare using thresholds config.
