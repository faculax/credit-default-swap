# Story 16.7: Risk Reporting & Aggregation

## Story
**As a** risk manager  
**I want** aggregated risk metrics at portfolio and firm level  
**So that** I can monitor risk limits and exposures efficiently

## Acceptance Criteria
- [ ] Calculate portfolio-level risk metrics (CS01, IR01, JTD, REC01)
- [ ] Support multiple aggregation levels:
  - [ ] Trade level
  - [ ] Counterparty level
  - [ ] Sector level
  - [ ] Portfolio level
  - [ ] Firm-wide level
- [ ] Generate daily risk reports
- [ ] Support risk limit monitoring and alerts
- [ ] Provide risk concentration analysis
- [ ] Calculate VaR (Value at Risk) metrics
- [ ] Export risk reports (PDF, Excel, CSV)
- [ ] Support historical risk trend analysis

## Technical Details

### Database Schema
```sql
-- Portfolio-level risk metrics
CREATE TABLE portfolio_risk_metrics (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    portfolio_id BIGINT NOT NULL REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    
    -- Credit spread sensitivity (CS01)
    cs01 DECIMAL(20, 4), -- P&L impact of 1bp parallel spread move
    cs01_long DECIMAL(20, 4), -- CS01 for protection bought
    cs01_short DECIMAL(20, 4), -- CS01 for protection sold
    
    -- Interest rate sensitivity (IR01)
    ir01 DECIMAL(20, 4), -- P&L impact of 1bp IR move
    ir01_usd DECIMAL(20, 4),
    ir01_eur DECIMAL(20, 4),
    ir01_gbp DECIMAL(20, 4),
    
    -- Jump-to-default risk (JTD)
    jtd DECIMAL(20, 4), -- Loss if all names default
    jtd_long DECIMAL(20, 4),
    jtd_short DECIMAL(20, 4),
    
    -- Recovery rate sensitivity (REC01)
    rec01 DECIMAL(20, 4), -- P&L impact of 1% recovery change
    
    -- Notional exposures
    gross_notional DECIMAL(20, 4),
    net_notional DECIMAL(20, 4),
    long_notional DECIMAL(20, 4),
    short_notional DECIMAL(20, 4),
    
    currency VARCHAR(3) NOT NULL,
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(calculation_date, portfolio_id)
);

-- Counterparty-level risk aggregation
CREATE TABLE counterparty_risk_metrics (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    counterparty_id BIGINT NOT NULL REFERENCES counterparties(id),
    
    cs01 DECIMAL(20, 4),
    ir01 DECIMAL(20, 4),
    jtd DECIMAL(20, 4),
    rec01 DECIMAL(20, 4),
    
    gross_notional DECIMAL(20, 4),
    net_notional DECIMAL(20, 4),
    
    trade_count INTEGER,
    currency VARCHAR(3) NOT NULL,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(calculation_date, counterparty_id)
);

-- Sector-level risk aggregation
CREATE TABLE sector_risk_metrics (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    sector VARCHAR(100) NOT NULL, -- e.g., Financials, Technology, Energy
    
    cs01 DECIMAL(20, 4),
    ir01 DECIMAL(20, 4),
    jtd DECIMAL(20, 4),
    rec01 DECIMAL(20, 4),
    
    gross_notional DECIMAL(20, 4),
    net_notional DECIMAL(20, 4),
    
    reference_entity_count INTEGER,
    trade_count INTEGER,
    
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(calculation_date, sector)
);

-- Firm-wide risk summary
CREATE TABLE firm_risk_summary (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL UNIQUE,
    
    -- Aggregated sensitivities
    total_cs01 DECIMAL(20, 4),
    total_ir01 DECIMAL(20, 4),
    total_jtd DECIMAL(20, 4),
    total_rec01 DECIMAL(20, 4),
    
    -- Notional exposures
    total_gross_notional DECIMAL(20, 4),
    total_net_notional DECIMAL(20, 4),
    
    -- Risk metrics
    var_95 DECIMAL(20, 4), -- 1-day 95% VaR
    var_99 DECIMAL(20, 4), -- 1-day 99% VaR
    expected_shortfall DECIMAL(20, 4), -- CVaR
    
    -- Counts
    total_trade_count INTEGER,
    total_portfolio_count INTEGER,
    total_counterparty_count INTEGER,
    
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Risk concentration analysis
CREATE TABLE risk_concentration (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    concentration_type VARCHAR(50) NOT NULL, -- TOP_10_NAMES, TOP_5_SECTORS, etc.
    
    reference_entity_id BIGINT,
    reference_entity_name VARCHAR(255),
    sector VARCHAR(100),
    
    cs01 DECIMAL(20, 4),
    jtd DECIMAL(20, 4),
    gross_notional DECIMAL(20, 4),
    net_notional DECIMAL(20, 4),
    
    percentage_of_total DECIMAL(5, 2), -- e.g., 15.25% of total risk
    
    ranking INTEGER,
    currency VARCHAR(3) NOT NULL,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Risk limits and thresholds
CREATE TABLE risk_limits (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT REFERENCES cds_portfolios(id),
    counterparty_id BIGINT REFERENCES counterparties(id),
    sector VARCHAR(100),
    
    limit_type VARCHAR(50) NOT NULL, -- CS01, IR01, JTD, NOTIONAL
    limit_value DECIMAL(20, 4) NOT NULL,
    warning_threshold DECIMAL(20, 4), -- e.g., 80% of limit
    
    currency VARCHAR(3) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Risk limit breaches/alerts
CREATE TABLE risk_limit_breaches (
    id BIGSERIAL PRIMARY KEY,
    breach_date DATE NOT NULL,
    risk_limit_id BIGINT NOT NULL REFERENCES risk_limits(id),
    
    current_value DECIMAL(20, 4) NOT NULL,
    limit_value DECIMAL(20, 4) NOT NULL,
    breach_percentage DECIMAL(5, 2), -- e.g., 105.50%
    
    breach_severity VARCHAR(20), -- WARNING, BREACH, CRITICAL
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_portfolio_risk_date ON portfolio_risk_metrics(calculation_date);
CREATE INDEX idx_portfolio_risk_portfolio ON portfolio_risk_metrics(portfolio_id);
CREATE INDEX idx_counterparty_risk_date ON counterparty_risk_metrics(calculation_date);
CREATE INDEX idx_sector_risk_date ON sector_risk_metrics(calculation_date);
CREATE INDEX idx_firm_risk_date ON firm_risk_summary(calculation_date);
CREATE INDEX idx_risk_concentration_date ON risk_concentration(calculation_date);
CREATE INDEX idx_risk_breaches_date ON risk_limit_breaches(breach_date);
CREATE INDEX idx_risk_breaches_unresolved ON risk_limit_breaches(is_resolved) 
    WHERE is_resolved = FALSE;
```

