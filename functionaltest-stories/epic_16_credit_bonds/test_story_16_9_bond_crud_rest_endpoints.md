# Functional Test Story 16.9 – Bond CRUD REST Endpoints

Trace: story_16_9_bond-crud-rest-endpoints
Tags: @EPIC_16 @CREDIT_BONDS @CRUD

## Objective
Validate REST endpoints for creating, reading, updating, and deleting bonds including error handling and RBAC.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-16-9-001 | POST create bond | API | @POSITIVE |
| FT-16-9-002 | GET single bond | API | @POSITIVE |
| FT-16-9-003 | GET bond list pagination | API | @PAGINATION |
| FT-16-9-004 | PUT update bond | API | @POSITIVE |
| FT-16-9-005 | DELETE bond | API | @POSITIVE |
| FT-16-9-006 | Unauthorized access -> 403 | API | @SECURITY |
| FT-16-9-007 | Drift CRUD latency baseline | API | @DRIFT |
| FT-16-9-008 | Logging redacts issuer id | API | @SECURITY |
| FT-16-9-009 | Metrics crudLatency | API | @METRICS |
| FT-16-9-010 | Performance p95 CRUD latency | API | @PERFORMANCE |
| FT-16-9-011 | Contract CRUD schema stable | Contract | @CONTRACT |
| FT-16-9-012 | Concurrency multi-CRUD isolation | API | @CONCURRENCY |
| FT-16-9-013 | Edge invalid pagination params | API | @NEGATIVE |
| FT-16-9-014 | Edge delete non-existent bond -> 404 | API | @NEGATIVE |
| FT-16-9-015 | Edge update stale version -> 409 | API | @NEGATIVE |
| FT-16-9-016 | Rate limiting exceeded -> 429 | API | @RESILIENCE |
| FT-16-9-017 | Export bond list CSV | API | @EXPORT |
| FT-16-9-018 | Partial update (PATCH) supported | API | @POSITIVE |
| FT-16-9-019 | Filter by maturity range | API | @FILTER |
| FT-16-9-020 | Search by ISIN substring | API | @SEARCH |

## Automation Strategy
Playwright request fixtures; negative/error matrix; performance & drift checks; CSV export.
