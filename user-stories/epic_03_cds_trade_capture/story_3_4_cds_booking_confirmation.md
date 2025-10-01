# Story 3.4 – Booking Confirmation UX & Error Handling

**As a user**,  
I want clear feedback after submitting a CDS trade  
So that I know whether it booked successfully or requires correction.

## ✅ Acceptance Criteria
- On success, a confirmation modal displays key attributes: Reference Entity, Notional, Spread, Maturity, Effective, Counterparty, Trade Date, Buy/Sell Protection, Trade Status, Created Timestamp.
- Modal includes a unique Trade ID prominently.
- User can: (a) Close modal, (b) Start a new trade (clears form), (c) (Future) View trade details.
- On validation error (400), inline errors re-bind to fields and page does NOT clear.
- On network/server error (>=500), show non-blocking alert/toast with retry option.
- Loading state disables submit button + shows spinner/label ("Booking Trade...").
- Accessibility: modal focus trap + ESC to close + ARIA labels.
- No duplicate submission while in-flight.

## 🧪 Test Scenarios
1. Successful submit → modal appears; closing leaves form reset.
2. Validation error → errors appear; no modal; data retained.
3. Server 500 → toast + retry allowed.
4. User hits submit twice quickly → only one request actually sent.
5. Accessibility: tab order cycles within modal.

## 🛠 Implementation Guidance
- Extend existing `ConfirmationModal` component.
- Introduce a `lastBookedTrade` state in parent container.
- Add a generic `useAsync` hook or inline promise handling with proper state flags.
- Provide a utility formatter for numeric fields (grouping separators optionally future).

## 📦 Deliverables
- Enhanced modal UI.
- Error + loading states.
- Basic unit test or storybook scenario (optional) for modal.

## ⏭ Dependencies / Links
- Relies on persistence (Story 3.3) and validation (Story 3.2).

