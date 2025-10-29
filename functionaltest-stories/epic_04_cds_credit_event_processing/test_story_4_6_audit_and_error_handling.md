# Functional Test Story 4.6 – Audit & Error Handling

Trace: story_4_6_audit_and_error_handling
Tags: @EPIC_04 @STORY_4_6 @CREDIT_EVENT @AUDIT

## Objective
Guarantee comprehensive audit coverage and robust error translation for credit event domain.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-4-6-001 | Audit entry on event success | API | @AUDIT |
| FT-4-6-002 | Audit entry on event validation failure | API | @AUDIT |
| FT-4-6-003 | Audit entry on settlement calc failure | API | @AUDIT |
| FT-4-6-004 | Error classification: validation -> 400 JSON body codes | API | @ERROR |
| FT-4-6-005 | Error classification: not found -> 404 | API | @ERROR |
| FT-4-6-006 | Error classification: security -> 403 | API | @SECURITY |
| FT-4-6-007 | Unhandled exception -> 500 generic message | API | @ERROR |
| FT-4-6-008 | Stack trace not leaked to client | API | @SECURITY |
| FT-4-6-009 | Correlation id propagated in responses | API | @OBSERVABILITY |
| FT-4-6-010 | Rate limit exceed error contains retry-after header | API | @RESILIENCE |
| FT-4-6-011 | Audit reads filterable by tradeId & eventType | API | @FILTER |
| FT-4-6-012 | Pagination stable ordering by timestamp desc | API | @PAGINATION |
| FT-4-6-013 | Sensitive fields redacted in audit payload | API | @SECURITY |
| FT-4-6-014 | Tamper attempt (invalid signature) logged | API | @SECURITY |
| FT-4-6-015 | Retry exhaustion logged with attempt count | API | @RESILIENCE |
| FT-4-6-016 | Circuit breaker open event logged (if impl) | API | @RESILIENCE |
| FT-4-6-017 | Audit retention policy respected (trim job) | API | @MAINTENANCE |
| FT-4-6-018 | Time skew handling (client future timestamp) | API | @NEGATIVE |
| FT-4-6-019 | Localization of error messages (default English) | API | @I18N |

## Automation Strategy
Integration tests with induced failures (mock settlement calc). DB assertions on audit table contents & redaction fields.
