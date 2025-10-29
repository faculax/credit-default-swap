# Functional Test Story 14.5 – Deterministic Bond Pricing & Accrual

Trace: story_14_5_deterministic-bond-pricing-accrual
Tags: @EPIC_14 @CREDIT_BONDS @PRICING

## Objective
Validate deterministic pricing and accrual calculations produce reproducible results under fixed inputs.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-5-001 | Deterministic clean price run | Engine | @DETERMINISM |
| FT-14-5-002 | Deterministic dirty price run | Engine | @DETERMINISM |
| FT-14-5-003 | Accrual reproducibility | Engine | @DETERMINISM |
| FT-14-5-004 | Unauthorized deterministic run -> 403 | API | @SECURITY |
| FT-14-5-005 | Drift deterministic pricing baseline | Engine | @DRIFT |
| FT-14-5-006 | Logging redacts internal seed/config | API | @SECURITY |
| FT-14-5-007 | Metrics deterministicRunCount | Engine | @METRICS |
| FT-14-5-008 | Performance deterministic latency p95 | Engine | @PERFORMANCE |
| FT-14-5-009 | Contract pricing schema stable | Contract | @CONTRACT |
| FT-14-5-010 | Accessibility deterministic pricing UI | E2E | @ACCESSIBILITY |
| FT-14-5-011 | Concurrency multi-deterministic isolation | Engine | @CONCURRENCY |
| FT-14-5-012 | Edge extreme coupon deterministic | Engine | @EDGE |
| FT-14-5-013 | Edge near maturity deterministic | Engine | @EDGE |
| FT-14-5-014 | Edge far maturity deterministic | Engine | @EDGE |
| FT-14-5-015 | Negative yield deterministic | Engine | @EDGE |
| FT-14-5-016 | Spread spike deterministic | Engine | @EDGE |

## Automation Strategy
Repeated runs with identical inputs; baseline hash compare; latency profiling.
