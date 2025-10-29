# Functional Test Story 4.5 – Settlement Instructions Persistence

Trace: story_4_5_settlement_instructions_persistence
Tags: @EPIC_04 @STORY_4_5 @CREDIT_EVENT @PERSISTENCE

## Objective
Ensure settlement instructions are validated, persisted, retrievable, and secure.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-4-5-001 | Add settlement instruction for trade | API | @CRUD |
| FT-4-5-002 | Fetch instruction returns masked sensitive fields | API | @SECURITY |
| FT-4-5-003 | Validation missing account number | API | @NEGATIVE |
| FT-4-5-004 | Invalid currency code | API | @NEGATIVE |
| FT-4-5-005 | Update instruction modifies version | API | @VERSIONING |
| FT-4-5-006 | Delete instruction soft-deletes | API | @CRUD |
| FT-4-5-007 | Duplicate active instruction prevented | API | @NEGATIVE |
| FT-4-5-008 | Audit log entry create/update/delete | API | @AUDIT |
| FT-4-5-009 | Unauthorized role cannot view instructions | API | @SECURITY |
| FT-4-5-010 | Encryption at rest (field not stored plaintext) | DB | @SECURITY |
| FT-4-5-011 | List pagination stable ordering | API | @PAGINATION |
| FT-4-5-012 | Search by beneficiary name case-insensitive | API | @FILTER |
| FT-4-5-013 | Concurrency update conflict -> 409 | API | @CONCURRENCY |
| FT-4-5-014 | Rate limit on modifications -> 429 | API | @RESILIENCE |
| FT-4-5-015 | Export instructions excludes masked section | API | @EXPORT |

## Automation Strategy
Integration: encryption verified by raw DB query (Testcontainers) ensuring ciphertext differs from input.
