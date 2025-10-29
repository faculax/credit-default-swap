# Test Epic 08 – Margin, Clearing & Capital

## Scope
CCP novation enrichment, daily VM/IM statements ingestion, SIMM sensitivities & IM calculation, SA-CCR exposure engine, reconciliation dashboard.

## Objectives
- Accurate enrichment of trades for CCP context
- Correct ingestion & parsing of daily variation and initial margin statements
- SIMM sensitivities generation & IM figure reproducibility
- SA-CCR exposure calculation correctness
- Clear reconciliation dashboard highlighting breaks

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Incorrect mapping of clearing accounts | Enrichment validation tests | FT-8-1-* |
| Statement ingestion misparses amounts | Ingestion schema tests | FT-8-2-* |
| SIMM drift unnoticed | Baseline comparison | FT-8-3-020 |
| SA-CCR exposure miscalculated | Formula component tests | FT-8-4-005..010 |
| Reconciliation dashboard false negatives | Break injection tests | FT-8-5-010 |

## Scenario Taxonomy
| Category | Stories |
|----------|---------|
| Novation & Enrichment | 8.1 |
| Daily Statements Ingestion | 8.2 |
| SIMM Sensitivities & IM | 8.3 |
| SA-CCR Exposure Engine | 8.4 |
| Reconciliation Dashboard | 8.5 |

## Tooling Matrix
| Layer | Tool | Notes |
|-------|------|-------|
| Ingestion | File fixture + REST-assured | CSV/JSON parsing |
| Engine Calc | JUnit numeric tolerance | Deterministic seeds |
| UI Dashboard | Playwright | Break visualization |

## Exit Criteria
All FT-8-* scenarios automated; SIMM drift < threshold; ingestion error rate < 0.1% test fixtures.
