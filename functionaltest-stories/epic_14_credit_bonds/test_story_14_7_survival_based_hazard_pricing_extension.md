# Functional Test Story 14.7 – Survival-Based Hazard Pricing Extension

Trace: story_14_7_survival-based-hazard-pricing-extension
Tags: @EPIC_14 @CREDIT_BONDS @HAZARD

## Objective
Validate hazard rate / survival probability based pricing extension matches baseline and handles edge cases.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-7-001 | Hazard curve bootstrap | Engine | @NUMERIC |
| FT-14-7-002 | Survival probability calc | Engine | @NUMERIC |
| FT-14-7-003 | Price using hazard model | Engine | @NUMERIC |
| FT-14-7-004 | Unauthorized hazard request -> 403 | API | @SECURITY |
| FT-14-7-005 | Drift hazard pricing baseline | Engine | @DRIFT |
| FT-14-7-006 | Logging redacts hazard raw data | API | @SECURITY |
| FT-14-7-007 | Metrics hazardPricingLatency | Engine | @METRICS |
| FT-14-7-008 | Performance hazard latency p95 | Engine | @PERFORMANCE |
| FT-14-7-009 | Contract hazard schema stable | Contract | @CONTRACT |
| FT-14-7-010 | Accessibility hazard pricing UI | E2E | @ACCESSIBILITY |
| FT-14-7-011 | Concurrency multi-hazard isolation | Engine | @CONCURRENCY |
| FT-14-7-012 | Edge negative intensity rejected | Engine | @NEGATIVE |
| FT-14-7-013 | Edge extremely high intensity handled | Engine | @EDGE |
| FT-14-7-014 | Export hazard diagnostics JSON | API | @EXPORT |
| FT-14-7-015 | Calibration iteration cap | Engine | @NEGATIVE |
| FT-14-7-016 | Non-convergence classification | Engine | @NEGATIVE |

## Automation Strategy
Bootstrap hazard curve; price vs baseline; negative intensity rejection; diagnostics export.
