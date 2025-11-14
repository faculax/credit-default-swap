# Epic 16: End-of-Day Valuation & P&L Reporting

## Epic Overview
Implement an end-of-day (EOD) valuation process that calculates mark-to-market valuations, accrued interest, and generates P&L and risk reports for CDS positions. The system will leverage ORE for NPV calculations and focus on delivering actionable P&L and risk metrics rather than building a full accounting system.

## Business Value
- **Regulatory Compliance**: Meet daily valuation and reporting requirements
- **Risk Management**: Provide accurate daily risk exposures and sensitivities
- **P&L Transparency**: Track daily profit and loss across portfolios
- **Decision Support**: Enable informed trading and risk management decisions
- **Operational Efficiency**: Automate manual EOD valuation processes

## Epic Goals
1. Automate daily valuation runs for all CDS positions
2. Calculate accurate NPV and accrued interest for each trade
3. Generate daily P&L reports (realized and unrealized)
4. Produce risk reports with aggregated exposures
5. Store valuation history for trend analysis and audit

## Key Stakeholders
- **Risk Managers**: Daily risk exposure monitoring
- **Traders**: P&L tracking and performance analysis
- **Finance Team**: Valuation data for accounting system
- **Regulators**: Compliance reporting
- **Operations**: EOD processing and reconciliation

## Technical Architecture

### Components
```
EOD Valuation System
├── Valuation Scheduler (nightly batch job)
├── Market Data Snapshot Service
├── ORE Integration (NPV calculation)
├── Accrued Interest Calculator
├── Valuation Storage (time series)
├── P&L Engine (delta calculations)
└── Risk Reporting Service
```

### Data Flow
```
Market Data (EOD) → ORE Pricing → NPV per Trade
                                    ↓
Trade Data ─────────────→ Accrued Interest Calculator
                                    ↓
                          Valuation Results Storage
                                    ↓
Previous Valuation ────→ P&L Calculator ← Current Valuation
                                    ↓
                          Risk Aggregator
                                    ↓
                          Reports & Analytics
```

## Success Criteria
- [ ] EOD valuations run automatically every business day
- [ ] All active trades valued with NPV and accrued interest
- [ ] P&L calculated within 15 minutes of market close
- [ ] Risk reports available by start of next business day
- [ ] Valuation audit trail maintained for regulatory review
- [ ] Integration with accounting system for GL posting

## Out of Scope
- Building a full accounting/GL system (use existing accounting engine)
- Real-time intraday valuations (EOD only)
- Complex trade lifecycle events during valuation
- Historical revaluations (except for corrections)

## Dependencies
- ORE risk engine (Epic 7 - already implemented)
- Market data feeds
- Portfolio management (Epic 12 - already implemented)
- Trade repository

## Stories in Epic
1. **Story 16.1**: Market Data Snapshot Service
2. **Story 16.2**: EOD Valuation Batch Job Framework
3. **Story 16.3**: NPV Calculation via ORE Integration
4. **Story 16.4**: Accrued Interest Calculator
5. **Story 16.5**: Valuation Results Storage & History
6. **Story 16.6**: Daily P&L Calculation Engine
7. **Story 16.7**: Risk Reporting & Aggregation
8. **Story 16.8**: Valuation Reconciliation & Exceptions

## Timeline Estimate
- **Story 16.1-16.2**: 1-2 weeks (infrastructure)
- **Story 16.3-16.4**: 2-3 weeks (core valuation logic)
- **Story 16.5**: 1 week (storage layer)
- **Story 16.6-16.7**: 2-3 weeks (P&L and reporting)
- **Story 16.8**: 1-2 weeks (reconciliation)
- **Total**: 8-12 weeks

## Risk & Mitigation
| Risk | Impact | Mitigation |
|------|--------|-----------|
| Market data quality issues | High | Implement validation rules and fallback logic |
| ORE performance bottleneck | Medium | Batch processing, parallel execution |
| Data volume growth | Medium | Implement data archival and partitioning |
| Model risk (pricing discrepancies) | High | Independent price verification, tolerance checks |
| Business day calendar issues | Low | Robust holiday calendar management |

## Non-Functional Requirements
- **Performance**: Complete valuation run within 2 hours for 10K trades
- **Reliability**: 99.9% successful execution rate
- **Auditability**: Full audit trail of all valuations and adjustments
- **Scalability**: Support 50K+ trades without redesign
- **Monitoring**: Alerts for failed valuations, data issues, P&L outliers

## Related Epics
- Epic 7: Pricing and Risk Analytics (ORE integration)
- Epic 12: CDS Portfolio Aggregation
- Epic 8: Margin, Clearing, and Capital (SIMM/SA-CCR)
- Epic 10: Reporting, Audit, and Replay

## Glossary
- **NPV**: Net Present Value - current fair value of a trade
- **MTM**: Mark-to-Market - daily revaluation at market prices
- **P&L**: Profit and Loss - change in value between periods
- **EOD**: End of Day - close of business valuation
- **Accrued Interest**: Accumulated premium since last payment date
- **ORE**: Open Source Risk Engine - pricing and risk calculation engine
