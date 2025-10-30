# Functional Test Story 16.7 – Survival-Based Hazard Pricing Extension

Trace: story_16_7_survival-based-hazard-pricing-extension
Tags: @EPIC_16 @CREDIT_BONDS @HAZARD

## Objective
Validate hazard rate / survival probability based pricing extension matches baseline and handles edge cases.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-7-001 | Hazard curve bootstrap | Engine | @NUMERIC |
| FT-16-7-002 | Survival probability calc | Engine | @NUMERIC |
| FT-16-7-003 | Price using hazard model | Engine | @NUMERIC |
| FT-16-7-004 | Unauthorized hazard request -> 403 | API | @SECURITY |
| FT-16-7-005 | Drift hazard pricing baseline | Engine | @DRIFT |
| FT-16-7-006 | Logging redacts hazard raw data | API | @SECURITY |
| FT-16-7-007 | Metrics hazardPricingLatency | Engine | @METRICS |
| FT-16-7-008 | Performance hazard latency p95 | Engine | @PERFORMANCE |
| FT-16-7-009 | Contract hazard schema stable | Contract | @CONTRACT |
| FT-16-7-010 | Accessibility hazard pricing UI | E2E | @ACCESSIBILITY |
| FT-16-7-011 | Concurrency multi-hazard isolation | Engine | @CONCURRENCY |
| FT-16-7-012 | Edge negative intensity rejected | Engine | @NEGATIVE |
| FT-16-7-013 | Edge extremely high intensity handled | Engine | @EDGE |
| FT-16-7-014 | Export hazard diagnostics JSON | API | @EXPORT |
| FT-16-7-015 | Calibration iteration cap | Engine | @NEGATIVE |
| FT-16-7-016 | Non-convergence classification | Engine | @NEGATIVE |

## Automation Strategy
Bootstrap hazard curve; price vs baseline; negative intensity rejection; diagnostics export.
