# Functional Test Story 5.5 – Novation Party Role Transition

Trace: story_5_5_novation_party_role_transition
Tags: @EPIC_05 @STORY_5_5 @LIFECYCLE @NOVATION

## Objective
Validate novation workflow transitions party roles (Transferor, Transferee, Remaining Party) with proper status, audit, and permission checks.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-5-5-001 | Initiate novation request | API | @CRUD |
| FT-5-5-002 | Validation missing transferee | API | @NEGATIVE |
| FT-5-5-003 | Unauthorized role cannot initiate | API | @SECURITY |
| FT-5-5-004 | Approval transitions state PENDING->APPROVED | API | @STATE |
| FT-5-5-005 | Rejection transitions to REJECTED with reason | API | @STATE |
| FT-5-5-006 | Apply novation updates counterparty | API | @STATE |
| FT-5-5-007 | Audit entries each step | API | @AUDIT |
| FT-5-5-008 | Duplicate approval idempotent | API | @IDEMPOTENCY |
| FT-5-5-009 | Concurrency apply conflict -> 409 | API | @CONCURRENCY |
| FT-5-5-010 | No orphaned references to old counterparty | DB | @CONSISTENCY |
| FT-5-5-011 | Notification to stakeholders | API | @NOTIFY |
| FT-5-5-012 | Metrics novationCount increments | API | @METRICS |
| FT-5-5-013 | Search novations by status | API | @FILTER |
| FT-5-5-014 | Pagination stable order | API | @PAGINATION |
| FT-5-5-015 | Time zone normalization | API | @TIME |
| FT-5-5-016 | Export novation report CSV | API | @EXPORT |

## Automation Strategy
Integration tests with mock parties; DB-level assertion for referential integrity after apply.
