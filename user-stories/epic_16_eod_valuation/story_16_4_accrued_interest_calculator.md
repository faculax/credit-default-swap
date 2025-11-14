# Story 16.4: Accrued Interest Calculator

## Story
**As a** risk manager  
**I want** accurate accrued interest calculations for CDS trades  
**So that** the total trade value includes accumulated but unpaid premium

## Acceptance Criteria
- [ ] Calculate accrued premium for CDS trades as of valuation date
- [ ] Accrual calculation accounts for:
  - [ ] Premium frequency (quarterly, semi-annual, annual)
  - [ ] Day count convention (ACT/360, ACT/365, 30/360)
  - [ ] Last premium payment date
  - [ ] Next premium payment date
  - [ ] Trade effective date
- [ ] Support various CDS contract types (single-name, index, basket)
- [ ] Handle edge cases:
  - [ ] Trade inception (no prior payment)
  - [ ] Trade close to maturity
  - [ ] Weekend/holiday adjustments
  - [ ] Stub periods (short first/last periods)
- [ ] Batch processing for portfolio-level calculations
- [ ] Performance: Calculate accrued for 1000 trades in < 1 minute
- [ ] Validation against expected accrual ranges

## Technical Details

### Accrual Calculation Formula
```
Accrued Interest = Notional × Spread × Day Count Fraction

Where:
- Day Count Fraction = Days Accrued / Days in Year (per convention)
- Days Accrued = Valuation Date - Last Payment Date (or Effective Date)
- Days in Year = 360 or 365 (depending on convention)
```

### Java Implementation
```java
@Service
public class AccruedInterestService {
    
    @Autowired
    private CouponScheduleService couponScheduleService;
    
    /**
     * Calculate accrued interest for a single trade
     */
    public AccruedInterestResult calculateAccruedInterest(
        CDSTrade trade, LocalDate valuationDate
    ) {
        AccruedInterestResult result = new AccruedInterestResult();
        result.setTradeId(trade.getId());
        result.setValuationDate(valuationDate);
        
        try {
            // Get coupon schedule
            List<CouponPeriod> couponPeriods = couponScheduleService
                .generateCouponSchedule(trade);
            
            // Find current accrual period
            CouponPeriod currentPeriod = findCurrentAccrualPeriod(
                couponPeriods, valuationDate
            );
            
            if (currentPeriod == null) {
                // Trade not yet effective or already matured
                result.setAccruedAmount(BigDecimal.ZERO);
                result.setAccrualDays(0);
                return result;
            }
            
            // Calculate days accrued
            LocalDate accrualStartDate = currentPeriod.getPeriodStart();
            int daysAccrued = calculateDaysAccrued(
                accrualStartDate, valuationDate, trade.getDayCountConvention()
            );
            
            // Calculate accrued amount
            BigDecimal notional = trade.getNotionalAmount();
            BigDecimal spread = trade.getSpread();
            
            // Day count fraction
            double dayCountFraction = calculateDayCountFraction(
                accrualStartDate, 
                valuationDate,
                trade.getDayCountConvention()
            );
            
            // Accrued = Notional × Spread × Day Count Fraction
            BigDecimal accruedAmount = notional
                .multiply(spread)
                .multiply(BigDecimal.valueOf(dayCountFraction))
                .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP); // spread in bps
            
            result.setAccruedAmount(accruedAmount);
            result.setAccrualDays(daysAccrued);
            result.setAccrualStartDate(accrualStartDate);
            result.setAccrualEndDate(currentPeriod.getPeriodEnd());
            result.setDayCountFraction(BigDecimal.valueOf(dayCountFraction));
            result.setStatus(AccrualStatus.SUCCESS);
            
        } catch (Exception e) {
            log.error("Accrued interest calculation failed for trade {}: {}", 
                     trade.getId(), e.getMessage());
            result.setStatus(AccrualStatus.FAILED);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Batch calculate accrued interest for multiple trades
     */
    public Map<Long, AccruedInterestResult> calculateAccruedBatch(
        List<CDSTrade> trades, LocalDate valuationDate
    ) {
        Map<Long, AccruedInterestResult> results = new ConcurrentHashMap<>();
        
        trades.parallelStream().forEach(trade -> {
            try {
                AccruedInterestResult result = calculateAccruedInterest(
                    trade, valuationDate
                );
                results.put(trade.getId(), result);
            } catch (Exception e) {
                log.error("Failed to calculate accrued for trade {}: {}", 
                         trade.getId(), e.getMessage());
            }
        });
        
        return results;
    }
    
    private CouponPeriod findCurrentAccrualPeriod(
        List<CouponPeriod> periods, LocalDate valuationDate
    ) {
        return periods.stream()
            .filter(period -> 
                !valuationDate.isBefore(period.getPeriodStart()) &&
                !valuationDate.isAfter(period.getPeriodEnd())
            )
            .findFirst()
            .orElse(null);
    }
    
    private int calculateDaysAccrued(
        LocalDate startDate, LocalDate valuationDate, String dayCountConvention
    ) {
        switch (dayCountConvention.toUpperCase()) {
            case "ACT/360":
            case "ACT/365":
            case "ACT/ACT":
                return (int) ChronoUnit.DAYS.between(startDate, valuationDate);
                
            case "30/360":
                return calculate30_360Days(startDate, valuationDate);
                
            default:
                throw new IllegalArgumentException(
                    "Unsupported day count convention: " + dayCountConvention
                );
        }
    }
    
    private double calculateDayCountFraction(
        LocalDate startDate, LocalDate endDate, String dayCountConvention
    ) {
        int daysAccrued = calculateDaysAccrued(startDate, endDate, dayCountConvention);
        
        switch (dayCountConvention.toUpperCase()) {
            case "ACT/360":
                return daysAccrued / 360.0;
                
            case "ACT/365":
                return daysAccrued / 365.0;
                
            case "ACT/ACT":
                // Actual days in the year containing the period
                int daysInYear = Year.of(endDate.getYear()).length();
                return daysAccrued / (double) daysInYear;
                
            case "30/360":
                return daysAccrued / 360.0;
                
            default:
                throw new IllegalArgumentException(
                    "Unsupported day count convention: " + dayCountConvention
                );
        }
    }
    
    private int calculate30_360Days(LocalDate startDate, LocalDate endDate) {
        int d1 = Math.min(startDate.getDayOfMonth(), 30);
        int d2 = Math.min(endDate.getDayOfMonth(), 30);
        
        if (d1 == 31) d1 = 30;
        if (d2 == 31 && d1 >= 30) d2 = 30;
        
        return 360 * (endDate.getYear() - startDate.getYear()) +
               30 * (endDate.getMonthValue() - startDate.getMonthValue()) +
               (d2 - d1);
    }
}

// Result DTO
@Data
public class AccruedInterestResult {
    private Long tradeId;
    private LocalDate valuationDate;
    private BigDecimal accruedAmount;
    private Integer accrualDays;
    private LocalDate accrualStartDate;
    private LocalDate accrualEndDate;
    private BigDecimal dayCountFraction;
    private AccrualStatus status;
    private String errorMessage;
    
    public enum AccrualStatus {
        SUCCESS, FAILED
    }
}
```

