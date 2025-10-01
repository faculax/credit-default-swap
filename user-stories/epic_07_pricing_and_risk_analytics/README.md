# Epic 07 – Pricing & Risk Analytics

## 📌 Overview
Implements the quantitative core: valuation parity (ISDA Standard Model alignment), risk sensitivities, scenario engines, and regression harnesses ensuring stable, explainable measures across lifecycles.

## 🎯 Business Value
- Trusted PV & Greeks underpin trading decisions and limit checks.
- Deterministic risk outputs enable PnL explain and capital optimization.
- Regression harness prevents silent numerical drift.

## 🧠 Scope
In Scope:
- ISDA CDS Standard Model integration (or validated equivalent).
- Core measures: PV (clean/dirty), Par Spread, Protection/Premium legs, CS01, DV01, JTD, Recovery01.
- Curve bucket & scenario shock engine.
- Benchmark dataset & regression tests.
Out of Scope (Here):
- SIMM IM (Epic 08).
- SA-CCR & capital metrics (Epic 08).
- UI visualization components.

## 🚫 Out of Scope Detail
- No intraday streaming recalcs yet (batch/event trigger only initially).

## 🔐 Domain Terms
| Term | Definition |
|------|------------|
| CS01 | Change in PV for 1 bp parallel spread shift |
| JTD | Jump-To-Default loss metric |
| Recovery01 | PV change for 1% recovery shift |
| Dirty PV | PV including accrued |

## 🔄 Core Processing Flow
Market Data → Curve Build → Trade PV & Legs → Measures → Persist Snapshot → Regression Compare

## 📚 Stories
- Story 7.1 – ISDA Standard Model Integration & Parity Tests
- Story 7.2 – Core Risk Measures Engine
- Story 7.3 – Curve Bucket & Scenario Shock Module
- Story 7.4 – Benchmark Regression Harness
- Story 7.5 – ORE Process Supervisor & Adapter (Phase B)
- Story 7.6 – Batched Scenarios & Bucket CS01 (Phase C)

## ✅ Acceptance Criteria Mapping (Initial)
| Story | Theme | Key Acceptance (Draft) |
|-------|-------|------------------------|
| 7.1 | Parity | Spread ↔ upfront alignment ± tolerance |
| 7.2 | Measures | PV, CS01, DV01, JTD, Recovery01 outputs |
| 7.3 | Scenarios | Parallel/non-parallel shocks + defaults |
| 7.4 | Regression | Locked dataset; failure gates build |

## 🧪 Quality Approach
- Reference vectors vs ISDA model library outputs.
- Numerical tolerance envelope stored alongside results.
- Continuous integration hook for regressions.

## 🧱 Architectural Decision (Pricing Engine Deployment)
We will implement a **Long-Lived Sidecar Risk Service** rather than ad-hoc CLI invocations:

**Rationale**
- Avoid per-request cold start cost of native engine initialization.
- Centralize curve & market snapshot caching for lower latency scenario runs.
- Isolate quantitative failure modes (segfault/native crash) away from core trading/lifecycle service JVMs.

**Service Shape**
- New Spring Boot microservice module: `risk-engine` (Java 21) for operational consistency with existing services.
- Internally invokes ORE (or future ISDA library bridge) via one of:
	1. Embedded native process supervisor (start once, monitor health).
	2. Direct JNI binding (deferred optimization phase).
- Exposes REST API (versioned):
	- `GET /api/risk/cds/{tradeId}` → base measures (pvClean, pvDirty, parSpread, legs, accrued, cs01?, dv01?, jtd?, recovery01?).
	- `POST /api/risk/cds/{tradeId}/scenarios` → base + scenario vector results.
	- `GET /api/risk/health` → engine + market snapshot metadata.
	- `POST /api/risk/rebuild-curves` → trigger rebuild (admin only).

