# Epic 15 – Basket & Multi-Name Credit Derivatives (First-to-Default, N-th-to-Default, Tranche Foundations)

## 1. Background
With correlated Monte Carlo simulation (Epic 13) and cash / survival-based bonds (Epic 14) established, the platform can now extend into **multi-name credit derivatives**: first-to-default, general N-th-to-default, and the groundwork for tranche (attach / detach) products. These instruments require joint default time modeling, correlation sensitivity, and efficient path reuse across instruments sharing the same constituent set. This epic formalizes reusable basket primitives so later structured features (index tranches, bespoke portfolio tranching) can be added without rearchitecting core simulation.

## 2. Problem Statement
Currently all risk & pricing is either single-name (CDS / Bond) or portfolio-level aggregation without payoff non-linearities. Users cannot:
- Define a basket of reference entities with contractual N-th default payoff logic
- Compute fair spread for first-to-default or N-th-to-default structures
- Price tranche-like loss layer instruments (attachment/detachment) reusing simulated loss distributions
- Attribute correlation or constituent contribution to basket PV / spread
- Reuse a single correlated default scenario set across multiple baskets for performance consistency

Without basket instruments the platform cannot service structured credit desks or advanced hedging analytics connecting single-name exposures to basket hedges.

## 3. Objectives
| # | Objective | Success Criteria |
|---|-----------|------------------|
| 1 | Basket domain model & persistence | CRUD definitions storing constituents, weights, recovery overrides |
| 2 | First-to-default pricing (fair spread) | Spread solver converges; MC convergence tests pass |
| 3 | General N-th-to-default extension | N parameterized; N=1 regression matches first-to-default |
| 4 | Tranche layer payoff groundwork | Attach/Detach modeled; expected tranche loss computed |
| 5 | Path reuse & simulation integration | Single scenario matrix reused across instruments in a request |
| 6 | Correlation & recovery bump sensitivities | Finite diff outputs (ΔPV/Δρ_ij aggregated) exposed (initial aggregate) |
| 7 | UI builder & valuation panels | Users compose basket, set N / layer, price & view metrics |
| 8 | Portfolio aggregation integration | Basket risk incorporated without duplicating underlying single-name notionals (clear flag) |

## 4. In Scope
- Basket definition with ordered constituent list (issuer, notional share or equal weight)
- Instrument types: FIRST_TO_DEFAULT, NTH_TO_DEFAULT, TRANCHETTE (simple attach/detach slice)
- Monte Carlo pricing via existing correlated default engine (Epic 13)
- Fair spread solver (premium leg vs protection leg PV neutrality)
- Finite difference correlation sensitivity (aggregate dPV/dβ or dPV/dρ scalar metrics)
- Recovery deterministic (Phase 1) with optional override per constituent
- Basic convergence diagnostics (paths, standard error) returned

## 5. Out of Scope (Deferred)
- Base correlation calibration & index tranche market calibration
- Closed-form copula approximations (analytic first-to-default) – may add later as performance optimization
- Stochastic recovery distributions (reuse Epic 13 Phase B later)
- Bespoke dynamic constituent re-weighting post defaults
- Counterparty credit adjustment (CVA) overlay

## 6. Domain Model Additions
### 6.1 Entities
`BasketDefinition`
| Field | Type | Notes |
|-------|------|-------|
| id | Long | PK |
| name | String(80) | Unique |
| type | ENUM(FIRST_TO_DEFAULT, NTH_TO_DEFAULT, TRANCHETTE) | Basket category |
| nth | Integer (nullable) | Required for NTH_TO_DEFAULT |
| attachmentPoint | DECIMAL(9,6) | For TRANCHETTE (e.g. 0.03 = 3% portfolio loss) |
| detachmentPoint | DECIMAL(9,6) | For TRANCHETTE (must be > attachmentPoint) |
| premiumFrequency | ENUM(QUARTERLY, SEMI_ANNUAL) | Premium accrual |
| dayCount | ENUM(ACT_360, ACT_365F) | Accrual convention |
| currency | String(3) | Pricing currency |
| notional | DECIMAL(18,2) | Contract notional |
| createdAt / updatedAt | TIMESTAMP | Audit |

`BasketConstituent`
| Field | Type | Notes |
|-------|------|-------|
| id | Long | PK |
| basketId | FK -> BasketDefinition | |
| issuer | String(40) | Reference entity code |
| weight | DECIMAL(18,10) | If null treat as equal weight; else fraction of notional |
| recoveryOverride | DECIMAL(5,4) (nullable) | If absent fallback to issuer recovery |
| seniority | ENUM | Align with CDS |
| sector | String(30) | Inherited / cached for sim factor mapping |

### 6.2 Derived / Runtime
- Normalized weight vector (if any provided)
- Constituent hazard curves & survival functions
- Joint default times (from simulation) cached per run key

