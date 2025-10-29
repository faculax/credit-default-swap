# Functional Test Story 13.13 – Correlation Matrix Validation

Trace: story_13_13_correlation-matrix-validation
Tags: @EPIC_13 @SIMULATION @CORRELATION

## Objective
Validate correlation matrix inputs (dimension, symmetry, PSD) and diagnostic reporting.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-13-001 | Valid matrix accepted | API | @POSITIVE |
| FT-13-13-002 | Non-symmetric matrix rejected | API | @NEGATIVE |
| FT-13-13-003 | Non-PSD matrix flagged | Engine | @NUMERIC |
| FT-13-13-004 | Dimension mismatch -> error | API | @NEGATIVE |
| FT-13-13-005 | Duplicate labels -> error | API | @NEGATIVE |
| FT-13-13-006 | Missing labels -> error | API | @NEGATIVE |
| FT-13-13-007 | Unauthorized submission -> 403 | API | @SECURITY |
| FT-13-13-008 | Drift PSD eigenvalues baseline | Engine | @DRIFT |
| FT-13-13-009 | Logging redacts full matrix | API | @SECURITY |
| FT-13-13-010 | Metrics correlationValidationFailureCount | API | @METRICS |
| FT-13-13-011 | Accessibility matrix error UI | E2E | @ACCESSIBILITY |
| FT-13-13-012 | API contract stable | Contract | @CONTRACT |
| FT-13-13-013 | Large matrix performance test | Engine | @SCALING |
| FT-13-13-014 | Eigen decomposition deterministic seed | Engine | @DETERMINISM |

## Automation Strategy
Matrix generation harness; linear algebra checks; negative test matrix variations.
