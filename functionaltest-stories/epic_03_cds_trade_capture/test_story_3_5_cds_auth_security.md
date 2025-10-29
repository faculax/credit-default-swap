# Functional Test Story 3.5 – Auth & Security (Trade Capture)

Trace: story_3_5_cds_auth_security
Tags: @EPIC_03 @STORY_3_5 @TRADE @SECURITY

## Objective
Confirm enforcement of authentication & authorization boundaries for all trade capture endpoints & UI features.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-3-5-001 | Unauthenticated create -> 401 | API | @NEGATIVE |
| FT-3-5-002 | Unauthenticated fetch -> 401 | API | @NEGATIVE |
| FT-3-5-003 | Role VIEWER cannot see New Trade button | E2E | @UI |
| FT-3-5-004 | Role VIEWER cannot POST trade (403) | API | @NEGATIVE |
| FT-3-5-005 | Role TRADER can POST trade (201) | API | @POSITIVE |
| FT-3-5-006 | Role TRADER cannot access admin endpoint | API | @NEGATIVE |
| FT-3-5-007 | Token expired refresh works | E2E | @RESILIENCE |
| FT-3-5-008 | Tampered token signature -> 401 | API | @SECURITY |
| FT-3-5-009 | Rate limit exceeded after N creates -> 429 | API | @RESILIENCE |
| FT-3-5-010 | Sensitive fields absent from logs (scrub) | API | @LOGGING |
| FT-3-5-011 | Audit log entry contains actor & action | API | @AUDIT |
| FT-3-5-012 | CORS preflight success for allowed origin | API | @CORS |
| FT-3-5-013 | Disallowed origin blocked | API | @CORS |
| FT-3-5-014 | Replay attack (reuse nonce/idempotency key diff payload) -> 409 | API | @IDEMPOTENCY |
| FT-3-5-015 | JWT minimal required claims enforced | API | @SECURITY |
| FT-3-5-016 | Missing scope claim -> 403 | API | @SECURITY |
| FT-3-5-017 | Security headers present (X-Content-Type-Options etc.) | API | @HEADERS |
| FT-3-5-018 | CSRF not applicable (API token model) documented | Review | @DOCS |
| FT-3-5-019 | Brute force detection triggers after threshold | API | @SECURITY |
| FT-3-5-020 | Password never logged (sanitization) | Review | @LOGGING |

## Automation Strategy
Combination: Playwright for UI visibility & REST-assured for API RBAC matrix.

## Metrics
- Auth failure median latency < success latency (early reject).

## Open Questions
- Central rate limiting vs per-endpoint specification.
