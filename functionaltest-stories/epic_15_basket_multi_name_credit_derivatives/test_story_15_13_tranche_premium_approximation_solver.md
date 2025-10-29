# Story 15.13 - Tranche Premium Approximation Solver

## Objective
Validate initial approximation solver for tranche premiums (pre full pricing model): ensure convergence within iteration limits, deterministic results with fixed seed, proper error handling for divergence, and integration with tranche groundwork structures.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-13-001 | Run approximation on 3-layer structure returns premiums array | Domain/API | @TRANCHE @SOLVER |
| FT-15-13-002 | Iteration count below maxIterations (convergent case) | Domain | @PERFORMANCE @CONVERGENCE |
| FT-15-13-003 | Divergent input triggers graceful error object | Domain/API | @ERROR @NEGATIVE |
| FT-15-13-004 | Deterministic seed yields identical premiums | Domain | @DETERMINISM @REPRODUCIBILITY |
| FT-15-13-005 | Premium monotonicity: lower layer >= upper layer? (rule check) | Domain | @VALIDATION @NUMERIC |
| FT-15-13-006 | Convergence metrics exported (iterations, residual) | API | @METRICS @OBSERVABILITY |
| FT-15-13-007 | Performance: solver p95 latency < threshold | Domain | @PERFORMANCE @LATENCY |
| FT-15-13-008 | Unauthorized solver invocation returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-13-009 | Rate limit on rapid solver calls (429) | API | @SECURITY @RATE_LIMIT |
| FT-15-13-010 | JSON schema stability for solver response | API | @SCHEMA @STABILITY |
| FT-15-13-011 | Accessibility (UI panel): convergence badge has aria-label | UI | @ACCESSIBILITY @A11Y |
| FT-15-13-012 | Drift detection: premiums array hash stable across identical runs | Domain | @DRIFT @SNAPSHOT |
| FT-15-13-013 | Logging: divergence includes residual trend snippet | Domain | @LOGGING @ERROR |

## Automation Strategy
1. Create tranche structure (reuse baseline from 15.12).
2. Invoke approximation solver with normal parameters; assert premiums length equals layers count.
3. Capture iteration metrics; ensure iterations < maxIterations.
4. Re-run with same seed; compare arrays.
5. Submit divergent parameters (e.g., extreme correlation) to force non-convergence; assert error payload.
6. Measure solver latency over N runs; compute p50/p95.
7. Validate response schema hash; record premium array hash for drift.
8. Check monotonic rule (adjust rules as domain specified); fail if violated.
9. Security & rate limit tests.
10. UI: open tranche solver panel; check accessibility badge.
11. Logging: capture divergence log (spy/mock) containing residual trend summary.

## Metrics
- trancheSolverLatency
- trancheSolverIterations
- trancheSolverDivergenceCount

## Thresholds
- p95 latency < 400ms; iterations <= 40

## Fixtures
- `fixtures/tranche/FT-15-13-premiums-baseline.json`

## Exit Criteria
Solver converges within limits; deterministic; proper divergence handling; performance & schema stable.
