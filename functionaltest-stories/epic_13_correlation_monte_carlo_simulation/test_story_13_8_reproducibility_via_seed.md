# Functional Test Story 13.8 – Reproducibility via Seed

Trace: story_13_8_reproducibility-via-seed
Tags: @EPIC_13 @SIMULATION @DETERMINISM

## Objective
Validate reproducibility of simulation runs with explicit seed parameter.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-8-001 | Provide seed parameter | API | @CONFIG |
| FT-13-8-002 | Two runs same seed identical outputs | Engine | @DETERMINISM |
| FT-13-8-003 | Different seeds produce different outputs | Engine | @DETERMINISM |
| FT-13-8-004 | Omitted seed defaults random | Engine | @CONFIG |
| FT-13-8-005 | Export seed in metadata (non-sensitive) | API | @EXPORT |
| FT-13-8-006 | Unauthorized seed usage -> 403 | API | @SECURITY |
| FT-13-8-007 | Drift seed baseline monitoring | Engine | @DRIFT |
| FT-13-8-008 | Logging redacts seed details | API | @SECURITY |
| FT-13-8-009 | Concurrency same seed separate runs | Engine | @CONCURRENCY |
| FT-13-8-010 | Deterministic performance stability | Engine | @PERFORMANCE |
| FT-13-8-011 | API contract stable | Contract | @CONTRACT |
| FT-13-8-012 | Accessibility seed input UI | E2E | @ACCESSIBILITY |
| FT-13-8-013 | Metrics deterministicRunCount | Engine | @METRICS |
| FT-13-8-014 | Time zone normalized timestamps | API | @TIME |

## Automation Strategy
Comparison JSON fixtures; hashing outputs; UI seed field presence.
