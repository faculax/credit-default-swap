# Epic 11 – Single-Name CDS Technical Debt Remediation (Minimal Scope Refined)

## 1. Background
The current `CDSTrade` implementation provides CRUD persistence but omits several essential attributes and behaviors required for a production‑grade single‑name CDS life‑cycle and pricing flow. Recovery assumptions, seniority linkage, and curve identification are either missing or implicit, limiting downstream risk analytics (PV decomposition, fair spread computation, sensitivities). This refined epic narrows scope to the **minimum viable enhancements** required to unlock Epics 12 (portfolio aggregation) and 13 (correlated Monte Carlo) without over‑scoping (e.g. excluding upfront / standard coupon logic, settlement workflows, restructuring nuance for now).

## 2. Problem Statement
Without explicit recovery, seniority, and curve identifiers, the platform cannot:
* Reconcile premium vs protection leg PVs
* Compute fair (par) spread or Rec01 consistently
* Distinguish multiple curves for the same issuer (senior vs subordinated)
* Provide inputs needed for portfolio loss & correlation modeling (LGD, sector mapping)
* Reliably aggregate or attribute risk across multiple trades

## 3. Objectives (Minimal Set)
| # | Objective | Success Criteria |
|---|-----------|------------------|
| 1 | Add essential CDS economics (recovery, seniority, curve id, enum frequencies) | New columns + enums deployed; legacy fields deprecated but readable |
| 2 | Pricing & sensitivity endpoint | `/api/cds-trades/{id}/price` returns PV components + fair spread + CS01 + Rec01 |
| 3 | Validation | Reject invalid date ordering, negative / zero notional or spread, invalid enums |
| 4 | ORE integration consistency | Generated trade XML includes RecoveryRate & CreditCurveId; outputs parsed reliably |
| 5 | Frontend enablement | Forms & detail panel show new fields + valuation card with new metrics |
| 6 | Integration test upgrade | Script asserts metric changes across valuation dates & recovery shift scenario |

## 4. In Scope
* Data model enhancement & DB migration (minimal fields only)
* REST DTO layer (entity decoupling)
* Pricing service integration with ORE (single trade) – PV legs, fair spread, sensitivities
* Sensitivity calculations (parallel reprice approach for CS01 & Rec01)
* Basic enrichment for sector tagging (for correlation readiness)

## 5. Out of Scope (Deferred to Later Epics / Backlog)
* Correlation / portfolio simulation (Epic 12+)
* Stochastic recovery modeling (Epic 13 Phase B)
* Index / basket / tranche support
* Corporate actions & successor mapping
* Upfront / standard coupon structure
* Settlement workflow & restructuring clause logic
* JTD (Jump-to-Default) metric (optional, can add later)

## 6. New / Updated Domain Model
### 6.1 Enum Summary
Only ONE new enum is introduced in this epic: `Seniority`.

The project already contains and uses string fields (not strong enums yet) for premium frequency and day count in `CDSTrade` plus supporting logic (e.g. `CouponScheduleService`). We will:
* Introduce a proper `Seniority` enum: `SR_UNSEC`, `SR_SEC`, `SUBORD`.
* (Optional Hardening – can defer) Later replace the existing string fields `premiumFrequency` and `dayCountConvention` with strong typed enums `PremiumFrequency` and `DayCount` if/when we refactor scheduling. For minimal scope we keep them as-is to avoid broad refactors today.

```java
public enum Seniority { SR_UNSEC, SR_SEC, SUBORD }
// PremiumFrequency & DayCount already functionally present as string representations; enum hardening deferred.
```

### 6.2 Entity Additions (`CDSTrade`) – Minimal Set
| Field | Type | Example | Purpose |
|-------|------|---------|---------|
| recoveryRate | DECIMAL(5,4) | 0.4000 | LGD = 1 - recoveryRate; fair spread & Rec01 |
| seniority | ENUM | SR_UNSEC | Differentiated curves (capital structure) |
| premiumFrequency | (existing STRING) | QUARTERLY | Schedule generation & accrual (leave as string for now) |
| dayCount | (existing STRING) | ACT_360 | Accurate accrual & PV legs (leave as string for now) |
| creditCurveId | VARCHAR(40) | AAPL_SR_USD | Stable mapping to market data / ORE curve config |
| sector | VARCHAR(30) | TECH | Grouping for portfolio & future correlation (Epic 13) |

### 6.3 Deferred (Do NOT Implement Now)
| Field | Rationale |
|-------|-----------|
| settlementType | Not required until default workflow UI |
| restructuring | No modeling yet – keep as future extension |
| upfrontAmount / standardCoupon | Avoid complexity until benchmarking standard coupon trades |
| JTD metric | Optional analytics (can be derived later) |

Deprecate legacy string field `restructuringClause` (read-only fallback for migration phase). Retain `premiumFrequency` & `dayCountConvention` as-is (no enum migration in minimal scope) to minimize change surface.

