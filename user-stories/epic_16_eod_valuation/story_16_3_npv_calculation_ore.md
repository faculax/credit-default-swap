# Story 16.3: NPV Calculation via ORE Integration

## Story
**As a** risk analyst  
**I want** NPV calculations for all CDS trades using ORE  
**So that** valuations reflect accurate market pricing

## Acceptance Criteria
- [ ] ORE integration calculates NPV for individual CDS trades
- [ ] Batch processing capability for portfolio-level valuations
- [ ] Valuation uses market data from EOD snapshot
- [ ] NPV broken down into components:
  - [ ] Premium leg PV (present value of premium payments)
  - [ ] Protection leg PV (present value of protection)
  - [ ] Net NPV (protection PV - premium PV)
- [ ] Support for various CDS contract types:
  - [ ] Single-name CDS
  - [ ] Index CDS
  - [ ] Basket CDS
- [ ] Handle trade lifecycle states (active, matured, defaulted)
- [ ] Performance: Value 1000 trades in < 5 minutes
- [ ] Error handling for failed valuations (mark as failed, continue batch)
- [ ] Valuation results include calculation metadata

## Technical Details

### Database Schema
```sql
-- Trade valuations (NPV results)
CREATE TABLE trade_valuations (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    npv DECIMAL(20, 4) NOT NULL,
    premium_leg_pv DECIMAL(20, 4),
    protection_leg_pv DECIMAL(20, 4),
    currency VARCHAR(3) NOT NULL,
    valuation_status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, STALE_DATA
    calculation_time_ms INTEGER,
    error_message TEXT,
    ore_version VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(valuation_date, trade_id)
);

-- Valuation sensitivities (risk metrics)
CREATE TABLE trade_valuation_sensitivities (
    id BIGSERIAL PRIMARY KEY,
    valuation_id BIGINT NOT NULL REFERENCES trade_valuations(id) ON DELETE CASCADE,
    risk_metric VARCHAR(20) NOT NULL, -- CS01, IR01, JTD, REC01
    sensitivity_value DECIMAL(20, 4) NOT NULL,
    unit VARCHAR(10) -- USD, BPS, etc.
);

-- Create indexes
CREATE INDEX idx_valuations_date_trade ON trade_valuations(valuation_date, trade_id);
CREATE INDEX idx_valuations_date ON trade_valuations(valuation_date);
CREATE INDEX idx_valuations_status ON trade_valuations(valuation_status);
CREATE INDEX idx_sensitivities_valuation ON trade_valuation_sensitivities(valuation_id);
```

### Java Models
```java
@Entity
@Table(name = "trade_valuations")
public class TradeValuation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private CDSTrade trade;
    
    @Column(name = "npv", nullable = false, precision = 20, scale = 4)
    private BigDecimal npv;
    
    @Column(name = "premium_leg_pv", precision = 20, scale = 4)
    private BigDecimal premiumLegPv;
    
    @Column(name = "protection_leg_pv", precision = 20, scale = 4)
    private BigDecimal protectionLegPv;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "valuation_status", nullable = false)
    private ValuationStatus valuationStatus;
    
    @Column(name = "calculation_time_ms")
    private Integer calculationTimeMs;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "ore_version")
    private String oreVersion;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "valuation", cascade = CascadeType.ALL)
    private List<TradeValuationSensitivity> sensitivities = new ArrayList<>();
    
    public enum ValuationStatus {
        SUCCESS, FAILED, STALE_DATA
    }
}

@Entity
@Table(name = "trade_valuation_sensitivities")
public class TradeValuationSensitivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valuation_id", nullable = false)
    private TradeValuation valuation;
    
    @Column(name = "risk_metric", nullable = false)
    private String riskMetric; // CS01, IR01, JTD, REC01
    
    @Column(name = "sensitivity_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal sensitivityValue;
    
    @Column(name = "unit")
    private String unit;
}
```

