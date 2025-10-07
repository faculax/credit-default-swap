# Epic 13 – Correlated Monte Carlo Simulation & Joint Default Metrics

## 1. Background
With portfolios and linear aggregation (Epic 12) in place, the next evolution is to model joint default behavior and portfolio loss distributions. This introduces *systemic vs idiosyncratic* credit drivers and unlocks non-linear metrics (loss VaR, Expected Shortfall, probability of multiple defaults). The objective is an extensible simulation framework that can later price baskets, nth‑to‑default, and tranches without rework.

## 2. Problem Statement
Current risk outputs are linear sums. They ignore:
- Default clustering / correlation
- Loss tail distribution
- Diversification benefit quantification
- Path-dependent payoffs (future structured products)

## 3. Objectives
| # | Objective | Success Criteria |
|---|-----------|------------------|
| 1 | Implement a correlated default time generator | Gaussian one-factor copula producing realistic joint default scenarios |
| 2 | Produce portfolio loss distribution metrics | EL, VaR (95/99), ES (97.5) per horizon |
| 3 | Provide marginal / conditional analytics | Name contribution, diversification benefit |
| 4 | Add recovery scenario & stochastic recovery option (phase B) | Wider tail modeling optional toggle |
| 5 | Expose simulation via REST & UI | User-triggered run with progress & downloadable results |

## 3.1 User Stories (Backlog)

### Phase A – Core Correlated Simulation

**US-13-01 Run Correlated Simulation**  
As a Risk Analyst, I want to trigger a correlated Monte Carlo simulation for a selected portfolio with configurable paths, horizons, and correlation model so that I can see joint default risk metrics.  
Acceptance Criteria:
- POST `/api/credit-simulation/portfolio/{id}` accepts payload schema 6.1.
- Returns runId immediately (202 Accepted or 200 with status=QUEUED).
- Run status retrievable via GET endpoint until COMPLETE or FAILED.

**US-13-02 View Run Progress**  
As a Risk Analyst, I want to see simulation status (queued, running, complete, failed, canceled) so that I know when results are ready.  
Acceptance Criteria:
- Status transitions exposed in GET response.
- UI auto-polls until terminal state.
- Failed state includes error message (non-stacktrace user safe).

**US-13-03 Retrieve Portfolio Metrics**  
As a Risk Analyst, I want per-horizon EL, VaR95, VaR99, ES97.5, pAnyDefault, expected defaults so that I can assess tail risk.  
Acceptance Criteria:
- Metrics present for every requested horizon.
- pAnyDefault and EL are non-decreasing with tenor (allow minor MC noise tolerance).
- VaR and ES computed from empirical distribution (no parametric assumption).

**US-13-04 Diversification Benefit**  
As a Risk Analyst, I want to see diversification benefit percentage so that I can quantify correlation impact on expected loss.  
Acceptance Criteria:
- `benefitPct = (Σ standalone EL - portfolio EL)/Σ standalone EL` rounded to 0.1%.
- If single name, benefitPct = 0.0%.
- With β=0 for all names benefitPct within ±1% of 0 (MC noise).

**US-13-05 Contributors Table**  
As a Risk Analyst, I want a contributors table listing each entity’s marginal EL% and β so that I can identify main drivers.  
Acceptance Criteria:
- `marginalELPct` sums to 100% ±1%.
- β shown matches input (after overrides).
- Sorted descending by `marginalELPct`.

**US-13-06 Deterministic Recovery Support**  
As a Quant, I want constant recovery applied in loss generation so that Phase A results align with existing assumptions.  
Acceptance Criteria:
- Recovery taken from trade static (or default config).
- `LGD = 1 - recovery`.
- Recovery uniform across paths in Phase A.

**US-13-07 JSON Download**  
As a Risk Analyst, I want to download the full simulation results JSON so that I can archive and audit runs.  
Acceptance Criteria:
- Download button provides exact GET payload.
- Includes original request parameters snapshot.

