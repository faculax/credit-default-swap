# Epic 14 – Cash & Survival-Based Credit Bonds Enablement

## 1. Background
After strengthening single-name CDS trade data (Epic 11), introducing portfolio aggregation (Epic 12), and standing up a correlated Monte Carlo framework (Epic 13), the platform is ready to extend into **cash corporate / sovereign credit bonds**. These instruments unlock broader credit inventory coverage, duration & spread analytics, jump‑to‑default exposure metrics, and serve as foundational building blocks for later structured credit (baskets, tranches). Delivering bonds first (before baskets) isolates deterministic pricing and static data concerns from multi‑name simulation complexity.

## 2. Problem Statement
Currently the platform supports only CDS-based credit risk views. Users cannot:
- Capture or value cash bonds (coupon schedules, accrued interest, clean vs dirty price)
- Derive yield / Z-spread / G-spread / asset swap spread from price inputs
- Produce portfolio duration / spread DV01 including cash instruments
- Report jump‑to‑default (JTD) exposure across both CDS and bonds
- Use a unified issuer + curve mapping across derivative and cash credit assets

Without bond support, analytics and reporting remain derivative‑only, limiting adoption by trading, treasury, and risk stakeholders who manage mixed portfolios.

## 3. Objectives
| # | Objective | Success Criteria |
|---|-----------|------------------|
| 1 | Introduce Bond domain model & persistence | CRUD endpoints; mandatory economics stored; migrations deployed |
| 2 | Implement deterministic (risk‑free) PV & accrual | PV matches analytic baseline for flat curve test set (< 0.01% error) |
| 3 | Add survival-based risky PV (hazard curve) | Risky PV vs deterministic delta non-zero for positive hazard; tolerance tests pass |
| 4 | Support price ↔ yield ↔ spread conversions | Round-trip price→yield→price < 0.5 bp error (standard coupons) |
| 5 | Compute credit & rate sensitivities | Spread DV01, IR DV01, Jump-to-Default calculated & exposed |
| 6 | Integrate UI creation & detail valuation panel | Users can create, price, and view metrics alongside CDS trades |
| 7 | Portfolio aggregation inclusion | Bond metrics aggregated with CDS; new columns appear in portfolio views |
| 8 | Provide API / schema groundwork for future callable features | Optional fields reserved / documented without implementation |

## Stories
- Story 14.1 – DB Migration & Bonds Table
- Story 14.2 – Bond Domain Entity & Repository Mapping
- Story 14.3 – Bond Validation Layer
- Story 14.4 – Cashflow Schedule & Day Count Utilities
- Story 14.5 – Deterministic Bond Pricing & Accrual
- Story 14.6 – Yield & Z-Spread Solvers
- Story 14.7 – Survival-Based Hazard Pricing Extension
- Story 14.8 – Bond Sensitivities (IR DV01, Spread DV01, JTD)
- Story 14.9 – Bond CRUD REST Endpoints
- Story 14.10 – Bond Pricing Endpoint
- Story 14.11 – Portfolio Aggregation Integration (Bonds)
- Story 14.12 – Frontend Bond Creation & Detail View
- Story 14.13 – Frontend Portfolio Bond Metrics & Columns
- Story 14.14 – Bond Testing Suite (Unit & Integration)
- Story 14.15 – Performance & Batch Pricing Preparation
- Story 14.16 – Documentation & Agent Guide Update

## 4. In Scope
- Plain vanilla fixed coupon bullet bonds (no amortization)
- Fixed rate, ACT/ACT or 30/360 (subset) + quarterly / semi-annual / annual frequency (configurable)
- Deterministic discount curve (existing risk-free curve infra) + hazard curve integration for survival discounting
- Price ↔ yield ↔ spread (Z-spread) conversions
- Z-spread computed via iterative root solve (Brent/secant)
- Sensitivities: IR DV01 (parallel risk-free curve bump), Spread DV01 (parallel hazard bump), JTD (LGD * Exposure)
- REST endpoints + DTOs; integration into existing aggregation service
- UI form & valuation card + portfolio table enhancements

## 5. Out of Scope (Deferred)
- Floating rate / FRN bonds
- Callable / putable / make-whole features
- Inflation-linked or amortizing structures
- OAS (Option Adjusted Spread) / curve-specific G-spread (phase 2)
- Stochastic recovery modeling (Epic 13 Phase B alignment only, not needed here)
- Convertible / subordinated hybrid logic beyond seniority & recovery rate

