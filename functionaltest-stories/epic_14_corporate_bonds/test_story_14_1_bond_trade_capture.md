# Functional Test Story 14.1 – Bond Trade Capture

Trace: story_14_1_bond-trade-capture
Tags: @EPIC_14 @CORPORATE_BONDS @TRADE_CAPTURE

## Objective
Validate UI/API capture of new corporate bond trades including validation rules, persistence, confirmation, and audit.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-1-001 | Capture new fixed coupon bond trade UI | E2E | @UI |
| FT-14-1-002 | Capture floating rate note trade UI | E2E | @UI |
| FT-14-1-003 | Validation missing ISIN -> error | API | @NEGATIVE |
| FT-14-1-004 | Validation invalid notional -> error | API | @NEGATIVE |
| FT-14-1-005 | Validation past settlement date -> error | API | @NEGATIVE |
| FT-14-1-006 | Persist trade record | DB | @PERSISTENCE |
| FT-14-1-007 | Generate booking confirmation PDF | API | @EXPORT |
| FT-14-1-008 | Unauthorized capture -> 403 | API | @SECURITY |
| FT-14-1-009 | RBAC trader vs viewer restrictions | API | @SECURITY |
| FT-14-1-010 | Audit entry stored | API | @AUDIT |
| FT-14-1-011 | Performance booking latency p95 | API | @PERFORMANCE |
| FT-14-1-012 | Drift booking latency baseline | API | @DRIFT |
| FT-14-1-013 | Logging redacts counterparty | API | @SECURITY |
| FT-14-1-014 | Accessibility capture form | E2E | @ACCESSIBILITY |
| FT-14-1-015 | Contract schema stable | Contract | @CONTRACT |
| FT-14-1-016 | Concurrency multi-booking isolation | API | @CONCURRENCY |
| FT-14-1-017 | Large notional edge case | API | @EDGE |
| FT-14-1-018 | Duplicate trade idempotency key | API | @RESILIENCE |

## Automation Strategy
Playwright form submission; REST negative matrix; DB assertion; PDF export hash baseline.
