# Story 16.5: Valuation Results Storage & History

## Story
**As a** risk analyst  
**I want** valuation results stored with full history  
**So that** I can track P&L over time and perform trend analysis

## Acceptance Criteria
- [ ] Store NPV and accrued interest for each trade daily
- [ ] Maintain historical valuations (time series data)
- [ ] Efficient storage and retrieval for large datasets
- [ ] Support querying by:
  - [ ] Trade ID
  - [ ] Valuation date
  - [ ] Date range
  - [ ] Portfolio
- [ ] Data retention policy (e.g., keep 5 years of history)
- [ ] Data archival for older valuations
- [ ] Export capabilities (CSV, JSON)
- [ ] Data integrity constraints (prevent duplicates)

## Technical Details

### Database Schema (Enhanced)
```sql
-- Main valuation results table
CREATE TABLE eod_valuation_results (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    
    -- NPV components
    npv DECIMAL(20, 4) NOT NULL,
    premium_leg_pv DECIMAL(20, 4),
    protection_leg_pv DECIMAL(20, 4),
    
    -- Accrued interest
    accrued_interest DECIMAL(20, 4) NOT NULL,
    accrual_days INTEGER,
    
    -- Total value
    total_value DECIMAL(20, 4) NOT NULL, -- NPV + Accrued
    
    currency VARCHAR(3) NOT NULL,
    
    -- Status and metadata
    valuation_status VARCHAR(20) NOT NULL,
    calculation_time_ms INTEGER,
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(valuation_date, trade_id)
);

-- Partitioning by date for performance (PostgreSQL 10+)
-- Create monthly partitions automatically
CREATE TABLE eod_valuation_results_2025_01 
    PARTITION OF eod_valuation_results
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

-- Portfolio-level aggregations
CREATE TABLE eod_portfolio_valuations (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    portfolio_id BIGINT NOT NULL REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    
    total_npv DECIMAL(20, 4) NOT NULL,
    total_accrued DECIMAL(20, 4) NOT NULL,
    total_value DECIMAL(20, 4) NOT NULL,
    
    trade_count INTEGER NOT NULL,
    failed_count INTEGER DEFAULT 0,
    
    currency VARCHAR(3) NOT NULL,
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(valuation_date, portfolio_id)
);

-- Historical valuation changes (for audit/replay)
CREATE TABLE valuation_history_log (
    id BIGSERIAL PRIMARY KEY,
    valuation_result_id BIGINT NOT NULL REFERENCES eod_valuation_results(id),
    change_type VARCHAR(20) NOT NULL, -- INSERT, UPDATE, REVALUE, CORRECTION
    old_npv DECIMAL(20, 4),
    new_npv DECIMAL(20, 4),
    old_accrued DECIMAL(20, 4),
    new_accrued DECIMAL(20, 4),
    changed_by VARCHAR(100),
    change_reason TEXT,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_valuation_results_date ON eod_valuation_results(valuation_date);
CREATE INDEX idx_valuation_results_trade ON eod_valuation_results(trade_id);
CREATE INDEX idx_valuation_results_date_trade ON eod_valuation_results(valuation_date, trade_id);
CREATE INDEX idx_valuation_results_status ON eod_valuation_results(valuation_status);

CREATE INDEX idx_portfolio_valuations_date ON eod_portfolio_valuations(valuation_date);
CREATE INDEX idx_portfolio_valuations_portfolio ON eod_portfolio_valuations(portfolio_id);

CREATE INDEX idx_history_log_result ON valuation_history_log(valuation_result_id);
CREATE INDEX idx_history_log_date ON valuation_history_log(changed_at);
```

### Java Models
```java
@Entity
@Table(name = "eod_valuation_results")
public class EodValuationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private CDSTrade trade;
    
    // NPV components
    @Column(name = "npv", nullable = false, precision = 20, scale = 4)
    private BigDecimal npv;
    
    @Column(name = "premium_leg_pv", precision = 20, scale = 4)
    private BigDecimal premiumLegPv;
    
    @Column(name = "protection_leg_pv", precision = 20, scale = 4)
    private BigDecimal protectionLegPv;
    
    // Accrued interest
    @Column(name = "accrued_interest", nullable = false, precision = 20, scale = 4)
    private BigDecimal accruedInterest;
    
    @Column(name = "accrual_days")
    private Integer accrualDays;
    
    // Total value
    @Column(name = "total_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalValue; // NPV + Accrued
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "valuation_status", nullable = false)
    private ValuationStatus valuationStatus;
    
    @Column(name = "calculation_time_ms")
    private Integer calculationTimeMs;
    
    @Column(name = "job_id")
    private String jobId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    public void calculateTotalValue() {
        this.totalValue = this.npv.add(this.accruedInterest);
    }
}

@Entity
@Table(name = "eod_portfolio_valuations")
public class EodPortfolioValuation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private CdsPortfolio portfolio;
    
    @Column(name = "total_npv", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalNpv;
    
    @Column(name = "total_accrued", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalAccrued;
    
    @Column(name = "total_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalValue;
    
    @Column(name = "trade_count", nullable = false)
    private Integer tradeCount;
    
    @Column(name = "failed_count")
    private Integer failedCount = 0;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "job_id")
    private String jobId;
}
```