## 6. Domain Model Additions
### 6.1 Entity: Bond
| Field | Type | Example | Notes |
|-------|------|---------|-------|
| id | Long | 101 | PK |
| isin | String(12) | US037833AZ00 | Unique identifier (nullable until assigned) |
| issuer | String(40) | AAPL | Maps to reference entity used by CDS |
| seniority | ENUM (SR_UNSEC, SR_SEC, SUBORD) | SR_UNSEC | Reuse from CDS enum |
| currency | String(3) | USD | Pricing & aggregation |
| notional | DECIMAL(18,2) | 1000000.00 | Face amount |
| couponRate | DECIMAL(9,6) | 0.045000 | Annualized fixed coupon rate |
| couponFrequency | ENUM (ANNUAL, SEMI_ANNUAL, QUARTERLY) | SEMI_ANNUAL | Schedule generation |
| dayCount | ENUM (ACT_ACT, THIRTY_360) | ACT_ACT | Accrual calc |
| issueDate | DATE | 2024-01-15 | Start of accrual |
| maturityDate | DATE | 2030-01-15 | Redemption date |
| settlementDays | INT | 2 | Basic settlement assumption |
| creditCurveId | String(40) | AAPL_SR_USD | Hazard curve linkage |
| recoveryRate | DECIMAL(5,4) | 0.4000 | For JTD & risky PV |
| sector | String(30) | TECH | Aggregation & correlation alignment |
| faceValue | DECIMAL(18,2) | 100.00 | Per-unit face (if quoting per 100) |
| priceConvention | ENUM (CLEAN, DIRTY) | CLEAN | Input interpretation |
| createdAt / updatedAt | TIMESTAMP | - | Audit |

### 6.2 Derived (Runtime)
- Accrued Interest
- Clean Price, Dirty Price (both stored in valuation response, not persisted long-term)
- Yield to Maturity (annualized, ACT/ACT basis for IRR)
- Z-Spread (flat hazard add-on or constant spread to discount curve)

## 7. Pricing Approaches
### 7.1 Deterministic PV (Phase 1)
PV_clean = Σ (Coupon_i * DF(t_i)) + Redemption * DF(T) - Accrued
Where Coupon_i = notional * couponRate / freqFactor.

### 7.2 Survival-Based Risky PV (Phase 2)
PV_risky = Σ (Coupon_i * P(Survive to t_i) * DF(t_i)) + Redemption * P(Survive to T) * DF(T) + Recovery * (1 - P(Survive to T)) * DF(T*) - Accrued
Simplification: assume default proceeds paid at maturity (T* ≈ T) for MVP; refine later to continuous default timing approximation.

### 7.3 Yield & Z-Spread
Root solve f(y) = PresentValueFromYield(y) - ObservedDirtyPrice = 0.
Root solve g(z) = PresentValueWithZSpread(z) - ObservedDirtyPrice = 0, where DF_z(t) = DF_base(t) * exp(-z * t).

### 7.4 Sensitivities
| Sensitivity | Method | Notes |
|-------------|--------|-------|
| IR DV01 | Parallel +1 bp bump of discount curve reprice | Return currency per 1 bp |
| Spread DV01 | Parallel +1 bp hazard bump reprice (survival-based only) | If hazard curve absent, null |
| JTD | Notional * (1 - RecoveryRate) - (Current Mark-to-Market Gain) | Approx: Notional * LGD for long exposure |

## 8. API Design
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/bonds | Create bond |
| GET | /api/bonds/{id} | Fetch bond details |
| PUT | /api/bonds/{id} | Update editable fields |
| POST | /api/bonds/{id}/price?valuationDate=YYYY-MM-DD | Price bond & compute metrics |
| POST | /api/bonds/price/batch | Batch pricing (future optimization) |

### 8.1 Create / Update Request (Minimal)
```json
{
  "issuer": "AAPL",
  "seniority": "SR_UNSEC",
  "currency": "USD",
  "notional": 1000000,
  "couponRate": 0.045,
  "couponFrequency": "SEMI_ANNUAL",
  "dayCount": "ACT_ACT",
  "issueDate": "2024-01-15",
  "maturityDate": "2030-01-15",
  "creditCurveId": "AAPL_SR_USD",
  "recoveryRate": 0.40,
  "sector": "TECH",
  "priceConvention": "CLEAN"
}
```

### 8.2 Pricing Response (Phase 2)
```json
{
  "bondId": 101,
  "valuationDate": "2025-10-07",
  "cleanPrice": 101.2345,
  "dirtyPrice": 101.7890,
  "accruedInterest": 0.5545,
  "yieldToMaturity": 0.04215,
  "zSpread": 0.00185,
  "pv": 1012345.67,
  "pvRisky": 1009022.11,
  "sensitivities": {
    "irDv01": 845.22,
    "spreadDv01": 912.77,
    "jtd": -600000.00
  },
  "inputs": {
    "couponRate": 0.045,
    "couponFrequency": "SEMI_ANNUAL",
    "dayCount": "ACT_ACT",
    "recoveryRate": 0.40,
    "creditCurveId": "AAPL_SR_USD"
  }
}
```

## 9. UI Impact
### 9.1 New Screens / Panels
- Bond Creation Modal (accessible from Instruments → New → Bond)
- Bond Detail View (tabs: Overview, Cashflows, Valuation History (future), JSON Debug)
- Portfolio View: Additional columns (Bond Count, Avg Yield, Duration, Spread DV01) and per-instrument rows show Type (CDS / Bond)

