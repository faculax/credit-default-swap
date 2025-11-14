# Story 16.6: Daily P&L Calculation Engine

## Story
**As a** trader or portfolio manager  
**I want** automated daily P&L calculations  
**So that** I can track profit and loss across my portfolio with proper attribution

## Acceptance Criteria
- [ ] Calculate daily P&L (T vs T-1 comparison)
- [ ] Break down P&L into components:
  - [ ] Clean P&L (market moves only)
  - [ ] Theta decay (time decay)
  - [ ] Carry/accrued change
  - [ ] New trade P&L
  - [ ] Realized P&L (matured/terminated trades)
- [ ] Calculate cumulative P&L (MTD, QTD, YTD)
- [ ] Support multiple currencies
- [ ] FX translation for consolidated P&L
- [ ] P&L explain/attribution report
- [ ] Handle edge cases:
  - [ ] Missing previous day valuations
  - [ ] New trades
  - [ ] Trade terminations
  - [ ] Credit events

## Technical Details

### Database Schema
```sql
-- Daily P&L results per trade
CREATE TABLE daily_pnl_results (
    id BIGSERIAL PRIMARY KEY,
    pnl_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    
    -- Current day valuations (T)
    current_npv DECIMAL(20, 4) NOT NULL,
    current_accrued DECIMAL(20, 4) NOT NULL,
    current_total_value DECIMAL(20, 4) NOT NULL,
    
    -- Previous day valuations (T-1)
    previous_npv DECIMAL(20, 4),
    previous_accrued DECIMAL(20, 4),
    previous_total_value DECIMAL(20, 4),
    
    -- P&L breakdown
    total_pnl DECIMAL(20, 4) NOT NULL,
    clean_pnl DECIMAL(20, 4), -- NPV change excluding time decay
    theta_decay DECIMAL(20, 4), -- Time value decay
    accrued_change DECIMAL(20, 4), -- Change in accrued interest
    new_trade_pnl DECIMAL(20, 4), -- P&L from new trades
    realized_pnl DECIMAL(20, 4), -- P&L from closed/matured trades
    
    currency VARCHAR(3) NOT NULL,
    
    -- Attribution factors
    spread_pnl DECIMAL(20, 4), -- P&L from spread moves
    ir_pnl DECIMAL(20, 4), -- P&L from interest rate moves
    fx_pnl DECIMAL(20, 4), -- P&L from FX moves
    
    -- Status
    calculation_status VARCHAR(20) NOT NULL,
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(pnl_date, trade_id)
);

-- Portfolio-level P&L aggregation
CREATE TABLE portfolio_pnl_results (
    id BIGSERIAL PRIMARY KEY,
    pnl_date DATE NOT NULL,
    portfolio_id BIGINT NOT NULL REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    
    -- Aggregated P&L
    total_pnl DECIMAL(20, 4) NOT NULL,
    clean_pnl DECIMAL(20, 4),
    theta_decay DECIMAL(20, 4),
    accrued_change DECIMAL(20, 4),
    new_trade_pnl DECIMAL(20, 4),
    realized_pnl DECIMAL(20, 4),
    
    -- Cumulative P&L
    mtd_pnl DECIMAL(20, 4),
    qtd_pnl DECIMAL(20, 4),
    ytd_pnl DECIMAL(20, 4),
    
    currency VARCHAR(3) NOT NULL,
    trade_count INTEGER,
    
    job_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(pnl_date, portfolio_id)
);

-- P&L attribution by risk factor
CREATE TABLE pnl_attribution (
    id BIGSERIAL PRIMARY KEY,
    pnl_date DATE NOT NULL,
    portfolio_id BIGINT,
    
    -- Attribution by factor
    spread_pnl DECIMAL(20, 4),
    ir_pnl DECIMAL(20, 4),
    fx_pnl DECIMAL(20, 4),
    recovery_rate_pnl DECIMAL(20, 4),
    correlation_pnl DECIMAL(20, 4),
    unexplained_pnl DECIMAL(20, 4), -- Residual
    
    total_attributed_pnl DECIMAL(20, 4),
    currency VARCHAR(3) NOT NULL,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_daily_pnl_date ON daily_pnl_results(pnl_date);
CREATE INDEX idx_daily_pnl_trade ON daily_pnl_results(trade_id);
CREATE INDEX idx_portfolio_pnl_date ON portfolio_pnl_results(pnl_date);
CREATE INDEX idx_portfolio_pnl_portfolio ON portfolio_pnl_results(portfolio_id);
```