### Service Implementation
```java
@Service
public class ValuationStorageService {
    
    @Autowired
    private EodValuationResultRepository valuationRepository;
    
    @Autowired
    private EodPortfolioValuationRepository portfolioValuationRepository;
    
    @Transactional
    public void storeValuations(
        LocalDate valuationDate,
        Map<Long, BigDecimal> npvResults,
        Map<Long, BigDecimal> accruedResults,
        String jobId
    ) {
        List<EodValuationResult> results = new ArrayList<>();
        
        npvResults.forEach((tradeId, npv) -> {
            BigDecimal accrued = accruedResults.getOrDefault(
                tradeId, BigDecimal.ZERO
            );
            
            EodValuationResult result = new EodValuationResult();
            result.setValuationDate(valuationDate);
            result.setTradeId(tradeId);
            result.setNpv(npv);
            result.setAccruedInterest(accrued);
            result.setJobId(jobId);
            result.setValuationStatus(ValuationStatus.SUCCESS);
            
            results.add(result);
        });
        
        // Batch insert for performance
        valuationRepository.saveAll(results);
        
        log.info("Stored {} valuation results for date {}", 
                results.size(), valuationDate);
    }
    
    @Transactional
    public void storePortfolioValuation(
        LocalDate valuationDate,
        Long portfolioId,
        List<EodValuationResult> tradeResults,
        String jobId
    ) {
        BigDecimal totalNpv = tradeResults.stream()
            .map(EodValuationResult::getNpv)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalAccrued = tradeResults.stream()
            .map(EodValuationResult::getAccruedInterest)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int failedCount = (int) tradeResults.stream()
            .filter(r -> r.getValuationStatus() == ValuationStatus.FAILED)
            .count();
        
        EodPortfolioValuation portfolioVal = new EodPortfolioValuation();
        portfolioVal.setValuationDate(valuationDate);
        portfolioVal.setPortfolioId(portfolioId);
        portfolioVal.setTotalNpv(totalNpv);
        portfolioVal.setTotalAccrued(totalAccrued);
        portfolioVal.setTotalValue(totalNpv.add(totalAccrued));
        portfolioVal.setTradeCount(tradeResults.size());
        portfolioVal.setFailedCount(failedCount);
        portfolioVal.setJobId(jobId);
        
        portfolioValuationRepository.save(portfolioVal);
    }
    
    public List<EodValuationResult> getValuationHistory(
        Long tradeId, LocalDate fromDate, LocalDate toDate
    ) {
        return valuationRepository.findByTradeIdAndDateRange(
            tradeId, fromDate, toDate
        );
    }
    
    public EodValuationResult getLatestValuation(Long tradeId) {
        return valuationRepository.findFirstByTradeIdOrderByValuationDateDesc(
            tradeId
        );
    }
}
```

### REST API
```java
@RestController
@RequestMapping("/api/valuations")
public class ValuationStorageController {
    
    @GetMapping("/trades/{tradeId}/history")
    public ResponseEntity<List<EodValuationResult>> getValuationHistory(
        @PathVariable Long tradeId,
        @RequestParam LocalDate fromDate,
        @RequestParam LocalDate toDate
    );
    
    @GetMapping("/trades/{tradeId}/latest")
    public ResponseEntity<EodValuationResult> getLatestValuation(
        @PathVariable Long tradeId
    );
    
    @GetMapping("/{date}")
    public ResponseEntity<List<EodValuationResult>> getValuationsByDate(
        @PathVariable LocalDate date
    );
    
    @GetMapping("/portfolio/{portfolioId}/history")
    public ResponseEntity<List<EodPortfolioValuation>> getPortfolioHistory(
        @PathVariable Long portfolioId,
        @RequestParam LocalDate fromDate,
        @RequestParam LocalDate toDate
    );
    
    @GetMapping("/{date}/export")
    public ResponseEntity<byte[]> exportValuations(
        @PathVariable LocalDate date,
        @RequestParam(defaultValue = "CSV") String format
    );
}
```

## Test Scenarios
1. **Store Valuations**: Save 500 trade valuations successfully
2. **Retrieve History**: Get valuation history for single trade
3. **Date Range Query**: Retrieve valuations for 30-day period
4. **Portfolio Aggregation**: Calculate portfolio-level totals
5. **Duplicate Prevention**: Attempt to store duplicate (same date/trade)
6. **Export**: Export valuations to CSV format
7. **Performance**: Query 1000 trades in < 2 seconds

## Definition of Done
- [ ] Code implemented and reviewed
- [ ] Database migration scripts created (with partitioning)
- [ ] Unit tests written (>80% coverage)
- [ ] Integration tests for storage/retrieval
- [ ] Performance tests (bulk operations)
- [ ] Export functionality tested
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] QA sign-off

## Dependencies
- Story 16.3: NPV Calculation
- Story 16.4: Accrued Interest Calculator

## Effort Estimate
**5 story points** (1 week)

## Notes
- Consider using time-series database (TimescaleDB) for better performance
- Implement data archival job for old valuations (>5 years)
- Monitor database growth and implement compression
- Consider read replicas for heavy reporting queries
- Partition tables by month for better query performance
