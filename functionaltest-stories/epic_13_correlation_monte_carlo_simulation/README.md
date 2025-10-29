# Test Epic 13 – Correlation Monte Carlo Simulation

## Scope
Correlated default simulations, recovery modeling (deterministic & stochastic), metrics extraction, UI progress & controls, performance & scalability.

## Objectives
- Correct correlation matrix validation & sampling
- Deterministic reproducibility when seeded
- Efficient path batching & scaling
- Accurate portfolio loss & contributor metrics

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Non-PSD correlation matrix used | PSD validation tests | FT-13-1-005 |
| Non-deterministic seeded runs | Seed harness tests | FT-13-8-002 |
| Memory blow-up at high path count | Performance tests | FT-13-10-006 |

## Scenario Taxonomy
Multiple story docs enumerating feature slices (run, progress, metrics, diversification, contributors, recovery modes, exports, validation, performance, UI, error handling, audit, security, determinism, consistency, observability, accessibility, docs, scaling).

## Tooling Matrix
| Layer | Tool |
|-------|------|
| Engine | JUnit simulation harness |
| API | REST-assured |
| UI | Playwright (progress, cancellation) |
| Performance | Timed batch runs |

## Exit Criteria
All FT-13-* scenarios green; reproducibility proven for seed; correlation sampling tests pass PSD check.
