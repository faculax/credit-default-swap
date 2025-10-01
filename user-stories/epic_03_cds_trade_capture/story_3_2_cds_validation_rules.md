# Story 3.2 – Validation & Business Rules

**As the system**,  
I want to enforce CDS-specific validation and provide clear error messages  
So that only coherent, bookable trades are persisted.

## ✅ Acceptance Criteria
- Server rejects invalid submissions with HTTP 400 and JSON body: `{ fieldErrors: { field: message }, globalErrors: [] }`.
- All client validations from Story 3.1 mirrored server-side.
- Additional rules:
  - `notionalAmount` decimal(15,2) > 0
  - `spread` decimal(10,4) >= 0
  - `maturityDate` strictly after `effectiveDate`
  - `effectiveDate` >= `tradeDate`
  - `tradeDate` not in future (grace: allow same-day UTC vs local mismatch)
  - `accrualStartDate` <= `effectiveDate`
  - `buySellProtection` in { BUY, SELL }
  - `premiumFrequency` in allowed set
  - `dayCountConvention` in allowed set
  - `paymentCalendar` in allowed set
  - `restructuringClause` if present must be from allowed list
- Error aggregation supports multiple field errors in one response.
- Impossible combinations (future extension placeholder) logged at WARN for observability.

## 🧪 Test Scenarios
1. Maturity = Effective → reject.
2. Effective < Trade → reject.
3. Notional = 0 → reject.
4. Spread negative → reject.
5. Invalid enum value injected → reject.
6. Multiple errors returned together.
7. Valid submission passes through unchanged to persistence layer.

## 🛠 Implementation Guidance
- Add a `CDSTradeValidator` service (pure function style) returning list of violations.
- Controller maps violations to response structure.
- Keep domain model free of partial states (validate before persist).
- Consider central exception handler (`@ControllerAdvice`).

## 📦 Deliverables
- Validator + unit tests.
- Updated controller integration test for error cases.
- Documentation of response shape in README or OpenAPI stub (optional).

## ⏭ Dependencies / Links
- Builds on Story 3.1 form field set.
- Prerequisite for Story 3.3 (persistence integrity).