## 7. API Changes
### 7.1 Request DTO (Create / Update) – Minimal
```json
{
  "referenceEntity": "AAPL",
  "notional": 10000000,
  "currency": "USD",
  "direction": "BUY",
  "effectiveDate": "2025-09-20",
  "maturityDate": "2030-12-20",
  "spreadBps": 100.0,            // If using running spread pattern
  "standardCouponBps": 100.0,     // Optional standard coupon model
  "upfrontAmount": 125000.00,     // Optional if standard coupon provided
  "recoveryRate": 0.40,
  "seniority": "SR_UNSEC",
  "settlementType": "PHYSICAL",
  "premiumFrequency": "QUARTERLY",
  "dayCount": "ACT_360",
  "restructuring": "MMR",
  "creditCurveId": "AAPL_SR_USD",
  "sector": "TECH"
}
```

### 7.2 Pricing Endpoint (Minimal Response Contract)
`POST /api/cds-trades/{id}/price?valuationDate=YYYY-MM-DD`
Mandatory output keys (others ignored by frontend until later):
```json
{
  "tradeId": 42,
  "valuationDate": "2025-10-07",
  "legs": {
    "premiumLegPv": 1234.56,
    "protectionLegPv": 1400.12,
    "accrued": 45.90
  },
  "pv": 165.56,
  "inputSpreadBps": 100.0,
  "fairSpreadBps": 98.4,
  "sensitivities": {
    "cs01": 455.2,
    "rec01": -250.7,
    "jtd": -5960000.00
  },
  "recoveryRate": 0.40,
  "seniority": "SR_UNSEC",
  "creditCurveId": "AAPL_SR_USD"
}
```

## 8. Pricing & Sensitivity Implementation
| Metric | Method | Notes |
|--------|--------|-------|
| Premium / Protection Leg PV | Parse ORE trade report / additional results | Keep monetary precision (scale=2) |
| Accrued | From ORE output | Display in valuation card |
| Fair Spread | Solve PV=0 (brent / secant) OR use ORE fair spread field if exposed | Show bps (4 dp) |
| CS01 | Parallel +1bp shift reprice (fast path) | Central diff later |
| Rec01 | Reprice with recovery +0.01 absolute | Return per 1pt change |
| Risky PV01 (optional if ORE outputs) | Parse directly | If present, show advanced metrics toggle |

### 8.1 Additional ORE Fields Potentially Available (If trade report exposes)
| Field | Use |
|-------|-----|
| riskyAnnuity | Improves fair spread calibration | 
| protectionLegNPV (already) | Validation vs recomputed PV |
| premiumLegNPV (clean/dirty) | Display & schedule debugging |

## 9. Database Migration (Minimal)
```sql
ALTER TABLE cds_trades ADD COLUMN recovery_rate DECIMAL(5,4);
ALTER TABLE cds_trades ADD COLUMN seniority VARCHAR(16);
-- Keep existing premiumFrequency (string) & dayCountConvention columns; no new enum columns in minimal scope
ALTER TABLE cds_trades ADD COLUMN credit_curve_id VARCHAR(40);
ALTER TABLE cds_trades ADD COLUMN sector VARCHAR(30);
-- Optional: if a restructuringClause column exists and is now deprecated, leave it untouched for backward compatibility.
UPDATE cds_trades SET recovery_rate=0.40, seniority='SR_UNSEC' WHERE recovery_rate IS NULL OR seniority IS NULL;
```

## 10. UI Changes (Acceptance-Oriented)
### Create / Edit Trade Form (Minimal Additions)
| Section | Fields | Default / Behavior |
|---------|--------|--------------------|
| Economics | Recovery Rate | 40.00% prefilled; numeric validation 0–1 |
| Structure | Seniority | SR_UNSEC default |
| Schedule | Premium Frequency, Day Count | QUARTERLY / ACT_360 |
| Market Mapping | Credit Curve ID | Auto derived: `{REF}_{SENIORITY}_{CCY}` editable |
| Classification | Sector | Optional tag (TECH, FINANCIALS, ... ) |

Deferred UI (hide for now): settlementType, restructuring, upfront, standard coupon.

### Trade Detail – Valuation Card (Mandatory Fields)
| Metric | Display Format |
|--------|----------------|
| PV | Currency (2 dp) |
| Fair Spread | bps (2–4 dp) |
| Accrued | Currency |
| Premium Leg PV | Currency |
| Protection Leg PV | Currency |
| CS01 | Currency per 1bp (2 dp) |
| Rec01 | Currency per 1pt recovery (2 dp) |

Advanced toggle (future): show riskyAnnuity if available.

### UX Rules
* Recalculate pricing only after user clicks "Price" (avoid chatty calls) or debounce 800ms on change.
* Warn (⚠) if |inputSpread - fairSpread| > 5 bps.
* Show grey placeholder for metrics before first pricing call.