## 7. Pricing & Payoff Logic
### 7.1 First-to-Default (FTD)
Protection triggers at earliest default among constituents before maturity; premium leg accrues until default time τ(1) or maturity. Protection leg payoff = Notional * (1 - Recovery_effective(first defaulter)).
Fair spread solves: PV_premium(spread) = PV_protection.

### 7.2 N-th-to-Default
Trigger at τ(N). Premium accrues until τ(N) or maturity. Protection payoff: Notional * average LGD of Nth defaulter OR (Phase 1 simplification) use LGD of Nth default name; advanced variant could sum incremental losses at each default starting from 1 to N (deferred). For MVP we model single payoff at Nth event.

### 7.3 Tranche (Attach/Detach Slice)
Compute cumulative portfolio loss L(t) across constituents (using per-name notional * LGD if default). Payoff at maturity (Phase 1) or incrementally at default (Phase 2). Expected tranche loss ETL = E[(min(L(T), D) - A)+]/(D-A). Protection leg PV = Notional * ETL discounted; Premium leg based on outstanding tranche notional (D - A - realized loss fraction).

### 7.4 Premium Leg Approximation (Phase 1)
Discretize premium payment dates; for each date accrue spread * yearFraction * survivalState. For FTD/Nth use indicator {τ(N) > paymentDate}. For tranche use expected outstanding notional approximation (use MC path average).

### 7.5 Spread Solver
Root solve h(s) = PV_premium(s) - PV_protection = 0 using Brent (bounded [1 bp, 5000 bps]). Provide convergence status + iterations.

## 8. Simulation Integration
- Leverage Epic 13 one-factor correlated default generator
- Input: constituent issuers → hazard curves, β loadings via sector mapping + overrides
- Reuse simulation seeds for reproducibility
- Path reuse: generate matrix DefaultTime[path][issuerIndex]. Multi-instrument pricing iterates on this matrix without regeneration.

### 8.1 Convergence Diagnostics
Return: `pathsUsed`, `standardErrorProtectionLeg`, `standardErrorFairSpread` (estimated via sample variance of PV contributions / sqrt(paths)).

## 9. Sensitivities
| Sensitivity | Method | Notes |
|-------------|--------|-------|
| Spread DV01 (aggregate) | Parallel +1bp hazard bump all constituents; reprice | Provide currency per 1bp |
| Correlation Sensitivity (dPV/dβ_avg) | Bump systemic loading β_i + Δ (e.g. +0.01) globally; reprice; finite diff | Report scalar & optionally per-constituent if cheap |
| Recovery Sensitivity | +1pt recovery all constituents (or defaulters for tranche) | Finite diff |
| Name Contribution | Average marginal PV impact removing name i (approx, one-pass) – Phase 2 |

## 10. API Design
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/baskets | Create basket definition |
| GET | /api/baskets/{id} | Fetch basket definition & constituents |
| PUT | /api/baskets/{id} | Update mutable fields (name, premium params) |
| POST | /api/baskets/{id}/price?valuationDate=YYYY-MM-DD | Price single basket |
| POST | /api/baskets/price/batch | Batch pricing (path reuse) |

### 10.1 Create Request (FTD Example)
```json
{
  "name": "TECH_FTD_5Y",
  "type": "FIRST_TO_DEFAULT",
  "currency": "USD",
  "notional": 10000000,
  "premiumFrequency": "QUARTERLY",
  "dayCount": "ACT_360",
  "maturityDate": "2030-12-20",
  "constituents": [
    { "issuer": "AAPL", "weight": null },
    { "issuer": "MSFT" },
    { "issuer": "GOOGL" },
    { "issuer": "NVDA" }
  ]
}
```

### 10.2 Pricing Response (FTD Example)
```json
{
  "basketId": 17,
  "valuationDate": "2025-10-07",
  "type": "FIRST_TO_DEFAULT",
  "notional": 10000000,
  "fairSpreadBps": 182.45,
  "premiumLegPv": 215000.32,
  "protectionLegPv": 214998.90,
  "pv": 105.40,
  "convergence": {
    "pathsUsed": 50000,
    "standardErrorFairSpreadBps": 0.72,
    "iterations": 6
  },
  "sensitivities": {
    "spreadDv01": 15230.4,
    "correlationBeta": 18450.2,
    "recovery01": -8200.7
  },
  "constituents": [
    { "issuer": "AAPL", "hazardCurveId": "AAPL_SR_USD", "weight": 0.25 },
    { "issuer": "MSFT", "hazardCurveId": "MSFT_SR_USD", "weight": 0.25 }
  ]
}
```

