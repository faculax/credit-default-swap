# Functional Test Story 15.5 – Convergence Diagnostics & Exposure

Trace: story_15_5_convergence-diagnostics-exposure
Tags: @EPIC_15 @BASKET @DIAGNOSTICS

## Objective
Validate convergence diagnostics display (iterations, error residual, elapsed) and exposure metrics generation.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-5-001 | Diagnostics panel loads | E2E | @UI |
| FT-15-5-002 | Iteration count displayed | E2E | @UI |
| FT-15-5-003 | Error residual displayed | E2E | @UI |
| FT-15-5-004 | Exposure metrics displayed | E2E | @UI |
| FT-15-5-005 | Unauthorized diagnostics view -> 403 | API | @SECURITY |
| FT-15-5-006 | Drift diagnostics baseline | Engine | @DRIFT |
| FT-15-5-007 | Logging redacts internal solver logs | API | @SECURITY |
| FT-15-5-008 | Metrics diagnosticsLatency | Engine | @METRICS |
| FT-15-5-009 | Performance panel render latency | E2E | @PERFORMANCE |
| FT-15-5-010 | Accessibility diagnostics UI | E2E | @ACCESSIBILITY |
| FT-15-5-011 | Concurrency multi-diagnostics isolation | Engine | @CONCURRENCY |
| FT-15-5-012 | Export diagnostics JSON | API | @EXPORT |
| FT-15-5-013 | Edge non-converged diagnostics classification | Engine | @NEGATIVE |
| FT-15-5-014 | Refresh updates metrics without stale | E2E | @UI |
| FT-15-5-015 | Numeric formatting iteration count | E2E | @FORMAT |
| FT-15-5-016 | Error residual threshold breach flagged | Engine | @NEGATIVE |

## Automation Strategy
Panel UI snapshot; baseline diagnostics JSON; non-converged scenario injection; refresh cycle test.
