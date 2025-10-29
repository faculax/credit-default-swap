# Functional Test Story 5.2 – Accrual & Net Cash Posting Engine

Trace: story_5_2_accrual_net_cash_posting_engine
Tags: @EPIC_05 @STORY_5_2 @LIFECYCLE @ACCRUAL

## Objective
Validate accrual calculations, net cash posting entries, and reconciliation.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-5-2-001 | Daily accrual run posts entries | Engine | @CRON |
| FT-5-2-002 | Accrual formula base case | Engine | @CALCULATION |
| FT-5-2-003 | Day count ACT/360 correctness | Engine | @CALCULATION |
| FT-5-2-004 | Day count 30/360 correctness | Engine | @CALCULATION |
| FT-5-2-005 | Partial period first coupon | Engine | @CALCULATION |
| FT-5-2-006 | Trade terminated stops accrual | Engine | @STATE |
| FT-5-2-007 | Amend notional affects next accrual | Engine | @AMEND |
| FT-5-2-008 | Rounding rules applied | Engine | @ROUNDING |
| FT-5-2-009 | Re-run same date idempotent | Engine | @IDEMPOTENCY |
| FT-5-2-010 | Backdated amendment adjustment | Engine | @AMEND |
| FT-5-2-011 | Negative accrual impossible | Engine | @NEGATIVE |
| FT-5-2-012 | Net cash posting aggregated | Engine | @AGGREGATION |
| FT-5-2-013 | Ledger balanced (debits=credits) | Engine | @ACCOUNTING |
| FT-5-2-014 | Performance run (1000 trades) < threshold | Engine | @PERFORMANCE |
| FT-5-2-015 | Error in one trade logs & continues | Engine | @RESILIENCE |
| FT-5-2-016 | Metrics emitted accrualCount | Engine | @METRICS |
| FT-5-2-017 | Audit entries per batch | API | @AUDIT |
| FT-5-2-018 | Unauthorized manual trigger -> 403 | API | @SECURITY |
| FT-5-2-019 | Rate limit manual triggers -> 429 | API | @RESILIENCE |
| FT-5-2-020 | Clock skew detection | Engine | @TIME |
| FT-5-2-021 | DST transition handling | Engine | @TIME |
| FT-5-2-022 | Currency FX rounding stable | Engine | @CURRENCY |

## Automation Strategy
Deterministic test clocks & seeded trades; ledger assertions via aggregated sums.