## 11. Testing Strategy (Updated)
| Layer | Tests |
|-------|-------|
| Unit | Enum parsing, pricing service (mock ORE), recovery shift logic |
| Integration | Create → Price → Update → Price diff |
| Migration | Verify NULL backfill defaults |
| UI | Form validation (invalid recovery > 1.0, date ordering) |

## 12. Risks & Mitigations (Refined)
| Risk | Mitigation |
|------|------------|
| Inconsistent curve ID mapping | Central utility to derive `creditCurveId` |
| Performance on repeated repricing for sensitivities | Batch ORE runs (future) / cache base results |
| Legacy trades missing new fields | Migration defaults + progressive enhancement |

## 13. Acceptance Criteria (Updated)
### Data / API
1. Can create trade with minimal fields (`recoveryRate`, `seniority`, `creditCurveId`, existing `premiumFrequency`, existing `dayCountConvention`, optional `sector`).
2. Pricing endpoint returns: pv, premiumLegPv, protectionLegPv, accrued, fairSpreadBps, cs01, rec01 (riskAnnuity if available – optional).
3. Recovery rate change (Δ +0.05) **reduces** PV for BUY protection (directionally consistent) and yields Rec01 within tolerance (|rec01| > 0).
4. Invalid enum or negative notional rejected (HTTP 400 with error payload).

### ORE / Config
5. `creditCurveId` present in ORE generated XML and maps to existing curve quotes (update `CurveConfig.xml` if naming scheme differs).
6. If curve naming mismatch detected, pricing call fails fast with descriptive error.
7. (If required) `market.txt` extended to include spreads for new curve IDs introduced by seniority (e.g. `_SR_` vs `_SUBORD_`).

### Frontend
8. Create/Edit form shows new fields; defaults applied; validation prevents save on invalid ranges.
9. Valuation card appears with placeholders then populated after pricing.
10. Warning badge appears when |inputSpread - fairSpread| > 5 bps.

### Integration Test Enhancements
11. `integration-tests/test-cds-lifecycle.sh` extended to:
  * Capture new fields (if returned by risk API) or call pricing endpoint if separate.
  * Perform a recovery bump scenario: call pricing, then reprice with recovery+0.01 (temp patch via endpoint or mock) and assert PV delta sign.
  * Assert fairSpreadBps remains within ±5 bps over short valuation date shifts (e.g. T0 vs T7) if spreads static.
12. Script logs cs01 & rec01; ensures both non-null and numeric.

### Non-Goals Verified
13. Upfront / standard coupon fields remain absent from JSON unless explicitly added later.

### Performance / Robustness
14. Pricing round-trip (single trade) completes < 2s under normal load.
15. Repeated pricing with unchanged inputs is optionally cached (header `X-Cache: HIT` – future optimization, informational only).

## 14. Implementation Steps (Chronological – Trimmed)
1. Add enums & fields to entity + migration script
2. Introduce DTOs & mapper (MapStruct or manual)
3. Implement validation annotations + custom validator
4. Pricing service stub (fake numbers) + endpoint
5. Integrate real ORE call and parse outputs
6. Add CS01 / Rec01 by repricing
7. UI form changes + risk panel
8. Documentation & demo script

## 15. Example ORE Trade Snippet
```xml
<Trade id="CDS-42" tradeType="CreditDefaultSwap">
  <CreditDefaultSwap>
    <IssuerId>AAPL</IssuerId>
    <CreditCurveId>AAPL_SR_USD</CreditCurveId>
    <Seniority>SR</Seniority>
    <Currency>USD</Currency>
    <Notional>10000000</Notional>
    <RecoveryRate>0.40</RecoveryRate>
    <Side>Buy</Side>
    <StartDate>2025-09-20</StartDate>
    <Maturity>2030-12-20</Maturity>
    <Coupon>0.0100</Coupon>
    <PaymentFrequency>Quarterly</PaymentFrequency>
    <DayCounter>Act/360</DayCounter>
    <Calendar>NYC</Calendar>
  </CreditDefaultSwap>
</Trade>
```

## 16. Config & Market Data Amendments
| File | Change |
|------|--------|
| `risk-engine/src/main/resources/ore/CurveConfig.xml` | Ensure credit curve IDs align with new `creditCurveId` naming (add additional `<CreditCurve>` blocks if seniority variants used). |
| `risk-engine/src/main/resources/ore/market.txt` | Add spread quotes for any new curve IDs (e.g., `CDS/CREDIT_SPREAD/AAPL/SR/USD/5Y`). |
| `risk-engine/src/main/resources/ore/Conventions.xml` | No change unless day count / frequency variants added beyond existing. |
| `risk-engine/.../pricingengine.xml` | No change (MidPointCdsEngine still fine). |
| Frontend `.env` | Add pricing endpoint base URL if separate from risk engine API. |

## 17. Security Note
Remove any hardcoded tokens (Jira, Confluence, Render) from committed configs; replace with environment variables before integrating new pricing endpoints.

**Next Epic:** After this remediation, proceed to *Epic 12 – CDS Portfolio & Aggregation* to introduce multi-name risk views.
