# Functional Test Story 8.3 – SIMM Sensitivities & IM Calculator

Trace: story_8_3_simm_sensitivities_im_calculator
Tags: @EPIC_08 @STORY_8_3 @MARGIN @SIMM

## Objective
Validate generation of SIMM sensitivities and Initial Margin calculation reproducibility and correctness.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-8-3-001 | Generate sensitivities baseline | Engine | @NUMERIC |
| FT-8-3-002 | IM calculation baseline | Engine | @NUMERIC |
| FT-8-3-003 | Sensitivity netting set grouping | Engine | @AGGREGATION |
| FT-8-3-004 | Delta sensitivity sign correctness | Engine | @NUMERIC |
| FT-8-3-005 | Vega sensitivity magnitude | Engine | @NUMERIC |
| FT-8-3-006 | Curvature sensitivity calculation | Engine | @NUMERIC |
| FT-8-3-007 | Cross-bucket correlation application | Engine | @CORRELATION |
| FT-8-3-008 | Risk weight application correct | Engine | @WEIGHT |
| FT-8-3-009 | Add-on (if any) applied | Engine | @ADDON |
| FT-8-3-010 | Rounding rules final IM | Engine | @ROUNDING |
| FT-8-3-011 | Stress scenario sensitivity recompute | Engine | @SCENARIO |
| FT-8-3-012 | Missing required risk class error | Engine | @NEGATIVE |
| FT-8-3-013 | Performance compute < threshold | Engine | @PERFORMANCE |
| FT-8-3-014 | Deterministic seeds reproducibility | Engine | @DETERMINISM |
| FT-8-3-015 | Golden baseline drift check | Engine | @DRIFT |
| FT-8-3-016 | Metrics imCalculationLatency | Engine | @METRICS |
| FT-8-3-017 | Logging redacts netting set identifiers | Engine | @SECURITY |
| FT-8-3-018 | Concurrency consistent totals | Engine | @CONCURRENCY |
| FT-8-3-019 | API endpoint returns IM figure | API | @API |
| FT-8-3-020 | API contract stable | Contract | @CONTRACT |
| FT-8-3-021 | UI dashboard shows sensitivities table | E2E | @UI |
| FT-8-3-022 | Accessibility sensitivities table | E2E | @ACCESSIBILITY |
| FT-8-3-023 | Negative notional sensitivity handling | Engine | @EDGE |
| FT-8-3-024 | Zero sensitivity risk class excluded | Engine | @OPTIMIZATION |
| FT-8-3-025 | High correlation stress path | Engine | @SCENARIO |
| FT-8-3-026 | Memory usage under threshold | Engine | @PERFORMANCE |
| FT-8-3-027 | Export sensitivities JSON | API | @EXPORT |
| FT-8-3-028 | Failure path rollback no partial save | Engine | @TRANSACTION |

## Automation Strategy
Engine calc harness with risk class fixture JSON; API + UI coverage; drift file stored under `risk-baselines/simm`.
