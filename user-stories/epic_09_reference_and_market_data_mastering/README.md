# Epic 09 – Reference & Market Data Mastering

## 📌 Overview
Centralized, versioned master data for reference entities, Standard Reference Obligations (SRO), deliverable obligations, index constituents, business day calendars, and recovery defaults feeding pricing, lifecycle, and events.

## 🎯 Business Value
- Consistent, auditable data reduces reconciliation breaks.
- Deterministic replay relies on version‑accurate reference context.
- Faster onboarding of new names/indices with controlled workflows.

## 🧠 Scope
In Scope:
- Entity & obligation store (RED code, LEI, seniority, currency).
- SRO lifecycle management and successor mapping.
- Deliverables list ingestion (DC outputs) with effective dating.
- Index constituents & weight snapshots.
- Calendar & holiday service.
- Default recovery baseline configuration.
Out of Scope (Here):
- Pricing curve bootstrapping logic (Epic 07) – consumes outputs.
- Regulatory report emission (Epic 10).

## 🚫 Out of Scope Detail
- No external licensing integration automation (manual feed load assumed initially).

## 🔐 Domain Terms
| Term | Definition |
|------|------------|
| SRO | Standard Reference Obligation used for quoting/standardization |
| Deliverables | List of obligations eligible in auction physical settlement |
| Successor Mapping | Re-pointing of reference entity exposures post corporate event |

## 🔄 Data Versioning Principles
Immutable effective-dated rows; queries specify as-of; replay selects correct slice.

## 📚 Stories
- Story 9.1 – Reference Entity & SRO Versioning Framework
- Story 9.2 – Deliverable Obligations Loader & DC Sync
- Story 9.3 – Index Constituents & Weight Snapshot Store
- Story 9.4 – Business Day Calendar & Holiday Service
- Story 9.5 – Recovery Defaults Configuration Store
- Story 9.6 – Successor Mapping & Weight Allocation

## ✅ Acceptance Criteria Mapping (Initial)
| Story | Theme | Key Acceptance (Draft) |
|-------|-------|------------------------|
| 9.1 | Entities | Versioned CRUD; as-of retrieval |
| 9.2 | Deliverables | Effective-dated list stored; audit link to DC resolution |
| 9.3 | Index | Snapshot per roll/version; reproducible risk mapping |
| 9.4 | Calendar | Multi-market holiday resolution; caching |
| 9.5 | Recovery | Default recovery per sector/currency fallback order |
| 9.6 | Successor | Split weights applied; position continuity |

## 🧪 Quality Approach
- Fixture loads for DC deliverables.
- As-of query regression tests.
- Successor split scenario test.

## 🎨 UI / UX Acceptance Criteria (Provisional)
- Reference Entity directory: searchable list (entity name, RED code, sector, currency).
- Entity detail panel: current SRO, active deliverables count, successor mapping timeline.
- Deliverables table: obligation ID, effective start/end, seniority, currency.
- Index constituents view (link if Epic 06 present) with as-of date picker.
- Calendar service panel: market selection + holiday list.
- Recovery defaults table: sector, currency, recovery %, last updated.
- Visual status badges for overlapping effective date errors (red) or missing successor mapping (amber).
- Manual QA Flow:
	1. Open Reference directory.
	2. Search entity by RED code.
	3. Open detail; verify SRO and deliverables list.
	4. Add successor mapping (mock) → timeline updates.
	5. Change as-of date → constituents snapshot adjusts.

## ⚠️ Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Data staleness | Feed health checks & freshness SLA metrics |
| Incorrect effective dating | Validation of non-overlapping periods |
| Calendar inconsistency | Cross-validation vs external market calendars |

## 🔮 Backlog Seeds
- Automated RED feed ingestion.
- Data lineage visualization.
- Sector-based recovery curve modeling.