**US-13-08 Reproducibility via Seed**  
As a Quant, I want to supply a random seed so that runs are reproducible for validation.  
Acceptance Criteria:
- Same seed + identical inputs ⇒ identical aggregated metrics (bit-for-bit within float rounding).
- Omitted seed generates random seed returned in results.

**US-13-09 Input Validation**  
As a User, I want validation errors before a run starts so that I avoid long failed jobs.  
Acceptance Criteria:
- Reject paths <=0 or > configured max.
- Reject empty horizons.
- Reject invalid β overrides (<0 or >0.95).
- Return 400 with field-level messages.

**US-13-10 Performance Baseline**  
As a Platform Engineer, I want a 20k-path simulation (10 names, 3 horizons) to finish under target latency so that UI remains responsive.  
Acceptance Criteria:
- Document measured median runtime (target placeholder baseline).
- Parallelization flag enabled without changing numeric results.

**US-13-11 Metrics Glossary Modal**  
As a New User, I want a glossary explaining EL, VaR, ES, diversification so that I can interpret metrics.  
Acceptance Criteria:
- Modal accessible from simulation tab.
- Content matches section 20 glossary terms.

**US-13-12 Cancel Running Simulation**  
As a Risk Analyst, I want to cancel an in-progress simulation so that I can free resources after changing my mind.  
Acceptance Criteria:
- DELETE endpoint sets status=CANCELED.
- Partial data not returned once canceled.

### Phase B – Stochastic Recovery

**US-13-13 Enable Stochastic Recovery**  
As a Risk Analyst, I want to toggle stochastic recovery so that I can assess tail sensitivity.  
Acceptance Criteria:
- Toggle adds `stochasticRecovery.enabled=true` to request.
- When false behavior identical to Phase A.

**US-13-14 Configure Recovery Distribution**  
As a Quant, I want to choose Beta recovery parameters so that loss tails reflect variability.  
Acceptance Criteria:
- `distribution.type=BETA` requires alpha,beta >0.
- Mean & stdev of sampled recoveries reported per horizon (or portfolio-level) in response.

**US-13-15 Factor-Recovery Correlation**  
As a Quant, I want to set correlation between systemic factor and recovery so that stress dependence is modeled.  
Acceptance Criteria:
- `corrWithFactor` in [-0.5, 0.5].
- Negative correlation increases ES (statistically verifiable in test with fixed seed).

**US-13-16 Extended Contributors (Recovery Stats)**  
As a Risk Analyst, I want per-name mean recovery when stochastic recovery is enabled so that I can explain LGD shifts.  
Acceptance Criteria:
- `contributors[]` includes `recovery.mean`, `recovery.stdev` when enabled.
- Not present (or null) when disabled.

### Cross-Cutting

**US-13-17 Error Handling**  
As a User, I want clear error messages for invalid model config so that I can correct inputs quickly.  
Acceptance Criteria:
- JSON: `{ "errorCode": "...", "message": "...", "fields": {...} }`.

**US-13-18 Audit Trail (Optional Phase A+)**  
As a Compliance Officer, I want persisted run metadata so that I can reconstruct risk reports.  
Acceptance Criteria:
- Persist request payload, runtime stats, version hash.
- Retrieval endpoint lists past runs filtered by date.

**US-13-19 Security & Resource Limits**  
As a Platform Engineer, I want guardrails on simulation size so that infrastructure remains stable.  
Acceptance Criteria:
- Max paths configurable (reject above threshold with 400).
- Per-path raw outputs disabled unless `includePerPath=true`.
- Hard timeout yields FAILED with errorCode=TIMEOUT.

**US-13-20 Determinism Test Harness**  
As a Quant, I want a harness to compare two seeds or configs so that I can validate stability across releases.  
Acceptance Criteria:
- CLI or test logs relative drift of EL & VaR (< specified tolerance) across code changes.
- Fails build if drift exceeds threshold.

**US-13-21 Monotonicity & Consistency Checks**  
As a Quant, I want automatic checks (EL, pAnyDefault non-decreasing with horizon) so that I catch data or interpolation regressions.  
Acceptance Criteria:
- Post-run validation block sets status=FAILED if violated beyond tolerance.

