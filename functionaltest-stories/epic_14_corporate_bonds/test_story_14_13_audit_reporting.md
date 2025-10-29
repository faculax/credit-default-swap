# Functional Test Story 14.13 – Audit & Reporting

Trace: story_14_13_audit-reporting
Tags: @EPIC_14 @CORPORATE_BONDS @AUDIT

## Objective
Validate completeness of audit entries for bond lifecycle events and integrity of reporting exports.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-13-001 | Audit trade capture | API | @AUDIT |
| FT-14-13-002 | Audit pricing request | API | @AUDIT |
| FT-14-13-003 | Audit risk calculation | API | @AUDIT |
| FT-14-13-004 | Audit corporate action | API | @AUDIT |
| FT-14-13-005 | Unauthorized audit fetch -> 403 | API | @SECURITY |
| FT-14-13-006 | Drift audit counts baseline | API | @DRIFT |
| FT-14-13-007 | Logging redacts user principal | API | @SECURITY |
| FT-14-13-008 | Metrics auditEntryCount | API | @METRICS |
| FT-14-13-009 | Export audit report CSV | API | @EXPORT |
| FT-14-13-010 | Contract audit schema stable | Contract | @CONTRACT |
| FT-14-13-011 | Accessibility audit report UI | E2E | @ACCESSIBILITY |
| FT-14-13-012 | Concurrency audit writes atomic | API | @CONCURRENCY |
| FT-14-13-013 | Performance audit write latency | API | @PERFORMANCE |
| FT-14-13-014 | Edge missing audit entry detection | API | @NEGATIVE |

## Automation Strategy
Ingest events; query audit API; CSV export hash; baseline counts.
