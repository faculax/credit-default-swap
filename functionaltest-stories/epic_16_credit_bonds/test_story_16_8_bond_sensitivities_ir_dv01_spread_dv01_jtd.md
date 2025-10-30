# Functional Test Story 16.8 – Bond Sensitivities IR DV01 / Spread DV01 / JtD

Trace: story_16_8_bond-sensitivities-ir-dv01-spread-dv01-jtd
Tags: @EPIC_16 @CREDIT_BONDS @SENSITIVITY

## Objective
Validate sensitivity calculations across interest rate and spread shifts plus Jump-to-Default estimation.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-8-001 | IR DV01 +1bp | Engine | @NUMERIC |
| FT-16-8-002 | Spread DV01 +1bp | Engine | @NUMERIC |
| FT-16-8-003 | JtD estimation | Engine | @NUMERIC |
| FT-16-8-004 | Unauthorized sensitivity request -> 403 | API | @SECURITY |
| FT-16-8-005 | Drift sensitivity baseline | Engine | @DRIFT |
| FT-16-8-006 | Logging redacts bump PV deltas | API | @SECURITY |
| FT-16-8-007 | Metrics sensitivityCalcLatency | Engine | @METRICS |
| FT-16-8-008 | Performance p95 sensitivity latency | Engine | @PERFORMANCE |
| FT-16-8-009 | Contract sensitivity schema stable | Contract | @CONTRACT |
| FT-16-8-010 | Accessibility sensitivity UI | E2E | @ACCESSIBILITY |
| FT-16-8-011 | Concurrency multi-sensitivity isolation | Engine | @CONCURRENCY |
| FT-16-8-012 | Edge near maturity DV01 minimal | Engine | @EDGE |
| FT-16-8-013 | Edge distressed spread DV01 large | Engine | @EDGE |
| FT-16-8-014 | Extreme rate shock classification | Engine | @NEGATIVE |
| FT-16-8-015 | Negative spread shock classification | Engine | @NEGATIVE |
| FT-16-8-016 | Export sensitivity detail JSON | API | @EXPORT |
| FT-16-8-017 | Bump size configurable | Engine | @CONFIG |
| FT-16-8-018 | Large batch sensitivity processing | Engine | @SCALING |

## Automation Strategy
Run bumps vs baseline; JtD formula; export JSON; batch sensitivity scaling test.