### Java Models
```java
@Entity
@Table(name = "portfolio_risk_metrics")
public class PortfolioRiskMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private CdsPortfolio portfolio;
    
    // Credit spread sensitivity
    @Column(name = "cs01", precision = 20, scale = 4)
    private BigDecimal cs01;
    
    @Column(name = "cs01_long", precision = 20, scale = 4)
    private BigDecimal cs01Long;
    
    @Column(name = "cs01_short", precision = 20, scale = 4)
    private BigDecimal cs01Short;
    
    // Interest rate sensitivity
    @Column(name = "ir01", precision = 20, scale = 4)
    private BigDecimal ir01;
    
    // Jump-to-default risk
    @Column(name = "jtd", precision = 20, scale = 4)
    private BigDecimal jtd;
    
    @Column(name = "jtd_long", precision = 20, scale = 4)
    private BigDecimal jtdLong;
    
    @Column(name = "jtd_short", precision = 20, scale = 4)
    private BigDecimal jtdShort;
    
    // Recovery sensitivity
    @Column(name = "rec01", precision = 20, scale = 4)
    private BigDecimal rec01;
    
    // Notional exposures
    @Column(name = "gross_notional", precision = 20, scale = 4)
    private BigDecimal grossNotional;
    
    @Column(name = "net_notional", precision = 20, scale = 4)
    private BigDecimal netNotional;
    
    @Column(name = "long_notional", precision = 20, scale = 4)
    private BigDecimal longNotional;
    
    @Column(name = "short_notional", precision = 20, scale = 4)
    private BigDecimal shortNotional;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}

@Entity
@Table(name = "firm_risk_summary")
public class FirmRiskSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "calculation_date", nullable = false, unique = true)
    private LocalDate calculationDate;
    
    @Column(name = "total_cs01", precision = 20, scale = 4)
    private BigDecimal totalCs01;
    
    @Column(name = "total_ir01", precision = 20, scale = 4)
    private BigDecimal totalIr01;
    
    @Column(name = "total_jtd", precision = 20, scale = 4)
    private BigDecimal totalJtd;
    
    @Column(name = "var_95", precision = 20, scale = 4)
    private BigDecimal var95;
    
    @Column(name = "var_99", precision = 20, scale = 4)
    private BigDecimal var99;
    
    @Column(name = "total_trade_count")
    private Integer totalTradeCount;
}
```

