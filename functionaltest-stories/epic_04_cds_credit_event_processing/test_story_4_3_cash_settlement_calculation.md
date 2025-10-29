# Functional Test Story 4.3 – Cash Settlement Calculation

Trace: story_4_3_cash_settlement_calculation
Tags: @EPIC_04 @STORY_4_3 @CREDIT_EVENT @CALCULATION

## Objective
Validate correctness and resilience of cash settlement payout calculations for credit events.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-4-3-001 | Invoke calculation after event record | API | @CRITPATH |
| FT-4-3-002 | Formula baseline payout (notional * (1 - recovery)) | Engine | @FORMULA |
| FT-4-3-003 | Rounding to cents | Engine | @ROUNDING |
| FT-4-3-004 | Recovery boundary 0 | Engine | @BOUNDARY |
| FT-4-3-005 | Recovery boundary 1 -> zero payout | Engine | @BOUNDARY |
| FT-4-3-006 | Recovery >1 rejected | API | @NEGATIVE |
| FT-4-3-007 | Negative recovery rejected | API | @NEGATIVE |
| FT-4-3-008 | Large notional no overflow | Engine | @PERFORMANCE |
| FT-4-3-009 | Currency precision differences (JPY no decimals) | Engine | @CURRENCY |
| FT-4-3-010 | Multi-trade batch calculation total matches sum | Engine | @BATCH |
| FT-4-3-011 | Latency p95 < target | API | @PERFORMANCE |
| FT-4-3-012 | Timeout triggers retry (config) | API | @RESILIENCE |
| FT-4-3-013 | Retry exhaustion surfaces error | API | @RESILIENCE |
| FT-4-3-014 | Audit entry includes payout | API | @AUDIT |
| FT-4-3-015 | Idempotent recalculation no duplicate audit | API | @IDEMPOTENCY |
| FT-4-3-016 | Precision invariance across serialization | Engine | @NUMERIC |
| FT-4-3-017 | Parallel batch calculations thread-safe | Engine | @CONCURRENCY |
| FT-4-3-018 | Stress 1000 trades batch under threshold | Engine | @PERFORMANCE |
| FT-4-3-019 | Log no sensitive data (only ids & payout) | Engine | @LOGGING |
| FT-4-3-020 | Drift guard vs baseline payout fixture | Engine | @DRIFT |

## Automation Strategy
Engine-level JUnit with deterministic fixtures & integration wrapper for latency & retry behavior.
