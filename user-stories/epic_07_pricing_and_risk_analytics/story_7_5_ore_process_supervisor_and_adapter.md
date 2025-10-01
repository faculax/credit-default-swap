# Story 7.5 – ORE Process Supervisor & Adapter (Phase B)

## Narrative
Introduce the ORE native engine into the risk-engine service via a supervised long-lived process with an adapter translating trades & market data to ORE input/output structures.

## Acceptance Criteria
- Placeholder – detailed acceptance to be refined during implementation.

## Implementation Notes
- Add multi-stage Docker build compiling ORE/QuantLib binary.
- Implement `OreProcessManager` (start, health-check, restart on failure, warmup state).
- Implement `OreInputBuilder` and `OreOutputParser` abstractions (XML/JSON intermediate representation choice TBD).
- Feature flag: `RISK_IMPL=ORE` switches from stub to real adapter; default remains stub until validated.
- Graceful degradation: if ORE unavailable return 503 with retry-after header.

## Test Scenarios (Provisional)
- Process start success path returns base measures.
- Simulated crash triggers supervisor restart and 503 during warmup.
- Timeout > configured limit returns error envelope.

## UI / UX Acceptance (Provisional)
- Risk tab unchanged visually; underlying values sourced from ORE when flag enabled.
- Error banner appears if ORE not ready (reuse existing error surface pattern).

## Traceability
Epic: epic_07_pricing_and_risk_analytics
Story ID: 7.5
