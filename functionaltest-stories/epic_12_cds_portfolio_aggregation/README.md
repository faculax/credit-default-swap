# Test Epic 12 – CDS Portfolio Aggregation

## Scope
Aggregation of positions & risk across portfolios, hierarchical grouping, filters, and metrics consolidation.

## Objectives
- Accurate summation & netting across portfolios
- Performance at scale (hundreds of trades)
- Correct filtering & grouping semantics

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Double counting trades | Aggregation validation | FT-12-0-005 |
| Incorrect FX conversion during aggregation | Currency tests | FT-12-0-009 |
| Slow large aggregation | Performance tests | FT-12-0-015 |

## Scenario Taxonomy
Single consolidated story doc.

## Tooling Matrix
| Layer | Tool |
|-------|------|
| Engine | JUnit numeric tests |
| API | REST-assured |
| UI | Playwright (portfolio view) |

## Exit Criteria
All FT-12-0-* scenarios automated; performance within SLA.