### Database Schema
```sql
-- Accrued interest results (stored with valuations)
CREATE TABLE trade_accrued_interest (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    accrued_amount DECIMAL(20, 4) NOT NULL,
    accrual_days INTEGER NOT NULL,
    accrual_start_date DATE NOT NULL,
    accrual_end_date DATE NOT NULL,
    day_count_fraction DECIMAL(12, 10),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(valuation_date, trade_id)
);

CREATE INDEX idx_accrued_date ON trade_accrued_interest(valuation_date);
CREATE INDEX idx_accrued_trade ON trade_accrued_interest(trade_id);
```

### REST API
```java
@RestController
@RequestMapping("/api/accrued-interest")
public class AccruedInterestController {
    
    @GetMapping("/trades/{tradeId}")
    public ResponseEntity<AccruedInterestResult> calculateAccrued(
        @PathVariable Long tradeId,
        @RequestParam LocalDate valuationDate
    );
    
    @PostMapping("/portfolio/{portfolioId}")
    public ResponseEntity<Map<Long, AccruedInterestResult>> calculatePortfolioAccrued(
        @PathVariable Long portfolioId,
        @RequestParam LocalDate valuationDate
    );
    
    @GetMapping("/trades/{tradeId}/history")
    public ResponseEntity<List<TradeAccruedInterest>> getAccruedHistory(
        @PathVariable Long tradeId,
        @RequestParam LocalDate fromDate,
        @RequestParam LocalDate toDate
    );
}
```

## Test Scenarios

### 1. **Standard Quarterly CDS**
```
Trade Details:
- Notional: $10,000,000
- Spread: 100 bps (0.01)
- Premium Frequency: Quarterly
- Day Count: ACT/360
- Last Payment: Jan 20, 2025
- Valuation Date: Feb 15, 2025

Calculation:
- Days Accrued: 26 days (Jan 20 to Feb 15)
- Day Count Fraction: 26/360 = 0.07222
- Accrued = $10,000,000 × 0.01 × 0.07222 = $7,222.22
```

### 2. **New Trade (No Prior Payment)**
```
Trade Details:
- Effective Date: Feb 1, 2025
- Valuation Date: Feb 15, 2025
- Days Accrued: 14 days
```

### 3. **30/360 Day Count**
```
Trade Details:
- Day Count: 30/360
- Start: Jan 31, 2025
- End: Feb 28, 2025
- Days Accrued: 30 days (treated as full month)
```

### 4. **Leap Year (ACT/ACT)**
```
Trade Details:
- Day Count: ACT/ACT
- Year: 2024 (leap year, 366 days)
- Day Count Fraction adjusts to 366
```

### 5. **Matured Trade**
```
Trade Details:
- Maturity Date: Jan 31, 2025
- Valuation Date: Feb 15, 2025
- Accrued = $0 (no accrual after maturity)
```

## Definition of Done
- [ ] Code implemented and reviewed
- [ ] Database migration scripts created
- [ ] Unit tests written (>80% coverage)
- [ ] Test scenarios validated (all 5 cases)
- [ ] Performance test (1000 trades < 1 minute)
- [ ] Integration with coupon schedule service
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] QA sign-off

## Dependencies
- Coupon schedule service (already exists)
- Day count convention utilities

## Effort Estimate
**5 story points** (1 week)

## Notes
- Consider pre-calculating accruals for frequently queried trades
- Validate against Bloomberg/Markit accrual calculations
- Edge cases around holidays need special handling
- Some CCPs use different accrual conventions (verify with ops team)
- Accrued interest is typically displayed separately from NPV
