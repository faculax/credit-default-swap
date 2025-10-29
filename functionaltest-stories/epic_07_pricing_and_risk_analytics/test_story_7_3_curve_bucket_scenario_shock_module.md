# Functional Test Story 7.3 – Curve & Bucket Scenario Shock Module

Trace: story_7_3_curve-bucket-scenario-shock-module
Tags: @EPIC_07 @STORY_7_3 @RISK @SCENARIO

## Objective
Validate correctness of applying curve and bucket shocks, isolation of impacts, aggregation, and performance.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-7-3-001 | Load base curve set | Engine | @SETUP |
| FT-7-3-002 | Apply parallel +10bp shock | Engine | @PARALLEL |
| FT-7-3-003 | Apply key rate shock single tenor | Engine | @KEYRATE |
| FT-7-3-004 | Shock isolation unaffected tenors stable | Engine | @ISOLATION |
| FT-7-3-005 | Multi-bucket simultaneous shocks | Engine | @COMPOSITE |
| FT-7-3-006 | Negative shock -25bp | Engine | @PARALLEL |
| FT-7-3-007 | Large shock +500bp boundary | Engine | @BOUNDARY |
| FT-7-3-008 | Scenario aggregation PV deltas sum | Engine | @AGGREGATION |
| FT-7-3-009 | CS01 bucket sum ≈ parallel PV diff | Engine | @CONSISTENCY |
| FT-7-3-010 | Drift guard baseline scenario set | Engine | @DRIFT |
| FT-7-3-011 | Serialization of scenario definitions stable | Engine | @SERIALIZATION |
| FT-7-3-012 | Invalid tenor rejection | Engine | @NEGATIVE |
| FT-7-3-013 | Missing curve point interpolation | Engine | @INTERPOLATION |
| FT-7-3-014 | Performance batch 100 scenarios | Engine | @PERFORMANCE |
| FT-7-3-015 | Memory use under threshold | Engine | @PERFORMANCE |
| FT-7-3-016 | Concurrency run scenarios multi-thread | Engine | @CONCURRENCY |
| FT-7-3-017 | Cancel mid-run leaves consistent results | Engine | @RESILIENCE |
| FT-7-3-018 | Metrics scenariosProcessed | Engine | @METRICS |
| FT-7-3-019 | Logging hides raw curve proprietary data | Engine | @SECURITY |
| FT-7-3-020 | API endpoint returns scenario list | API | @API |
| FT-7-3-021 | API contract unchanged | Contract | @CONTRACT |
| FT-7-3-022 | UI scenario selection renders list | E2E | @UI |
| FT-7-3-023 | Accessibility scenario panel | E2E | @ACCESSIBILITY |
| FT-7-3-024 | Export scenario results JSON | API | @EXPORT |

## Automation Strategy
Engine harness enumerates shock sets; API contract snapshot; Playwright UI scenario panel test.
