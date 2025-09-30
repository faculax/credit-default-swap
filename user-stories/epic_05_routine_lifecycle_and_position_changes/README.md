# Epic 05 – Routine Lifecycle & Position Changes

## 📌 Overview
Operational and economic lifecycle events that adjust cashflows, versioning, counterparty relationships, and position size after initial trade capture. Focus: predictable, recurring or corrective changes (coupon accrual, payments, amendments, notional adjustments, novations, compression) with full auditability.

## 🎯 Business Value
- Accurate accrual & payment processing reduces PnL noise.
- Controlled amendments & novations prevent data divergence across venues.
- Compression and partial terminations lower gross notional and capital usage.

## 🧠 Scope
In Scope:
- Coupon accrual generation and net payment events.
- Day‑count/calendar adjustments via amendment.
- Partial and full terminations (notional schedule changes).
- Novation workflow & version lineage.
- Compression proposal ingestion & execution.
Out of Scope (Here):
- Index lifecycle specifics (Epic 06).
- Credit-event driven lifecycle (Epic 04).
- Pricing model internals (Epic 07).

## 🚫 Out of Scope Detail
- No margin/IM logic (Epic 08)
- No regulatory emission specifics (Epic 10)

## 🔐 Domain Terms
| Term | Definition |
|------|------------|
| IMM Date | Standard quarterly coupon/payment date (Mar/Jun/Sep/Dec 20) |
| Compression | Multilateral unwind to reduce gross notional |
| Novation | Transfer of contractual obligations to a new party |
| Partial Termination | Notional reduction while trade persists |

## 🔄 Lifecycle States (Added / Impacted)
PENDING_CONFIRM → CONFIRMED → ACTIVE → (PARTIALLY_TERMINATED | NOVATED | TERMINATED | COMPRESSED)

## 📚 Stories
- Story 5.1 – Schedule & Generate IMM Coupon Events
- Story 5.2 – Accrual & Net Cash Posting Engine
- Story 5.3 – Economic Amend Workflow
- Story 5.4 – Notional Adjustment & Termination Logic
- Story 5.5 – Novation & Party Role Transition
- Story 5.6 – Compression Proposal Ingestion & Execution

## ✅ Acceptance Criteria Mapping (Initial)
| Story | Theme | Key Acceptance (Draft) |
|-------|-------|------------------------|
| 5.1 | Coupons | IMM schedule generation; business day roll |
| 5.2 | Accrual | ACT/360 accrual; netting; posting |
| 5.3 | Amend | Version +1, audit link, PnL explain delta |
| 5.4 | Notional | Adjust schedules; PV-aligned unwind cash |
| 5.5 | Novation | Terminate old; create new; UTI hygiene |
| 5.6 | Compression | Apply proposal; CS01 delta within tolerance |

## 🧪 Quality Approach
- Unit tests for schedule generator edge dates.
- Golden accrual vs reference spreadsheet.
- Novation lineage replay test.
- Compression CS01 tolerance check.

## ⚠️ Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Calendar drift | Externalized calendar service (Epic 09 dependency) |
| Double accrual on amendments | Idempotent accrual events with version guard |
| Compression cash mismatch | Pre/post risk & cash reconciliation step |

## 🔮 Backlog Seeds
- Multi-currency netting optimization.
- Bulk novation tooling.
- Automated compression run scheduler.
