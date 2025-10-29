# Functional Test Story 7.2 – Core Risk Measures Engine

Trace: story_7_2_core-risk-measures-engine
Tags: @EPIC_07 @STORY_7_2 @RISK @MEASURES

## Objective
Ensure correctness of risk measures (PV, DV01, CS01, Jump-to-Default) and stability across runs.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-7-2-001 | Compute baseline measures | Engine | @NUMERIC |
| FT-7-2-002 | PV tolerance within 0.01 | Engine | @NUMERIC |
| FT-7-2-003 | DV01 finite diff vs analytic | Engine | @NUMERIC |
| FT-7-2-004 | CS01 bump vs analytic | Engine | @NUMERIC |
| FT-7-2-005 | Jump-to-Default correctness | Engine | @NUMERIC |
| FT-7-2-006 | Input mapping completeness | Engine | @INPUT |
| FT-7-2-007 | Missing curve interpolation | Engine | @DATA |
| FT-7-2-008 | Negative rate handling | Engine | @EDGE |
| FT-7-2-009 | Extreme spread scenario | Engine | @EDGE |
| FT-7-2-010 | Hazard shift shock response sign | Engine | @SCENARIO |
| FT-7-2-011 | Parallel spread +10bp PV delta direction | Engine | @SCENARIO |
| FT-7-2-012 | Bucket shift isolates relevant tenors | Engine | @SCENARIO |
| FT-7-2-013 | Performance cold run | Engine | @PERFORMANCE |
| FT-7-2-014 | Performance warm cache | Engine | @PERFORMANCE |
| FT-7-2-015 | Cache correctness identical outputs | Engine | @CACHE |
| FT-7-2-016 | Multi-thread safety | Engine | @CONCURRENCY |
| FT-7-2-017 | Deterministic random seeds stable | Engine | @DETERMINISM |
| FT-7-2-018 | Golden baseline drift test | Engine | @DRIFT |
| FT-7-2-019 | Serialization round-trip invariance | Engine | @SERIALIZATION |
| FT-7-2-020 | Memory usage under threshold | Engine | @PERFORMANCE |
| FT-7-2-021 | Metrics emitted (calcLatency) | Engine | @METRICS |
| FT-7-2-022 | Error: missing input surfaces code | Engine | @ERROR |
| FT-7-2-023 | Error: divide by zero guarded | Engine | @ERROR |
| FT-7-2-024 | Logging redacts config secrets | Engine | @SECURITY |
| FT-7-2-025 | PV sign consistency buy vs sell | Engine | @CONSISTENCY |
| FT-7-2-026 | Stress large batch 500 trades | Engine | @SCALING |
| FT-7-2-027 | Cancel mid-batch safe stop (if supported) | Engine | @RESILIENCE |
| FT-7-2-028 | API endpoint returns all measures | API | @API |
| FT-7-2-029 | API schema stable | Contract | @CONTRACT |
| FT-7-2-030 | Accessibility risk panel UI (axe) | E2E | @ACCESSIBILITY |

## Automation Strategy
Engine numeric tests with tolerance file `tolerances.yml`, API contract snapshot, Playwright UI risk tab test.
