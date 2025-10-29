# Story 15.12 - Tranche Groundwork Structures

## Objective
Establish and validate foundational representation for tranche layers (attachment/detachment points) prior to pricing: ensure domain objects persist, API returns structured JSON, UI preview displays layer intervals, and validation prevents overlapping or malformed intervals.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-15-12-001 | Create tranche structure with 0%-3%, 3%-7%, 7%-12% layers | Domain/API | @TRANCHE @DOMAIN |
| FT-15-12-002 | Persisted structure retrievable via GET /tranche/{id} | API | @PERSISTENCE @RETRIEVE |
| FT-15-12-003 | Overlapping layer rejected (3%-7% then 6%-10%) | API | @VALIDATION @NEGATIVE |
| FT-15-12-004 | Detachment < attachment rejected | API | @VALIDATION @NEGATIVE |
| FT-15-12-005 | UI preview table lists layers sorted ascending | UI | @UI @ORDER |
| FT-15-12-006 | Accessibility: layer row has descriptive aria-label | UI | @ACCESSIBILITY @A11Y |
| FT-15-12-007 | JSON schema stable; hash unchanged across creation/retrieval | API | @SCHEMA @STABILITY |
| FT-15-12-008 | Unauthorized creation attempt returns 403 | API | @SECURITY @NEGATIVE |
| FT-15-12-009 | Rate limit protection on rapid create attempts (429) | API | @SECURITY @RATE_LIMIT |
| FT-15-12-010 | Performance: create+retrieve round trip p95 < threshold | API | @PERFORMANCE @LATENCY |
| FT-15-12-011 | Deterministic ordering enforced even if inputs unsorted | Domain | @DETERMINISM @NORMALIZATION |
| FT-15-12-012 | Large number of layers (15) still renders without layout shift | UI | @SCALING @UI |
| FT-15-12-013 | Export structure JSON matches baseline | API | @EXPORT @DRIFT |
| FT-15-12-014 | Error logged when malformed payload (missing detachment) | API | @ERROR @LOGGING |

## Automation Strategy
1. POST valid tranche structure (unordered input) and capture returned normalized structure.
2. GET structure; assert equality & schema hash vs baseline fixture.
3. Attempt overlapping & invalid intervals; assert 400 with validation details.
4. Render UI preview; verify sorted order, aria-labels, absence of layout shift via performance markers.
5. Unauthorized token tests for 403; brute create loop for 429.
6. Export JSON; compare hashed content to baseline fixture.
7. Large layer stress: generate 15 layers; scroll snapshot; check no overflow.

## Metrics
- trancheCreateLatency
- trancheValidationFailureCount
- trancheLayerCount

## Thresholds
- p95 create+retrieve < 300ms
- Validation must identify overlapping intervals with specific code TRANCHE_OVERLAP

## Fixtures
- `fixtures/tranche/FT-15-12-baseline.json`

## Exit Criteria
All validations enforced; ordering deterministic; performance within bounds; schema stable.
