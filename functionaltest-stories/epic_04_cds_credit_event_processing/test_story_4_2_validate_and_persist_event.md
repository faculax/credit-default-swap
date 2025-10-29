# Functional Test Story 4.2 – Validate & Persist Event

Trace: story_4_2_validate_and_persist_event
Tags: @EPIC_04 @STORY_4_2 @CREDIT_EVENT @NEGATIVE

## Objective
Server validation matrix & persistence integrity for credit events.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-4-2-001 | Missing eventDate | API | @NEGATIVE |
| FT-4-2-002 | Event date future beyond allowed horizon | API | @NEGATIVE |
| FT-4-2-003 | Notice date before event date | API | @NEGATIVE |
| FT-4-2-004 | Unsupported event type | API | @NEGATIVE |
| FT-4-2-005 | Duplicate same key composite blocked | API | @IDEMPOTENCY |
| FT-4-2-006 | Large comments length truncated | API | @SANITIZE |
| FT-4-2-007 | Audit persisted on success | API | @AUDIT |
| FT-4-2-008 | Audit persisted on failure (reason) | API | @AUDIT |
| FT-4-2-009 | Unauthorized -> 403 | API | @SECURITY |
| FT-4-2-010 | Missing auth -> 401 | API | @SECURITY |
| FT-4-2-011 | DB constraint violation rollback | API | @TRANSACTION |
| FT-4-2-012 | Rate limit exceeded -> 429 | API | @RESILIENCE |
| FT-4-2-013 | Concurrency unique constraint robust | API | @CONCURRENCY |
| FT-4-2-014 | JSON schema invalid type -> 400 | Contract | @SCHEMA |
| FT-4-2-015 | Invalid settlement method vs event type | API | @NEGATIVE |
| FT-4-2-016 | UTF-8 multibyte comments stored correctly | API | @I18N |
| FT-4-2-017 | SQL injection attempt neutralized | API | @SECURITY |
| FT-4-2-018 | Null optional comments handled | API | @NULL |

## Automation Strategy
Integration with deliberate constraint violation (duplicate PK) injection.
