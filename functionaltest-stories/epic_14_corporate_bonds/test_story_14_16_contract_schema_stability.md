# Functional Test Story 14.16 – Contract Schema Stability

Trace: story_14_16_contract-schema-stability
Tags: @EPIC_14 @CORPORATE_BONDS @CONTRACT

## Objective
Validate stability of public bond-related API schemas (trade, pricing, risk, schedule) using snapshot hashing.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-14-16-001 | Trade schema snapshot match | Contract | @CONTRACT |
| FT-14-16-002 | Pricing schema snapshot match | Contract | @CONTRACT |
| FT-14-16-003 | Risk schema snapshot match | Contract | @CONTRACT |
| FT-14-16-004 | Schedule schema snapshot match | Contract | @CONTRACT |
| FT-14-16-005 | Unauthorized schema fetch -> 403 | API | @SECURITY |
| FT-14-16-006 | Drift schema hash baseline | Contract | @DRIFT |
| FT-14-16-007 | Logging redacts internal field names | API | @SECURITY |
| FT-14-16-008 | Metrics contractChangeDetectedCount | API | @METRICS |
| FT-14-16-009 | Performance schema fetch latency | API | @PERFORMANCE |
| FT-14-16-010 | Accessibility contract docs UI | E2E | @ACCESSIBILITY |
| FT-14-16-011 | Concurrency multi-schema fetch isolation | API | @CONCURRENCY |
| FT-14-16-012 | Edge missing required field triggers failure | Contract | @NEGATIVE |

## Automation Strategy
JSON schema snapshot hashing; negative altered field test; metrics validation.
