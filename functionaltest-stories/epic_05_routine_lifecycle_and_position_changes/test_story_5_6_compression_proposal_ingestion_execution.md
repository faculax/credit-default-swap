# Functional Test Story 5.6 – Compression Proposal Ingestion & Execution

Trace: story_5_6_compression_proposal_ingestion_execution
Tags: @EPIC_05 @STORY_5_6 @LIFECYCLE @COMPRESSION

## Objective
Validate ingestion, validation, and execution of compression proposals reducing gross notional while preserving net risk.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-5-6-001 | Upload proposal file accepted | API | @INGEST |
| FT-5-6-002 | Invalid file format rejected | API | @NEGATIVE |
| FT-5-6-003 | Schema validation missing column | API | @NEGATIVE |
| FT-5-6-004 | Duplicate proposal id blocked | API | @IDEMPOTENCY |
| FT-5-6-005 | Proposal parse populates staging table | DB | @PERSISTENCE |
| FT-5-6-006 | Validation mismatched trade ids | API | @NEGATIVE |
| FT-5-6-007 | Net risk invariance pre vs post | Engine | @RISK |
| FT-5-6-008 | Execute reduces gross notional target | Engine | @CALCULATION |
| FT-5-6-009 | Execution transactional (partial fail rollback) | Engine | @TRANSACTION |
| FT-5-6-010 | Audit entries for each compressed trade | API | @AUDIT |
| FT-5-6-011 | Performance execution < threshold | Engine | @PERFORMANCE |
| FT-5-6-012 | Unauthorized execution -> 403 | API | @SECURITY |
| FT-5-6-013 | Metrics compressionReductionNotional | Engine | @METRICS |
| FT-5-6-014 | Idempotent re-execute blocked | API | @IDEMPOTENCY |
| FT-5-6-015 | Large proposal memory usage within limit | Engine | @PERFORMANCE |
| FT-5-6-016 | Export post-compression summary | API | @EXPORT |
| FT-5-6-017 | Notification dispatch summary | API | @NOTIFY |
| FT-5-6-018 | Drift detection net risk deviation threshold | Engine | @DRIFT |
| FT-5-6-019 | Logging redacts sensitive identifiers | Engine | @SECURITY |
| FT-5-6-020 | Rate limit ingestion | API | @RESILIENCE |
| FT-5-6-021 | Time zone normalization timestamps | API | @TIME |

## Automation Strategy
Integration: ingest sample CSV; engine: compute pre/post aggregates; failure injection for rollback.