**US-13-22 Observability / Metrics**  
As an SRE, I want runtime metrics (paths/sec, queue wait) so that I can tune performance.  
Acceptance Criteria:
- Expose Prometheus counters & histograms (if infra ready) or log structured metrics.

**US-13-23 UI Parameter Persistence**  
As a User, I want my last simulation parameters pre-filled so that I can iterate faster.  
Acceptance Criteria:
- Local storage (frontend) remembers last successful run config.

**US-13-24 Accessibility & Responsiveness**  
As a User, I want the simulation tab to be keyboard navigable and responsive so that it’s usable across devices.  
Acceptance Criteria:
- Tab order logical; charts have ARIA labels.
- Layout adapts to 1280px and 1920px without overflow.

**US-13-25 Documentation & Glossary Sync**  
As a New Team Member, I want README stories and glossary aligned with UI help modal so that terminology is consistent.  
Acceptance Criteria:
- Glossary modal content sourced from a single markdown snippet or JSON to avoid drift.

**US-13-26 Cancellation Robustness**  
As a Platform Engineer, I want cancellation to free memory promptly so that large abandoned runs don’t degrade throughput.  
Acceptance Criteria:
- Cancellation interrupts path loop batch boundary (< 1s average delay).
- Memory footprint returns near baseline (monitored in test harness).

**US-13-27 Scaling to Multi-Portfolio (Future)**  
As a Risk Analyst, I want (future) ability to queue multiple portfolio runs so that I can batch daily risk.  
Acceptance Criteria:
- Placeholder story; out of scope Phase A/B (tracked for dependency planning).

---

> NOTE: Story numbering continues sequentially; future Phase C (structured products) will begin at US-13-30+ to avoid renumbering.

## 4. Phasing
| Phase | Scope |
|-------|-------|
| A | One-factor Gaussian copula + constant recovery |
| B | Stochastic recovery (beta / mixture) + recovery sensitivity integration |
| C | Structural products (nth-to-default, basket pricing) |
| D | Tranche base correlation calibration (future) |

## 5. Core Modeling Components
### 5.1 Input Data
- Per-name hazard/survival curve (from ORE calibration or internal bootstrap)
- Recovery rate (base) per trade
- Sector / factor mapping (from `sector` field on trade)
- Factor loadings β_i (derived from sector map + overrides config)
- Correlation config file: `credit-correlation.json`
```json
{
  "model": "ONE_FACTOR",
  "systemicLoadingDefault": 0.35,
  "sectorOverrides": { "TECH": 0.40, "FINANCIALS": 0.30 },
  "idOverrides": { "AAPL": 0.45 },
  "seed": 12345
}
```

### 5.2 Default Time Simulation (Phase A)
1. Draw systemic factor Z ~ N(0,1)
2. For each name i: draw ε_i ~ N(0,1)
3. Compute latent variable: X_i = β_i Z + sqrt(1-β_i^2) ε_i
4. Transform to uniform: U_i = Φ(X_i)
5. Map to default time τ_i by inverting survival: S_i(t) = exp(-∫λ_i) ; find smallest t s.t. U_i ≥ S_i(t)
   - Practical: discretize hazard curve buckets and interpolate.
6. Record default indicator per horizon.

### 5.3 Loss Calculation
Loss_i = Notional_i * LGD_i * I(τ_i ≤ Horizon)
PortfolioLoss = Σ Loss_i

### 5.4 Metrics
| Metric | Definition |
|--------|------------|
| Expected Loss (EL) | E[PortfolioLoss] |
| VaR_q | Smallest L s.t. P(Loss ≤ L) ≥ q |
| ES_q | E[Loss | Loss > VaR_q] |
| pAnyDefault | P(∃ i: τ_i ≤ T) |
| Expected Defaults | E[ Σ I(τ_i ≤ T) ] |
| Diversification Benefit | (Σ standalone EL - Portfolio EL)/Σ standalone EL |
| Name Marginal EL | ΔEL removing name i (approx via difference or Shapley baseline later) |

