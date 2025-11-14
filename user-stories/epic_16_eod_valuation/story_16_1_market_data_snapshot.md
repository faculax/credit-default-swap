# Story 16.1: Market Data Snapshot Service

## Story
**As a** risk manager  
**I want** a reliable market data snapshot captured at end-of-day  
**So that** all valuations use consistent market prices and curves

## Acceptance Criteria
- [ ] Market data snapshot captured at configurable EOD time (default: 5:00 PM EST)
- [ ] Snapshot includes:
  - [ ] CDS spreads for all reference entities with active trades
  - [ ] Interest rate curves (risk-free rates for all currencies)
  - [ ] FX rates for multi-currency positions
  - [ ] Recovery rate assumptions per seniority/sector
  - [ ] Credit curve tenors and interpolation parameters
- [ ] Data validation rules applied:
  - [ ] No missing required data points
  - [ ] Spreads within reasonable bounds (e.g., 0-10,000 bps)
  - [ ] Stale data detection (age > 1 business day flagged)
- [ ] Snapshot stored with timestamp and version
- [ ] API endpoint to retrieve snapshot by date
- [ ] Audit log of all snapshot creation events
- [ ] Fallback logic for missing data (use T-1, mark as stale)

## Technical Details

### Database Schema
```sql
-- Market data snapshots
CREATE TABLE market_data_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL UNIQUE,
    snapshot_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, COMPLETE, PARTIAL, FAILED
    total_entities INTEGER,
    missing_data_count INTEGER,
    stale_data_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- CDS spread data
CREATE TABLE snapshot_cds_spreads (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES market_data_snapshots(id) ON DELETE CASCADE,
    reference_entity VARCHAR(50) NOT NULL,
    tenor VARCHAR(10) NOT NULL, -- 1Y, 3Y, 5Y, 7Y, 10Y
    spread_bps DECIMAL(10, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    seniority VARCHAR(20),
    restructuring_clause VARCHAR(10),
    source VARCHAR(50), -- BLOOMBERG, MARKIT, etc.
    is_stale BOOLEAN DEFAULT false,
    data_age_days INTEGER,
    UNIQUE(snapshot_id, reference_entity, tenor, currency, seniority)
);

-- Interest rate curves
CREATE TABLE snapshot_ir_curves (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES market_data_snapshots(id) ON DELETE CASCADE,
    currency VARCHAR(3) NOT NULL,
    curve_type VARCHAR(20) NOT NULL, -- RISK_FREE, DISCOUNT, FORWARD
    tenor VARCHAR(10) NOT NULL,
    rate DECIMAL(12, 8) NOT NULL,
    UNIQUE(snapshot_id, currency, curve_type, tenor)
);

-- FX rates
CREATE TABLE snapshot_fx_rates (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES market_data_snapshots(id) ON DELETE CASCADE,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(18, 8) NOT NULL,
    UNIQUE(snapshot_id, base_currency, quote_currency)
);

-- Recovery rates
CREATE TABLE snapshot_recovery_rates (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES market_data_snapshots(id) ON DELETE CASCADE,
    seniority VARCHAR(20) NOT NULL,
    sector VARCHAR(30),
    recovery_rate DECIMAL(5, 4) NOT NULL, -- 0.0000 to 1.0000
    UNIQUE(snapshot_id, seniority, sector)
);

-- Create indexes
CREATE INDEX idx_snapshots_date ON market_data_snapshots(snapshot_date);
CREATE INDEX idx_cds_spreads_snapshot ON snapshot_cds_spreads(snapshot_id);
CREATE INDEX idx_cds_spreads_entity ON snapshot_cds_spreads(reference_entity);
CREATE INDEX idx_ir_curves_snapshot ON snapshot_ir_curves(snapshot_id);
CREATE INDEX idx_fx_rates_snapshot ON snapshot_fx_rates(snapshot_id);
```

### Java Models
```java
@Entity
@Table(name = "market_data_snapshots")
public class MarketDataSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;
    
    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SnapshotStatus status;
    
    @Column(name = "total_entities")
    private Integer totalEntities;
    
    @Column(name = "missing_data_count")
    private Integer missingDataCount;
    
    @Column(name = "stale_data_count")
    private Integer staleDataCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private List<SnapshotCdsSpread> cdsSpreads = new ArrayList<>();
    
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private List<SnapshotIrCurve> irCurves = new ArrayList<>();
    
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private List<SnapshotFxRate> fxRates = new ArrayList<>();
    
    public enum SnapshotStatus {
        PENDING, COMPLETE, PARTIAL, FAILED
    }
}
```

### REST API
```java
@RestController
@RequestMapping("/api/market-data-snapshots")
public class MarketDataSnapshotController {
    
    @PostMapping("/capture")
    public ResponseEntity<MarketDataSnapshot> captureSnapshot(
        @RequestParam(required = false) LocalDate date
    );
    
    @GetMapping("/{date}")
    public ResponseEntity<MarketDataSnapshot> getSnapshotByDate(
        @PathVariable LocalDate date
    );
    
    @GetMapping("/latest")
    public ResponseEntity<MarketDataSnapshot> getLatestSnapshot();
    
    @GetMapping("/{date}/cds-spreads")
    public ResponseEntity<List<SnapshotCdsSpread>> getCdsSpreads(
        @PathVariable LocalDate date,
        @RequestParam(required = false) String referenceEntity
    );
    
    @GetMapping("/{date}/ir-curves")
    public ResponseEntity<List<SnapshotIrCurve>> getIrCurves(
        @PathVariable LocalDate date,
        @RequestParam(required = false) String currency
    );
}
```

## Test Scenarios
1. **Complete Snapshot**: All market data available and current
2. **Partial Snapshot**: Some data missing, flagged appropriately
3. **Stale Data**: Data older than threshold, marked as stale
4. **Invalid Data**: Spread out of bounds, rejected with error
5. **Duplicate Snapshot**: Attempt to create snapshot for existing date
6. **Retrieval**: Get snapshot by date, retrieve spreads for entity
7. **Fallback Logic**: Missing data uses T-1 value

## Definition of Done
- [ ] Code implemented and reviewed
- [ ] Database migration scripts created
- [ ] Unit tests written (>80% coverage)
- [ ] Integration tests for API endpoints
- [ ] Data validation rules tested
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] QA sign-off

## Dependencies
- Market data provider integration (external)
- Business day calendar service

## Effort Estimate
**5 story points** (1 week)

## Notes
- Consider using external market data provider APIs (Bloomberg, Refinitiv)
- May need to implement rate limiting for API calls
- Consider caching frequently accessed snapshots
- Monitor snapshot creation time (SLA: < 5 minutes)
