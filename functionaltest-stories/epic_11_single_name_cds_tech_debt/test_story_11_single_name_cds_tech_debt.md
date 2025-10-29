# Functional Test Story 11.0 – Single Name CDS Tech Debt Regression

Trace: epic_11_single_name_cds_tech_debt/README.md
Tags: @EPIC_11 @TECH_DEBT @REFACTOR

## Objective
Ensure refactors preserve functional behavior, performance, security posture, and observability.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-11-0-001 | Baseline snapshot pre-refactor | Engine | @BASELINE |
| FT-11-0-002 | Post-refactor PV parity | Engine | @PARITY |
| FT-11-0-003 | Post-refactor CS01 parity | Engine | @PARITY |
| FT-11-0-004 | Post-refactor DV01 parity | Engine | @PARITY |
| FT-11-0-005 | Pricing edge cases unchanged | Engine | @EDGE |
| FT-11-0-006 | Error messages improved formatting | API | @USABILITY |
| FT-11-0-007 | No new warnings in logs | Engine | @LOGGING |
| FT-11-0-008 | Memory allocation within baseline +5% | Engine | @PERFORMANCE |
| FT-11-0-009 | CPU time within baseline +5% | Engine | @PERFORMANCE |
| FT-11-0-010 | Latency p95 within baseline +5% | Engine | @PERFORMANCE |
| FT-11-0-011 | Sensitive fields redacted logs | API | @SECURITY |
| FT-11-0-012 | Metrics unchanged cardinality | Engine | @OBSERVABILITY |
| FT-11-0-013 | API contract unchanged | Contract | @CONTRACT |
| FT-11-0-014 | Deterministic seed reproducibility | Engine | @DETERMINISM |
| FT-11-0-015 | Accessibility unaffected (risk panel) | E2E | @ACCESSIBILITY |
| FT-11-0-016 | Code coverage regression threshold | Meta | @QUALITY |

## Automation Strategy
Run pre vs post refactor harness capturing metrics; enforce thresholds; contract snapshot diff.