### 5.5 Stochastic Recovery (Phase B)
Config example:
```json
{
  "stochasticRecovery": {
    "enabled": true,
    "distribution": { "type": "BETA", "alpha": 2.5, "beta": 4.0 },
    "corrWithFactor": -0.25
  }
}
```
Recovery draw R_i scenario → LGD_i = 1 - R_i.

## 6. API Design
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/credit-simulation/portfolio/{portfolioId} | Run simulation with payload params |
| GET | /api/credit-simulation/runs/{runId} | Fetch status/results |
| DELETE | /api/credit-simulation/runs/{runId} | Cancel/cleanup |

### 6.1 Simulation Request
```json
{
  "valuationDate": "2025-10-07",
  "horizons": ["1Y","3Y","5Y"],
  "paths": 20000,
  "factorModel": {
    "type": "ONE_FACTOR",
    "systemicLoadingDefault": 0.35,
    "sectorOverrides": {"TECH": 0.40},
    "idOverrides": {"AAPL": 0.45}
  },
  "stochasticRecovery": { "enabled": false },
  "seed": 12345,
  "includePerPath": false
}
```

### 6.2 Simulation Response (Phase A)
```json
{
  "runId": "SIM-20251007-001",
  "portfolioId": 7,
  "valuationDate": "2025-10-07",
  "paths": 20000,
  "horizons": [
    {
      "tenor": "1Y",
      "pAnyDefault": 0.0431,
      "expectedDefaults": 0.061,
      "loss": {
        "mean": 210000.0,
        "var95": 690000.0,
        "var99": 1250000.0,
        "es97_5": 980000.0
      },
      "diversification": { "sumStandaloneEL": 245000.0, "portfolioEL": 210000.0, "benefitPct": 14.3 }
    }
  ],
  "contributors": [
    { "entity": "AAPL", "marginalELPct": 32.0, "beta": 0.45 },
    { "entity": "MSFT", "marginalELPct": 21.5, "beta": 0.40 }
  ],
  "settings": { "stochasticRecovery": false }
}
```

### 6.3 Phase B Additions
Add per-name recovery stats:
```json
"recovery": { "mean": 0.39, "stdev": 0.07 }
```

## 7. Internal Architecture
| Component | Responsibility |
|-----------|---------------|
| CorrelationConfigLoader | Parse & validate factor model JSON |
| HazardCurveProvider | Supplies survival inversion utility |
| DefaultTimeSimulator | Core loop generating τ_i per path |
| LossAggregator | Accumulates horizon losses & stats |
| MetricsCalculator | VaR / ES / diversification computations |
| SimulationRepository | Persist run metadata (optional Redis or DB) |

## 8. Performance Considerations
| Concern | Mitigation |
|---------|------------|
| GC pressure from large arrays | Reuse primitive arrays per path batch |
| Tail metric accuracy | Use 20k–50k paths; bootstrap CI later |
| Parallelization | Java parallel streams or ForkJoin across path batches |
| Numerical Inversion | Precompute cumulative survival grid per name |

## 9. Validation & Sanity Checks
- Single-name portfolio: MC EL ≈ analytic EL (tolerance < 2%)
- Zero correlation: diversification benefit ≈ 0 (small MC noise)
- High correlation (β→1): pAnyDefault rises; VaR approaches sum of losses in extreme tail
- Recovery fixed vs stochastic: ES should increase with stochastic recovery enabled

## 10. UI Changes
### 10.1 Portfolio Risk Panel (New Simulation Tab)
Sections:
1. Controls: Paths, Horizons (chips), Correlation Model preset, Stochastic Recovery toggle, Seed
2. Run Button + Progress bar (queued → running → complete)
3. Summary Cards (per horizon): pAnyDefault, EL, VaR99, ES97.5, Diversification Benefit
4. Loss Distribution Chart (histogram for selected horizon)
5. Contributors Table: Entity | Marginal EL % | β | Standalone EL | Conditional EL*
6. JSON Download of full results

Conditional EL* (optional later): EL contribution given at least one default.

### 10.2 UX Enhancements
- Disable Run while a job is active (or allow cancel)
- Indicate last run parameters snapshot for reproducibility
- Provide link “Explain Metrics” modal (glossary for VaR, ES, Diversification)

