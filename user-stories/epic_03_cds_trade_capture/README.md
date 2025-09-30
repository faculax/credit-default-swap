# Epic 3: Single-Name CDS Trade Capture

## Overview
Enable users to fully capture and book a single‑name Credit Default Swap (CDS) trade with all core Calypso‑style fields, validating inputs and persisting the trade with booking confirmation.

## Scope (Phase 1)
- Capture + submit single-name CDS trade
- All standard fields surfaced & marked required where applicable
- Client + server validation (dates, enums, numeric ranges)
- Reference data driven dropdowns (static lists initial)
- Persist trade + return identifier + timestamps
- Booking confirmation modal
- Basic auth/authorization guard (placeholder mechanism)

## Out of Scope (Later)
- Pricing / PV / accrual calculations
- Credit events lifecycle (defaults, restructuring workflow)
- Amend / cancel flows
- Bulk import / trade enrichment feeds
- Audit search UI

## Stories
- [Story 3.1 – CDS Trade Capture UI & Reference Data](./story_3_1_cds_trade_capture_ui.md)
- [Story 3.2 – Validation & Business Rules](./story_3_2_cds_validation_rules.md)
- [Story 3.3 – Persist & Book Trade](./story_3_3_cds_trade_persist.md)
- [Story 3.4 – Booking Confirmation UX & Error Handling](./story_3_4_cds_booking_confirmation.md)
- [Story 3.5 – Authorization & Security Guardrails](./story_3_5_cds_auth_security.md)

## Field Inventory
| Field | Required | Notes |
|-------|----------|-------|
| Reference Entity | Yes | Dropdown (ticker + name) |
| Notional Amount | Yes | Positive decimal (2 dp stored) |
| Spread (bps) | Yes | Positive decimal (4 dp) |
| Maturity Date | Yes | > Effective Date |
| Effective Date | Yes | >= Trade Date |
| Counterparty | Yes | Dropdown |
| Trade Date | Yes | Not future |
| Currency | Yes | Dropdown (ISO 4217) |
| Premium Frequency | Yes | ENUM: QUARTERLY/SEMI_ANNUAL/ANNUAL |
| Day Count Convention | Yes | ENUM: ACT_360 / ACT_365 / 30_360 / ACT_ACT |
| Buy/Sell Protection | Yes | ENUM: BUY / SELL |
| Restructuring Clause | Conditional | Optional (business rule only if market requires) |
| Payment Calendar | Yes | ENUM: NYC / LON / TARGET / TOK / SYD |
| Accrual Start Date | Yes | Typically = Effective or earlier backdated scenario |
| Trade Status | System | Default PENDING; future: CONFIRMED/SETTLED |

## Acceptance Criteria Mapping
| Acceptance Theme | Story |
|------------------|-------|
| All fields displayed | 3.1 |
| Mandatory enforcement | 3.1 / 3.2 |
| Reference data dropdowns | 3.1 |
| Validation & messaging | 3.2 / 3.4 |
| Persist + confirmation | 3.3 / 3.4 |
| Auth/authorization | 3.5 |
| Storage + timestamps | 3.3 |
| Confirmation handling UX | 3.4 |
| Server validation parity | 3.2 / 3.3 |

## Risks / Notes
- Need consistent enum alignment between frontend + backend.
- Potential timezone handling pitfalls (store dates as ISO local vs UTC?).
- Auth placeholder must not block local dev (allow override header in dev mode).

## Future Enhancements (Backlog Seeds)
- Trade amendment & versioning
- Exposure aggregation by reference entity
- Real-time pricing integration
- Credit event workflow integration

