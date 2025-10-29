# Functional Test Story 13.14 – Progress WebSocket Stream

Trace: story_13_14_progress-websocket-stream
Tags: @EPIC_13 @SIMULATION @WEBSOCKET

## Objective
Validate real-time progress updates over WebSocket including percentage, ETA, and status transitions.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-14-001 | Establish WebSocket connection | API | @WEBSOCKET |
| FT-13-14-002 | Receive initial progress frame | API | @WEBSOCKET |
| FT-13-14-003 | Percentage increments monotonically | Engine | @NUMERIC |
| FT-13-14-004 | ETA decreases reasonably | Engine | @NUMERIC |
| FT-13-14-005 | Completion frame status=COMPLETED | API | @WEBSOCKET |
| FT-13-14-006 | Error frame on failure | API | @NEGATIVE |
| FT-13-14-007 | Unauthorized connection -> 403/close | API | @SECURITY |
| FT-13-14-008 | Drift progress pattern baseline | Engine | @DRIFT |
| FT-13-14-009 | Logging redacts session token | API | @SECURITY |
| FT-13-14-010 | Metrics websocketProgressSubscribers | API | @METRICS |
| FT-13-14-011 | Accessibility progress live region | E2E | @ACCESSIBILITY |
| FT-13-14-012 | Concurrency multiple subscribers | API | @CONCURRENCY |
| FT-13-14-013 | API contract stable | Contract | @CONTRACT |
| FT-13-14-014 | Performance frame interval < threshold | API | @PERFORMANCE |

## Automation Strategy
WebSocket client harness; capture frames; assert ordering and monotonicity; baseline frame log.
