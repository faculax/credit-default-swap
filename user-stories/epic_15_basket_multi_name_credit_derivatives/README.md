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

## 3.1 User Stories (Backlog)

<!-- Canonical stories list for automation (DO NOT REMOVE). Each line format: - Story 15.<n> – <Title> -->
- Story 15.1 – Basket Domain Model & Persistence
- Story 15.2 – Create & View First-to-Default Basket
- Story 15.3 – First-to-Default Pricing & Fair Spread Solver
- Story 15.4 – Pricing API Error Handling & Validation
- Story 15.5 – Convergence Diagnostics Exposure
- Story 15.6 – Basket Builder UI (Phase A)
- Story 15.7 – Basket Detail & Valuation Panel
- Story 15.8 – N-th-to-Default Extension
- Story 15.9 – Correlation & Recovery Sensitivities (Aggregate)
- Story 15.10 – Batch Pricing & Path Reuse
- Story 15.11 – Portfolio Aggregation Integration
- Story 15.12 – Tranche (Attach/Detach) Groundwork
- Story 15.13 – Tranche Premium & Loss Approximation
- Story 15.14 – Performance Baseline & Benchmark
- Story 15.15 – Documentation & Diagnostics
- Story 15.16 – Error & Validation Consistency
- Story 15.17 – UI Enhancements & Refresh Cycle
- Story 15.18 – Portfolio View Integration (UI)
- Story 15.19 – Sensitivities Toggle & Display
- Story 15.20 – Correlation Bump Configuration
- Story 15.21 – Recovery Override Handling
- Story 15.22 – Deterministic vs Random Seed Control
- Story 15.23 – Weight Rebalancing After Validation
- Story 15.24 – Tranche Interval Validation Errors
- Story 15.25 – Batch Pricing UI (Phase B)

### Phase A – Core Basket Foundations (First-to-Default)

**US-15-01 Basket Domain Model & Persistence**  
As a Platform Engineer, I want to persist basket definitions and constituents so that multi-name instruments can be referenced and priced consistently.  
Acceptance Criteria:
- Tables `basket_definitions` and `basket_constituents` created with fields in section 6.1.  
- Create + Read + Update (name, frequency, day count) + List endpoints available.  
- Weight normalization logic: if any weight provided, normalized vector sums to 1.000 (±1e-9); if none provided, equal weights assigned.  
- Validation rejects: duplicate issuer, empty constituent list, weight <0, sum(weights)=0, invalid type-specific fields (nth/attachment/detachment present when not applicable).  
- 400 response returns field-level errors JSON.

**US-15-02 Create & View First-to-Default Basket**  
As a Risk Analyst, I want to define a first-to-default basket (type=FIRST_TO_DEFAULT) so that I can request its fair spread.  
Acceptance Criteria:
- POST payload example in section 10.1 accepted; returns persisted ID & normalized weights.  
- GET by ID returns full constituent list with weights and derived fields (normalizedWeight).  
- Missing `nth`, `attachmentPoint`, `detachmentPoint` ignored (null) for FIRST_TO_DEFAULT.

**US-15-03 First-to-Default Pricing & Fair Spread Solver**  
As a Quant, I want to compute the fair spread of a first-to-default basket so that premium and protection legs are PV-neutral.  
Acceptance Criteria:
- POST `/api/baskets/{id}/price` with valuationDate returns fields in 10.2 example (fairSpreadBps, premiumLegPv, protectionLegPv, pv≈0 ± tolerance).  
- Spread solver converges with Brent or secant within max iterations; response includes iteration count.  
- Standard error (SE) of fair spread returned when paths >= 10k.  
- Deterministic seed yields repeatable fairSpreadBps (± floating rounding).  
- Performance: 10-name FTD (50k paths) < target latency baseline recorded.

**US-15-04 Pricing API Error Handling & Validation**  
As a User, I want clear errors when pricing inputs are invalid so that I can correct them quickly.  
Acceptance Criteria:
- Reject pricing if basket has <2 constituents.  
- Reject if basket type unsupported for requested pricing path.  
- 400 JSON: `{ "errorCode": "VALIDATION_ERROR", "message": "...", "fields": { ... } }`.  
- Non-existent basket ID returns 404 with `errorCode=NOT_FOUND`.

