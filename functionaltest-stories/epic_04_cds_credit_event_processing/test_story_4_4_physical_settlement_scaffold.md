# Functional Test Story 4.4 – Physical Settlement Scaffold

Trace: story_4_4_physical_settlement_scaffold
Tags: @EPIC_04 @STORY_4_4 @CREDIT_EVENT @PHYSICAL

## Objective
Validate initial scaffold for physical settlement workflow: capture of delivery obligations, placeholder status transitions, validation rules.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-4-4-001 | Enable physical settlement option in UI | E2E | @UI |
| FT-4-4-002 | Mandatory delivery obligation fields shown | E2E | @UI @VALIDATION |
| FT-4-4-003 | Add multiple obligations (list add/remove) | E2E | @UI |
| FT-4-4-004 | Validation missing ISIN on obligation | API | @NEGATIVE |
| FT-4-4-005 | Duplicate obligation ISIN blocked | API | @NEGATIVE |
| FT-4-4-006 | Submit stores obligations stub state PENDING_DELIVERY | API | @STATE |
| FT-4-4-007 | Fetch returns obligations collection | API | @CRUD |
| FT-4-4-008 | Unauthorized role cannot enable physical | API | @SECURITY |
| FT-4-4-009 | Audit entry includes obligations count | API | @AUDIT |
| FT-4-4-010 | Large obligation list (50) performance < threshold | API | @PERFORMANCE |
| FT-4-4-011 | Remove obligation updates payload accurately | API | @CRUD |
| FT-4-4-012 | Placeholder settlement confirmation absent (deferred) | API | @PLACEHOLDER |

## Automation Strategy
Playwright for dynamic form sections; integration for obligation persistence & validation.
