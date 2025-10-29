# Test Epic 09 – Reference & Market Data Mastering

## Scope
Mastering of reference entities, instruments, and market data snapshots (curves, spreads, recovery assumptions), including validation, caching, and update propagation.

## Objectives
- Accurate ingestion & validation of reference data
- Market data snapshot consistency & versioning
- Efficient cache invalidation & propagation to dependent services

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Stale market data served | Cache invalidation tests | FT-9-0-010 |
| Incorrect entity attribute mapping | Validation matrix | FT-9-0-004 |
| Duplicate instruments | Uniqueness tests | FT-9-0-005 |

## Scenario Taxonomy
Single consolidated story document enumerating scenarios.

## Tooling Matrix
| Layer | Tool |
|-------|------|
| Ingestion | REST-assured + fixture files |
| Cache | Integration tests with explicit invalidation |
| UI (if present) | Playwright |

## Exit Criteria
All FT-9-0-* scenarios automated; cache invalidation latency within target.
