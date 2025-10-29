# Functional Test Story 3.4 – Booking Confirmation

Trace: story_3_4_cds_booking_confirmation
Tags: @EPIC_03 @STORY_3_4 @TRADE @CONFIRMATION

## Objective
Validate confirmation artifact is generated correctly, retrievable, and stable across runs excluding dynamic timestamp.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-3-4-001 | POST trade triggers confirmation generation | API | @CRITPATH |
| FT-3-4-002 | Confirmation status endpoint eventually READY | API | @ASYNC |
| FT-3-4-003 | Download confirmation returns PDF (content-type) | API | @CONTENT |
| FT-3-4-004 | PDF contains trade id, notional, dates | API | @CONTENT |
| FT-3-4-005 | Hash stable across re-download | API | @REGRESSION |
| FT-3-4-006 | Timestamp difference allowed | API | @REGRESSION |
| FT-3-4-007 | Missing confirmation id -> 404 | API | @NEGATIVE |
| FT-3-4-008 | Unauthorized user cannot download | API | @SECURITY |
| FT-3-4-009 | Retry polling interval backoff logic | API | @RESILIENCE |
| FT-3-4-010 | Drift detection triggers alert if hash changes | API | @DRIFT |
| FT-3-4-011 | Large notional formatting correct in PDF | API | @FORMAT |
| FT-3-4-012 | Multi-currency trade displays currency code | API | @FORMAT |
| FT-3-4-013 | PDF size within limit (<1MB) | API | @NONFUNCTIONAL |
| FT-3-4-014 | Audit log entry for download | API | @AUDIT |

## Automation Strategy
Integration test downloads binary, compute SHA256 excluding dynamic metadata via stripping lines with pattern `Generated:`.

## Metrics
- Confirmation ready time p95 recorded.

## Open Questions
- Confirm retention policy for confirmations directory.
