# Functional Test Story 15.8 – N-th to Default Extension

Trace: story_15_8_n-th-to-default-extension
Tags: @EPIC_15 @BASKET @EXTENSION

## Objective
Validate extension for N-th to default basket pricing with correct solver convergence and boundary conditions.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-8-001 | Price 2nd-to-default basket | Engine | @NUMERIC |
| FT-15-8-002 | Price N near basket size (edge) | Engine | @EDGE |
| FT-15-8-003 | Unauthorized N-th pricing -> 403 | API | @SECURITY |
| FT-15-8-004 | Drift N-th pricing baseline | Engine | @DRIFT |
| FT-15-8-005 | Logging redacts iteration details | API | @SECURITY |
| FT-15-8-006 | Metrics nthDefaultSolverLatency | Engine | @METRICS |
| FT-15-8-007 | Performance solver latency p95 | Engine | @PERFORMANCE |
| FT-15-8-008 | Contract N-th pricing schema stable | Contract | @CONTRACT |
| FT-15-8-009 | Concurrency multi-N-th isolation | Engine | @CONCURRENCY |
| FT-15-8-010 | Edge N=1 degenerates to first-to-default | Engine | @EDGE |
| FT-15-8-011 | Non-convergence classification | Engine | @NEGATIVE |
| FT-15-8-012 | Export N-th pricing diagnostics | API | @EXPORT |
| FT-15-8-013 | Parameter validation errors aggregated | API | @NEGATIVE |
| FT-15-8-014 | Deterministic seed reproducibility | Engine | @DETERMINISM |
| FT-15-8-015 | Wide spread range sensitivity | Engine | @EDGE |
| FT-15-8-016 | Large constituent count scaling | Engine | @SCALING |

## Automation Strategy
Solver harness baseline compare; edge degeneracy; non-convergence and scaling tests; diagnostics export.
