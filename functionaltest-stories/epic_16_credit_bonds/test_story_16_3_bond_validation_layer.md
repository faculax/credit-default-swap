# Functional Test Story 16.3 – Bond Validation Layer

Trace: story_16_3_bond-validation-layer
Tags: @EPIC_16 @CREDIT_BONDS @VALIDATION

## Objective
Validate bond input validation rules to prevent invalid data entries.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-3-001 | Missing ISIN -> error | API | @NEGATIVE |
| FT-16-3-002 | Invalid coupon negative -> error | API | @NEGATIVE |
| FT-16-3-003 | Maturity before issue date -> error | API | @NEGATIVE |
| FT-16-3-004 | Unsupported currency -> error | API | @NEGATIVE |
| FT-16-3-005 | Invalid day count code -> error | API | @NEGATIVE |
| FT-16-3-006 | Unauthorized validation attempt -> 403 | API | @SECURITY |
| FT-16-3-007 | Drift validation baseline error set | API | @DRIFT |
| FT-16-3-008 | Logging redacts invalid values | API | @SECURITY |
| FT-16-3-009 | Metrics validationFailureCount | API | @METRICS |
| FT-16-3-010 | Accessibility error message UI | E2E | @ACCESSIBILITY |
| FT-16-3-011 | Concurrency multi-validation isolation | API | @CONCURRENCY |
| FT-16-3-012 | Edge very high coupon accepted | API | @EDGE |
| FT-16-3-013 | Edge extremely long maturity accepted | API | @EDGE |
| FT-16-3-014 | Contract validation error schema stable | Contract | @CONTRACT |
| FT-16-3-015 | Multiple errors aggregated | API | @NEGATIVE |
| FT-16-3-016 | Rate limit exceeded -> 429 | API | @RESILIENCE |

## Automation Strategy
Negative input matrix; check error aggregation; metrics and security; rate limit simulation.