### Service Implementation
```java
@Service
public class RiskAggregationService {
    
    @Autowired
    private TradeValuationSensitivityRepository sensitivityRepo;
    
    @Autowired
    private PortfolioRiskMetricsRepository portfolioRiskRepo;
    
    @Transactional
    public void aggregatePortfolioRisk(LocalDate date, Long portfolioId) {
        // Get all trade sensitivities for portfolio
        List<TradeValuationSensitivity> sensitivities = 
            sensitivityRepo.findByDateAndPortfolio(date, portfolioId);
        
        PortfolioRiskMetrics metrics = new PortfolioRiskMetrics();
        metrics.setCalculationDate(date);
        metrics.setPortfolioId(portfolioId);
        
        // Aggregate CS01
        BigDecimal totalCs01 = sensitivities.stream()
            .map(TradeValuationSensitivity::getCs01)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.setCs01(totalCs01);
        
        // Separate long/short CS01
        BigDecimal cs01Long = sensitivities.stream()
            .filter(s -> s.getTrade().getDirection() == Direction.BUY_PROTECTION)
            .map(TradeValuationSensitivity::getCs01)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.setCs01Long(cs01Long);
        
        BigDecimal cs01Short = sensitivities.stream()
            .filter(s -> s.getTrade().getDirection() == Direction.SELL_PROTECTION)
            .map(TradeValuationSensitivity::getCs01)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.setCs01Short(cs01Short);
        
        // Aggregate IR01
        BigDecimal totalIr01 = sensitivities.stream()
            .map(TradeValuationSensitivity::getIr01)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.setIr01(totalIr01);
        
        // Aggregate JTD (Jump-to-Default)
        BigDecimal totalJtd = sensitivities.stream()
            .map(TradeValuationSensitivity::getJtd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.setJtd(totalJtd);
        
        // Calculate notional exposures
        metrics.setGrossNotional(calculateGrossNotional(sensitivities));
        metrics.setNetNotional(calculateNetNotional(sensitivities));
        
        portfolioRiskRepo.save(metrics);
        
        log.info("Aggregated risk for portfolio {} on {}", portfolioId, date);
    }
    
    @Transactional
    public void aggregateFirmRisk(LocalDate date) {
        List<PortfolioRiskMetrics> allPortfolios = 
            portfolioRiskRepo.findByCalculationDate(date);
        
        FirmRiskSummary summary = new FirmRiskSummary();
        summary.setCalculationDate(date);
        
        // Sum across all portfolios
        BigDecimal firmCs01 = allPortfolios.stream()
            .map(PortfolioRiskMetrics::getCs01)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalCs01(firmCs01);
        
        BigDecimal firmIr01 = allPortfolios.stream()
            .map(PortfolioRiskMetrics::getIr01)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalIr01(firmIr01);
        
        BigDecimal firmJtd = allPortfolios.stream()
            .map(PortfolioRiskMetrics::getJtd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalJtd(firmJtd);
        
        // Calculate VaR (simplified - would use historical simulation or Monte Carlo)
        summary.setVar95(calculateVaR(allPortfolios, 0.95));
        summary.setVar99(calculateVaR(allPortfolios, 0.99));
        
        firmRiskSummaryRepo.save(summary);
    }
    
    @Transactional
    public void calculateRiskConcentration(LocalDate date) {
        // Find top 10 reference entities by JTD
        List<ReferenceEntityRisk> topEntities = 
            sensitivityRepo.findTopReferenceEntitiesByJtd(date, 10);
        
        BigDecimal totalJtd = getTotalFirmJtd(date);
        
        for (int i = 0; i < topEntities.size(); i++) {
            ReferenceEntityRisk entity = topEntities.get(i);
            
            RiskConcentration concentration = new RiskConcentration();
            concentration.setCalculationDate(date);
            concentration.setConcentrationType("TOP_10_NAMES");
            concentration.setReferenceEntityId(entity.getEntityId());
            concentration.setReferenceEntityName(entity.getEntityName());
            concentration.setJtd(entity.getJtd());
            concentration.setRanking(i + 1);
            
            // Calculate percentage of total
            BigDecimal percentage = entity.getJtd()
                .divide(totalJtd, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            concentration.setPercentageOfTotal(percentage);
            
            riskConcentrationRepo.save(concentration);
        }
    }
    
    public void checkRiskLimits(LocalDate date) {
        List<RiskLimit> activeLimits = riskLimitRepo.findByIsActiveTrue();
        
        for (RiskLimit limit : activeLimits) {
            BigDecimal currentValue = getCurrentRiskValue(date, limit);
            
            if (currentValue.compareTo(limit.getLimitValue()) > 0) {
                // Limit breached
                createRiskBreach(date, limit, currentValue, "BREACH");
            } else if (limit.getWarningThreshold() != null &&
                       currentValue.compareTo(limit.getWarningThreshold()) > 0) {
                // Warning threshold breached
                createRiskBreach(date, limit, currentValue, "WARNING");
            }
        }
    }
}
```

