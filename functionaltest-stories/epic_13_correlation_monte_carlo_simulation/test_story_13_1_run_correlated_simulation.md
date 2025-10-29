# Functional Test Story 13.1 – Run Correlated Simulation

Trace: story_13_1_run-correlated-simulation
Tags: @EPIC_13 @SIMULATION @CRITPATH

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-1-001 | Submit simulation request | API | @SUBMIT |
| FT-13-1-002 | Validate correlation matrix symmetry | Engine | @VALIDATION |
| FT-13-1-003 | PSD check (eigenvalues >= 0) | Engine | @VALIDATION |
| FT-13-1-004 | Path generation baseline | Engine | @NUMERIC |
| FT-13-1-005 | Invalid PSD matrix rejection | Engine | @NEGATIVE |
| FT-13-1-006 | Sampling distribution sanity (mean hazard) | Engine | @STATISTICS |
| FT-13-1-007 | Portfolio loss aggregation base | Engine | @AGGREGATION |
| FT-13-1-008 | Performance 10k paths < threshold | Engine | @PERFORMANCE |
| FT-13-1-009 | Deterministic seed reproduction | Engine | @DETERMINISM |
| FT-13-1-010 | Cancel mid-run | Engine | @CANCELLATION |
| FT-13-1-011 | Metrics pathsPerSecond | Engine | @METRICS |
| FT-13-1-012 | Logging redacts random seed | Engine | @SECURITY |
| FT-13-1-013 | API returns runId | API | @API |
| FT-13-1-014 | API contract stable | Contract | @CONTRACT |
| FT-13-1-015 | Memory usage under threshold | Engine | @PERFORMANCE |
| FT-13-1-016 | Export raw loss distribution | API | @EXPORT |
| FT-13-1-017 | Retry transient submission failure | API | @RESILIENCE |
| FT-13-1-018 | Concurrency multi-run isolation | Engine | @CONCURRENCY |
| FT-13-1-019 | Time zone normalized timestamps | API | @TIME |
| FT-13-1-020 | Accessibility run form (UI) | E2E | @ACCESSIBILITY |
| FT-13-1-021 | Progress endpoint polling | API | @PROGRESS |
| FT-13-1-022 | Drift baseline distribution check | Engine | @DRIFT |
| FT-13-1-023 | High correlation 0.99 cluster behavior | Engine | @EDGE |
| FT-13-1-024 | Low correlation ~0 independence behavior | Engine | @EDGE |
| FT-13-1-025 | Negative correlation rejection | Engine | @NEGATIVE |
| FT-13-1-026 | CPU utilization within limit | Engine | @PERFORMANCE |
| FT-13-1-027 | GC pause time within limit | Engine | @PERFORMANCE |
| FT-13-1-028 | Metrics runFailureCount increments | Engine | @METRICS |
| FT-13-1-029 | Logging correlation matrix size only | Engine | @SECURITY |
| FT-13-1-030 | SLA p95 completion time | Engine | @PERFORMANCE |

## Automation Strategy
Engine harness with linear algebra library for PSD; baseline JSON distribution snapshot.
