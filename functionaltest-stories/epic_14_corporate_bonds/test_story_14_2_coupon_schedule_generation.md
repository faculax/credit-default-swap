# Functional Test Story 14.2 – Coupon Schedule Generation

Trace: story_14_2_coupon-schedule-generation
Tags: @EPIC_14 @CORPORATE_BONDS @COUPON_SCHEDULE

## Objective
Validate system generates accurate coupon payment schedules including stubs, business day adjustments, and holiday calendars.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-2-001 | Generate standard semi-annual schedule | Engine | @POSITIVE |
| FT-14-2-002 | Generate quarterly schedule | Engine | @POSITIVE |
| FT-14-2-003 | Long stub first period handling | Engine | @EDGE |
| FT-14-2-004 | Short stub last period handling | Engine | @EDGE |
| FT-14-2-005 | Business day adjustment following | Engine | @NUMERIC |
| FT-14-2-006 | Business day adjustment modified following | Engine | @NUMERIC |
| FT-14-2-007 | Holiday calendar shift | Engine | @NUMERIC |
| FT-14-2-008 | Unauthorized schedule fetch -> 403 | API | @SECURITY |
| FT-14-2-009 | Drift schedule baseline comparison | Engine | @DRIFT |
| FT-14-2-010 | Logging redacts holiday calendar ids | API | @SECURITY |
| FT-14-2-011 | Metrics scheduleGenerationLatency | Engine | @METRICS |
| FT-14-2-012 | Performance latency p95 threshold | Engine | @PERFORMANCE |
| FT-14-2-013 | Accessibility schedule UI | E2E | @ACCESSIBILITY |
| FT-14-2-014 | Contract schedule schema stable | Contract | @CONTRACT |
| FT-14-2-015 | Concurrency multi-generation isolation | Engine | @CONCURRENCY |
| FT-14-2-016 | Edge leap year coupon date | Engine | @EDGE |
| FT-14-2-017 | Edge end-of-month rule | Engine | @EDGE |

## Automation Strategy
Generate schedules; compare with baseline JSON; negative unauthorized; metrics/time assertions.
