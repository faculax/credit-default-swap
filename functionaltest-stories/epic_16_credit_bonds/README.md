# Epic 16 – Credit Bonds Functional Test Plan

Tags: @EPIC_16 @CREDIT_BONDS @FIXED_INCOME

## Scope
Credit bond foundational components: DB migration, domain entity & repository, validation layer, schedule/day count utilities, deterministic pricing & accrual, yield/Z-spread solvers, hazard/survival pricing extension, sensitivities (IR DV01, spread DV01, JtD), CRUD endpoints, pricing endpoint, portfolio aggregation integration, front-end creation & detail, portfolio metrics columns, testing suite, performance prep for batch pricing, documentation updates.

## Objectives
- Ensure structural DB migration correctness (schema objects created and versioned).
- Validate domain entity mapping and repository CRUD operations.
- Enforce comprehensive validation rules (ISIN, coupon, maturity, day count, currency).
- Correct coupon schedule generation & day count calculations.
- Accurate deterministic pricing & accrual with reproducibility.
- Yield/Z-spread solvers convergence within iteration limits.
- Survival/hazard-based pricing extension parity baseline.
- Sensitivities (IR DV01, spread DV01, JtD) numeric correctness within tolerances.
- Robust REST endpoints (CRUD + pricing) with schema stability, RBAC, error handling.
- Portfolio aggregation integration returns correct aggregated metrics.
- Front-end UI flows accessible, responsive, and secure.
- Performance baseline captured for batch pricing preparation.
- Documentation updates traceable and versioned.

## Risks & Edge Cases
- Illiquid spread curves causing solver instability.
- Stub periods, leap year day counts.
- Hazard curve negative intensity edge.
- Convergence failures at extreme spreads/yields.
- Repository transaction rollback integrity.

## Scenario ID Pattern
FT-16-<storyNumber>-NNN where storyNumber matches credit bond story (1..16).

## Tooling
| Layer | Tools |
|-------|-------|
| DB | Testcontainers Postgres + migration verification |
| API | REST-assured / Playwright request fixtures |
| Engine | Pricing/risk harness + numeric baselines |
| UI | Playwright + axe-core |
| Contract | JSON schema snapshots |
| Performance | Timing & resource profiling |
| Drift | Baseline JSON comparisons |
| Security | RBAC negative tests, log redaction |

## Coverage Map
| Story | Topic | Metrics |
|-------|-------|---------|
| 16-1 | DB Migration | migrationVersionApplied |
| 16-2 | Domain Entity Mapping | repositoryLatency |
| 16-3 | Validation Layer | validationFailureCount |
| 16-4 | Schedule Day Count Utilities | scheduleAccuracyPct |
| 16-5 | Deterministic Pricing & Accrual | pricingDriftBps |
| 16-6 | Yield & Z-Spread Solvers | solverIterationCount |
| 16-7 | Survival/Hazard Pricing Extension | hazardPricingDriftBps |
| 16-8 | Sensitivities (DV01/Spread/JtD) | dv01DriftBps |
| 16-9 | CRUD REST Endpoints | crudLatency |
| 16-10 | Pricing Endpoint | pricingLatency |
| 16-11 | Portfolio Aggregation Integration | aggregationLatency |
| 16-12 | Frontend Bond Creation & Detail | uiRenderTime |
| 16-13 | Portfolio Bond Metrics Columns | metricsColumnAccuracyPct |
| 16-14 | Bond Testing Suite | testCoveragePct |
| 16-15 | Performance Batch Pricing Preparation | batchPricingLatency |
| 16-16 | Documentation Agent Guide Update | docSyncLagSec |

## Baselines
In `sample_data/credit_bonds_baselines/`: pricing_baseline.json, sensitivities_baseline.json, schedules_baseline.json, hazard_pricing_baseline.json.

## Exit Criteria
- All 16 story docs present, >=12 scenarios each.
- Solver convergence tests green within max iterations.
- No drift failures vs baselines.
- CRUD and pricing endpoints contract stable.
- Accessibility checks pass (no serious violations).

---