### REST API
```java
@RestController
@RequestMapping("/api/risk")
public class RiskReportingController {
    
    @GetMapping("/portfolio/{portfolioId}/metrics/{date}")
    public ResponseEntity<PortfolioRiskMetrics> getPortfolioRisk(
        @PathVariable Long portfolioId,
        @PathVariable LocalDate date
    );
    
    @GetMapping("/firm/summary/{date}")
    public ResponseEntity<FirmRiskSummary> getFirmRiskSummary(
        @PathVariable LocalDate date
    );
    
    @GetMapping("/concentration/{date}")
    public ResponseEntity<List<RiskConcentration>> getRiskConcentration(
        @PathVariable LocalDate date,
        @RequestParam(defaultValue = "TOP_10_NAMES") String type
    );
    
    @GetMapping("/counterparty/{counterpartyId}/metrics/{date}")
    public ResponseEntity<CounterpartyRiskMetrics> getCounterpartyRisk(
        @PathVariable Long counterpartyId,
        @PathVariable LocalDate date
    );
    
    @GetMapping("/sector/{sector}/metrics/{date}")
    public ResponseEntity<SectorRiskMetrics> getSectorRisk(
        @PathVariable String sector,
        @PathVariable LocalDate date
    );
    
    @GetMapping("/limits/breaches")
    public ResponseEntity<List<RiskLimitBreach>> getUnresolvedBreaches();
    
    @GetMapping("/report/{date}/export")
    public ResponseEntity<byte[]> exportRiskReport(
        @PathVariable LocalDate date,
        @RequestParam(defaultValue = "PDF") String format
    );
}
```

## Test Scenarios
1. **Portfolio Aggregation**: Aggregate risk for 100-trade portfolio
2. **Firm-wide Summary**: Calculate firm-level metrics across 5 portfolios
3. **Risk Concentration**: Identify top 10 reference entities by JTD
4. **Sector Aggregation**: Aggregate risk by industry sector
5. **Limit Breach Detection**: Detect CS01 limit breach
6. **VaR Calculation**: Calculate 95% and 99% VaR
7. **Report Export**: Generate PDF risk report

## Definition of Done
- [ ] Code implemented and reviewed
- [ ] Database migration scripts created
- [ ] Unit tests written (>80% coverage)
- [ ] Integration tests for aggregations
- [ ] VaR calculation validated
- [ ] Risk report templates created
- [ ] Performance tests (firm-wide aggregation < 1 minute)
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] QA sign-off

## Dependencies
- Story 16.3: NPV Calculation (for sensitivities)
- Story 16.5: Valuation Storage

## Effort Estimate
**13 story points** (2 weeks)

## Notes
- Risk aggregation runs after EOD valuation completes
- Consider using materialized views for performance
- Implement risk limit alert notifications (email/Slack)
- VaR calculation could use historical simulation or Monte Carlo
- Support drill-down from firm → portfolio → trade
- Consider real-time risk updates during trading day
- Implement risk dashboard with visualization
