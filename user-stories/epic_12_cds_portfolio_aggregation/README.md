# Epic 12 – CDS Portfolio Aggregation & Risk Attribution

## 1. Background
With single-name CDS trades enriched (Epic 11), the next capability is to group trades into portfolios to view aggregated valuation and risk. This establishes the structural basis for correlation modeling and Monte Carlo simulation (Epic 13). Initially aggregation is linear (sum of independent trades) but must be architected to incorporate joint default metrics later without data model churn.

## 2. Problem Statement
Currently each CDS trade is valued in isolation. Users lack:
- Portfolio-level PV / Sensitivity views
- Concentration metrics (by name, sector)
- A single payload to pass to ORE containing multiple trades for batch valuation
- A foundation for correlation simulation

## 3. Objectives
| # | Objective | Success Criteria |
|---|-----------|------------------|
| 1 | Introduce portfolio domain model | CRUD for portfolios; attach/detach CDS trades |
| 2 | Aggregate valuation metrics | PV, Accrued, Premium/Protection Leg sums, CS01 (sum) |
| 3 | Provide attribution breakdown | Top N contributors, sector concentration |
| 4 | Enable multi-trade ORE generation | Combined portfolio XML output or per-trade consolidation |
| 5 | Prepare simulation stubs | Endpoint shape compatible with future correlation MC engine |

## 4. In Scope
- Portfolio entities & REST API
- Aggregated pricing endpoint
- Portfolio blotter with Risk panel UI for portfolio similar to single trade view
- ORE integration generating a portfolio file with multiple `<Trade>` elements

## 5. Out of Scope
- Correlated default simulation (Epic 13)
- Nth-to-default / structured basket payoff modeling
- Tranche / base correlation

## 6. Domain Model Additions
### 6.1 Entities
`CdsPortfolio`
| Field | Type | Notes |
|-------|------|-------|
| id | Long | PK |
| name | String(60) | Unique (case-insensitive) |
| description | String (nullable) | Optional |
| createdAt | Timestamp | Auto |
| updatedAt | Timestamp | Auto |

`CdsPortfolioConstituent`
| Field | Type | Notes |
|-------|------|-------|
| id | Long | PK |
| portfolioId | FK -> CdsPortfolio | Many-to-one |
| tradeId | FK -> CDSTrade | Linked single-name trade |
| weightType | ENUM(NOTIONAL, PERCENT) | Interpretation of weightValue |
| weightValue | DECIMAL(15,8) | If PERCENT, sum ≈ 1.0; if NOTIONAL, absolute |
| active | Boolean | Soft removal |
| addedAt | Timestamp | Audit |