### 9.2 Form Fields (Creation / Edit)
| Field | Validation |
|-------|------------|
| Issuer | Required; must map to existing reference entity list |
| Notional | > 0 |
| Coupon Rate | ≥ 0 |
| Frequency | Supported enum only |
| Day Count | Supported enum only |
| Dates | issueDate < maturityDate |
| Recovery Rate | 0–1 inclusive |
| Credit Curve ID | Non-empty; must resolve to hazard curve (warning if missing) |

### 9.3 Valuation Card Metrics
Clean Price | Dirty Price | Accrued | YTM | Z-Spread | PV | Risky PV | IR DV01 | Spread DV01 | JTD

### 9.4 UX Rules
- Default frequency = SEMI_ANNUAL; dayCount = ACT_ACT
- Auto-suggest creditCurveId as `{ISSUER}_{SENIORITY}_{CCY}` with edit override
- Show warning banner if hazard curve not found (spreadDv01 & risky PV greyed out)
- Expandable section for Cashflow Table (coupon date, amount, DF, survival probability (if available))

## 10. Portfolio Aggregation Enhancements
Add to aggregation payload (per instrument): `type`, `duration`, `yield`, `spreadDv01`, `jtd`.
Portfolio-level: sum spreadDv01, sum jtd, compute notional-weighted average yield & duration (Macaulay or modified – choose modified for risk panel), extend concentration metrics to include top exposure by JTD.

## 11. Testing Strategy
| Layer | Tests |
|-------|-------|
| Unit | Accrual calc; yield <-> price round trip; Z-spread root solving convergence |
| Unit | Risky PV vs deterministic PV difference sign (hazard > 0 ⇒ PV_risky < PV) |
| Integration | Create → Price deterministic → Price with hazard curve → Compare metrics |
| Integration | Portfolio including CDS + Bond aggregated metrics correctness |
| Statistical | Convergence tolerance for Z-spread solver across sample bonds |
| UI | Form validation, disabled spread DV01 when hazard curve missing |

## 12. Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Yield solver non-convergence (deep discount/premium) | Bounded root interval [−50%, 200%]; fallback to secant |
| Missing hazard curve for issuer | Degrade gracefully: compute deterministic PV only; log structured warning |
| Incorrect accrual near coupon boundary | Use day count library / tested utility reused from CDS schedule logic |
| Performance (batch pricing many bonds) | Batch endpoint & cashflow precomputation cache (Phase 2) |
| JTD double counting with CDS (same issuer) | Clarify exposure definition per instrument; portfolio view separates CDS vs Bond JTD initially |

## 13. Acceptance Criteria
1. Can create a Bond with minimal required fields; response returns generated id.
2. Pricing endpoint returns cleanPrice, dirtyPrice, accruedInterest, yieldToMaturity for deterministic mode.
3. Adding hazard curve availability toggles survival-based outputs: pvRisky & spreadDv01 non-null.
4. Price → yield → price round trip absolute error < 0.5 bp for test set of standard coupon bonds (semi-annual 3–10Y).
5. Risky PV < Deterministic PV when hazard curve strictly positive (tolerance for rounding).
6. IR DV01 positive for typical fixed coupon bond (rising rates reduce PV). Spread DV01 positive (wider spreads reduce PV).
7. JTD equals `notional * (1 - recoveryRate)` (sign consistent with long position risk) within tolerance.
8. Portfolio aggregation includes bond metrics and sums correctly with CDS metrics (no duplicate issuer logic errors).
9. UI warns when hazard curve missing; spread DV01 greyed and explanatory tooltip shown.
10. All new fields documented and migration applied without altering existing CDS behavior.

## 14. Implementation Steps (Chronological)
1. DB Migration: create `bonds` table + indexes (issuer, maturity)
2. Domain entity + repository + DTOs & mapper
3. Validation layer (Bean Validation annotations)
4. Cashflow schedule generator (reuse or adapt CDS schedule util) + day count utils
5. Deterministic pricing service + accrual + yield solver
6. REST controller `/api/bonds` (CRUD) + `/api/bonds/{id}/price`
7. Survival-based pricing extension (hazard integration) + Z-spread solver
8. Sensitivities (IR DV01, Spread DV01, JTD)
9. Portfolio aggregation integration (add instrument type & metrics)
10. Frontend: form, detail panel, portfolio columns
11. Tests (unit + integration) + sample migration verification
12. Documentation: update `AGENTS.md` (service-level note if needed)

## 15. Future Extensions
- Floating / FRN bonds & asset swap spread
- Callable / putable with option-adjusted metrics (introduce OAS solver)
- Amortizing & sinking fund structures
- Link to rating & covenant metadata for risk scoring
- Stress scenario propagation (parallel hazard & rate shocks combined)
- Basket / tranche pricing (subsequent epic after survival-based bonds baseline)

---
**Next (Planned)**: Structured Credit Baskets / N-th-to-Default (separate epic) leveraging correlated simulation (Epic 13) and bond issuer data alignment.
