# Epic 15 – Basket & Multi-Name Credit Derivatives Functional Test Plan

Tags: @EPIC_15 @BASKET @MULTI_NAME @CREDIT_DERIVATIVES

## Scope
Basket credit derivatives lifecycle and analytics: domain model persistence, first-to-default basket creation UI, fair spread solver, pricing API validation, convergence diagnostics & exposure, basket builder UI (phase A), detail valuation panel, N-th to default extension, correlation & recovery sensitivities aggregation, path reuse performance, portfolio aggregation integration, tranche functionality groundwork & premium/loss approximation, performance baselines, diagnostics documentation, error/validation consistency, UI enhancements/refresh cycles, portfolio integration UI, sensitivities toggle display, correlation bump configuration, recovery override handling, deterministic vs random seed control, weight rebalancing after validation, tranche interval validation errors, batch pricing UI (phase B).

## Objectives
- Accurate pricing models (first-to-default, N-th to default, tranches) with solver stability.
- Convergence diagnostics produce actionable metrics (iterations, error bounds).
- Efficient path reuse reduces computational latency in batch pricing.
- Correlation & recovery sensitivities aggregated correctly per basket and per constituent.
- Robust UI flows (builder, detail, batch pricing) accessible and performant.
- Deterministic vs random seed control reproducibility semantics validated.
- Weight rebalancing ensures basket normalization post-validation.
- Tranche interval validation prevents overlapping/invalid attachment-detachment points.
- Documentation synchronization for diagnostics and configuration parameters.

## Risks & Edge Cases
- Solver non-convergence for extreme correlation matrices.
- Path reuse causing stale cache or incorrect exposure.
- Tranche intervals overlapping or out-of-order definitions.
- Recovery override leading to inconsistent hazard/PD assumptions.
- Correlation bump configuration applying incorrect sign or scale.
- Seed determinism broken by parallelism.

## Scenario ID Pattern
FT-15-<storyNumber>-NNN where storyNumber corresponds to original basket story (1..25).

## Tooling
| Layer | Tools |
|-------|-------|
| Engine | Pricing/solver harness + baseline JSON |
| API | REST-assured / Playwright request |
| UI | Playwright + axe-core |
| Performance | Timing harness, CPU/memory profiling |
| Drift | Baseline snapshot comparisons |
| Contract | JSON schema snapshots |
| Security | RBAC, log redaction tests |
| Observability | Metrics & trace assertions |

## Coverage Map (Representative)
| Story | Topic | Metrics |
|-------|-------|---------|
| 15-1 | Basket Domain Model Persistence | basketPersistLatency |
| 15-2 | First-to-Default Basket Creation UI | basketCreateLatency |
| 15-3 | Fair Spread Solver | solverIterationCount |
| 15-4 | Pricing API Error Handling & Validation | pricingLatency |
| 15-5 | Convergence Diagnostics Panel | diagnosticsLatency |
| 15-6 | Basket Builder UI Phase A | uiRenderTime |
| 15-7 | Basket Detail Valuation Panel | valuationLatency |
| 15-8 | N-th to Default Extension | nthDefaultSolverLatency |
| 15-9 | Correlation & Recovery Sensitivities Aggregation | corrSensCalcLatency |
| 15-10 | Batch Pricing Path Reuse Optimization | pathReuseHitRatio |
| 15-11 | Portfolio Aggregation Integration | aggregationLatency |
| 15-12 | Tranche Groundwork Structures | trancheSetupLatency |
| 15-13 | Tranche Premium Approximation Solver | trancheApproxErrorBps |
| 15-14 | Basket Pricing Performance Baseline | p95Runtime |
| 15-15 | Diagnostics & Metrics Documentation | docSyncLagSec |
| 15-16 | Validation Error Consistency | validationFailureCount |
| 15-17 | Batch Pricing UI Phase B | batchPricingLatency |
| 15-18 | Portfolio Integration UI Display | portfolioViewLatency |
| 15-19 | Sensitivities Toggle & Display | toggleLatency |
| 15-20 | Correlation Bump Configuration | corrBumpLatency |
| 15-21 | Recovery Override Configuration | recoveryOverrideLatency |
| 15-22 | Deterministic vs Random Seed Control | deterministicRunCount |
| 15-23 | Weight Rebalancing Workflow | weightRebalanceLatency |
| 15-24 | Tranche Interval Validation Errors | validationFailureCount |
| 15-25 | Batch Pricing UI Phase C Reporting | reportExportLatency |

## Baselines
In `sample_data/basket_baselines/`: fair_spread_baseline.json, nth_default_baseline.json, tranche_pricing_baseline.json, sensitivities_baseline.json, performance_profile_baseline.json.

## Exit Criteria
- All 25 story docs with >=12 scenarios each.
- All solver convergence tests pass within iteration limits.
- No unexpected drift vs baseline artifacts.
- Accessibility audits pass (no critical issues).
- Batch pricing path reuse shows improvement (>30% hit ratio).
- Contract schemas unchanged unless versioned.

---
