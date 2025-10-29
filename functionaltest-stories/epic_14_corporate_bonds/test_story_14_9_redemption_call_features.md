# Functional Test Story 14.9 – Redemption & Call Features

Trace: story_14_9_redemption-call-features
Tags: @EPIC_14 @CORPORATE_BONDS @CALL

## Objective
Validate processing of bond call events, early redemption logic, and schedule adjustments.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-9-001 | Execute call event | Engine | @POSITIVE |
| FT-14-9-002 | Early redemption updates schedule | Engine | @NUMERIC |
| FT-14-9-003 | Unauthorized call execution -> 403 | API | @SECURITY |
| FT-14-9-004 | Drift call impact baseline | Engine | @DRIFT |
| FT-14-9-005 | Logging redacts internal call pricing | API | @SECURITY |
| FT-14-9-006 | Metrics callEventCount | Engine | @METRICS |
| FT-14-9-007 | Performance call processing latency | Engine | @PERFORMANCE |
| FT-14-9-008 | Contract call event schema stable | Contract | @CONTRACT |
| FT-14-9-009 | Accessibility call event UI | E2E | @ACCESSIBILITY |
| FT-14-9-010 | Concurrency multiple calls isolation | Engine | @CONCURRENCY |
| FT-14-9-011 | Edge call price equals par | Engine | @EDGE |
| FT-14-9-012 | Edge deep in-the-money call | Engine | @EDGE |

## Automation Strategy
Inject call events; verify schedule and pricing changes vs baseline; negative unauthorized tests.
