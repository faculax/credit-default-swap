# Functional Test Story 6.0 – Index & Constituent Management

Trace: epic_06_index_and_constituent_management/README.md
Tags: @EPIC_06 @INDEX @MASTERING

## Objective
Validate index lifecycle: creation, versioning, constituent updates, reference data coherence, and caching semantics.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-6-0-001 | Create new index with constituents | API | @CRUD |
| FT-6-0-002 | Fetch index returns constituent list | API | @CRUD |
| FT-6-0-003 | Add constituent (version increments) | API | @VERSIONING |
| FT-6-0-004 | Duplicate constituent prevented | API | @NEGATIVE |
| FT-6-0-005 | Remove constituent archived correctly | API | @CRUD |
| FT-6-0-006 | Fetch historical version returns old list | API | @VERSIONING |
| FT-6-0-007 | Search index by name partial match | API | @FILTER |
| FT-6-0-008 | Pagination stable ordering | API | @PAGINATION |
| FT-6-0-009 | Unauthorized create -> 403 | API | @SECURITY |
| FT-6-0-010 | Missing auth -> 401 | API | @SECURITY |
| FT-6-0-011 | Cache warm fetch faster than cold | API | @PERFORMANCE |
| FT-6-0-012 | Cache invalidated after update | API | @CACHE |
| FT-6-0-013 | Reference data enrichment (issuer names) | API | @REFDATA |
| FT-6-0-014 | Invalid ISIN rejected | API | @NEGATIVE |
| FT-6-0-015 | Export index constituents CSV | API | @EXPORT |

## Automation Strategy
Integration tests with Testcontainers; time-stamped versions; cache layer test toggling.
