# Functional Test Story 13.4 – Diversification Benefit

Trace: story_13_4_diversification-benefit
Tags: @EPIC_13 @SIMULATION @DIVERSIFICATION

## Objective
Validate computation of diversification benefit metrics from correlated multi-name simulations.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-4-001 | Compute standalone vs portfolio loss | Engine | @NUMERIC |
| FT-13-4-002 | Diversification benefit formula | Engine | @FORMULA |
| FT-13-4-003 | High correlation reduces benefit | Engine | @EDGE |
| FT-13-4-004 | Low correlation increases benefit | Engine | @EDGE |
| FT-13-4-005 | Deterministic seed reproducibility | Engine | @DETERMINISM |
| FT-13-4-006 | Negative benefit (edge) flagged | Engine | @EDGE |
| FT-13-4-007 | Export benefit metrics | API | @EXPORT |
| FT-13-4-008 | Unauthorized access -> 403 | API | @SECURITY |
| FT-13-4-009 | Performance compute latency | Engine | @PERFORMANCE |
| FT-13-4-010 | Drift baseline check | Engine | @DRIFT |
| FT-13-4-011 | Logging redacts internal arrays | Engine | @SECURITY |
| FT-13-4-012 | Concurrency stable computations | Engine | @CONCURRENCY |
| FT-13-4-013 | Percent change calculation precision | Engine | @NUMERIC |
| FT-13-4-014 | API contract stable | Contract | @CONTRACT |
| FT-13-4-015 | Accessibility benefit panel | E2E | @ACCESSIBILITY |
| FT-13-4-016 | Metrics diversificationBenefit | Engine | @METRICS |
| FT-13-4-017 | Time zone normalized timestamp | API | @TIME |
| FT-13-4-018 | Large portfolio scaling | Engine | @SCALING |

## Automation Strategy
Comparison against precomputed standalone vs combined losses baseline dataset.
