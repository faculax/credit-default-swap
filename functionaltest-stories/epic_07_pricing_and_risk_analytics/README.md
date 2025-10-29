# Test Epic 07 – Pricing & Risk Analytics

## Scope
Core risk engine, ISDA model parity, curve/bucket scenario shocks, regression harness, ORE process supervision, batched scenarios & CS01 aggregation.

## Objectives
- Numerical parity vs reference ISDA implementation
- Stable & reproducible risk measures across runs (controlled drift)
- Scenario shock correctness & aggregation
- Regression harness detects performance & numeric drift
- Process supervision resilience & recovery
- Efficient batched scenario execution with scaling

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Parity divergence | Side-by-side comparator | FT-7-1-* |
| Drift unnoticed | Golden baseline comparisons | FT-7-4-005 |
| Shock misapplication | Shock vector verification | FT-7-3-003 |
| Process zombie ORE | Supervision heartbeat tests | FT-7-5-006 |
| Memory blow-up batched runs | Profiling thresholds | FT-7-6-014 |

## Scenario Taxonomy
| Category | Stories |
|----------|---------|
| ISDA Parity | 7.1 |
| Core Measures | 7.2 |
| Scenario Shock | 7.3 |
| Regression Harness | 7.4 |
| ORE Process Supervision | 7.5 |
| Batched Scenarios & Bucket CS01 | 7.6 |

## Tooling Matrix
| Layer | Tool |
|-------|------|
| Engine | JUnit + floating tolerance asserts |
| API | REST-assured |
| Drift | JSON baseline snapshots |
| Performance | Custom timing harness |

## Exit Criteria
All FT-7-* implemented; drift thresholds enforced; no unbounded memory growth under standard batch loads.
