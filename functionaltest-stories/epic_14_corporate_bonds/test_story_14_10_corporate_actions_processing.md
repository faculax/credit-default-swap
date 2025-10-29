# Functional Test Story 14.10 – Corporate Actions Processing

Trace: story_14_10_corporate-actions-processing
Tags: @EPIC_14 @CORPORATE_BONDS @CORP_ACTIONS

## Objective
Validate ingestion and processing of corporate actions (coupon rate change, ticker change, merger) with correct propagation.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-10-001 | Process coupon rate change | Engine | @POSITIVE |
| FT-14-10-002 | Process ticker change | Engine | @POSITIVE |
| FT-14-10-003 | Process merger action | Engine | @POSITIVE |
| FT-14-10-004 | Unauthorized corporate action -> 403 | API | @SECURITY |
| FT-14-10-005 | Drift action impact baseline | Engine | @DRIFT |
| FT-14-10-006 | Logging redacts issuer internal IDs | API | @SECURITY |
| FT-14-10-007 | Metrics corporateActionCount | Engine | @METRICS |
| FT-14-10-008 | Performance action propagation latency | Engine | @PERFORMANCE |
| FT-14-10-009 | Contract corporate action schema stable | Contract | @CONTRACT |
| FT-14-10-010 | Accessibility corporate actions UI | E2E | @ACCESSIBILITY |
| FT-14-10-011 | Concurrency multi-actions isolation | Engine | @CONCURRENCY |
| FT-14-10-012 | Edge conflicting coupon updates | Engine | @NEGATIVE |

## Automation Strategy
Action ingestion harness; verify downstream pricing/risk updates; conflict negative test.
