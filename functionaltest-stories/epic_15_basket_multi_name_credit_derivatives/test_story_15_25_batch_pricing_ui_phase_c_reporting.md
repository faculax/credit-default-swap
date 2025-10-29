# Story 15.25 - Batch Pricing UI Phase C Reporting

## Objective
Finalize batch pricing UI with reporting/export features: PDF/CSV/JSON artifact generation, accessibility of report components, schema stability, performance of export operations, error handling, and deterministic artifact hashing.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-25-001 | Generate PDF report with summary & metrics | UI/API | @REPORT @PDF |
| FT-15-25-002 | CSV export includes correlation bump results columns | API | @REPORT @CSV |
| FT-15-25-003 | JSON export schema hash stable | API | @SCHEMA @STABILITY |
| FT-15-25-004 | Deterministic seed yields identical PDF hash | Domain | @DETERMINISM @PDF |
| FT-15-25-005 | Unauthorized export returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-25-006 | Rate limit on repeated export requests | API | @SECURITY @RATE_LIMIT |
| FT-15-25-007 | Accessibility: report table headers and landmarks | UI | @ACCESSIBILITY @A11Y |
| FT-15-25-008 | Performance: export operation latency < threshold | API | @PERFORMANCE @LATENCY |
| FT-15-25-009 | Drift detection: CSV hash stable across deterministic runs | Domain | @DRIFT @CSV |
| FT-15-25-010 | Error handling: failed PDF generation logs error code REPORT_PDF_FAIL | API | @ERROR @LOGGING |
| FT-15-25-011 | Concurrency: two exports in parallel do not corrupt artifacts | API | @CONCURRENCY @RESILIENCE |

## Automation Strategy
1. Run batch pricing; trigger PDF export; capture file & hash.
2. Generate CSV & JSON exports; validate columns & schema hash.
3. Deterministic seed check: re-run & compare artifact hashes.
4. Negative tests (unauthorized, rate limit, forced PDF failure).
5. Concurrency test: parallel exports; verify distinct artifact integrity.
6. Accessibility audit on report UI (tables, landmarks, aria labels).
7. Performance measurement of export latency.
8. Drift: hash CSV artifact vs baseline fixture.

## Metrics
- reportExportLatency
- reportExportFailureCount

## Exit Criteria
Reporting exports stable, deterministic (seeded), accessible, performant, and resilient under concurrency.
