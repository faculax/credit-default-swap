# Story 3.1 – CDS Trade Capture UI & Reference Data

**As a trader / operations user**,  
I want to view and input all standard single-name CDS trade fields in a structured form  
So that I can accurately prepare a trade for booking.

## ✅ Acceptance Criteria
- Form displays all fields listed in Epic 3 field inventory.
- Required fields visually marked (e.g. asterisk) and cannot submit when empty.
- Dropdowns populated from reference data lists (frontend constants initially).
- Default pre-populations:
  - `tradeDate` = today (local)
  - `currency` = USD
  - `premiumFrequency` = QUARTERLY
  - `dayCountConvention` = ACT_360
  - `buySellProtection` = BUY
  - `paymentCalendar` = NYC
  - `tradeStatus` = PENDING (read-only/hidden if needed)
- Restructuring Clause optional: blank allowed.
- Basic inline validation triggers on blur or submit attempt (presence + numeric >= 0).
- Layout responsive: two/three column grid on desktop, single column on mobile.

## 🧪 Basic Validation Rules (UI Layer)
| Field | Rule |
|-------|------|
| Notional Amount | > 0 |
| Spread | >= 0 |
| Effective vs Trade | effectiveDate >= tradeDate |
| Maturity vs Effective | maturityDate > effectiveDate |
| Accrual Start Date | <= Effective Date OR = Effective Date (configurable) |

## 🧠 UX Notes
- Group temporal fields together.
- Group economic terms together (notional, spread, buy/sell).
- Provide helper text for restructuring clause when empty ("Not required for certain contracts").

## 🛠 Implementation Guidance
- Reuse existing `CDSTradeForm` component; extend to ensure all acceptance criteria ticked.
- Introduce a small `fieldMeta` descriptor to drive labels, required flags, placeholders, tooltips.
- Consider adding a `FormState` type to centralize form data & errors.

## 📦 Deliverables
- Updated React form component.
- Reference data module (already exists) confirmed / extended if needed.
- Story-level snapshot or screenshot (optional).

## ⏭ Dependencies / Links
- Feeds Story 3.2 (enhanced validation) & Story 3.3 (submission).

