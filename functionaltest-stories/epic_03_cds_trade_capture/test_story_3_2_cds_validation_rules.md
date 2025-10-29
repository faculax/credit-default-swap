# Functional Test Story 3.2 – Server Validation Rules

Trace: story_3_2_cds_validation_rules
Tags: @EPIC_03 @STORY_3_2 @TRADE @NEGATIVE

## Objective
Assert server-side validation matrix enforces all business constraints independently of UI.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-3-2-001 | Missing required fields returns aggregate error map | API | @NEGATIVE |
| FT-3-2-002 | Notional zero rejected | API | @NEGATIVE |
| FT-3-2-003 | Negative notional rejected | API | @NEGATIVE |
| FT-3-2-004 | Spread below minimum bound | API | @NEGATIVE |
| FT-3-2-005 | Spread above maximum bound | API | @NEGATIVE |
| FT-3-2-006 | Unsupported currency code | API | @NEGATIVE |
| FT-3-2-007 | Recovery < 0 | API | @NEGATIVE |
| FT-3-2-008 | Recovery > 1 | API | @NEGATIVE |
| FT-3-2-009 | Effective date < trade date | API | @NEGATIVE |
| FT-3-2-010 | Maturity < effective date | API | @NEGATIVE |
| FT-3-2-011 | Tenor exceeds configured max | API | @NEGATIVE |
| FT-3-2-012 | Duplicate client reference id -> 409 | API | @CONFLICT |
| FT-3-2-013 | Unknown ISIN reference lookup failure 422 | API | @REFDATA |
| FT-3-2-014 | Invalid restructuring clause enum value | API | @NEGATIVE |
| FT-3-2-015 | Unsupported day count convention | API | @NEGATIVE |
| FT-3-2-016 | Invalid premium frequency | API | @NEGATIVE |
| FT-3-2-017 | Wrong JSON type (string for number) -> 400 | Contract | @SCHEMA |
| FT-3-2-018 | Payload size exceeds limit -> 413 | API | @SECURITY |
| FT-3-2-019 | Idempotency key reuse identical payload returns same tradeId | API | @IDEMPOTENCY |
| FT-3-2-020 | Idempotency key reuse different payload -> 409 conflict | API | @IDEMPOTENCY |
| FT-3-2-021 | Missing auth -> 401 | API | @SECURITY |
| FT-3-2-022 | Insufficient role -> 403 | API | @SECURITY |
| FT-3-2-023 | Rate limit exceeded -> 429 | API | @RESILIENCE |
| FT-3-2-024 | Audit record exists on rejection | API | @AUDIT |

## Automation Strategy
Java integration tests with Testcontainers Postgres & WireMock for reference data.
File: `backend/src/test/java/.../it/epic03/TradeValidationIT.java`

## Data & Config
- Use dynamic idempotency key header `X-Idempotency-Key`.
- Rate limit simulation via sequential rapid POST loop.

## Metrics
- Count of distinct error codes returned.
- Ensure latency < 150ms median for validation rejects.

## Open Questions
- Confirm 422 vs 400 usage for reference data errors.
