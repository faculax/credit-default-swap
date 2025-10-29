# Functional Test Story 9.0 – Reference & Market Data Mastering

Trace: epic_09_reference_and_market_data_mastering/README.md
Tags: @EPIC_09 @REFDATA @MASTERING

## Objective
Validate mastering lifecycle for reference & market data (entities, instruments, curves) including validation, versioning, caching.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-9-0-001 | Ingest reference entity record | API | @INGEST |
| FT-9-0-002 | Ingest instrument mapping | API | @INGEST |
| FT-9-0-003 | Ingest market data snapshot | API | @INGEST |
| FT-9-0-004 | Validation missing required attribute | API | @NEGATIVE |
| FT-9-0-005 | Duplicate instrument prevented | API | @NEGATIVE |
| FT-9-0-006 | Fetch latest snapshot versioned | API | @VERSIONING |
| FT-9-0-007 | Fetch historical snapshot | API | @VERSIONING |
| FT-9-0-008 | Snapshot diff endpoint | API | @DIFF |
| FT-9-0-009 | Unauthorized ingestion -> 403 | API | @SECURITY |
| FT-9-0-010 | Cache invalidation after new snapshot | API | @CACHE |
| FT-9-0-011 | Dependent risk service notified | API | @NOTIFY |
| FT-9-0-012 | Metrics snapshotIngestLatency | API | @METRICS |
| FT-9-0-013 | Large snapshot performance | API | @PERFORMANCE |
| FT-9-0-014 | Export snapshot JSON | API | @EXPORT |
| FT-9-0-015 | Negative value validation failure | API | @NEGATIVE |
| FT-9-0-016 | Time zone normalization effective date | API | @TIME |
| FT-9-0-017 | Drift baseline detection (curve shape) | Engine | @DRIFT |
| FT-9-0-018 | Logging redacts proprietary vendor ids | API | @SECURITY |
| FT-9-0-019 | Rate limit ingestion | API | @RESILIENCE |
| FT-9-0-020 | Accessibility UI mastering table (if exists) | E2E | @ACCESSIBILITY |

## Automation Strategy
Integration ingestion tests with fixture JSON; caching tests verifying stale vs fresh timestamps.