### 6.2 Derived Metrics (Stored or On-the-Fly)
- Aggregated notional (sum of constituents' notionals or weighted sum if using scaled portfolio)
- Weight normalization (runtime if weightType = NOTIONAL)

## 7. API Design
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/cds-portfolios | Create portfolio |
| GET | /api/cds-portfolios/{id} | Fetch portfolio metadata & constituents |
| POST | /api/cds-portfolios/{id}/constituents | Attach trade(s) |
| DELETE | /api/cds-portfolios/{id}/constituents/{constId} | Detach constituent |
| POST | /api/cds-portfolios/{id}/price?valuationDate=YYYY-MM-DD | Aggregate pricing & risk |
| GET | /api/cds-portfolios/{id}/risk-summary | Cached last risk result |

### 7.1 Attach Trades Request
```json
{
  "trades": [
    { "tradeId": 42, "weightType": "NOTIONAL", "weightValue": 10000000 },
    { "tradeId": 43, "weightType": "PERCENT", "weightValue": 0.15 }
  ]
}
```

### 7.2 Pricing Response (Initial Linear Aggregation)
```json
{
  "portfolioId": 7,
  "valuationDate": "2025-10-07",
  "aggregate": {
    "pv": 10450.12,
    "accrued": 320.44,
    "premiumLegPv": 81234.90,
    "protectionLegPv": 91685.02,
    "fairSpreadBpsWeighted": 102.3,
    "cs01": 15234.5,
    "rec01": -8300.7,
    "jtd": -22500000.00
  },
  "byTrade": [
    {
      "tradeId": 42,
      "referenceEntity": "AAPL",
      "notional": 10000000,
      "pv": 165.56,
      "cs01": 455.2,
      "rec01": -250.7,
      "weight": 0.41
    }
  ],
  "concentration": {
    "top5PctCs01": 72.3,
    "sectorBreakdown": [ { "sector": "TECH", "cs01Pct": 45.1 } ]
  },
  "completeness": { "constituents": 10, "priced": 10 }
}
```

## 8. ORE Integration Strategy
### 8.1 Combined Portfolio File
Generate a single ORE portfolio XML:
```xml
<Portfolio>
  <Trade id="CDS-42" tradeType="CreditDefaultSwap"> ... </Trade>
  <Trade id="CDS-43" tradeType="CreditDefaultSwap"> ... </Trade>
</Portfolio>
```
Run one ORE process → parse per-trade results → aggregate.

### 8.2 Mapping Weights
Phase 1: Ignore weights; treat actual stored notionals.
Phase 2: If PERCENT weights supplied, scale notionals: `scaledNotional_i = totalTargetNotional * percent_i`.

### 8.3 Fair Spread Aggregation
Weighted by risky PV01 (approximated by CS01 / (1 - Recovery) or direct from ORE’s risky annuity if exposed). For MVP use notional weighting.

## 9. Metrics Computation Details
| Metric | Formula / Approach |
|--------|--------------------|
| Portfolio PV | Σ trade PV |
| Portfolio CS01 | Σ trade CS01 |
| Weighted Fair Spread | Σ (fairSpread_i * weight_i) |
| Rec01 | Σ trade Rec01 |
| JTD | Σ trade JTD |
| Top5PctCs01 | (Σ CS01 of 5 largest contributors)/ (Total CS01) *100 |
| Sector Breakdown | Group Σ CS01 by sector / total CS01 |

## 10. UI Changes
### 10.1 Portfolio List View
Columns: Name | # Trades | Aggregate Notional | PV | CS01 (if cached) | Updated

### 10.2 Create Portfolio Modal
1. Portfolio Name
2. Description
3. Select Trades (multi-select with search) → show each trade: entity, notional, spread
4. Assign Weight Type (global default) & editable weight per row
5. Save

### 10.3 Portfolio Detail / Risk Panel
Tabs:
- Overview: Aggregate metrics table
- Constituents: Data grid (entity, notional, PV, CS01 share, sector)
- Concentration: Pie (sector), Bar (top contributors), Table (top 10)
- Valuation Run: Button “Reprice Now” (shows spinner + last run timestamp)

### 10.4 UX Notes
- Color code high concentration rows (>25% CS01)
- Provide export (CSV) of byTrade attribution
- Show placeholder card “Correlation Simulation (Epic 13)” with disabled state

## 11. Performance & Caching
- Cache last pricing result per portfolio keyed by (portfolioId, valuationDate)
- Reprice only trades changed since last run (future optimization) – Phase 2

## 12. Testing Strategy
| Layer | Tests |
|-------|-------|
| Unit | Weight normalization, aggregation math |
| Integration | Create portfolio → attach trades → price → add trade → reprice diff |
| ORE Integration | Multi-trade XML generation & parsing correctness |
| UI | Adding/removing trades updates weight recalculation |

## 13. Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Weight inconsistencies | Validate Σ percent ≈ 1.0 ± tolerance |
| Missing sector tagging | Default sector = "UNCLASSIFIED" to avoid null grouping |

## 14. Acceptance Criteria
- Create portfolio with ≥2 trades returns 201
- calculate risk endpoint in risk engine returns aggregated metrics with per-trade breakdown
- UI risk panel displays PV & CS01 matching manual sum of constituents
- Removing a trade and repricing updates metrics correctly

## 15. Future Hooks (for Epic 13)
- Store sector per trade (already in Epic 11) for factor loadings
- Add placeholder correlation config (not yet applied)

## 16. Example Multi-Trade ORE Snippet
```xml
<Portfolio>
  <Trade id="CDS-42" tradeType="CreditDefaultSwap"> ... </Trade>
  <Trade id="CDS-43" tradeType="CreditDefaultSwap"> ... </Trade>
</Portfolio>
```

## 17. Implementation Steps
1. Create entities & repositories
2. Portfolio controller + DTOs
3. Attach/detach endpoints + validation
4. Aggregation service reusing single-trade pricing
5. ORE multi-trade generation
6. Portfolio pricing endpoint + JSON schema
7. UI portfolio CRUD + risk panel


---
**Next Epic:** *Epic 13 – Correlated Monte Carlo Simulation & Joint Default Metrics* (adds non-linear portfolio behavior & loss distributions).
