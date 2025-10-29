# Functional Test Story 14.10 – Bond Pricing Endpoint

Trace: story_14_10_bond-pricing-endpoint
Tags: @EPIC_14 @CREDIT_BONDS @PRICING

## Objective
Validate bond pricing endpoint returns correct clean/dirty price, yield, accrued interest, and handles errors.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-10-001 | Price standard fixed bond | API | @POSITIVE |
| FT-14-10-002 | Price FRN bond | API | @POSITIVE |
| FT-14-10-003 | Unauthorized pricing -> 403 | API | @SECURITY |
| FT-14-10-004 | Drift pricing baseline | API | @DRIFT |
| FT-14-10-005 | Logging redacts curve data | API | @SECURITY |
| FT-14-10-006 | Metrics pricingEndpointLatency | API | @METRICS |
| FT-14-10-007 | Performance p95 pricing latency | API | @PERFORMANCE |
| FT-14-10-008 | Contract pricing endpoint schema stable | Contract | @CONTRACT |
| FT-14-10-009 | Concurrency multi-pricing isolation | API | @CONCURRENCY |
| FT-14-10-010 | Edge negative yield pricing | API | @EDGE |
| FT-14-10-011 | Edge near maturity pricing | API | @EDGE |
| FT-14-10-012 | Edge distressed spread pricing | API | @EDGE |
| FT-14-10-013 | Error invalid request payload -> 400 | API | @NEGATIVE |
| FT-14-10-014 | Export pricing response JSON | API | @EXPORT |
| FT-14-10-015 | Rate limit exceeded -> 429 | API | @RESILIENCE |
| FT-14-10-016 | Absent bond id -> 404 | API | @NEGATIVE |

## Automation Strategy
API tests vs baseline pricing; negative error matrix; performance and concurrency checks.