**US-15-05 Convergence Diagnostics Exposure**  
As a Risk Analyst, I want diagnostics (pathsUsed, SE) so that I can assess pricing stability.  
Acceptance Criteria:
- Response includes convergence object (pathsUsed, standardErrorFairSpreadBps, iterations).  
- Increasing paths 4x reduces SE roughly by factor ~2 (statistical tolerance ±25%).

**US-15-06 Basket Builder UI (Phase A)**  
As a User, I want a UI form to create a first-to-default basket so that I can price it without API tooling.  
Acceptance Criteria:
- Fields: name, type (radio), currency, notional, premiumFrequency, dayCount, maturityDate, constituents (multi-select + weight optional).  
- Dynamic validation summary showing normalized weights (or equal weight message).  
- On submit success, navigates to Basket Detail panel and triggers initial pricing.  
- Errors displayed inline next to fields.  
- Accessible labels & keyboard navigation for constituent multi-select.

**US-15-07 Basket Detail & Valuation Panel**  
As a User, I want to view basket metrics (fair spread, PV legs, diagnostics, sensitivities placeholders) so that I can interpret pricing output.  
Acceptance Criteria:
- Cards: Fair Spread (bps), Premium PV, Protection PV, Paths Used, SE(Fair Spread).  
- Constituents table: issuer, weight (normalized), recovery (base or override).  
- Refresh / Reprice button triggers new pricing call (optionally with updated paths parameter).  
- Graceful loading & error states shown.

### Phase B – N-th-to-Default & Sensitivities

**US-15-08 N-th-to-Default Extension**  
As a Quant, I want to price an N-th-to-default basket so that I can value structures with a trigger later than the first default.  
Acceptance Criteria:
- Basket type NTH_TO_DEFAULT requires `nth` integer (1 ≤ nth ≤ numberOfNames).  
- Pricing response includes `type=NTH_TO_DEFAULT` and uses correct payoff (pays at τ(N)).  
- For nth=1 results match FTD fair spread within MC tolerance (< 1.0 bps difference for identical seed & paths).  
- Validation rejects nth > numberOfNames.

**US-15-09 Correlation & Recovery Sensitivities (Aggregate)**  
As a Risk Analyst, I want spread DV01, correlation beta sensitivity, and recovery01 for a basket so that I can understand key risk drivers.  
Acceptance Criteria:
- Response `sensitivities` object populated (spreadDv01, correlationBeta, recovery01).  
- Finite difference bump sizes documented; sign conventions correct (long protection: higher recovery → PV down).  
- Runtime overhead < 2.5x base pricing (50k paths) for sensitivities on.

**US-15-10 Batch Pricing & Path Reuse**  
As a Platform Engineer, I want to price multiple baskets sharing constituents with one scenario matrix so that I reduce total latency.  
Acceptance Criteria:
- POST `/api/baskets/price/batch` accepts array of basket IDs & valuationDate.  
- Total runtime for pricing M baskets < naive sum by ≥30% for test fixture (document).  
- Deterministic seed yields identical individual basket results vs single pricing endpoint.

**US-15-11 Portfolio Aggregation Integration**  
As a Risk Analyst, I want basket positions reflected in portfolio views without double counting constituent notionals so that exposure metrics remain accurate.  
Acceptance Criteria:
- Portfolio API & UI show basket row with type & constituent count.  
- Underlying single-name exposures not auto-summed unless an "Expand Constituents" action is triggered (Phase 2 placeholder).  
- Concentration metrics exclude constituent duplication by default.

### Phase C – Tranche Foundations

**US-15-12 Tranche (Attach/Detach) Groundwork**  
As a Quant, I want to define a tranche slice on a basket so that I can compute expected tranche loss and fair spread.  
Acceptance Criteria:
- Basket type TRANCHETTE requires attachmentPoint < detachmentPoint (both in (0,1]).  
- Pricing returns expectedTrancheLossPct & fairSpreadBps (structure in 10.3 preview).  
- Validation ensures no overlap or invalid intervals; A >= D rejected.  
- N > names or empty constituent list rejected with clear error.

**US-15-13 Tranche Premium & Loss Approximation**  
As a Quant, I want a first-phase approximation for tranche premium leg using expected outstanding notional so that I can produce indicative pricing before full path-dependent settlement is implemented.  
Acceptance Criteria:
- Premium leg approximation documented in code (Javadoc / comments).  
- ETL timeline (selected horizons or internal grid) returned when request flag `includeEtlTimeline=true`.  
- Regression test ensures stable ETL within tolerance across code changes.

### Cross-Cutting & Quality

