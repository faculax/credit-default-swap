# Functional Test Story 14.3 – Spread Curve Construction

Trace: story_14_3_spread-curve-construction
Tags: @EPIC_14 @CORPORATE_BONDS @CURVE

## Objective
Validate construction of corporate bond spread/yield curves from market quotes with interpolation and bootstrapping.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-3-001 | Build curve from standard tenors | Engine | @POSITIVE |
| FT-14-3-002 | Missing tenor fills via interpolation | Engine | @NUMERIC |
| FT-14-3-003 | Bootstrapping algorithm correctness | Engine | @NUMERIC |
| FT-14-3-004 | Extrapolation beyond longest tenor | Engine | @EDGE |
| FT-14-3-005 | Unauthorized curve request -> 403 | API | @SECURITY |
| FT-14-3-006 | Drift curve shape baseline | Engine | @DRIFT |
| FT-14-3-007 | Logging redacts raw quotes | API | @SECURITY |
| FT-14-3-008 | Metrics curveBuildLatency | Engine | @METRICS |
| FT-14-3-009 | Performance p95 build time | Engine | @PERFORMANCE |
| FT-14-3-010 | Contract curve schema stable | Contract | @CONTRACT |
| FT-14-3-011 | Accessibility curve UI | E2E | @ACCESSIBILITY |
| FT-14-3-012 | Concurrency multi-curve isolation | Engine | @CONCURRENCY |
| FT-14-3-013 | Edge illiquid tenor spread spike | Engine | @EDGE |
| FT-14-3-014 | Curve serialization export hash | API | @EXPORT |

## Automation Strategy
Curve build harness with baseline JSON; interpolation math checks; UI chart snapshot.
