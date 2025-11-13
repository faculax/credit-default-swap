## Frontend Test Report

Generated: (placeholder timestamp)

### Test Suites Added

1. Unit Component Tests: SimulationConfigForm, SimulationStatusBadge, SimulationResults, SimulationPanel, CreditEventModal, BasketList, TradeDetailModal, CDSTradeForm.
2. Contract Tests (5): simulationService, riskService, lifecycleService, creditEventService, basketService.
3. Integration Tests (5): Simulation flow, Trade Detail risk tab, CDSTradeForm submission, Credit Event recording, RiskMeasuresPanel coupon flow.

### Classification

- Unit tests focus on isolated components rendering & basic interactions.
- Contract tests validate request construction and response parsing boundaries of service layer.
- Integration tests exercise multi-component flows and side-effect wiring with mocked services.

### Next Steps

- Run full Jest suite with coverage to populate actual metrics.
- Address uncovered branches (error paths, alternative statuses, cancellation flows).
- Incrementally raise coverage threshold once stable.
