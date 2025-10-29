# Functional Test Story 13.9 – Input Validation

Trace: story_13_9_input-validation
Tags: @EPIC_13 @SIMULATION @NEGATIVE

## Objective
Validate robust input parameter validation for simulation configuration.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-9-001 | Missing required parameter | API | @NEGATIVE |
| FT-13-9-002 | Invalid numeric range negative paths | API | @NEGATIVE |
| FT-13-9-003 | Excessive path count > max -> error | API | @NEGATIVE |
| FT-13-9-004 | Invalid correlation matrix dimension | API | @NEGATIVE |
| FT-13-9-005 | Non-numeric parameter type -> 400 | Contract | @SCHEMA |
| FT-13-9-006 | Missing recovery distribution config | API | @NEGATIVE |
| FT-13-9-007 | Invalid enum value | API | @NEGATIVE |
| FT-13-9-008 | Duplicate entity IDs | API | @NEGATIVE |
| FT-13-9-009 | Path count zero -> error | API | @NEGATIVE |
| FT-13-9-010 | Risk-free curve missing point -> error | API | @NEGATIVE |
| FT-13-9-011 | Unauthorized submit -> 403 | API | @SECURITY |
| FT-13-9-012 | Rate limit exceeded -> 429 | API | @RESILIENCE |
| FT-13-9-013 | Audit entry failure classification | API | @AUDIT |
| FT-13-9-014 | Logging redacts invalid values | API | @SECURITY |
| FT-13-9-015 | Metrics validationFailureCount | API | @METRICS |
| FT-13-9-016 | Accessibility validation message UI | E2E | @ACCESSIBILITY |
| FT-13-9-017 | Time zone normalized timestamps | API | @TIME |
| FT-13-9-018 | API contract stable | Contract | @CONTRACT |
| FT-13-9-019 | Combined multiple errors aggregated | API | @NEGATIVE |
| FT-13-9-020 | Correlation matrix not symmetric -> error | API | @NEGATIVE |

## Automation Strategy
REST negative test matrix; UI validation messages with Playwright; audit/metrics asserts.