**US-15-14 Performance Baseline & Benchmark**  
As a Platform Engineer, I want a benchmark for FTD/N-th/tranche pricing so that optimization progress is measurable.  
Acceptance Criteria:
- Script or test logs timing for 10, 25, 50 names (50k paths).  
- Results captured in docs (IMPLEMENTATION_SUMMARY or performance log).  
- CI fails if latency regresses > defined threshold (placeholder optional).

**US-15-15 Documentation & Diagnostics**  
As a New Team Member, I want README & API docs aligned with implemented endpoints so that I can onboard quickly.  
Acceptance Criteria:
- README updated with any new endpoint parameters (seeds, sensitivity toggles).  
- Glossary additions for FTD, N-th, tranche ETL.  
- Example curl commands verified against running service.

**US-15-16 Error & Validation Consistency**  
As a User, I want consistent error schema across basket endpoints so that integration is simplified.  
Acceptance Criteria:
- All basket/tranche pricing errors follow shared schema (code/message/fields).  
- Negative tests in test suite cover: invalid type transitions, nth out of range, invalid attachment/detachment, duplicate issuers.

**US-15-17 UI Enhancements & Refresh Cycle**  
As a User, I want to reprice baskets with different path counts and see loading states clearly so that I can balance accuracy vs speed.  
Acceptance Criteria:
- Path count input with helper text explaining SE trade-off.  
- Disabled state for Price button while request in flight.  
- Last priced timestamp displayed.

**US-15-18 Portfolio View Integration (UI)**  
As a User, I want baskets to appear in portfolio view with expandable detail so that I can reconcile exposures.  
Acceptance Criteria:
- Basket row shows type (FTD/NTH/TRANCHETTE), fair spread, # constituents.  
- Expand control placeholder (even if Phase 2) without breaking layout.  
- No duplication of PV in totals.

**US-15-19 Sensitivities Toggle & Display**  
As a Risk Analyst, I want to optionally request sensitivities to minimize latency when I only need fair spread.  
Acceptance Criteria:
- Pricing request flag `includeSensitivities` (default true or documented).  
- When false, sensitivities object omitted or empty without error.  
- UI hides sensitivities cards when not returned.

**US-15-20 Correlation Bump Configuration**  
As a Quant, I want to configure correlation bump size for beta sensitivity so that I can adjust numerical stability.  
Acceptance Criteria:
- Config property (e.g., `pricing.correlation.bump=0.01`) with sane default.  
- Response includes `sensitivities.meta.bumpSizes.beta` when sensitivities returned.

**US-15-21 Recovery Override Handling**  
As a Risk Analyst, I want per-constituent recovery overrides honored in pricing so that LGD assumptions reflect current views.  
Acceptance Criteria:
- Override present → used; else default recovery.  
- Pricing response constituents section includes effectiveRecovery.  
- Recovery01 sensitivity uses effective base recovery.

**US-15-22 Deterministic vs Random Seed Control**  
As a Quant, I want optional seed input for reproducibility so that I can validate spread changes.  
Acceptance Criteria:
- Request parameter `seed` (optional); if absent system generates seed & echoes in response.  
- Identical seed + inputs ⇒ identical fairSpreadBps (bitwise where feasible).  
- UI shows seed used in last pricing.

**US-15-23 Weight Rebalancing After Validation**  
As a Platform Engineer, I want transparent normalized weights so that auditing is straightforward.  
Acceptance Criteria:
- Response includes both originalWeight (nullable) and normalizedWeight.  
- Sum(normalizedWeight)=1.000 ± small epsilon logged if outside tolerance.  
- Audit log entry records any normalization adjustments.

**US-15-24 Tranche Interval Validation Errors**  
As a User, I want clear messages when tranche intervals are invalid so that I can correct them quickly.  
Acceptance Criteria:
- Error messages specify which field (attachmentPoint/detachmentPoint) failed and why (e.g., A >= D).  
- Negative tests cover boundary conditions A=0, D=1, A just below D.

**US-15-25 Batch Pricing UI (Phase B)**  
As a User, I want to select multiple baskets and price them together so that I can compare spreads consistently.  
Acceptance Criteria:
- Multi-select list with Price Batch button.  
- Shared seed option for comparability.  
- Table displays each basket’s fair spread & SE.  
- Latency improvement vs sequential documented (placeholder snapshot).

> NOTE: Story numbering will continue for further phases (e.g., advanced loss settlement) at US-15-30+ to avoid renumbering.

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
