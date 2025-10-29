# Functional Test Story 13.20 – Cancellation Flow

Trace: story_13_20_cancellation-flow
Tags: @EPIC_13 @SIMULATION @CANCELLATION

## Objective
Validate ability to cancel a running simulation and observe proper state transitions.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-20-001 | Issue cancellation request RUNNING | API | @POSITIVE |
| FT-13-20-002 | State transitions to CANCELLING | Engine | @STATE |
| FT-13-20-003 | Final state CANCELLED | Engine | @STATE |
| FT-13-20-004 | Unauthorized cancellation -> 403 | API | @SECURITY |
| FT-13-20-005 | Drift cancellation latency baseline | Engine | @DRIFT |
| FT-13-20-006 | Logging redacts cancellation reason | API | @SECURITY |
| FT-13-20-007 | Metrics cancellationCount | Engine | @METRICS |
| FT-13-20-008 | Concurrency multiple cancellations | API | @CONCURRENCY |
| FT-13-20-009 | API contract cancellation response | Contract | @CONTRACT |
| FT-13-20-010 | UI cancel button accessible | E2E | @ACCESSIBILITY |
| FT-13-20-011 | Performance cancellation completion time | Engine | @PERFORMANCE |
| FT-13-20-012 | Edge already completed cannot cancel | API | @NEGATIVE |

## Automation Strategy
Run submission and cancellation; WebSocket progress termination; metrics assertion.
