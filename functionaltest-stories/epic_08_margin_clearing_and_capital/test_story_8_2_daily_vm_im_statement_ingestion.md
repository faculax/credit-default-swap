# Functional Test Story 8.2 – Daily VM & IM Statement Ingestion

Trace: story_8_2_daily_vm_im_statement_ingestion
Tags: @EPIC_08 @STORY_8_2 @MARGIN @INGESTION

## Objective
Validate ingestion, parsing, validation, and persistence of daily variation & initial margin statements.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-8-2-001 | Upload VM statement CSV | API | @INGEST |
| FT-8-2-002 | Upload IM statement CSV | API | @INGEST |
| FT-8-2-003 | Invalid header rejected | API | @NEGATIVE |
| FT-8-2-004 | Missing row field -> error with line number | API | @NEGATIVE |
| FT-8-2-005 | Duplicate file hash blocked | API | @IDEMPOTENCY |
| FT-8-2-006 | Large file performance < threshold | API | @PERFORMANCE |
| FT-8-2-007 | Partial parse failure rollback | API | @TRANSACTION |
| FT-8-2-008 | Statement metrics captured | API | @METRICS |
| FT-8-2-009 | Export parsed statement JSON | API | @EXPORT |
| FT-8-2-010 | Unauthorized upload -> 403 | API | @SECURITY |
| FT-8-2-011 | Rate limit ingestion -> 429 | API | @RESILIENCE |
| FT-8-2-012 | Audit record ingestion | API | @AUDIT |
| FT-8-2-013 | File with extra columns tolerated | API | @LENIENT |
| FT-8-2-014 | Encoding UTF-8 BOM handled | API | @I18N |
| FT-8-2-015 | Retry transient storage failure | API | @RESILIENCE |
| FT-8-2-016 | Data quality check (sum matches total row) | API | @QUALITY |
| FT-8-2-017 | Metrics ingestionFailureCount increments | API | @METRICS |
| FT-8-2-018 | Decompression .gz supported | API | @INGEST |
| FT-8-2-019 | Clock skew on statement date flagged | API | @TIME |
| FT-8-2-020 | Logging redacts account ids | API | @SECURITY |

## Automation Strategy
Integration with fixture files (valid/invalid); hashing to block duplicates; metrics assertions.