## 11. Stochastic Recovery (Phase B) UI Additions
- Distribution Type select (Fixed | Beta | Mixture)
- Parameters dynamic form
- Correlation with Factor slider (-0.5 to +0.5)

## 12. Security / Resource Controls
| Issue | Control |
|-------|---------|
| Excessive path counts (DoS) | Enforce max paths (e.g. 200k) |
| Large result payload | Truncate per-path outputs unless explicit request |
| Seed misuse | Validate numeric range + optional random seed fallback |

## 13. Testing Strategy
| Layer | Tests |
|-------|-------|
| Unit | Inversion accuracy, VaR calculation, Beta recovery sampling |
| Property | Monotonicity: Higher β → higher pAnyDefault for fixed marginal PDs |
| Integration | End-to-end run: request → results JSON shape |
| Statistical | Kolmogorov-Smirnov on marginal PD vs simulated default frequency |
| Performance | Benchmark 20k vs 50k paths latency |

## 14. Metrics Formulas Reference
Let F_L be empirical CDF of losses.
- VaR_q = inf { l | F_L(l) ≥ q }
- ES_q = (1 / (1 - q)) ∫_{q}^{1} VaR_u du (approx: mean of tail sample > VaR_q)
- DiversificationBenefit = (Σ EL_i - EL_portfolio)/Σ EL_i

## 15. Implementation Steps
### Phase A
1. Correlation config schema & loader
2. Hazard curve access (reuse ORE bootstrapped term structures or ingest survival points per trade)
3. Default time simulator (one-factor)
4. Loss aggregator + metrics
5. REST controller + async job handling
6. UI Simulation tab (basic)
7. Validation & statistical tests

### Phase B
8. Stochastic recovery module
9. Recovery-correlated sampling (Cholesky 2D correlation for factor & recovery noise)
10. Extended metrics & UI fields

### Phase C (Preview)
11. Basket / nth-to-default payoff analyzer
12. Structured product endpoints

## 16. Demo Script
1. Open a portfolio (≥5 trades across 2 sectors)
2. Run simulation with 10k paths (quick) → show pAnyDefault & EL
3. Increase paths to 50k → show tail stabilization
4. Toggle correlation (β default vs β=0) → show diversification benefit change
5. Enable stochastic recovery (Phase B) → highlight ES increase

## 17. Example Pseudocode (Core Loop)
```java
for (int p=0; p<paths; p++) {
  double Z = normal();
  for (int i=0; i<n; i++) {
    double eps = normal();
    double Xi = beta[i] * Z + sqrt(1 - beta[i]*beta[i]) * eps;
    double Ui = cdfNormal(Xi);
    double tau = invertSurvival(i, Ui); // piecewise linear search
    for (Horizon h : horizons) {
       if (tau <= h.time) losses[h.index] += notional[i] * LGD[i];
    }
  }
}
```

## 18. Acceptance Criteria (Phase A)
- Simulation run returns JSON with horizons populated and monotonic metrics (pAnyDefault increasing with tenor)
- Single-name portfolio: MC EL within ±2% of analytic EL over 3 horizons
- β=0 vs β>0 scenario: VaR99 increases with β>0 (same inputs)

## 19. Future Extensions
- Multi-factor model (region + sector)
- Base correlation calibration for tranches
- CVA exposure overlay (joint credit & exposure simulation)
- Importance sampling for tail precision

## 20. Glossary (User-Facing)
| Term | Meaning |
|------|---------|
| EL | Expected portfolio loss up to horizon |
| VaR_q | Loss threshold not exceeded with probability q |
| ES_q | Average loss conditional on exceeding VaR_q |
| pAnyDefault | Probability at least one reference entity defaults |
| Expected Defaults | Mean count of defaulting names |
| Diversification Benefit | Relative EL reduction due to imperfect correlation |
| β (Loading) | Sensitivity of name to systemic factor |

---
**Prerequisites:** Epics 11 & 12 complete (recovery, sector tagging, portfolio aggregation). Next evolution after this: structured basket & tranche pricing.
