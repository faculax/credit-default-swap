# Story 8.3 – SIMM Sensitivities & IM Calculator

**As a risk manager**,  
I want to ingest SIMM sensitivity data and calculate initial margin using ISDA SIMM methodology  
So that I can determine collateral requirements for uncleared OTC derivatives accurately.

## ✅ Acceptance Criteria
- CRIF (Common Risk Interchange Format) file ingestion with validation and error handling.
- SIMM sensitivity data stored with proper bucket classification and risk factor mapping.
- Initial margin calculation engine implementing ISDA SIMM 2.6+ methodology.
- Cross-bucket correlation matrix application for portfolio-level IM aggregation.
- Currency conversion support for multi-currency portfolios using current FX rates.
- IM calculation results validated within tolerance against external reference calculations.
- Parameter versioning system for ISDA model updates and regulatory changes.
- Calculation audit trail capturing inputs, parameters, and step-by-step breakdown.

## 🧪 SIMM Validation Rules
| Component | Rule |
|-----------|------|
| CRIF Format | Must conform to ISDA CRIF specification version 2.6+ |
| Risk Factors | Must map to valid SIMM risk factor taxonomy |
| Bucket Classification | Sensitivities assigned to correct SIMM buckets by asset class |
| Currency Conversion | FX rates within 1 business day of calculation date |
| Parameter Version | SIMM parameters effective for calculation date |

## 🧠 UX Notes
- SIMM breakdown displays total IM with expandable bucket-level contributions.
- Parameter version selector shows effective dates and impact of changes.
- Calculation progress indicator for large portfolios with estimated completion time.

## 🛠 Implementation Guidance
- Implement SIMM calculation as modular service supporting version evolution.
- Use externalized parameter configuration for correlation matrices and risk weights.
- Consider caching intermediate results for incremental portfolio updates.
- Validate against ISDA test portfolios during implementation.

## 📦 Deliverables
- CRIF file parser with validation and error reporting.
- SIMM calculation engine with configurable parameters.
- Parameter versioning system with effective date management.
- Calculation results storage with audit trail capabilities.

## ⏭ Dependencies / Links
- May require FX rate service integration for currency conversion.
- Feeds into Story 8.5 (dashboard) for IM reporting and analysis.

## Traceability
Epic: epic_08_margin_clearing_and_capital
Story ID: 8.3