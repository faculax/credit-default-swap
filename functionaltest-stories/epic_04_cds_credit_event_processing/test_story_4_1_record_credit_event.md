# Functional Test Story 4.1 – Record Credit Event

Trace: story_4_1_record_credit_event
Tags: @EPIC_04 @STORY_4_1 @CREDIT_EVENT @CRITPATH

## Objective
Validate recording of a credit event through UI & API including propagation messaging.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-4-1-001 | Open credit events tab loads list | E2E | @UI |
| FT-4-1-002 | Record DEFAULT single trade success | E2E | @UI |
| FT-4-1-003 | Propagate DEFAULT to related trades | API | @PROPAGATION |
| FT-4-1-004 | UI success toast shows propagation count | E2E | @UI |
| FT-4-1-005 | Prevent duplicate event same type+date | API | @IDEMPOTENCY |
| FT-4-1-006 | Validation missing eventType | API | @NEGATIVE |
| FT-4-1-007 | Invalid settlement method | API | @NEGATIVE |
| FT-4-1-008 | Settlement calc invoked correct parameters | API | @CALCULATION |
| FT-4-1-009 | Trade status ACTIVE -> SETTLED | API | @STATE |
| FT-4-1-010 | All propagated trades status updated | API | @STATE |
| FT-4-1-011 | Partial failure rollback (simulate) | API | @TRANSACTION |
| FT-4-1-012 | Audit entry per affected trade | API | @AUDIT |
| FT-4-1-013 | Toast auto dismiss timing | E2E | @UX |
| FT-4-1-014 | List refresh after record | E2E | @UI |
| FT-4-1-015 | Unauthorized role -> 403 | API | @SECURITY |
| FT-4-1-016 | Token refresh mid-submit | E2E | @SECURITY |
| FT-4-1-017 | Loading spinner visible > 300ms latency | E2E | @UX |
| FT-4-1-018 | Retry on transient 502 (max3) | API | @RESILIENCE |
| FT-4-1-019 | Idempotent resubmit identical payload no duplicate | API | @IDEMPOTENCY |
| FT-4-1-020 | Concurrency race two submits single record | API | @CONCURRENCY |
| FT-4-1-021 | Negative payout prevented | API | @CALCULATION |
| FT-4-1-022 | Batch propagation perf (<1.2s /50) | API | @PERFORMANCE |

## Automation Strategy
Playwright for UI flows, integration for propagation & rollback using explicit transaction failure injection.
