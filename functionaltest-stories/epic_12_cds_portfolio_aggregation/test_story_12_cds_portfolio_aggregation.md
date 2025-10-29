# Functional Test Story 12.0 – CDS Portfolio Aggregation

Trace: epic_12_cds_portfolio_aggregation/README.md
Tags: @EPIC_12 @PORTFOLIO @AGGREGATION

## Objective
Validate aggregation of trade-level measures into portfolio-level metrics with correct netting & grouping.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-12-0-001 | Aggregate PV across portfolio | Engine | @NUMERIC |
| FT-12-0-002 | Aggregate CS01 netted | Engine | @NUMERIC |
| FT-12-0-003 | Group by counterparty sums | Engine | @GROUP |
| FT-12-0-004 | Group by sector sums | Engine | @GROUP |
| FT-12-0-005 | Duplicate trade not double counted | Engine | @CONSISTENCY |
| FT-12-0-006 | Filter by currency subset | API | @FILTER |
| FT-12-0-007 | Filter by rating subset | API | @FILTER |
| FT-12-0-008 | Drill-down fetch underlying trades | API | @DRILL |
| FT-12-0-009 | FX conversion accuracy | Engine | @CURRENCY |
| FT-12-0-010 | Negative PV handling | Engine | @EDGE |
| FT-12-0-011 | Zero position portfolio edge | Engine | @EDGE |
| FT-12-0-012 | Performance aggregation 500 trades | Engine | @PERFORMANCE |
| FT-12-0-013 | Cache warm vs cold improvement | Engine | @CACHE |
| FT-12-0-014 | Memory usage under threshold | Engine | @PERFORMANCE |
| FT-12-0-015 | SLA p95 latency < target | API | @PERFORMANCE |
| FT-12-0-016 | Metrics aggregationLatency | API | @METRICS |
| FT-12-0-017 | Drift baseline comparison | Engine | @DRIFT |
| FT-12-0-018 | Logging redacts trade ids (optional) | API | @SECURITY |
| FT-12-0-019 | Accessibility portfolio table | E2E | @ACCESSIBILITY |
| FT-12-0-020 | Export portfolio metrics CSV | API | @EXPORT |
| FT-12-0-021 | Time zone normalized timestamps | API | @TIME |
| FT-12-0-022 | Concurrency multi-aggregation isolation | Engine | @CONCURRENCY |

## Automation Strategy
Engine-level aggregator tests plus API filter tests; UI drill-down Playwright spec; baseline snapshots for drift.
