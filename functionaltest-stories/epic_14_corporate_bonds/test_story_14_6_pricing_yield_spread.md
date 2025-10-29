# Functional Test Story 14.6 – Pricing Yield & Spread

Trace: story_14_6_pricing-yield-spread
Tags: @EPIC_14 @CORPORATE_BONDS @PRICING

## Objective
Validate pricing engine computes clean price, dirty price, yield to maturity, and Z-spread accurately.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-6-001 | Compute clean vs dirty price fixed bond | Engine | @NUMERIC |
| FT-14-6-002 | Compute yield to maturity | Engine | @NUMERIC |
| FT-14-6-003 | Compute Z-spread from curve | Engine | @NUMERIC |
| FT-14-6-004 | Unauthorized pricing request -> 403 | API | @SECURITY |
| FT-14-6-005 | Drift pricing baseline comparison | Engine | @DRIFT |
| FT-14-6-006 | Logging redacts internal curve nodes | API | @SECURITY |
| FT-14-6-007 | Metrics pricingLatency | Engine | @METRICS |
| FT-14-6-008 | Performance p95 pricing latency | Engine | @PERFORMANCE |
| FT-14-6-009 | Contract pricing schema stable | Contract | @CONTRACT |
| FT-14-6-010 | Accessibility pricing UI | E2E | @ACCESSIBILITY |
| FT-14-6-011 | Concurrency multi-pricing requests | API | @CONCURRENCY |
| FT-14-6-012 | Edge negative yield (market stress) | Engine | @EDGE |
| FT-14-6-013 | Edge very short maturity | Engine | @EDGE |
| FT-14-6-014 | Edge very long maturity | Engine | @EDGE |

## Automation Strategy
Pricing harness vs baseline JSON; concurrency stress; negative yield scenario injection.
