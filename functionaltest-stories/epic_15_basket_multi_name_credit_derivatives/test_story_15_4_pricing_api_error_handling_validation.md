# Functional Test Story 15.4 – Pricing API Error Handling & Validation

Trace: story_15_4_pricing-api-error-handling-validation
Tags: @EPIC_15 @BASKET @ERROR

## Objective
Validate pricing API robust error handling and validation of basket pricing requests.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-4-001 | Missing basketId -> 400 | API | @NEGATIVE |
| FT-15-4-002 | Invalid correlation matrix dimension -> 400 | API | @NEGATIVE |
| FT-15-4-003 | Unauthorized pricing request -> 403 | API | @SECURITY |
| FT-15-4-004 | Drift error response schema baseline | API | @DRIFT |
| FT-15-4-005 | Logging redacts invalid inputs | API | @SECURITY |
| FT-15-4-006 | Metrics pricingErrorCount | API | @METRICS |
| FT-15-4-007 | Rate limit exceeded -> 429 | API | @RESILIENCE |
| FT-15-4-008 | Edge extremely large basket size -> 413 | API | @NEGATIVE |
| FT-15-4-009 | Edge non-PSD correlation matrix -> 400 | API | @NEGATIVE |
| FT-15-4-010 | Concurrency multi-error isolation | API | @CONCURRENCY |
| FT-15-4-011 | Accessibility error message UI | E2E | @ACCESSIBILITY |
| FT-15-4-012 | Aggregated multi-field errors | API | @NEGATIVE |
| FT-15-4-013 | Contract pricing error schema stable | Contract | @CONTRACT |
| FT-15-4-014 | Export error response JSON | API | @EXPORT |
| FT-15-4-015 | Invalid numeric parameter -> 400 | API | @NEGATIVE |
| FT-15-4-016 | Missing required parameter -> 400 | API | @NEGATIVE |
| FT-15-4-017 | Unsupported pricing mode -> 400 | API | @NEGATIVE |
| FT-15-4-018 | Invalid seed format -> 400 | API | @NEGATIVE |

## Automation Strategy
Negative request matrix; PSD validation; rate limit simulation; schema snapshot compare.
