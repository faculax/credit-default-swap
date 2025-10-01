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