### Service Implementation
```java
@Service
public class OreValuationService {
    
    @Autowired
    private RiskCalculationService riskCalculationService; // Existing ORE service
    
    @Autowired
    private MarketDataSnapshotService snapshotService;
    
    @Autowired
    private TradeValuationRepository valuationRepository;
    
    /**
     * Calculate NPV for a single trade
     */
    public TradeValuation calculateNPV(CDSTrade trade, LocalDate valuationDate) {
        long startTime = System.currentTimeMillis();
        
        TradeValuation valuation = new TradeValuation();
        valuation.setTrade(trade);
        valuation.setValuationDate(valuationDate);
        valuation.setCurrency(trade.getCurrency());
        
        try {
            // Get market data snapshot
            MarketDataSnapshot snapshot = snapshotService.getSnapshotByDate(valuationDate);
            
            // Prepare ORE input
            OreValuationRequest oreRequest = buildOreRequest(trade, snapshot, valuationDate);
            
            // Call ORE for NPV calculation
            RiskMeasures riskMeasures = riskCalculationService.calculateRisk(
                trade.getId(), valuationDate
            );
            
            // Extract NPV components
            valuation.setNpv(riskMeasures.getNpv());
            valuation.setPremiumLegPv(riskMeasures.getPremiumLegPv());
            valuation.setProtectionLegPv(riskMeasures.getProtectionLegPv());
            valuation.setValuationStatus(ValuationStatus.SUCCESS);
            
            // Add sensitivities
            addSensitivities(valuation, riskMeasures);
            
        } catch (Exception e) {
            log.error("NPV calculation failed for trade {}: {}", 
                     trade.getId(), e.getMessage());
            valuation.setValuationStatus(ValuationStatus.FAILED);
            valuation.setErrorMessage(e.getMessage());
        } finally {
            valuation.setCalculationTimeMs(
                (int)(System.currentTimeMillis() - startTime)
            );
            valuation.setOreVersion(getOreVersion());
        }
        
        return valuationRepository.save(valuation);
    }
    
    /**
     * Batch calculate NPVs for multiple trades
     */
    @Transactional
    public Map<Long, TradeValuation> calculateNPVBatch(
        List<CDSTrade> trades, LocalDate valuationDate
    ) {
        Map<Long, TradeValuation> results = new ConcurrentHashMap<>();
        
        // Process in parallel batches
        int batchSize = 100;
        List<List<CDSTrade>> batches = partition(trades, batchSize);
        
        batches.parallelStream().forEach(batch -> {
            batch.forEach(trade -> {
                try {
                    TradeValuation valuation = calculateNPV(trade, valuationDate);
                    results.put(trade.getId(), valuation);
                } catch (Exception e) {
                    log.error("Failed to value trade {}: {}", 
                             trade.getId(), e.getMessage());
                }
            });
        });
        
        return results;
    }
    
    private OreValuationRequest buildOreRequest(
        CDSTrade trade, MarketDataSnapshot snapshot, LocalDate valuationDate
    ) {
        // Build ORE XML input with trade details and market data
        OreValuationRequest request = new OreValuationRequest();
        request.setValuationDate(valuationDate);
        
        // Trade portfolio
        request.addTrade(convertToOreTrade(trade));
        
        // Market data
        request.setMarketData(convertToOreMarketData(snapshot));
        
        return request;
    }
    
    private void addSensitivities(TradeValuation valuation, RiskMeasures riskMeasures) {
        // CS01
        if (riskMeasures.getCs01() != null) {
            TradeValuationSensitivity cs01 = new TradeValuationSensitivity();
            cs01.setValuation(valuation);
            cs01.setRiskMetric("CS01");
            cs01.setSensitivityValue(riskMeasures.getCs01());
            cs01.setUnit("USD");
            valuation.getSensitivities().add(cs01);
        }
        
        // IR01 (DV01)
        if (riskMeasures.getDv01() != null) {
            TradeValuationSensitivity ir01 = new TradeValuationSensitivity();
            ir01.setValuation(valuation);
            ir01.setRiskMetric("IR01");
            ir01.setSensitivityValue(riskMeasures.getDv01());
            ir01.setUnit("USD");
            valuation.getSensitivities().add(ir01);
        }
        
        // Jump to Default
        if (riskMeasures.getJumpToDefault() != null) {
            TradeValuationSensitivity jtd = new TradeValuationSensitivity();
            jtd.setValuation(valuation);
            jtd.setRiskMetric("JTD");
            jtd.setSensitivityValue(riskMeasures.getJumpToDefault());
            jtd.setUnit("USD");
            valuation.getSensitivities().add(jtd);
        }
    }
    
    private <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
}
```

### REST API
```java
@RestController
@RequestMapping("/api/valuations")
public class ValuationController {
    
    @PostMapping("/trades/{tradeId}/npv")
    public ResponseEntity<TradeValuation> calculateNPV(
        @PathVariable Long tradeId,
        @RequestParam LocalDate valuationDate
    );
    
    @PostMapping("/portfolio/npv")
    public ResponseEntity<Map<Long, TradeValuation>> calculatePortfolioNPV(
        @RequestParam Long portfolioId,
        @RequestParam LocalDate valuationDate
    );
    
    @GetMapping("/trades/{tradeId}")
    public ResponseEntity<List<TradeValuation>> getTradeValuationHistory(
        @PathVariable Long tradeId,
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate
    );
    
    @GetMapping("/{valuationDate}")
    public ResponseEntity<List<TradeValuation>> getValuationsByDate(
        @PathVariable LocalDate valuationDate
    );
}
```

## Test Scenarios
1. **Single Trade NPV**: Calculate NPV for one CDS trade
2. **Batch Calculation**: Value 500 trades in parallel
3. **Missing Market Data**: Handle gracefully, mark as STALE_DATA
4. **Failed Calculation**: ORE error captured, status = FAILED
5. **Performance Test**: 1000 trades valued in < 5 minutes
6. **Sensitivity Calculation**: CS01, IR01, JTD captured correctly
7. **Matured Trade**: Trade past maturity, NPV should be zero
8. **Index CDS**: Valuation works for index products

## Definition of Done
- [ ] Code implemented and reviewed
- [ ] Database migration scripts created
- [ ] Unit tests written (>80% coverage)
- [ ] Integration tests with ORE
- [ ] Performance tests (1000 trades benchmark)
- [ ] Error handling tested
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] QA sign-off

## Dependencies
- Story 16.1: Market Data Snapshot Service
- Epic 7: ORE integration (already exists)
- Trade repository

## Effort Estimate
**13 story points** (2-3 weeks)

## Notes
- Reuse existing ORE integration from Epic 7
- Consider caching ORE results for frequently valued trades
- Monitor ORE performance and optimize batch sizes
- May need to upgrade ORE version for better performance
- Consider using async processing for large portfolios
