# Functional Test Story 14.8 – Accrual & Amortization

Trace: story_14_8_accrual-amortization
Tags: @EPIC_14 @CORPORATE_BONDS @ACCRUAL

## Objective
Validate accrual interest calculation and amortization adjustments after partial redemptions.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-8-001 | Accrual calculation fixed bond | Engine | @NUMERIC |
| FT-14-8-002 | Accrual calculation FRN | Engine | @NUMERIC |
| FT-14-8-003 | Partial redemption adjusts notional | Engine | @NUMERIC |
| FT-14-8-004 | Unauthorized accrual request -> 403 | API | @SECURITY |
| FT-14-8-005 | Drift accrual baseline | Engine | @DRIFT |
| FT-14-8-006 | Logging redacts internal accrual factors | API | @SECURITY |
| FT-14-8-007 | Metrics accrualCalcLatency | Engine | @METRICS |
| FT-14-8-008 | Performance p95 accrual latency | Engine | @PERFORMANCE |
| FT-14-8-009 | Contract accrual schema stable | Contract | @CONTRACT |
| FT-14-8-010 | Accessibility accrual UI | E2E | @ACCESSIBILITY |
| FT-14-8-011 | Concurrency multi-accrual isolation | API | @CONCURRENCY |
| FT-14-8-012 | Edge leap year accrual day count | Engine | @EDGE |
| FT-14-8-013 | Edge amortization to zero notional | Engine | @EDGE |

## Automation Strategy
Accrual engine vs baseline; partial redemption scenario; concurrency stress.
