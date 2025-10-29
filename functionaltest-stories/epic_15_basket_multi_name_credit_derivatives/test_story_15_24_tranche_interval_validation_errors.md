# Story 15.24 - Tranche Interval Validation Errors

## Objective
Dedicated edge-case coverage for tranche interval validation beyond groundwork: touching boundaries, micro-overlaps, floating precision, and large layer counts ensuring accurate error codes and messages.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-24-001 | Touching boundaries (3%-7%, 7%-10%) allowed | API | @TRANCHE @VALIDATION |
| FT-15-24-002 | Micro-overlap (7.0001%-10% after 3%-7%) rejected | API | @TRANCHE @VALIDATION @NEGATIVE |
| FT-15-24-003 | Floating precision normalization resolves minor epsilon | Domain | @TRANCHE @NORMALIZATION |
| FT-15-24-004 | Large layer set (20) validates without performance degradation | API | @TRANCHE @SCALING |
| FT-15-24-005 | Unauthorized creation returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-24-006 | Rate limit on rapid layer submissions | API | @SECURITY @RATE_LIMIT |
| FT-15-24-007 | Deterministic normalization ordering stable | Domain | @DETERMINISM @NORMALIZATION |
| FT-15-24-008 | Drift: normalized layers hash stable | Domain | @DRIFT @SNAPSHOT |
| FT-15-24-009 | Performance: validation latency p95 < threshold | API | @PERFORMANCE @LATENCY |
| FT-15-24-010 | Error logging: micro-overlap includes specific code TRANCHE_MICRO_OVERLAP | API | @ERROR @LOGGING |

## Automation Strategy
1. Submit valid touching boundary set; assert success.
2. Submit micro-overlap; assert rejection & specific code.
3. Test epsilon normalization (e.g., 7.0000004%) results in normalized display.
4. Large 20-layer payload; measure latency.
5. Unauthorized & rate limit negative tests.
6. Hash normalized layers for drift.
7. Logging capture for micro-overlap rejection.

## Metrics
- trancheIntervalValidationLatency
- trancheMicroOverlapFailureCount

## Exit Criteria
All interval edge cases validated; normalization deterministic; performance within threshold.
