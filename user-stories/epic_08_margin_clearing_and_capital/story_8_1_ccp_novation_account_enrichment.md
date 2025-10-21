# Story 8.1 – CCP Novation & Account Enrichment

**As a operations user**,  
I want to execute CCP novation workflows that terminate bilateral trades and create CCP-linked trades  
So that I can properly clear derivatives and maintain accurate trade lifecycle records.

## ✅ Acceptance Criteria
- Bilateral trade status updated to TERMINATED with novation timestamp and reference.
- New CCP trade legs created with identical economic terms but CCP as counterparty.
- Account enrichment includes CCP member ID, clearing account, and netting set assignment.
- Novation event captured in audit trail with original trade references and user context.
- Risk positions transferred seamlessly without exposure gaps during transition.
- Trade identifiers (UTI/USI) properly linked between bilateral and CCP trades.
- Margin account setup triggered automatically for new CCP trades.
- Validation prevents novation of ineligible trades (e.g., already cleared, terminated).

## 🧪 Novation Validation Rules
| Field | Rule |
|-------|------|
| Trade Status | Must be ACTIVE or PENDING |
| CCP Eligibility | Trade type must be in CCP-eligible product list |
| Account Mapping | CCP member account must exist for counterparty |
| Economic Terms | All required fields populated for CCP trade creation |

## 🧠 UX Notes
- Novation action requires explicit confirmation modal with trade summary.
- Display both original and resulting CCP trade details side-by-side.
- Show estimated margin impact and account assignments before confirmation.

## 🛠 Implementation Guidance
- Implement novation as atomic transaction to prevent partial state.
- Use existing trade entity; add CCP-specific fields and relationship mappings.
- Consider event sourcing pattern for audit trail and potential reversal scenarios.
- Validate CCP account setup completion before marking novation successful.

## 📦 Deliverables
- Novation service with atomic trade state transitions.
- CCP account mapping and enrichment logic.
- Audit trail integration for novation events.
- UI modal for novation confirmation and progress tracking.

## ⏭ Dependencies / Links
- Requires CCP account reference data (may need separate setup story).
- Feeds into Story 8.2 (margin statement processing for CCP trades).

## Traceability
Epic: epic_08_margin_clearing_and_capital
Story ID: 8.1