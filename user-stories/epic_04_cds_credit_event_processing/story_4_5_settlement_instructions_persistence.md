# Story 4.5 – Persist & Expose Settlement Instructions

**As an operations user**,  
I want to review settlement details (cash or physical) tied to a credit event  
So that downstream processing and reconciliation are enabled.

## ✅ Acceptance Criteria
- Unified endpoint: `GET /api/cds-trades/{id}/credit-events/{eventId}/settlement` returns either cash calculation or physical scaffold metadata with a `type` discriminator.
- Cash: includes notional, recovery_rate, payout_amount, calculated_at.
- Physical: includes reference_obligation_isin, proposed_delivery_date, notes, status.
- 404 if neither settlement record exists.
- Response includes trade_id and credit_event_id always.
- Simple HAL-style links (self, trade, credit-event) optional but documented.

## 🧪 Test Scenarios
1. Cash event → returns type=cash with fields.
2. Physical event → returns type=physical with fields.
3. Unknown event → 404.
4. Event exists but settlement still pending creation (should not happen given earlier stories) → 404.

## 🛠 Implementation Guidance
- Introduce projection DTO `SettlementView` with sealed-style mapping (if using Java 17) or type field.
- Service chooses repo path based on existence (check cash first, then physical).
- Add controller method performing negotiation and returning JSON.

## 📦 Deliverables
- DTO + controller method.
- Tests for both branches.
- Documentation in README acceptance matrix linking to this story.

## 🔮 Backlog Seeds
- Pagination / history if multiple adjustments allowed later.
- HATEOAS enrichment.

