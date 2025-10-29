# Functional Test Story 13.17 – Seed Redaction and Audit

Trace: story_13_17_seed-redaction-and-audit
Tags: @EPIC_13 @SIMULATION @SECURITY

## Objective
Validate seed values are redacted in logs while retained in secure audit store for reproducibility.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-17-001 | Submit run with seed | API | @CONFIG |
| FT-13-17-002 | Log entry shows redacted seed | API | @SECURITY |
| FT-13-17-003 | Audit store retains seed value | Engine | @AUDIT |
| FT-13-17-004 | Unauthorized audit read -> 403 | API | @SECURITY |
| FT-13-17-005 | Drift audit seed baseline | Engine | @DRIFT |
| FT-13-17-006 | Metrics auditSeedAccessCount | API | @METRICS |
| FT-13-17-007 | Logging does not duplicate seed | API | @SECURITY |
| FT-13-17-008 | API contract audit response schema | Contract | @CONTRACT |
| FT-13-17-009 | Accessibility audit seed UI | E2E | @ACCESSIBILITY |
| FT-13-17-010 | Concurrency multiple seed audit reads | API | @CONCURRENCY |

## Automation Strategy
Log scanning; secure audit API read; negative unauthorized access; metrics scrape.
