# Functional Test Story 14.7 – Risk Measures DV01/CS01

Trace: story_14_7_risk-measures-dv01-cs01
Tags: @EPIC_14 @CORPORATE_BONDS @RISK

## Objective
Validate computation of DV01 (interest rate) and CS01 (credit spread) sensitivities and JtD.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-7-001 | Compute DV01 bump +1bp | Engine | @NUMERIC |
| FT-14-7-002 | Compute CS01 bump +1bp | Engine | @NUMERIC |
| FT-14-7-003 | JtD calculation correctness | Engine | @NUMERIC |
| FT-14-7-004 | Unauthorized risk request -> 403 | API | @SECURITY |
| FT-14-7-005 | Drift sensitivities baseline | Engine | @DRIFT |
| FT-14-7-006 | Logging redacts raw PVs | API | @SECURITY |
| FT-14-7-007 | Metrics riskCalcLatency | Engine | @METRICS |
| FT-14-7-008 | Performance p95 risk latency | Engine | @PERFORMANCE |
| FT-14-7-009 | Contract risk schema stable | Contract | @CONTRACT |
| FT-14-7-010 | Accessibility risk panel UI | E2E | @ACCESSIBILITY |
| FT-14-7-011 | Concurrency multi-risk requests isolation | API | @CONCURRENCY |
| FT-14-7-012 | Edge zero coupon bond DV01 | Engine | @EDGE |
| FT-14-7-013 | Edge distressed spread large CS01 | Engine | @EDGE |

## Automation Strategy
Run bump calculations vs baseline sensitivities JSON; concurrency stress; edge case portfolios.
