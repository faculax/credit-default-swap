# Epic 4: CDS Credit Event Processing

## Overview
Enable recording and processing of credit events (bankruptcy, failure to pay, restructuring, etc.) for single‑name CDS trades, triggering settlement calculation (cash or physical) and producing auditable settlement instructions while updating the trade lifecycle state.

## Business Value
Accurate, timely handling of credit events is essential to fulfill contractual obligations, reduce operational / legal risk, and maintain regulatory and counterparty trust.

## Scope (Phase 1)
- Manual capture of credit event for eligible active CDS trade
- Validation of eligibility (trade status + event type)
- Trade status transition (e.g. ACTIVE → TRIGGERED → SETTLED)
- Cash settlement amount calculation using recovery rate
- Physical settlement instruction generation scaffold
- Persistence of lifecycle event + audit trail
- Settlement instruction record creation & exposure
- Robust error handling / no partial state on failure

## Out of Scope (Later Phases)
- Automated event feed ingestion (e.g. ISDA / DTCC)
- Auction price sourcing integration (external market data adapter)
- Multi-tranche aggregate processing
- Partial unwinds / amendments post trigger
- Complex restructuring option workflows
- Automated document (Notice of Physical Settlement) generation

## Key Domain Terms
| Term | Definition |
|------|------------|
| Credit Event | Contractually defined trigger (bankruptcy, failure to pay, restructuring, etc.) activating settlement obligations. |
| Recovery Rate | Percentage value representing market expectation of post-default asset value. |
| Cash Settlement | Payout = Notional × (1 − Recovery Rate). |
| Physical Settlement | Delivery of eligible obligations vs par value settlement. |
| Lifecycle Event | Immutable record appended to a trade’s lifecycle history capturing a state transition or notable action. |

## Proposed Lifecycle States
ACTIVE → TRIGGERED → (SETTLED_CASH | SETTLED_PHYSICAL)  
Errors do not transition state; retries allowed.

## Stories
- [Story 4.1 – Record Credit Event Action & Form](./story_4_1_record_credit_event.md)
- [Story 4.2 – Validate Eligibility & Persist Lifecycle Event](./story_4_2_validate_and_persist_event.md)
- [Story 4.3 – Cash Settlement Calculation](./story_4_3_cash_settlement_calculation.md)
- [Story 4.4 – Physical Settlement Instruction Generation (Scaffold)](./story_4_4_physical_settlement_scaffold.md)
- [Story 4.5 – Settlement Instructions Persistence & Exposure](./story_4_5_settlement_instructions_persistence.md)
- [Story 4.6 – Audit & Error Integrity Handling](./story_4_6_audit_and_error_handling.md)

## Acceptance Criteria Mapping
| Acceptance Criterion / Theme | 4.1 | 4.2 | 4.3 | 4.4 | 4.5 | 4.6 |
|------------------------------|:---:|:---:|:---:|:---:|:---:|:---:|
| UI action to record event | ✔ |  |  |  |  |  |
| Required event fields capture | ✔ |  |  |  |  |  |
| Eligibility (trade active, event valid) |  | ✔ |  |  |  |  |
| Lifecycle history persisted |  | ✔ |  |  | ✔ (instructions link) | ✔ |
| Status transition to TRIGGERED |  | ✔ |  |  |  |  |
| Cash settlement amount formula |  |  | ✔ |  |  |  |
| Physical instruction generation |  |  |  | ✔ |  |  |
| Settlement instruction records view/export |  |  |  |  | ✔ |  |
| Audit all actions (who/when) |  | ✔ | ✔ | ✔ | ✔ | ✔ |
| Error handling no partial state |  | ✔ | ✔ | ✔ | ✔ | ✔ |
| Distinct records for instructions |  |  |  |  | ✔ |  |

## Risks / Considerations
- Recovery rate sourcing integrity (placeholder value vs live auction result).
- Timezone & event date vs notice date ordering.
- Ensuring idempotency if user retries a partially failed trigger attempt.
- Physical settlement complexity deferred (eligible obligations filtering).

## Backlog Seeds
- Automated feed ingestion for credit events.
- Auction integration for dynamic recovery rate.
- Multi-entity net settlement processor.
- Rich lifecycle UI timeline.
- Re-notification / reminder workflow.

