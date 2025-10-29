# Test Epic 06 – Index & Constituent Management

## Scope
Index definition, constituent management, updates, and reference data synchronization.

## Objectives
- Accurate index creation and versioning
- Correct constituent list persistence & retrieval
- Safe updates preserving historical versions
- Reference data lookup & cache coherency

## Risks & Failure Modes
| Risk | Mitigation | Scenario IDs |
|------|-----------|--------------|
| Lost historical constituent set | Versioned fetch tests | FT-6-0-006 |
| Stale cache after update | Cache invalidation tests | FT-6-0-012 |
| Duplicate constituent entries | Validation tests | FT-6-0-004 |

## Scenario Taxonomy
All scenarios consolidated in single test story doc.

## Tooling Matrix
| Layer | Tool |
|-------|------|
| API | REST-assured |
| Cache | Integration with forced invalidation |
| E2E | Playwright (index management UI if exists) |

## Exit Criteria
All FT-6-0-* scenarios automated; version history retrieval consistent.