### Java Models
```java
@Entity
@Table(name = "daily_pnl_results")
public class DailyPnlResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pnl_date", nullable = false)
    private LocalDate pnlDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private CDSTrade trade;
    
    // Current day (T)
    @Column(name = "current_npv", nullable = false, precision = 20, scale = 4)
    private BigDecimal currentNpv;
    
    @Column(name = "current_accrued", nullable = false, precision = 20, scale = 4)
    private BigDecimal currentAccrued;
    
    @Column(name = "current_total_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal currentTotalValue;
    
    // Previous day (T-1)
    @Column(name = "previous_npv", precision = 20, scale = 4)
    private BigDecimal previousNpv;
    
    @Column(name = "previous_accrued", precision = 20, scale = 4)
    private BigDecimal previousAccrued;
    
    @Column(name = "previous_total_value", precision = 20, scale = 4)
    private BigDecimal previousTotalValue;
    
    // P&L breakdown
    @Column(name = "total_pnl", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalPnl;
    
    @Column(name = "clean_pnl", precision = 20, scale = 4)
    private BigDecimal cleanPnl;
    
    @Column(name = "theta_decay", precision = 20, scale = 4)
    private BigDecimal thetaDecay;
    
    @Column(name = "accrued_change", precision = 20, scale = 4)
    private BigDecimal accruedChange;
    
    @Column(name = "new_trade_pnl", precision = 20, scale = 4)
    private BigDecimal newTradePnl;
    
    @Column(name = "realized_pnl", precision = 20, scale = 4)
    private BigDecimal realizedPnl;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    // Attribution
    @Column(name = "spread_pnl", precision = 20, scale = 4)
    private BigDecimal spreadPnl;
    
    @Column(name = "ir_pnl", precision = 20, scale = 4)
    private BigDecimal irPnl;
    
    @Column(name = "fx_pnl", precision = 20, scale = 4)
    private BigDecimal fxPnl;
}
```

### Service Implementation
```java
@Service
public class DailyPnlCalculationService {
    
    @Autowired
    private ValuationStorageService valuationService;
    
    @Autowired
    private DailyPnlResultRepository pnlRepository;
    
    @Transactional
    public void calculateDailyPnl(LocalDate pnlDate) {
        LocalDate previousDate = getPreviousBusinessDay(pnlDate);
        
        // Get all active trades
        List<CDSTrade> activeTrades = tradeRepository.findActiveTrades(pnlDate);
        
        List<DailyPnlResult> pnlResults = new ArrayList<>();
        
        for (CDSTrade trade : activeTrades) {
            DailyPnlResult pnlResult = calculateTradePnl(
                trade, pnlDate, previousDate
            );
            pnlResults.add(pnlResult);
        }
        
        // Save all P&L results
        pnlRepository.saveAll(pnlResults);
        
        log.info("Calculated P&L for {} trades on {}", 
                pnlResults.size(), pnlDate);
    }
    
    private DailyPnlResult calculateTradePnl(
        CDSTrade trade,
        LocalDate pnlDate,
        LocalDate previousDate
    ) {
        // Get current and previous valuations
        EodValuationResult currentVal = valuationService.getValuation(
            trade.getId(), pnlDate
        );
        EodValuationResult previousVal = valuationService.getValuation(
            trade.getId(), previousDate
        );
        
        DailyPnlResult pnl = new DailyPnlResult();
        pnl.setPnlDate(pnlDate);
        pnl.setTrade(trade);
        pnl.setCurrentNpv(currentVal.getNpv());
        pnl.setCurrentAccrued(currentVal.getAccruedInterest());
        pnl.setCurrentTotalValue(currentVal.getTotalValue());
        
        if (previousVal != null) {
            pnl.setPreviousNpv(previousVal.getNpv());
            pnl.setPreviousAccrued(previousVal.getAccruedInterest());
            pnl.setPreviousTotalValue(previousVal.getTotalValue());
            
            // Calculate total P&L
            BigDecimal totalPnl = currentVal.getTotalValue()
                .subtract(previousVal.getTotalValue());
            pnl.setTotalPnl(totalPnl);
            
            // Break down P&L components
            BigDecimal npvChange = currentVal.getNpv()
                .subtract(previousVal.getNpv());
            BigDecimal accruedChange = currentVal.getAccruedInterest()
                .subtract(previousVal.getAccruedInterest());
            
            pnl.setCleanPnl(npvChange); // Simplified - could be more granular
            pnl.setAccruedChange(accruedChange);
            
            // Theta decay (could be calculated from risk metrics)
            pnl.setThetaDecay(estimateThetaDecay(trade, currentVal));
            
        } else {
            // New trade scenario
            pnl.setTotalPnl(currentVal.getTotalValue());
            pnl.setNewTradePnl(currentVal.getTotalValue());
        }
        
        pnl.setCurrency(trade.getCurrency());
        pnl.setCalculationStatus("SUCCESS");
        
        return pnl;
    }
    
    @Transactional
    public void calculatePortfolioPnl(
        LocalDate pnlDate,
        Long portfolioId
    ) {
        List<DailyPnlResult> tradePnls = pnlRepository
            .findByPnlDateAndPortfolio(pnlDate, portfolioId);
        
        PortfolioPnlResult portfolioPnl = new PortfolioPnlResult();
        portfolioPnl.setPnlDate(pnlDate);
        portfolioPnl.setPortfolioId(portfolioId);
        
        // Aggregate P&L components
        BigDecimal totalPnl = tradePnls.stream()
            .map(DailyPnlResult::getTotalPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal cleanPnl = tradePnls.stream()
            .map(p -> p.getCleanPnl() != null ? p.getCleanPnl() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        portfolioPnl.setTotalPnl(totalPnl);
        portfolioPnl.setCleanPnl(cleanPnl);
        
        // Calculate cumulative P&L
        portfolioPnl.setMtdPnl(calculateMtdPnl(portfolioId, pnlDate));
        portfolioPnl.setQtdPnl(calculateQtdPnl(portfolioId, pnlDate));
        portfolioPnl.setYtdPnl(calculateYtdPnl(portfolioId, pnlDate));
        
        portfolioPnlRepository.save(portfolioPnl);
    }
    
    private BigDecimal calculateMtdPnl(Long portfolioId, LocalDate date) {
        LocalDate monthStart = date.withDayOfMonth(1);
        return pnlRepository.sumPnlForDateRange(
            portfolioId, monthStart, date
        );
    }
}
```

