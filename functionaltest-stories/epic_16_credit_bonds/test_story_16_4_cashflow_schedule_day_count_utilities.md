# Functional Test Story 16.4 – Cashflow Schedule Day Count Utilities

Trace: story_16_4_cashflow-schedule-day-count-utilities
Tags: @EPIC_16 @CREDIT_BONDS @CASHFLOW

## Objective
Validate day count utilities produce correct accrual fractions for various conventions and edge dates.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-4-001 | ACT/ACT ICMA fraction | Engine | @NUMERIC |
| FT-16-4-002 | ACT/360 fraction | Engine | @NUMERIC |
| FT-16-4-003 | 30/360 fraction | Engine | @NUMERIC |
| FT-16-4-004 | ACT/365 fraction | Engine | @NUMERIC |
| FT-16-4-005 | Unauthorized day count request -> 403 | API | @SECURITY |
| FT-16-4-006 | Drift fraction baseline comparison | Engine | @DRIFT |
| FT-16-4-007 | Logging redacts schedule details | API | @SECURITY |
| FT-16-4-008 | Metrics dayCountCalcLatency | Engine | @METRICS |
| FT-16-4-009 | Performance p95 calc latency | Engine | @PERFORMANCE |
| FT-16-4-010 | Accessibility day count UI | E2E | @ACCESSIBILITY |
| FT-16-4-011 | Concurrency multi-calculation isolation | Engine | @CONCURRENCY |
| FT-16-4-012 | Edge leap year Feb 29 handling | Engine | @EDGE |
| FT-16-4-013 | Edge end-of-month adjustment | Engine | @EDGE |
| FT-16-4-014 | Contract day count schema stable | Contract | @CONTRACT |
| FT-16-4-015 | Edge very short period fraction | Engine | @EDGE |
| FT-16-4-016 | Edge very long period fraction | Engine | @EDGE |

## Automation Strategy
Calculate fractions vs baseline dataset; edge date matrix; performance & concurrency.
