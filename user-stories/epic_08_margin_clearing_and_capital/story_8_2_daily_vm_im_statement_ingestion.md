# Story 8.2 – Daily VM/IM Statement Ingestion

**As a risk operations user**,  
I want to ingest and reconcile daily variation margin (VM) and initial margin (IM) statements from CCPs  
So that I can maintain accurate collateral positions and identify discrepancies promptly.

## ✅ Acceptance Criteria
- Support multiple CCP statement formats (CSV, XML, proprietary) via configurable parsers.
- Statement files validated against schema and business rules before processing.
- VM and IM positions updated in collateral ledger with effective dates and audit trail.
- Tolerance checking performed against internal calculations with configurable thresholds.
- Discrepancy alerts generated for material differences with detailed variance breakdown.
- Statement processing status tracked (PENDING, PROCESSED, FAILED, DISPUTED).
- Failed statements queued for retry with exponential backoff and manual intervention.
- Historical statement archive maintained for regulatory audit and replay scenarios.

## 🧪 Statement Validation Rules
| Field | Rule |
|-------|------|
| Statement Date | Must be valid business date, not future dated |
| CCP Source | Must be registered CCP with active account relationship |
| VM Amount | Numeric, can be negative (owed to/from CCP) |
| IM Amount | Numeric, must be non-negative |
| Currency | Must be valid ISO currency code |

## 🧠 UX Notes
- Statement ingestion dashboard shows processing status with color-coded indicators.
- Drag-and-drop file upload with format detection and preview.
- Discrepancy drill-down shows field-level variances with tolerance context.

## 🛠 Implementation Guidance
- Use strategy pattern for different CCP statement parsers.
- Implement idempotent processing to handle duplicate statement submissions.
- Consider batch processing for large statement files with progress tracking.
- Store raw statement data alongside parsed values for audit purposes.

## 📦 Deliverables
- Statement parsing framework with CCP-specific adapters.
- Collateral ledger updates with reconciliation logic.
- Discrepancy alerting and workflow management.
- Statement upload UI with processing status tracking.

## ⏭ Dependencies / Links
- Requires collateral ledger schema (may extend existing trade storage).
- Feeds into Story 8.5 (reconciliation dashboard) for variance reporting.

## Traceability
Epic: epic_08_margin_clearing_and_capital
Story ID: 8.2