**Caching / State**
- In‑memory caches: curve objects keyed by (curveSetId, valuationDate), survival probability term structures keyed by (entityId, curveVersion).
- Optional Postgres tables:
	- `curve_snapshot` (persist pillars + hash for replay / Epic 10 alignment).
	- `risk_measure_run` (store JSON, run_hash, parity_status, scenario_name).

**Concurrency Model**
- Request queue → worker pool (CPU-bound sizing = cores - 1).
- Batching window (configurable, e.g., 25–50 ms) to accumulate scenario requests for vectorized evaluation.

**Error Isolation**
- Native crash → supervisor auto-restarts engine; `risk-engine` REST returns 503 during warmup with `Retry-After` header.
- Timeouts (config: e.g., 3s single trade) → abort run; partial scenario set returns with warnings.

**Observability**
- Structured logs (JSON) with fields: `tradeId`, `scenarioName`, `runLatencyMs`, `engineVersion`, `parityDelta`.
- Micrometer metrics: `risk_runs_total`, `risk_run_latency`, `curve_cache_hits`, `engine_restarts`.

**Docker Image Strategy**
- Multi-stage build:
	1. `builder` stage compiles ORE/QuantLib + produces native engine binary.
	2. `app` stage (base: eclipse-temurin:21-jre) copies binary + Spring Boot fat jar.
- Image label metadata: `org.risk.engine.version`, `org.risk.ore.commit`.
- Added to `docker-compose.yml` as `risk-engine` service; depends_on Postgres if persistence enabled.

**Security / Hardening**
- Read-only mount for engine binary.
- No dynamic shell execution from request payloads.
- Input validation at boundary (tradeId existence + tradeStatus not TERMINATED).

**Phased Enablement**
1. Phase A: Stub Java calculators (flat hazard) behind same REST contract.
2. Phase B: Swap internal implementation to ORE sidecar process (feature flag `RISK_IMPL=ORE`).
3. Phase C: Introduce scenario batching + bucket CS01.
4. Phase D: JNI optimization (only if latency SLA fails).

**Success Metrics**
- P50 single trade base valuation < 150 ms (stub) / < 400 ms (initial ORE) / < 200 ms (batched ORE).
- Parity tolerance breaches < 2% of daily runs.
- Engine restarts < 1 per 24h under steady load.

**Open Questions**
- Do we persist raw curve construction inputs (market quotes) or only derived pillars? (Leaning: persist both for replay integrity.)
- How to align valuation date vs trade effective date for T+1 late booking scenarios (defer to Epic 10 replay semantics).

This section informs Stories 7.1–7.3 implementation planning and should be kept synchronized with future architecture docs.

## 🎨 UI / UX Acceptance Criteria (Provisional)
Initial UI exposure intentionally limited (backend focus), but minimal surfacing expected for validation:
- Risk Measures panel in Trade Detail: shows PV (clean/dirty), Par Spread, CS01, DV01, JTD, Recovery01.
- Scenario run modal: select predefined shock set (parallel + custom bp shift) and display results table.
- Regression status indicator: badge (PASS/FAIL) with tooltip linking last run timestamp.
- Loading skeleton while measures recompute.
- Error banner if pricing engine unavailable.
- Accessibility: table with column headers and aria-live region for updated measures.
- Manual QA Flow:
	1. Open trade detail → Risk tab.
	2. Trigger scenario run (e.g., +10bp) → results table populates.
	3. Validate CS01 approximates PV difference between base and +1bp scenario (rough check).
	4. Simulate engine failure (toggle flag) → error banner appears.

## ⚠️ Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Drift vs market standard | Daily benchmark comparison & alerting |
| Performance under batch | Pre-compute hazard curves & reuse legs |
| Curve bootstrapping instability | Robust day count & stub handling tests |

## 🔮 Backlog Seeds
- Multi-threaded portfolio Greeks.
- Incremental risk revaluation cache.
- Recovery surface modeling.
