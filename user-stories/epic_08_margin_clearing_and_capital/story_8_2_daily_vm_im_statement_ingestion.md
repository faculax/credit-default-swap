# Story 8.2 – Daily VM/IM Statement Generation & Reconciliation

**As a risk operations user**,  
I want to automatically generate daily variation margin (VM) and initial margin (IM) statements using existing CCP relationships and netting sets  
So that I can maintain accurate collateral positions without manual file uploads and reduce operational overhead.

## ✅ Acceptance Criteria
- Automatically generate VM/IM statements for all active CCP netting sets using existing trade and exposure data.
- VM calculations based on daily mark-to-market changes aggregated by netting set.
- IM calculations derived from SIMM results and SA-CCR exposures per netting set.
- Support manual statement upload as fallback for CCP statement reconciliation.
- VM and IM positions updated in collateral ledger with effective dates and audit trail.
- Tolerance checking performed against CCP statements (when available) with configurable thresholds.
- Discrepancy alerts generated for material differences with detailed variance breakdown.
- Statement generation status tracked (PENDING, GENERATED, RECONCILED, DISPUTED).
- Historical statement archive maintained for regulatory audit and replay scenarios.

## 🧪 Statement Validation Rules
| Field | Rule |
|-------|------|
| Statement Date | Must be valid business date, not future dated |
| CCP Source | Must be registered CCP with active account relationship |
| VM Amount | Numeric, can be negative (owed to/from CCP) |
| IM Amount | Numeric, must be non-negative |
| Currency | Must be valid ISO currency code |

## 🔄 VM/IM Generation Logic
| Component | Source Data | Logic |
|-----------|-------------|-------|
| VM Calculation | Daily trade MTM + Netting Set grouping | Aggregate P&L changes by netting set, apply netting |
| IM Calculation | SIMM results + SA-CCR exposures | Use max(SIMM_IM, SA-CCR_multiplier * EAD) per netting set |
| Netting Set Mapping | Existing CDS trades with `netting_set_id` | Group by CCP + Member + Currency combination |
| CCP Integration | Active CCP accounts from `ccp_accounts` table | Auto-discover active relationships |

## 🧠 UX Notes
- Statement generation dashboard shows auto-generated positions with real-time status.
- Optional drag-and-drop file upload for CCP statement reconciliation.
- Discrepancy drill-down shows field-level variances between generated vs. CCP statements.
- Netting set detail view shows underlying trades contributing to VM/IM.

## 🛠 Implementation Guidance
- Use existing netting set data from cleared trades to auto-generate statements.
- Implement daily batch job to calculate VM/IM for all active netting sets.
- Store generated statements alongside any manually uploaded CCP statements for comparison.
- Leverage existing SA-CCR and SIMM calculation results for IM components.

## 📦 Deliverables
- Automated VM/IM statement generation service using existing netting sets.
- Daily batch job to calculate margins for all active CCP relationships.
- Enhanced collateral ledger with auto-generated and reconciled positions.
- Optional statement upload capability for CCP reconciliation.
- Statement generation UI with netting set drill-down and variance analysis.

## ⏭ Dependencies / Links
- Requires collateral ledger schema (may extend existing trade storage).
- Feeds into Story 8.5 (reconciliation dashboard) for variance reporting.

## Traceability
Epic: epic_08_margin_clearing_and_capital
Story ID: 8.2