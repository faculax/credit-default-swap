# Epic 10 – Reporting, Audit & Deterministic Replay

## 📌 Overview
Implements outward-facing regulatory and repository reporting hooks, immutable event/audit storage, correlation IDs for traceability, and deterministic replay to reproduce historical PV and lifecycle state.

## 🎯 Business Value
- Regulatory compliance and reduced operational risk in reporting.
- Faster incident/root cause analysis via replay & correlation tracing.
- Auditability supports internal/external reviews and model validation.

## 🧠 Scope
In Scope:
- Emission of trade & lifecycle events to reporting gateways (SDR/TR/TIW hooks).
- Immutable event log (append-only) + market snapshot linkage.
- Correlation ID propagation through service layers.
- Replay engine to reconstruct state & valuations.
Out of Scope (Here):
- Real-time streaming dashboards (future enhancement).
- Advanced anomaly detection.

## 🚫 Out of Scope Detail
- No external regulator-specific XML schema generation in v1 (JSON canonical first).

## 🔐 Domain Terms
| Term | Definition |
|------|------------|
| Correlation ID | Unique request/execution identifier for tracing |
| Event Sourcing | Persisting ordered immutable events to derive state |
| Replay | Recomputing state from event history + reference snapshots |

## 🔄 Core Flow
Lifecycle Event → Persist (Event + Snapshot Refs) → Emit (Reporting) → Audit Entry → Replay On Demand

## 📚 Stories
- Story 10.1 – Regulatory Event Emission & ACK/NACK Tracking
- Story 10.2 – Immutable Event Store & Snapshot Capture
- Story 10.3 – Deterministic Replay Service
- Story 10.4 – Unified Audit & Correlation IDs
- Story 10.5 – Reporting Reconciliation & Drift Alerts (Optional)

## ✅ Acceptance Criteria Mapping (Initial)
| Story | Theme | Key Acceptance (Draft) |
|-------|-------|------------------------|
| 10.1 | Reporting | Emit + ACK/NACK persistence + retry |
| 10.2 | Event Store | Append-only; hash integrity; as-of query |
| 10.3 | Replay | PV/state match historical snapshot exactly |
| 10.4 | Audit | Correlation IDs; structured error format |
| 10.5 | Reconciliation | Drift alerts on unACKed or mismatched events |

## 🧪 Quality Approach
- Hash chain verification tests.
- Replay vs historical PV regression.
- Simulated NACK retry scenario.

## ⚠️ Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Hash mismatch | Integrity check & halt further appends |
| Reporting latency | Async queue + backpressure metrics |
| Replay divergence | Snapshot + reference data version pinning |

## 🔮 Backlog Seeds
- Streaming Kafka sink for analytics.
- Automated anomaly detection on replay diffs.
- Multi-region event store replication.
