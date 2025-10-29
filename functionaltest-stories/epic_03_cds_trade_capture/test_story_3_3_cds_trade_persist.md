# Functional Test Story 3.3 – Trade Persistence & Versioning

Trace: story_3_3_cds_trade_persist
Tags: @EPIC_03 @STORY_3_3 @TRADE @PERSISTENCE

## Objective
Ensure trades persist accurately with versioning, idempotency, and transactional integrity.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-3-3-001 | Initial trade POST persists row | API | @CRUD |
| FT-3-3-002 | Fetch by ID returns full payload | API | @CRUD |
| FT-3-3-003 | List endpoint includes new trade | API | @CRUD |
| FT-3-3-004 | Update (amend) increments version | API | @VERSIONING |
| FT-3-3-005 | Version field monotonic | API | @VERSIONING |
| FT-3-3-006 | Partial failure rollback leaves 0 rows | API | @TRANSACTION |
| FT-3-3-007 | Concurrency: two amendments only one wins (409) | API | @CONCURRENCY |
| FT-3-3-008 | Soft delete (if supported) sets status flag | API | @STATUS |
| FT-3-3-009 | Audit entry on create | API | @AUDIT |
| FT-3-3-010 | Idempotent create same payload same key returns same tradeId | API | @IDEMPOTENCY |
| FT-3-3-011 | Idempotent create different payload -> 409 | API | @IDEMPOTENCY |
| FT-3-3-012 | Pagination stable order by createdAt desc | API | @PAGINATION |
| FT-3-3-013 | Filtering by reference entity returns subset | API | @FILTER |
| FT-3-3-014 | Search by ISIN case-insensitive | API | @FILTER |
| FT-3-3-015 | Export endpoint returns CSV with header row | API | @EXPORT |
| FT-3-3-016 | Health check reports DB up | API | @OBSERVABILITY |

## Automation Strategy
Integration suite using Testcontainers for Postgres.

## Data Strategy
Seed 3 baseline trades for list & filter tests.

## Metrics
- Version increment latency < 250ms.

## Open Questions
- Confirm soft delete semantics vs hard delete.
