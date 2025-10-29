# Functional Test Story 14.5 – Cash Flow Projection

Trace: story_14_5_cash-flow-projection
Tags: @EPIC_14 @CORPORATE_BONDS @CASH_FLOW

## Objective
Validate generation of projected coupon and principal cash flows with correct day count and discounting.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-5-001 | Generate cash flow table fixed bond | Engine | @POSITIVE |
| FT-14-5-002 | Generate cash flow table FRN | Engine | @POSITIVE |
| FT-14-5-003 | Day count convention ACT/360 | Engine | @NUMERIC |
| FT-14-5-004 | Day count convention 30/360 | Engine | @NUMERIC |
| FT-14-5-005 | Discounting present value correctness | Engine | @NUMERIC |
| FT-14-5-006 | Unauthorized projection -> 403 | API | @SECURITY |
| FT-14-5-007 | Drift projection pv baseline | Engine | @DRIFT |
| FT-14-5-008 | Logging redacts internal schedule | API | @SECURITY |
| FT-14-5-009 | Metrics projectionLatency | Engine | @METRICS |
| FT-14-5-010 | Performance p95 projection time | Engine | @PERFORMANCE |
| FT-14-5-011 | Contract cash flow schema stable | Contract | @CONTRACT |
| FT-14-5-012 | Accessibility cash flow UI | E2E | @ACCESSIBILITY |
| FT-14-5-013 | Concurrency multi-projection isolation | Engine | @CONCURRENCY |
| FT-14-5-014 | Edge leap year day count | Engine | @EDGE |
| FT-14-5-015 | Edge partial principal redemption mid-period | Engine | @EDGE |

## Automation Strategy
Compute flows; compare pv vs baseline; negative unauthorized; day count variance tests.
