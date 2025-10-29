# Functional Test Story 14.2 – Bond Domain Entity Repository Mapping

Trace: story_14_2_bond-domain-entity-repository-mapping
Tags: @EPIC_14 @CREDIT_BONDS @DOMAIN

## Objective
Validate domain entity fields mapping and repository CRUD operations with transaction integrity.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-2-001 | Create bond entity | DB | @PERSISTENCE |
| FT-14-2-002 | Read bond entity | DB | @PERSISTENCE |
| FT-14-2-003 | Update bond entity | DB | @PERSISTENCE |
| FT-14-2-004 | Delete bond entity | DB | @PERSISTENCE |
| FT-14-2-005 | Unauthorized CRUD action -> 403 | API | @SECURITY |
| FT-14-2-006 | Drift entity schema baseline | DB | @DRIFT |
| FT-14-2-007 | Logging redacts internal IDs | API | @SECURITY |
| FT-14-2-008 | Metrics repositoryLatency | DB | @METRICS |
| FT-14-2-009 | Performance CRUD latency p95 | DB | @PERFORMANCE |
| FT-14-2-010 | Concurrency multi-CRUD isolation | DB | @CONCURRENCY |
| FT-14-2-011 | Edge update with stale version -> conflict | DB | @RESILIENCE |
| FT-14-2-012 | Edge large text fields truncated safe | DB | @EDGE |
| FT-14-2-013 | Transaction rollback on failure | DB | @RESILIENCE |
| FT-14-2-014 | Contract CRUD schema stable | Contract | @CONTRACT |

## Automation Strategy
Repository CRUD tests; concurrency; conflict simulation; transaction rollback verification.
