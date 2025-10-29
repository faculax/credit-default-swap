# Functional Test Story 7.1 – ISDA Standard Model Integration Parity

Trace: story_7_1_isda-standard-model-integration-parity-tests
Tags: @EPIC_07 @STORY_7_1 @RISK @PARITY

## Objective
Validate parity between in-house implementation and ISDA standard model for core measures across representative instruments.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-7-1-001 | Load baseline test set | Engine | @SETUP |
| FT-7-1-002 | PV parity within 0.01 | Engine | @NUMERIC |
| FT-7-1-003 | CS01 parity within 0.5% | Engine | @NUMERIC |
| FT-7-1-004 | DV01 parity within 0.5% | Engine | @NUMERIC |
| FT-7-1-005 | Hazard rate calibration parity | Engine | @CALIBRATION |
| FT-7-1-006 | Recovery assumption alignment | Engine | @INPUT |
| FT-7-1-007 | Curve bootstrap match tolerance | Engine | @CURVE |
| FT-7-1-008 | Edge short maturity case | Engine | @EDGE |
| FT-7-1-009 | Edge long maturity case | Engine | @EDGE |
| FT-7-1-010 | Zero spread instrument parity | Engine | @EDGE |
| FT-7-1-011 | High spread instrument parity | Engine | @EDGE |
| FT-7-1-012 | FX conversion parity cross-currency | Engine | @CURRENCY |
| FT-7-1-013 | Stress shock invariance test | Engine | @SCENARIO |
| FT-7-1-014 | Parallel shift vs recompute consistency | Engine | @SCENARIO |
| FT-7-1-015 | Data quality: missing point fallback parity | Engine | @DATA |
| FT-7-1-016 | Interpolation method differences flagged | Engine | @DATA |
| FT-7-1-017 | Negative rates parity | Engine | @EDGE |
| FT-7-1-018 | Low recovery parity | Engine | @EDGE |
| FT-7-1-019 | High recovery parity | Engine | @EDGE |
| FT-7-1-020 | Performance baseline parity batch | Engine | @PERFORMANCE |
| FT-7-1-021 | Golden baseline snapshot persisted | Engine | @BASELINE |
| FT-7-1-022 | Drift detection triggers on threshold breach | Engine | @DRIFT |
| FT-7-1-023 | Logging no sensitive data | Engine | @LOGGING |
| FT-7-1-024 | Metrics parityFailures count | Engine | @METRICS |
| FT-7-1-025 | Concurrency repeatable results | Engine | @CONCURRENCY |
| FT-7-1-026 | Deterministic seed reproducibility | Engine | @DETERMINISM |
| FT-7-1-027 | Serialization round-trip parity | Engine | @SERIALIZATION |
| FT-7-1-028 | Memory profile within limits | Engine | @PERFORMANCE |

## Automation Strategy
Compare outputs vs ISDA library via side-by-side compute harness with tolerance config file.
