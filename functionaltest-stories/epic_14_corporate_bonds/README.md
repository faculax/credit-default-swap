# Epic 14 – Corporate Bonds Functional Test Plan

Tags: @EPIC_14 @CORPORATE_BONDS @FIXED_INCOME

## Scope
Functional, numeric, and integration validation for corporate bond lifecycle: trade capture, schedule generation, pricing (yield, spread, dirty/clean), risk (DV01/CS01), accrual, amortization, redemption, call/put features, corporate actions, market data updates, audit/reporting, accessibility.

## Objectives
- Ensure accurate coupon schedule generation (adjusted for business days, holidays).
- Validate pricing engines across yield/spread, clean vs dirty price, accrued interest.
- Confirm risk sensitivities (DV01, CS01) correctness within tolerances.
- Verify corporate actions (coupon change, call notices, rating changes) correctly update positions and downstream metrics.
- Maintain audit trail completeness and export/report integrity.
- Enforce security, RBAC, performance SLAs, accessibility for UI components.

## Risks & Edge Cases
- Irregular first/last coupon periods (stub periods).
- Floating rate coupons (FRN) vs fixed rate interplay.
- Call schedule overlapping with coupon dates.
- Rating migration impact on spread curve selection.
- Partial redemptions adjusting notional mid-period.
- Time zone, holiday calendar drift causing schedule misalignment.

## Scenario ID Pattern
FT-14-<storyNumber>-NNN (e.g., FT-14-1-001). Each story starts at 001.

## Tooling Layers
| Layer | Tools |
|-------|-------|
| UI | Playwright + axe-core |
| API | REST-assured / Playwright request fixtures |
| Engine | Numeric baseline harness (golden JSON) |
| DB | Testcontainers Postgres |
| Contract | JSON schema snapshots |
| Performance | Timing harness + p50/p95 thresholds |
| Drift | Baseline snapshot diffs |
| Security | RBAC negative tests, log redaction checks |

## Coverage Map
| Story | Topic | Key Metrics |
|-------|-------|-------------|
| 14-1 | Bond Trade Capture | bookingLatency, validationFailureCount |
| 14-2 | Coupon Schedule Generation | scheduleAccuracyPct |
| 14-3 | Spread Curve Construction | curvePointCount, curveLatency |
| 14-4 | Rating Migration Events | ratingChangePropagationLatency |
| 14-5 | Cash Flow Projection | pvAccuracyBps, projectionLatency |
| 14-6 | Pricing Yield & Spread | pricingDriftBps, pricingLatency |
| 14-7 | Risk Measures DV01/CS01 | dv01DriftBps, cs01DriftBps |
| 14-8 | Accrual & Amortization | accrualDriftBps |
| 14-9 | Redemption & Call Features | callExecutionLatency |
| 14-10 | Corporate Actions Processing | actionProcessingLatency |
| 14-11 | Market Data Updates | mdUpdateLatency, cacheInvalidations |
| 14-12 | Performance & Scaling | p95Runtime, memoryMaxMB |
| 14-13 | Audit & Reporting | auditEntryCount |
| 14-14 | Accessibility UI | accessibilityIssueCount |
| 14-15 | Data Export Integrity | exportHashStable |
| 14-16 | Contract Schema Stability | contractChangeDetectedCount |

## Baseline Artifacts
Stored under `sample_data/fixed_income_baselines/` (assumed). Artifacts: pricing_baseline.json, risk_sensitivities_baseline.json, coupon_schedules_baseline.json.

## Exit Criteria
- All story docs present with >=15 scenarios each.
- Numeric drift tests pass vs baseline within tolerance.
- No accessibility violations (critical) remaining.
- p95 pricing latency within SLA.
- Zero contract schema changes without version bump.

---