### 10.3 Tranche Pricing Response (Preview)
```json
{
  "basketId": 21,
  "type": "TRANCHETTE",
  "attachment": 0.03,
  "detachment": 0.06,
  "fairSpreadBps": 412.10,
  "expectedTrancheLossPct": 0.0412,
  "etlTimeline": [ {"tenor": "1Y", "etl": 0.0041 }, {"tenor": "3Y", "etl": 0.0205 } ],
  "sensitivities": { "spreadDv01": 24000.1, "correlationBeta": 55210.9 }
}
```

## 11. UI Impact
### 11.1 Basket Builder
- Multi-select constituent issuers (searchable)
- Instrument Type toggle: First, Nth, Tranche
- Dynamic fields: N, Attachment/Detachment appear contextually
- Validation summary (weights sum ≈ 1 if provided)

### 11.2 Basket Detail / Valuation Panel
Cards: Fair Spread, Premium PV, Protection PV, Paths Used, SE(Fair Spread), Correlation Sensitivity, Spread DV01.
Table: Constituents (issuer, weight, β, hazard curve, recovery).
Chart: (Phase 2) Distribution of Nth default time or tranche loss density.

### 11.3 Portfolio Integration
- Show basket rows with Type=BASKET, underlying count, fair spread
- Option to "Expand constituents" (tree view) (Phase 2)
- Aggregation: default treat basket PV separately; optional toggle to include underlying exposures in concentration metrics to avoid double counting.

## 12. Testing Strategy
| Layer | Tests |
|-------|-------|
| Unit | Payoff extraction for τ(N); tranche loss calculation |
| Unit | Weight normalization logic; attachment/detachment validation |
| Integration | Create basket → price 1st default vs analytic 2-name symmetrical case |
| Integration | Convergence test increasing paths halves SE approximately (1/√k pattern) |
| Regression | FTD N=1 equals NTH_TO_DEFAULT with nth=1 within numerical tolerance |
| Performance | Path reuse benchmark for pricing 5 baskets simultaneously |
| UI | Builder validation, dynamic field visibility |

## 13. Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Monte Carlo noise causing unstable fair spread | Minimum path floor; relative SE threshold -> auto increase paths (Phase 2) |
| Slow multi-basket pricing | Shared default time matrix & batch processing |
| Correlation sensitivity expensive | Coarse bump (Δβ=0.01) configurable; cache base path metrics |
| Double counting underlying exposures in portfolio | Flag baskets; exclude constituents from top-level concentration unless expanded |
| Edge case N > #names | Validation fail fast (400) |
| Invalid tranche (A >= D) | Validation rule before persistence |

## 14. Acceptance Criteria
1. Basket creation with ≥2 constituents returns 201 and persists correct normalized weights.
2. First-to-default pricing returns fairSpreadBps with standardErrorFairSpreadBps field when MC paths >= threshold.
3. N-th-to-default with nth=1 matches (± tolerance) first-to-default fair spread using identical inputs & seed.
4. Increasing paths from 25k to 100k reduces standard error roughly by factor ~2 (statistical tolerance ±20%).
5. Correlation bump (β+0.05) increases fair spread for positively correlated diversified basket (test fixture) directionally as expected.
6. Recovery +1pt bump yields negative PV change (long protection holder) – sign test.
7. Tranche attach/detach validation rejects invalid intervals with descriptive message.
8. API latency for single basket (50 names, 50k paths) < performance target (document measured baseline).
9. UI builder enforces required dynamic fields (Nth, Attachment/Detachment) based on type selection.
10. Portfolio view shows basket entry with constituents count and fair spread; expansion does not duplicate basket PV.

## 15. Implementation Steps (Chronological)
1. DB Migrations: `basket_definitions`, `basket_constituents` tables
2. Domain entities, repositories, DTOs
3. Validation layer (type-specific field constraints)
4. Basket pricing service skeleton (interfaces, strategy per type)
5. Path reuse manager (obtain or generate default time matrix from Epic 13 engine)
6. First-to-default payoff + premium leg + spread solver
7. N-th-to-default extension
8. Tranche expected loss calculator (Phase 1 aggregated at maturity)
9. Sensitivities (spread, correlation, recovery) finite diff wrappers
10. REST controllers + endpoints (single + batch price)
11. UI basket builder + detail panel
12. Convergence diagnostics & response enrichment
13. Portfolio aggregation adjustments (avoid double counting)
14. Performance profiling & optimization pass
15. Documentation & sample scripts

## 16. Future Extensions
- Incremental tranche loss settlement & accrued premium reduction
- Analytical approximations (recursive inclusion-exclusion for small N) to reduce paths
- Base correlation calibration store & solver integration
- Stochastic recovery (reuse Epic 13 Phase B module)
- Multi-factor correlation model (region + sector) for diversified global baskets
- Path importance sampling for rare high-order default pricing

---
**Prerequisites:** Epics 11–14 complete (issuer data, portfolio structures, simulation engine, bond issuer alignment). Next potential epic: base correlation & tranche calibration layer.
