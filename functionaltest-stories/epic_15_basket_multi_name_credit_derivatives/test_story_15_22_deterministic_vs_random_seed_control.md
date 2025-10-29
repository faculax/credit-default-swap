# Story 15.22 - Deterministic vs Random Seed Control

## Objective
Validate seed control toggle producing deterministic outputs when enabled and varied outputs when disabled across pricing & simulation endpoints; ensure drift artifacts only generated for deterministic mode.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-22-001 | Deterministic mode: identical pricing fair spread across runs | Domain/API | @DETERMINISM @PRICING |
| FT-15-22-002 | Random mode: spread differs across runs (variance) | Domain/API | @RANDOM @VARIANCE |
| FT-15-22-003 | Toggle persists user preference | UI | @UI @PERSISTENCE |
| FT-15-22-004 | Drift artifacts only written in deterministic mode | Domain | @DRIFT @DETERMINISM |
| FT-15-22-005 | Unauthorized attempt to set seed returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-22-006 | Rate limit on rapid seed toggling | API | @SECURITY @RATE_LIMIT |
| FT-15-22-007 | Accessibility: seed toggle aria-pressed state | UI | @ACCESSIBILITY @A11Y |
| FT-15-22-008 | Performance: pricing latency unaffected by toggle (< threshold delta) | Domain | @PERFORMANCE @LATENCY |
| FT-15-22-009 | Schema stability for pricing response | API | @SCHEMA @STABILITY |
| FT-15-22-010 | Error handling: invalid seed value format validation | API | @VALIDATION @NEGATIVE |

## Automation Strategy
1. Enable deterministic mode; run pricing twice; assert identical outputs (hash).
2. Disable deterministic; run twice; assert difference beyond tolerance.
3. Check preference persistence (reload page; toggle state retained).
4. Confirm drift artifacts presence only in deterministic run folder.
5. Negative tests: unauthorized, invalid seed, rate limit.
6. Measure latency for both modes; ensure delta within tolerance.
7. Accessibility audit toggle component.
8. Schema hash stability.

## Metrics
- pricingDeterministicVariance
- pricingRandomVariance

## Exit Criteria
Deterministic vs random distinction enforced; artifacts & performance validated; accessibility & security pass.
