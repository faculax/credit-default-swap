# Functional Test Story 8.4 – SA-CCR Exposure Engine

Trace: story_8_4_sa_ccr_exposure_engine
Tags: @EPIC_08 @STORY_8_4 @MARGIN @EXPOSURE

## Objective
Validate Standardized Approach for Counterparty Credit Risk exposure calculation components and aggregation.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-8-4-001 | Calculate replacement cost component | Engine | @COMPONENT |
| FT-8-4-002 | Calculate add-on component | Engine | @COMPONENT |
| FT-8-4-003 | Maturity factor application | Engine | @FACTOR |
| FT-8-4-004 | Supervisory delta logic | Engine | @DELTA |
| FT-8-4-005 | Aggregated exposure formula | Engine | @FORMULA |
| FT-8-4-006 | Hedging set netting benefits | Engine | @NETTING |
| FT-8-4-007 | Correlation factor application | Engine | @CORRELATION |
| FT-8-4-008 | Negative exposure floored zero | Engine | @BOUNDARY |
| FT-8-4-009 | Extreme exposure stress scenario | Engine | @SCENARIO |
| FT-8-4-010 | Currency conversion accuracy | Engine | @CURRENCY |
| FT-8-4-011 | Performance compute batch | Engine | @PERFORMANCE |
| FT-8-4-012 | Drift baseline comparison | Engine | @DRIFT |
| FT-8-4-013 | Deterministic seed reproducibility | Engine | @DETERMINISM |
| FT-8-4-014 | API endpoint returns exposure | API | @API |
| FT-8-4-015 | API contract stable | Contract | @CONTRACT |
| FT-8-4-016 | Logging redacts counterparty IDs | Engine | @SECURITY |
| FT-8-4-017 | Metrics exposureCalcLatency | Engine | @METRICS |
| FT-8-4-018 | Concurrency safe multi-thread execution | Engine | @CONCURRENCY |
| FT-8-4-019 | Invalid maturity bucket rejection | Engine | @NEGATIVE |
| FT-8-4-020 | Export exposure JSON | API | @EXPORT |
| FT-8-4-021 | Memory utilization threshold | Engine | @PERFORMANCE |
| FT-8-4-022 | Large batch >100 trades scaling | Engine | @SCALING |

## Automation Strategy
Engine tests per component plus aggregate; API contract; drift baseline under `risk-baselines/saccr`.
