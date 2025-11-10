# Story 06: Data Lineage

**As a** data engineer  
**I want** basic data lineage tracking established  
**So that** we can trace critical data flows for audit and debugging.

## Acceptance Criteria
- [ ] Chosen approach (e.g. OpenLineage events or lightweight custom metadata)
- [ ] Emit lineage events for at least one ingestion → processing → persistence path
- [ ] Lineage artifacts stored (file, DB, or external service)
- [ ] Developer doc: how to add a new lineage emitter
- [ ] Security review of stored metadata (no PII leakage)

## Scope (Initial)
- Track trade ingestion pipeline
- Track transformation into risk calculation inputs

## Tasks
- [ ] Create `lineage/` folder with spec
- [ ] Implement event model (JSON schema)
- [ ] Add emitter to one pipeline component
- [ ] Provide viewer (simple CLI or markdown report)
- [ ] Document extension points

## Implementation Notes
- Start minimal: event timestamp, source component, target entity, version
- Consider future graph visualization integration

## Test Scenarios
- Generate lineage for sample run → verify stored
- Add second hop → confirm multi-step chain visible
- Invalid event schema → rejected with log

## UI / UX Acceptance (Provisional)
- Potential future UI (table + graph), not in this initial scope

## Traceability
Epic: release-chain-hardening  
Story ID: 6