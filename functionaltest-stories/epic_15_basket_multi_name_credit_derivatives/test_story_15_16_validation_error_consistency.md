# Story 15.16 - Validation Error Consistency

## Objective
Ensure all basket & tranche validation errors use consistent structure (code, message, field, severity) and appear uniformly across create/update endpoints, with correct localization keys and absence of ambiguous messages.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-16-001 | Invalid basket weight total > 100% | API | @VALIDATION @NEGATIVE |
| FT-15-16-002 | Missing constituent identifier | API | @VALIDATION @NEGATIVE |
| FT-15-16-003 | Duplicate constituent in basket prevented | API | @VALIDATION @NEGATIVE |
| FT-15-16-004 | Tranche overlap error returns code TRANCHE_OVERLAP | API | @VALIDATION @NEGATIVE |
| FT-15-16-005 | Field-level error includes field name & severity WARN/ERROR | API | @VALIDATION @STRUCTURE |
| FT-15-16-006 | Localization key present for each validation error | API | @I18N @VALIDATION |
| FT-15-16-007 | Bulk create returns array of error objects for each failed item | API | @BULK @VALIDATION |
| FT-15-16-008 | Unauthorized still returns 403 (not validation shape) | API | @SECURITY @NEGATIVE |
| FT-15-16-009 | Rate limit error distinct code RATE_LIMIT vs validation | API | @SECURITY @RATE_LIMIT |
| FT-15-16-010 | Accessibility: error summary region has role alert | UI | @ACCESSIBILITY @A11Y |
| FT-15-16-011 | Performance: multiple validation errors returned in single response < threshold | API | @PERFORMANCE @LATENCY |
| FT-15-16-012 | Drift: error schema hash stable | API | @SCHEMA @STABILITY |

## Automation Strategy
1. Send invalid payloads (weight overflow, missing fields, duplicates) and assert error object shape.
2. Confirm error codes list matches documented set (snapshot compare).
3. Verify localization keys pattern (e.g., validation.basket.weightTotal).
4. Bulk create with mixed valid/invalid entries; assert array response with per-item errors.
5. Negative: unauthorized path returns pure 403 error not validation object.
6. Rate limit test to ensure distinct code usage.
7. UI: trigger form errors; ensure alert role & focus management.
8. Schema hash drift check.

## Metrics
- validationErrorCount
- validationLatency

## Exit Criteria
All validations produce consistent structure; codes & localization keys present; performance within bounds.
