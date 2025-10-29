# Functional Test Story 15.1 – Basket Domain Model Persistence

Trace: story_15_1_basket-domain-model-persistence
Tags: @EPIC_15 @BASKET @DOMAIN

## Objective
Validate persistence of basket entity including constituent list, weights, correlation matrix reference, and configuration flags.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-1-001 | Persist basket entity | DB | @PERSISTENCE |
| FT-15-1-002 | Read basket entity | DB | @PERSISTENCE |
| FT-15-1-003 | Update basket weights | DB | @PERSISTENCE |
| FT-15-1-004 | Delete basket entity | DB | @PERSISTENCE |
| FT-15-1-005 | Unauthorized CRUD -> 403 | API | @SECURITY |
| FT-15-1-006 | Drift basket schema baseline | DB | @DRIFT |
| FT-15-1-007 | Logging redacts correlation matrix | API | @SECURITY |
| FT-15-1-008 | Metrics basketPersistLatency | DB | @METRICS |
| FT-15-1-009 | Performance CRUD p95 latency | DB | @PERFORMANCE |
| FT-15-1-010 | Concurrency multi-CRUD isolation | DB | @CONCURRENCY |
| FT-15-1-011 | Edge empty constituent list rejected | API | @NEGATIVE |
| FT-15-1-012 | Edge duplicate constituent rejected | API | @NEGATIVE |
| FT-15-1-013 | Version conflict on stale update | DB | @RESILIENCE |
| FT-15-1-014 | Export basket entity JSON | API | @EXPORT |
| FT-15-1-015 | Large basket scaling persistence | DB | @SCALING |
| FT-15-1-016 | Contract basket schema stable | Contract | @CONTRACT |

## Automation Strategy
Repository CRUD tests, negative validation matrix, scaling test, schema snapshot compare.