### REST API
```java
@RestController
@RequestMapping("/api/pnl")
public class PnlCalculationController {
    
    @GetMapping("/trades/{tradeId}/daily/{date}")
    public ResponseEntity<DailyPnlResult> getDailyPnl(
        @PathVariable Long tradeId,
        @PathVariable LocalDate date
    );
    
    @GetMapping("/portfolio/{portfolioId}/daily/{date}")
    public ResponseEntity<PortfolioPnlResult> getPortfolioPnl(
        @PathVariable Long portfolioId,
        @PathVariable LocalDate date
    );
    
    @GetMapping("/portfolio/{portfolioId}/history")
    public ResponseEntity<List<PortfolioPnlResult>> getPnlHistory(
        @PathVariable Long portfolioId,
        @RequestParam LocalDate fromDate,
        @RequestParam LocalDate toDate
    );
    
    @GetMapping("/portfolio/{portfolioId}/cumulative/{date}")
    public ResponseEntity<CumulativePnlDto> getCumulativePnl(
        @PathVariable Long portfolioId,
        @PathVariable LocalDate date
    );
    
    @GetMapping("/attribution/{portfolioId}/{date}")
    public ResponseEntity<PnlAttributionDto> getPnlAttribution(
        @PathVariable Long portfolioId,
        @PathVariable LocalDate date
    );
}
```

## Test Scenarios
1. **Simple P&L**: Calculate P&L for trade with unchanged valuation (should be accrued only)
2. **Spread Move**: Calculate P&L after 50bps spread widening
3. **New Trade**: Calculate P&L for trade opened today
4. **Matured Trade**: Calculate realized P&L for matured trade
5. **Portfolio Aggregation**: Sum P&L for 100-trade portfolio
6. **MTD/YTD**: Calculate cumulative P&L for month/year
7. **Missing T-1**: Handle case where previous valuation doesn't exist

## Definition of Done
- [ ] Code implemented and reviewed
- [ ] Database migration scripts created
- [ ] Unit tests written (>80% coverage)
- [ ] Integration tests with mock valuations
- [ ] P&L attribution logic validated
- [ ] Performance tests (1000 trades < 30 seconds)
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] QA sign-off

## Dependencies
- Story 16.5: Valuation Results Storage & History
- Business day calendar utility

## Effort Estimate
**8 story points** (1.5 weeks)

## Notes
- P&L calculation runs after EOD valuation job completes
- Consider using risk sensitivities (CS01, IR01) for better attribution
- Implement P&L explain report showing factor contributions
- Handle credit events separately (realized loss calculation)
- Support P&L drill-down from portfolio → trade level
- FX translation needed for multi-currency portfolios
