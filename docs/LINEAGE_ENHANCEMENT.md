# Data Lineage Enhancement Summary

## Overview
Enhanced the data lineage tracking system to capture **comprehensive, multi-dimensional lineage** instead of simple single-edge relationships.

## What Changed

### Before (Simple Lineage)
```
TRADE_CAPTURE_CDS -> produces -> cds_trades
```
- Only tracked: operation → primary output dataset
- No visibility into data sources consulted
- No visibility into downstream impacts
- Minimal metadata

### After (Comprehensive Lineage)

#### Inputs Captured (Data Sources Read)
1. **UI Trade Entry**
   - Source: user_interface
   - Form: new_single_name_cds
   - User: system

2. **Reference Data Lookup**
   - Dataset: issuer_master
   - Entity: [entity name]
   - Purpose: entity_validation_and_enrichment

3. **Market Data Lookup**
   - Dataset: market_quotes
   - Spread: [spread in bps]
   - Purpose: pricing_reference

4. **Position Check**
   - Dataset: portfolio_positions
   - Entity: [entity name]
   - Purpose: concentration_risk_check

#### Outputs Captured (Downstream Impacts)
1. **Trade Record Created**
   - Dataset: cds_trades
   - Trade ID, Notional, Entity, Maturity, Spread

2. **Position Updated**
   - Dataset: portfolio_positions
   - Action: increment_notional
   - Notional Delta

3. **Valuation Scheduled**
   - Dataset: cds_valuations
   - Action: mark_to_market_required
   - Trigger: new_trade_booking

4. **Risk Metrics Triggered**
   - Dataset: risk_metrics
   - Action: recalculate_credit_exposure
   - Metrics: credit_exposure, counterparty_risk, concentration_risk, jump_to_default

5. **Margin Calculation Triggered**
   - Dataset: margin_requirements
   - Action: recalculate_im_vm
   - Methods: SIMM, SA-CCR

6. **Regulatory Reporting Flagged**
   - Dataset: regulatory_reports
   - Reports: EMIR, Dodd-Frank, MiFID_II
   - Action: include_in_next_submission

## Graph Visualization

### Node Types
- **Green Nodes**: Datasets (tables, files, data sources)
- **Dark Nodes**: Operations (transformations, calculations)

### Edge Labels
- Input edges labeled with purpose (e.g., "reference_data_lookup", "market_data_lookup")
- Output edges labeled with action (e.g., "trade_record_created", "position_updated")

### Example Graph for Trade Capture
```
issuer_master ──reference_data_lookup──> TRADE_CAPTURE_SINGLE_NAME
market_quotes ──market_data_lookup────> TRADE_CAPTURE_SINGLE_NAME
portfolio_positions ──position_check──> TRADE_CAPTURE_SINGLE_NAME

TRADE_CAPTURE_SINGLE_NAME ──trade_record_created───────> cds_trades
TRADE_CAPTURE_SINGLE_NAME ──position_updated──────────> portfolio_positions
TRADE_CAPTURE_SINGLE_NAME ──valuation_scheduled───────> cds_valuations
TRADE_CAPTURE_SINGLE_NAME ──risk_metrics_triggered───> risk_metrics
TRADE_CAPTURE_SINGLE_NAME ──margin_calculation_triggered─> margin_requirements
TRADE_CAPTURE_SINGLE_NAME ──regulatory_reporting_flagged─> regulatory_reports
```

## Technical Implementation

### Backend Changes
1. **LineageService.trackTradeCapture()** - Enhanced to accept `tradeDetails` Map
   - Captures comprehensive inputs (UI, reference data, market data, position check)
   - Captures comprehensive outputs (trade record, position, valuation, risk, margin, regulatory)
   - All inputs/outputs stored as nested Map structures in JSONB columns

2. **CDSTradeController.createTrade()** - Enhanced to build tradeDetails Map
   - Extracts: entityName, notional, maturityDate, spread, upfrontAmount, buySellProtection, counterparty, tradeDate, currency, recoveryRate
   - Passes detailed trade information to lineageService

### Frontend Changes
1. **lineageService.transformToGraph()** - Enhanced to parse nested input/output structures
   - Iterates through `inputs` object to find datasets
   - Iterates through `outputs` object to find datasets
   - Creates nodes for each dataset found
   - Creates edges with descriptive labels derived from input/output keys
   - Handles fallback for legacy simple lineage format

## Benefits

### Compliance & Audit
- **Complete audit trail**: Shows exactly what data sources were consulted
- **Impact analysis**: Immediately see what downstream systems are affected
- **Regulatory reporting**: Automated tracking of EMIR, Dodd-Frank, MiFID II triggers

### Risk Management
- **Concentration risk**: Visibility into position aggregation checks
- **Credit exposure**: Tracking of risk calculation triggers
- **Margin requirements**: Automatic SIMM/SA-CCR recalculation tracking

### Operations & Troubleshooting
- **Root cause analysis**: Trace data quality issues back to source
- **Dependency mapping**: Understand system interdependencies
- **Change impact**: Assess downstream effects before making changes

## Next Steps

### Instrument Additional Operations
The same pattern can be applied to:
- Credit event processing (default, restructuring, succession)
- Lifecycle events (coupon payments, maturity settlements)
- Portfolio aggregation (netting, concentration analysis)
- Pricing calculations (mark-to-market, curve building)
- Margin calculations (SIMM, SA-CCR, CCP margin)
- Batch processing (EOD runs, CSV imports, reconciliation)
- Schema migrations (database changes via Flyway)

### Advanced Visualization Features
- **Time-based filtering**: View lineage at specific points in time
- **Diff visualization**: Compare lineage before/after changes
- **Anomaly detection**: Highlight unusual patterns in lineage
- **Performance metrics**: Add execution time, record counts to edges
- **Data quality scores**: Show quality metrics on dataset nodes

## Testing

To test the enhanced lineage:

1. **Clear existing data**:
   ```bash
   docker exec -it credit-default-swap-db-1 psql -U cdsuser -d cdsplatform -c "DELETE FROM lineage_events;"
   ```

2. **Create a new trade** in the frontend (New Single-Name CDS form)

3. **View lineage** on Data Lineage page:
   - Select "cds_trades" from dropdown
   - Click "Fetch Lineage"
   - Graph should show:
     - 4 input datasets (ui, issuer_master, market_quotes, portfolio_positions)
     - 1 operation node (TRADE_CAPTURE_SINGLE_NAME)
     - 6 output datasets (cds_trades, portfolio_positions, cds_valuations, risk_metrics, margin_requirements, regulatory_reports)
     - 10 edges total (4 inputs + 6 outputs)

## Files Modified

- `backend/src/main/java/com/creditdefaultswap/platform/service/LineageService.java`
- `backend/src/main/java/com/creditdefaultswap/platform/controller/CDSTradeController.java`
- `frontend/src/services/lineageService.ts`

## Schema Compatibility

The changes are **fully backward compatible**:
- Database schema unchanged (JSONB inputs/outputs support any structure)
- Legacy simple lineage events still render correctly (fallback logic in frontend)
- Incremental enhancement: can update other operations over time
