# Portfolio Aggregation Integration for Bonds - Implementation Note

## Epic 14: Story 14.11

### Status: PLACEHOLDER - Requires Future Enhancement

The full integration of bonds into the existing `PortfolioPricingService` requires:

1. **Constituent Model Extension**
   - Modify `CdsPortfolioConstituent` to support polymorphic instrument types (CDS + Bond)
   - Add `instrument_type` field (`CDS` | `BOND`)
   - Add `bond_id` foreign key (nullable, mutually exclusive with `trade_id`)

2. **Service Logic Updates**
   - Modify `PortfolioPricingService.pricePortfolio()` to:
     - Detect instrument type per constituent
     - For bonds: call `BondService.priceBond()` instead of risk engine
     - Aggregate mixed metrics:
       * PV (sum across all instruments)
       * Duration (notional-weighted average)
       * Spread DV01 (sum of bond spread DV01 + CDS CS01)
       * JTD (sum across all)

3. **DTO Enhancements**
   - Add to `AggregateMetrics`:
     * `avgDuration` (Double)
     * `avgYield` (Double)  
     * `totalSpreadDv01` (BigDecimal)
   - Add to `TradeBreakdown`:
     * `instrumentType` (String: "CDS" | "BOND")
     * `yield` (Double, null for CDS)
     * `duration` (Double, null for CDS)

4. **Migration Required**
   ```sql
   -- V16__add_bond_portfolio_support.sql
   ALTER TABLE cds_portfolio_constituents
   ADD COLUMN instrument_type VARCHAR(10) DEFAULT 'CDS',
   ADD COLUMN bond_id BIGINT REFERENCES bonds(id),
   ADD CONSTRAINT instrument_xor CHECK (
     (trade_id IS NOT NULL AND bond_id IS NULL) OR
     (trade_id IS NULL AND bond_id IS NOT NULL)
   );
   ```

5. **Testing Needs**
   - Mixed portfolio (2 CDS + 2 Bonds) integration test
   - Verify aggregation correctness
   - Verify concentration metrics handle both types

### Quick Win Alternative (Implemented in BondService)
- Bond metrics are available via `POST /api/bonds/{id}/price`
- Frontend can fetch bond data separately and display alongside CDS
- Portfolio-level aggregation deferred to Phase 2

### Acceptance
For MVP, bonds are standalone instruments with full CRUD and pricing.
Portfolio mixed-type aggregation is DEFERRED to future epic.
