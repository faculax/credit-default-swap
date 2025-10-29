# Functional Test Story 8.1 – CCP Novation Account Enrichment

Trace: story_8_1_ccp_novation_account_enrichment
Tags: @EPIC_08 @STORY_8_1 @MARGIN @NOVATION

## Objective
Validate enrichment of trades with CCP novation data and clearing account attributes.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-8-1-001 | Enrich trade with CCP account ids | API | @ENRICH |
| FT-8-1-002 | Missing CCP mapping -> validation error | API | @NEGATIVE |
| FT-8-1-003 | Unauthorized enrichment -> 403 | API | @SECURITY |
| FT-8-1-004 | Duplicate enrichment idempotent | API | @IDEMPOTENCY |
| FT-8-1-005 | Audit entry on enrichment | API | @AUDIT |
| FT-8-1-006 | Metrics enrichmentCount increments | API | @METRICS |
| FT-8-1-007 | Concurrency enrichment single result | API | @CONCURRENCY |
| FT-8-1-008 | Cache update after enrichment | API | @CACHE |
| FT-8-1-009 | Export enriched trades view | API | @EXPORT |
| FT-8-1-010 | Logging redacts account credentials | API | @SECURITY |
| FT-8-1-011 | Time zone normalization | API | @TIME |
| FT-8-1-012 | Partial batch failure rollback | API | @TRANSACTION |
| FT-8-1-013 | Performance batch 100 trades < threshold | API | @PERFORMANCE |
| FT-8-1-014 | Validation invalid clearing member | API | @NEGATIVE |
| FT-8-1-015 | Retry transient CCP lookup failure | API | @RESILIENCE |
| FT-8-1-016 | Deterministic enrichment order | API | @CONSISTENCY |
| FT-8-1-017 | Enrichment diff endpoint | API | @DIFF |

## Automation Strategy
Integration harness with mock CCP reference service & concurrency injection.
