# Functional Test Story 16.1 – DB Migration Bonds Table

Trace: story_16_1_db-migration-bonds-table
Tags: @EPIC_16 @CREDIT_BONDS @DB

## Objective
Validate database migration creates bonds table, indexes, constraints, and is idempotent.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-1-001 | Migration applies new bonds table | DB | @POSITIVE |
| FT-16-1-002 | Primary key constraint present | DB | @SCHEMA |
| FT-16-1-003 | Unique ISIN index created | DB | @SCHEMA |
| FT-16-1-004 | Not-null constraints enforced | DB | @SCHEMA |
| FT-16-1-005 | Foreign key (issuer) validated | DB | @SCHEMA |
| FT-16-1-006 | Idempotent re-run no changes | DB | @RESILIENCE |
| FT-16-1-007 | Unauthorized migration attempt -> 403 | API | @SECURITY |
| FT-16-1-008 | Drift schema hash baseline | DB | @DRIFT |
| FT-16-1-009 | Logging redacts credentials | DB | @SECURITY |
| FT-16-1-010 | Metrics migrationVersionApplied | DB | @METRICS |
| FT-16-1-011 | Performance migration execution time | DB | @PERFORMANCE |
| FT-16-1-012 | Edge rollback on failure restored | DB | @RESILIENCE |

## Automation Strategy
Testcontainers migration run; schema introspection; hash compare; negative unauthorized test.
