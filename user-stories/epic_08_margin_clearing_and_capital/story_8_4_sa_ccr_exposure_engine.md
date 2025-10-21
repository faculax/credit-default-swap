# Story 8.4 – SA-CCR Exposure Engine

**As a capital management analyst**,  
I want to calculate Exposure at Default (EAD) using SA-CCR methodology  
So that I can determine regulatory capital requirements and internal risk limits accurately.

## ✅ Acceptance Criteria
- SA-CCR calculation engine implementing Basel III formula: EAD = α × (RC + PFE).
- Replacement Cost (RC) computation from current mark-to-market values with netting set aggregation.
- Potential Future Exposure (PFE) calculation using supervisory add-ons by asset class.
- Netting set identification and trade aggregation with proper collateral offsetting.
- Asset class classification (Credit, FX, Interest Rate, Equity, Commodity) with supervisory parameters.
- Alpha factor application (1.4 default) with jurisdiction-specific override capability.
- Margin impact calculations for both VM and IM on RC and PFE components.
- Calculation audit trail with intermediate values and parameter application details.

## 🧪 SA-CCR Validation Rules
| Component | Rule |
|-----------|------|
| Netting Sets | Trades properly aggregated by legal netting agreement |
| Asset Classification | Each trade assigned to correct SA-CCR asset class |
| Supervisory Factors | Current regulatory parameters for jurisdiction |
| Margin Offsetting | VM and IM properly netted against gross exposure |
| Alpha Factor | Within regulatory range (typically 1.0 - 1.4) |

## 🧠 UX Notes
- SA-CCR exposure view shows EAD breakdown with RC and PFE components.
- Netting set drill-down displays constituent trades and aggregation logic.
- Parameter management interface for supervisory factors and jurisdiction settings.

## 🛠 Implementation Guidance
- Implement as configurable engine supporting multiple regulatory jurisdictions.
- Use externalized supervisory parameter tables with effective date versioning.
- Optimize for large portfolios with efficient netting set processing.
- Validate against regulatory test cases and industry benchmarks.

## 📦 Deliverables
- SA-CCR calculation engine with jurisdiction support.
- Netting set aggregation logic with margin offsetting.
- Supervisory parameter management with versioning.
- Exposure reporting with audit trail and breakdown details.

## ⏭ Dependencies / Links
- Requires netting agreement reference data for trade aggregation.
- Integrates with Story 8.5 (dashboard) for exposure monitoring and reporting.

## Traceability
Epic: epic_08_margin_clearing_and_capital
Story ID: 8.4