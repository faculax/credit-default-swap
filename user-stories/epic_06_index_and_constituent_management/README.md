# Epic 06 – Index & Constituent Management

## 📌 Overview
Covers management of CDS index trades (CDX / iTraxx), their series/version evolution, constituent events (defaults, removals), and factor adjustments while keeping pricing and risk continuity.

## 🎯 Business Value
- Accurate handling of series rolls preserves hedge effectiveness.
- Proper factor adjustments ensure correct risk & PnL attribution.
- Constituent default flows keep index exposure aligned to market conventions.

## 🧠 Scope
In Scope:
- Series roll orchestration and audit.
- Index factor recalculation after defaults.
- Constituent default cash component & continuation on reduced notional.
- Index snapshot versioning.
Out of Scope (Here):
- Single-name credit event settlement logic (Epic 04).
- Pricing model integration details (Epic 07).
- Index option exercise settlement (deferred or optional story).

## 🚫 Out of Scope Detail
- No cross-venue synchronization specifics (Epic 10).
- No risk aggregation UI (Epic 07 delivers measures; UI later).

## 🔐 Domain Terms
| Term | Definition |
|------|------------|
| Series Roll | Transition from off-the-run to on-the-run index series |
| Index Factor | Ratio adjusting notional after defaults |
| Constituent Default | Default of a single index member triggering factor change |

## 🔄 Lifecycle States (Index Specific)
ACTIVE_INDEX → (ROLLED | FACTOR_ADJUSTED | PARTIALLY_DEFAULTED)

## 📚 Stories
- Story 6.1 – Series Roll Orchestration
- Story 6.2 – Constituent Default Factor & Cashflow Handling
- Story 6.3 – Index Snapshot & Version History Store
- Story 6.4 – Index Option Exercise Settlement Hook (Optional)

## ✅ Acceptance Criteria Mapping (Initial)
| Story | Theme | Key Acceptance (Draft) |
|-------|-------|------------------------|
| 6.1 | Roll | Close prior; open new; carry risk; audit series/version |
| 6.2 | Default | Factor reduction; auction cash; continue trade |
| 6.3 | Snapshot | Persist constituents & weights per series/version |
| 6.4 | Option | Exercise hooks prepared; payout formula stub |

## 🧪 Quality Approach
- Roll replay using historical calendar.
- Factor math unit tests (precision & edge weights).
- Synthetic default scenario regression.

## 🎨 UI / UX Acceptance Criteria (Provisional)
If surfaced in UI (trade detail, index management console):
- Index series roll view shows: current series/version, effective date, previous series link.
- Constituent default event list: timestamp, entity, factor before/after, cash component.
- Factor history chart/table (basic table minimum).
- Snapshot viewer: list of constituents with weights; ability to filter by as-of date.
- Visual cues: status badges (ACTIVE, ROLLED, FACTOR_ADJUSTED, PARTIALLY_DEFAULTED).
- Colours/fonts align with `AGENTS.md` (use Tailwind approximations).
- Accessibility: table headers, aria-label on action buttons (e.g., “Apply Series Roll”).
- Error feedback: toast or inline alert on failed roll/default processing.
- Manual QA Flow (baseline):
	1. Open Index Management page.
	2. Trigger simulated constituent default.
	3. Observe factor reduction & history row append.
	4. Perform series roll; verify new series becomes active.
	5. View snapshot for old series via history link.

## ⚠️ Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Missing historical constituents | Cache snapshots per effective date |
| Factor miscalc under multiple defaults | Recompute from base notional via sequential application |
| Option path ambiguity | Explicit pre/post-default exercise examples |

## 🔮 Backlog Seeds
- Index tranche support.
- Basket custom index builder.
- Real-time index factor change feed.
