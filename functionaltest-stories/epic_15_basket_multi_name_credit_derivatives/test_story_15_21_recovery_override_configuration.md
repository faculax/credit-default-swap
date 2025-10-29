# Story 15.21 - Recovery Override Configuration

## Objective
Validate configuration workflow for recovery rate overrides per constituent and global default; ensure precedence rules, validation bounds, persistence, deterministic application, and UI accessibility.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-21-001 | Create override for single constituent (ISIN A) | API/UI | @RECOVERY @CONFIG |
| FT-15-21-002 | Global default override applied to all without specific override | API/UI | @RECOVERY @DEFAULT |
| FT-15-21-003 | Precedence: specific override supersedes global | Domain | @RECOVERY @PRECEDENCE |
| FT-15-21-004 | Validation rejects override <0% or >100% | API | @VALIDATION @NEGATIVE |
| FT-15-21-005 | Unauthorized configuration returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-21-006 | Rate limit enforced on rapid override edits | API | @SECURITY @RATE_LIMIT |
| FT-15-21-007 | Deterministic application ordering stable | Domain | @DETERMINISM @NORMALIZATION |
| FT-15-21-008 | JSON schema stable (hash baseline) | API | @SCHEMA @STABILITY |
| FT-15-21-009 | Drift detection: override set hash stable | API | @DRIFT @SNAPSHOT |
| FT-15-21-010 | Accessibility: override form fields labeled | UI | @ACCESSIBILITY @A11Y |
| FT-15-21-011 | Performance: apply overrides latency < threshold | API | @PERFORMANCE @LATENCY |
| FT-15-21-012 | Error logging: invalid structure recorded | API | @ERROR @LOGGING |

## Automation Strategy
1. Create global override; verify applied to constituents lacking specific overrides.
2. Create specific override; assert precedence & resulting recovery rates.
3. Attempt invalid bounds (<0, >100); assert validation codes RECOVERY_BOUNDS.
4. Unauthorized & rate limit tests.
5. Measure latency on apply.
6. Drift: hash override set vs baseline fixture.
7. Accessibility audit for form.
8. Error logging capture (spy).

## Metrics
- recoveryOverrideApplyLatency
- recoveryOverrideValidationFailureCount

## Exit Criteria
Overrides behave per rules; performance & accessibility pass; schema & drift stable.
