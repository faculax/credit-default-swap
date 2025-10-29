# Story 15.23 - Weight Rebalancing Workflow

## Objective
Validate workflow for rebalancing basket constituent weights: draft editing, preview impact metrics, commit transaction atomicity, validation, performance, drift stability and accessibility.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-23-001 | Draft edit adjusts weights maintaining total 100% | UI/API | @WEIGHTS @DRAFT |
| FT-15-23-002 | Preview shows impact on fair spread before commit | UI/API | @WEIGHTS @PREVIEW |
| FT-15-23-003 | Commit applies new weights & version increments | API | @VERSIONING @WEIGHTS |
| FT-15-23-004 | Validation prevents total != 100% | API | @VALIDATION @NEGATIVE |
| FT-15-23-005 | Unauthorized commit returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-23-006 | Rate limit on rapid commits | API | @SECURITY @RATE_LIMIT |
| FT-15-23-007 | Deterministic preview with fixed seed stable | Domain | @DETERMINISM @SNAPSHOT |
| FT-15-23-008 | Drift detection: weight config hash stable (post-commit) | API | @DRIFT @SNAPSHOT |
| FT-15-23-009 | Performance: commit latency < threshold | API | @PERFORMANCE @LATENCY |
| FT-15-23-010 | Accessibility: form labels & error summary roles | UI | @ACCESSIBILITY @A11Y |
| FT-15-23-011 | Error logging: failed commit due to validation recorded | API | @ERROR @LOGGING |

## Automation Strategy
1. Begin edit: modify weights; assert total maintained.
2. Preview call; capture metrics impact; deterministic seed check.
3. Commit; verify version increment & persisted weights.
4. Negative tests (invalid totals, unauthorized, rate limit).
5. Drift hash compare for weight config snapshot.
6. Measure commit latency.
7. Accessibility audit of weight form.
8. Logging verification for validation failure.

## Metrics
- weightCommitLatency
- weightValidationFailureCount

## Exit Criteria
Rebalance workflow atomic, validated, deterministic & accessible; performance and drift stable.
