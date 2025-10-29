# Functional Test Story 5.3 – Economic Amend Workflow

Trace: story_5_3_economic_amend_workflow
Tags: @EPIC_05 @STORY_5_3 @LIFECYCLE @WORKFLOW

## Objective
Validate multi-step economic amendment workflow with approvals, versioning, rollback safety, and audit compliance.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-5-3-001 | Initiate amendment draft | API | @CRUD |
| FT-5-3-002 | Draft captures proposed field changes | API | @VERSIONING |
| FT-5-3-003 | Validation rejects invalid field change | API | @NEGATIVE |
| FT-5-3-004 | Reviewer role approval transitions state | API | @STATE |
| FT-5-3-005 | Reject returns to DRAFT with reason | API | @STATE |
| FT-5-3-006 | Concurrent amendments blocked (one active) | API | @CONCURRENCY |
| FT-5-3-007 | Audit entry each state change | API | @AUDIT |
| FT-5-3-008 | Final apply increments trade version | API | @VERSIONING |
| FT-5-3-009 | Rollback on apply failure preserves old trade | API | @TRANSACTION |
| FT-5-3-010 | Unauthorized role cannot approve | API | @SECURITY |
| FT-5-3-011 | Amendment diff endpoint shows changed fields | API | @DIFF |
| FT-5-3-012 | Search amendments by status | API | @FILTER |
| FT-5-3-013 | Pagination stable ordering | API | @PAGINATION |
| FT-5-3-014 | Cancellation of draft allowed | API | @STATE |
| FT-5-3-015 | Metrics increment amendment count | API | @METRICS |
| FT-5-3-016 | Idempotent apply (double call) single version increment | API | @IDEMPOTENCY |
| FT-5-3-017 | Large amendment (many fields) performance | API | @PERFORMANCE |
| FT-5-3-018 | Notification emitted on approval | API | @NOTIFY |
| FT-5-3-019 | Time zone consistency in timestamps | API | @TIME |
| FT-5-3-020 | Comment sanitization (HTML stripped) | API | @SECURITY |

## Automation Strategy
Workflow path tests with synthetic approval roles; failure injection for rollback scenario.
