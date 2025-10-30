# Functional Test Story 16.10 – Bond Pricing Endpoint

Trace: story_16_10_bond-pricing-endpoint
Tags: @EPIC_16 @CREDIT_BONDS @PRICING

## Objective
Validate bond pricing endpoint returns correct clean/dirty price, yield, accrued interest, and handles errors.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-10-001 | Price standard fixed bond | API | @POSITIVE |
| FT-16-10-002 | Price FRN bond | API | @POSITIVE |
| FT-16-10-003 | Unauthorized pricing -> 403 | API | @SECURITY |
| FT-16-10-004 | Drift pricing baseline | API | @DRIFT |
| FT-16-10-005 | Logging redacts curve data | API | @SECURITY |
| FT-16-10-006 | Metrics pricingEndpointLatency | API | @METRICS |
| FT-16-10-007 | Performance p95 pricing latency | API | @PERFORMANCE |
| FT-16-10-008 | Contract pricing endpoint schema stable | Contract | @CONTRACT |
| FT-16-10-009 | Concurrency multi-pricing isolation | API | @CONCURRENCY |
| FT-16-10-010 | Edge negative yield pricing | API | @EDGE |
| FT-16-10-011 | Edge near maturity pricing | API | @EDGE |
| FT-16-10-012 | Edge distressed spread pricing | API | @EDGE |
| FT-16-10-013 | Error invalid request payload -> 400 | API | @NEGATIVE |
| FT-16-10-014 | Export pricing response JSON | API | @EXPORT |
| FT-16-10-015 | Rate limit exceeded -> 429 | API | @RESILIENCE |
| FT-16-10-016 | Absent bond id -> 404 | API | @NEGATIVE |

## Automation Strategy
API tests vs baseline pricing; negative error matrix; performance and concurrency checks.
