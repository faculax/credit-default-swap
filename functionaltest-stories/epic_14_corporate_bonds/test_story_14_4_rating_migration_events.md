# Functional Test Story 14.4 – Rating Migration Events

Trace: story_14_4_rating-migration-events
Tags: @EPIC_14 @CORPORATE_BONDS @RATING

## Objective
Validate processing of issuer rating changes and propagation to spread curves, pricing, and risk metrics.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-4-001 | Apply downgrade event | Engine | @POSITIVE |
| FT-14-4-002 | Apply upgrade event | Engine | @POSITIVE |
| FT-14-4-003 | Unauthorized rating event -> 403 | API | @SECURITY |
| FT-14-4-004 | Drift rating impact baseline | Engine | @DRIFT |
| FT-14-4-005 | Logging redacts issuer internal id | API | @SECURITY |
| FT-14-4-006 | Metrics ratingChangeCount | Engine | @METRICS |
| FT-14-4-007 | Performance propagation latency | Engine | @PERFORMANCE |
| FT-14-4-008 | Contract rating event schema stable | Contract | @CONTRACT |
| FT-14-4-009 | Accessibility rating change UI | E2E | @ACCESSIBILITY |
| FT-14-4-010 | Concurrency multiple events isolation | Engine | @CONCURRENCY |
| FT-14-4-011 | Edge multiple sequential downgrades | Engine | @EDGE |
| FT-14-4-012 | Edge simultaneous upgrade/downgrade conflict | Engine | @NEGATIVE |

## Automation Strategy
Inject events; recompute pricing/risk; compare baseline; negative conflict test.
