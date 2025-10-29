# Functional Test Story 14.11 – Market Data Updates

Trace: story_14_11_market-data-updates
Tags: @EPIC_14 @CORPORATE_BONDS @MARKET_DATA

## Objective
Validate processing of real-time market data updates (spreads, yields) and cache invalidation.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-11-001 | Ingest spread update | Engine | @POSITIVE |
| FT-14-11-002 | Ingest yield update | Engine | @POSITIVE |
| FT-14-11-003 | Unauthorized market data ingest -> 403 | API | @SECURITY |
| FT-14-11-004 | Drift curve recalculation baseline | Engine | @DRIFT |
| FT-14-11-005 | Logging redacts provider IDs | API | @SECURITY |
| FT-14-11-006 | Metrics mdUpdateLatency | Engine | @METRICS |
| FT-14-11-007 | Performance p95 update latency | Engine | @PERFORMANCE |
| FT-14-11-008 | Contract market data schema stable | Contract | @CONTRACT |
| FT-14-11-009 | Accessibility market data UI | E2E | @ACCESSIBILITY |
| FT-14-11-010 | Concurrency multi-updates isolation | Engine | @CONCURRENCY |
| FT-14-11-011 | Edge stale update ignored | Engine | @RESILIENCE |
| FT-14-11-012 | Edge out-of-order updates reconciled | Engine | @RESILIENCE |

## Automation Strategy
Inject updates; verify curve & pricing adjustments; stale/out-of-order negative tests.
