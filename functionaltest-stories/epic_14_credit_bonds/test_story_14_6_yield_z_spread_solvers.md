# Functional Test Story 14.6 – Yield & Z-Spread Solvers

Trace: story_14_6_yield-z-spread-solvers
Tags: @EPIC_14 @CREDIT_BONDS @SOLVER

## Objective
Validate iterative solvers converge for yield to maturity and Z-spread within iteration limits and tolerances.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-6-001 | Yield solver convergence | Engine | @NUMERIC |
| FT-14-6-002 | Z-spread solver convergence | Engine | @NUMERIC |
| FT-14-6-003 | Unauthorized solver request -> 403 | API | @SECURITY |
| FT-14-6-004 | Drift solver results baseline | Engine | @DRIFT |
| FT-14-6-005 | Logging redacts iteration details | API | @SECURITY |
| FT-14-6-006 | Metrics solverIterationCount | Engine | @METRICS |
| FT-14-6-007 | Performance solver latency p95 | Engine | @PERFORMANCE |
| FT-14-6-008 | Contract solver schema stable | Contract | @CONTRACT |
| FT-14-6-009 | Accessibility solver UI | E2E | @ACCESSIBILITY |
| FT-14-6-010 | Concurrency multi-solver isolation | Engine | @CONCURRENCY |
| FT-14-6-011 | Edge illiquid spread wide | Engine | @EDGE |
| FT-14-6-012 | Edge near maturity solver stability | Engine | @EDGE |
| FT-14-6-013 | Edge far maturity solver stability | Engine | @EDGE |
| FT-14-6-014 | Edge negative yield solver case | Engine | @EDGE |
| FT-14-6-015 | Edge extremely high yield | Engine | @EDGE |
| FT-14-6-016 | Non-convergence triggers failure | Engine | @NEGATIVE |
| FT-14-6-017 | Iteration cap enforcement | Engine | @NEGATIVE |
| FT-14-6-018 | Adaptive step size effectiveness | Engine | @NUMERIC |
| FT-14-6-019 | Delta vs analytic approximate | Engine | @NUMERIC |
| FT-14-6-020 | Solver diagnostics export | API | @EXPORT |

## Automation Strategy
Solver harness with baseline results; stress illiquid scenarios; failure/non-convergence tests.
