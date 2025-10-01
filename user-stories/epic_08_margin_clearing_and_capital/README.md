# Epic 08 – Margin, Clearing & Capital

## 📌 Overview
Integrates clearing lifecycle (novation & CCP linkage), daily collateral processes (VM/IM), SIMM initial margin for uncleared trades, and SA-CCR exposure metrics for capital & limits.

## 🎯 Business Value
- Regulatory compliance (UMR, SA-CCR) and reduced operational risk.
- Transparent margin & exposure data for funding decisions.
- Efficient dispute and reconciliation workflows.

## 🧠 Scope
In Scope:
- CCP novation linkage & account enrichment.
- Daily VM/IM ingestion + reconciliation.
- SIMM sensitivities ingestion & IM calculation.
- SA-CCR EAD computation pipeline.
Out of Scope (Here):
- Core pricing model (Epic 07).
- Settlement netting file generation (Epic 05/10).

## 🚫 Out of Scope Detail
- No advanced dispute portal UI (placeholder APIs only).

## 🔐 Domain Terms
| Term | Definition |
|------|------------|
| VM | Variation Margin – daily mark-to-market cash flows |
| IM | Initial Margin – risk-based collateral requirement |
| SIMM | Standard Initial Margin Model (ISDA) |
| SA-CCR | Standardized Approach for Counterparty Credit Risk |

## 🔄 Process Threads
Clearing Novation → Daily CCP Statements → Margin & Exposure Store → Capital / Limits Outputs

## 📚 Stories
- Story 8.1 – CCP Novation & Account Enrichment
- Story 8.2 – Daily VM/IM Statement Ingestion
- Story 8.3 – SIMM Sensitivities & IM Calculator
- Story 8.4 – SA-CCR Exposure Engine
- Story 8.5 – Margin & Exposure Reconciliation Dashboard (Optional)

## ✅ Acceptance Criteria Mapping (Initial)
| Story | Theme | Key Acceptance (Draft) |
|-------|-------|------------------------|
| 8.1 | Novation | Terminate bilateral; CCP legs active |
| 8.2 | VM/IM | Statements parsed; ledger updated; tolerance checks |
| 8.3 | SIMM | CRIF stored; IM within tolerance vs reference |
| 8.4 | SA-CCR | EAD computed = α×(RC+PFE) with audit |
| 8.5 | Dashboard | Aggregated exposures & margin deltas |

## 🧪 Quality Approach
- Golden CCP statement fixtures.
- SIMM calculation cross-check vs policy engine.
- SA-CCR component unit tests (RC, add-ons, multipliers).

## ⚠️ Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| CCP format drift | Schema versioning & validation layer |
| SIMM parameter updates | Externalized calibration version table |
| SA-CCR misclassification | Mandatory netting set metadata validation |

## 🔮 Backlog Seeds
- Margin optimization (what-if netting set changes).
- Intraday incremental IM deltas.
- Dispute workflow integration.
