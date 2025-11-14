package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.eod.TradeAccruedInterest;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.eod.TradeAccruedInterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for calculating accrued interest on CDS trades
 * 
 * Supports multiple day count conventions:
 * - ACT/360: Actual days / 360
 * - ACT/365: Actual days / 365
 * - ACT/ACT: Actual days / Actual days in year
 * - 30/360: 30-day months / 360-day year
 * 
 * Formula: Accrued Interest = Notional × Spread × Day Count Fraction
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccruedInterestService {
    
    private final TradeAccruedInterestRepository accruedRepository;
    private final CDSTradeRepository tradeRepository;
    
    /**
     * Calculate accrued interest for a single trade
     */
    @Transactional
    public TradeAccruedInterest calculateAccruedInterest(Long tradeId, LocalDate calculationDate, String jobId) {
        CDSTrade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));
        
        try {
            // Find the accrual period (last coupon payment to calculation date)
            LocalDate accrualStartDate = findLastCouponDate(trade, calculationDate);
            LocalDate accrualEndDate = calculationDate;
            
            // Check if trade has matured
            if (calculationDate.isAfter(trade.getMaturityDate())) {
                log.debug("Trade {} has matured, no accrued interest", tradeId);
                return createZeroAccrued(trade, calculationDate, jobId);
            }
            
            // Calculate day count fraction
            DayCountResult dayCount = calculateDayCountFraction(
                accrualStartDate,
                accrualEndDate,
                trade.getDayCountConvention()
            );
            
            // Calculate accrued interest: Notional × Spread × Day Count Fraction
            BigDecimal accruedInterest = trade.getNotionalAmount()
                .multiply(trade.getSpread())
                .multiply(dayCount.fraction)
                .setScale(4, RoundingMode.HALF_UP);
            
            // Create record
            TradeAccruedInterest accrued = TradeAccruedInterest.builder()
                .calculationDate(calculationDate)
                .trade(trade)
                .accruedInterest(accruedInterest)
                .accrualDays(dayCount.numerator)
                .notionalAmount(trade.getNotionalAmount())
                .spread(trade.getSpread())
                .dayCountConvention(trade.getDayCountConvention())
                .accrualStartDate(accrualStartDate)
                .accrualEndDate(accrualEndDate)
                .numeratorDays(dayCount.numerator)
                .denominatorDays(dayCount.denominator)
                .dayCountFraction(dayCount.fraction)
                .currency(trade.getCurrency())
                .calculationStatus(TradeAccruedInterest.CalculationStatus.SUCCESS)
                .jobId(jobId)
                .build();
            
            accrued = accruedRepository.save(accrued);
            
            log.debug("Calculated accrued interest for trade {}: {}", tradeId, accruedInterest);
            
            return accrued;
            
        } catch (Exception e) {
            log.error("Error calculating accrued interest for trade {}: {}", tradeId, e.getMessage(), e);
            
            TradeAccruedInterest failedAccrued = TradeAccruedInterest.builder()
                .calculationDate(calculationDate)
                .trade(trade)
                .accruedInterest(BigDecimal.ZERO)
                .accrualDays(0)
                .notionalAmount(trade.getNotionalAmount())
                .spread(trade.getSpread())
                .dayCountConvention(trade.getDayCountConvention())
                .accrualStartDate(calculationDate)
                .accrualEndDate(calculationDate)
                .numeratorDays(0)
                .denominatorDays(1)
                .dayCountFraction(BigDecimal.ZERO)
                .currency(trade.getCurrency())
                .calculationStatus(TradeAccruedInterest.CalculationStatus.FAILED)
                .errorMessage(e.getMessage())
                .jobId(jobId)
                .build();
            
            return accruedRepository.save(failedAccrued);
        }
    }
    
    /**
     * Calculate accrued interest for multiple trades in batch
     */
    @Transactional
    public List<TradeAccruedInterest> calculateAccruedBatch(List<Long> tradeIds, LocalDate calculationDate, String jobId) {
        log.info("Starting batch accrued interest calculation for {} trades on {}", 
            tradeIds.size(), calculationDate);
        
        List<TradeAccruedInterest> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (Long tradeId : tradeIds) {
            try {
                TradeAccruedInterest accrued = calculateAccruedInterest(tradeId, calculationDate, jobId);
                results.add(accrued);
                
                if (accrued.getCalculationStatus() == TradeAccruedInterest.CalculationStatus.SUCCESS) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("Failed to calculate accrued for trade {}: {}", tradeId, e.getMessage());
                failCount++;
            }
        }
        
        log.info("Batch accrued calculation completed: {} successful, {} failed", successCount, failCount);
        
        return results;
    }
    
    /**
     * Calculate day count fraction based on convention
     */
    public DayCountResult calculateDayCountFraction(LocalDate startDate, LocalDate endDate, String convention) {
        switch (convention.toUpperCase()) {
            case "ACT/360":
                return calculateAct360(startDate, endDate);
            case "ACT/365":
                return calculateAct365(startDate, endDate);
            case "ACT/ACT":
                return calculateActAct(startDate, endDate);
            case "30/360":
            case "30/360 US":
                return calculate30_360(startDate, endDate);
            default:
                log.warn("Unknown day count convention {}, defaulting to ACT/360", convention);
                return calculateAct360(startDate, endDate);
        }
    }
    
    /**
     * ACT/360: Actual days / 360
     */
    private DayCountResult calculateAct360(LocalDate startDate, LocalDate endDate) {
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal fraction = BigDecimal.valueOf(days)
            .divide(BigDecimal.valueOf(360), 8, RoundingMode.HALF_UP);
        return new DayCountResult(days, 360, fraction);
    }
    
    /**
     * ACT/365: Actual days / 365
     */
    private DayCountResult calculateAct365(LocalDate startDate, LocalDate endDate) {
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal fraction = BigDecimal.valueOf(days)
            .divide(BigDecimal.valueOf(365), 8, RoundingMode.HALF_UP);
        return new DayCountResult(days, 365, fraction);
    }
    
    /**
     * ACT/ACT: Actual days / Actual days in year
     */
    private DayCountResult calculateActAct(LocalDate startDate, LocalDate endDate) {
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate);
        
        // Use actual days in the year containing the end date
        int daysInYear = endDate.isLeapYear() ? 366 : 365;
        
        BigDecimal fraction = BigDecimal.valueOf(days)
            .divide(BigDecimal.valueOf(daysInYear), 8, RoundingMode.HALF_UP);
        
        return new DayCountResult(days, daysInYear, fraction);
    }
    
    /**
     * 30/360: 30-day months, 360-day year
     * Uses US (NASD) convention
     */
    private DayCountResult calculate30_360(LocalDate startDate, LocalDate endDate) {
        int d1 = Math.min(startDate.getDayOfMonth(), 30);
        int d2 = endDate.getDayOfMonth();
        
        // Adjust d2 if d1 is 30 or 31
        if (d1 == 30 && d2 == 31) {
            d2 = 30;
        }
        
        int days = 360 * (endDate.getYear() - startDate.getYear())
                 + 30 * (endDate.getMonthValue() - startDate.getMonthValue())
                 + (d2 - d1);
        
        BigDecimal fraction = BigDecimal.valueOf(days)
            .divide(BigDecimal.valueOf(360), 8, RoundingMode.HALF_UP);
        
        return new DayCountResult(days, 360, fraction);
    }
    
    /**
     * Find the last coupon payment date before calculation date
     */
    private LocalDate findLastCouponDate(CDSTrade trade, LocalDate calculationDate) {
        // Get premium frequency (QUARTERLY, SEMI_ANNUAL, ANNUAL)
        String frequency = trade.getPremiumFrequency();
        
        LocalDate lastCouponDate = trade.getEffectiveDate();
        
        // Determine period length in months
        int monthsPerPeriod;
        switch (frequency.toUpperCase()) {
            case "QUARTERLY":
            case "3M":
                monthsPerPeriod = 3;
                break;
            case "SEMI_ANNUAL":
            case "SEMI-ANNUAL":
            case "6M":
                monthsPerPeriod = 6;
                break;
            case "ANNUAL":
            case "12M":
            case "1Y":
                monthsPerPeriod = 12;
                break;
            default:
                log.warn("Unknown frequency {}, defaulting to quarterly", frequency);
                monthsPerPeriod = 3;
        }
        
        // Move forward from effective date to find last coupon before calculation date
        LocalDate nextCouponDate = lastCouponDate;
        while (nextCouponDate.plusMonths(monthsPerPeriod).isBefore(calculationDate) ||
               nextCouponDate.plusMonths(monthsPerPeriod).isEqual(calculationDate)) {
            nextCouponDate = nextCouponDate.plusMonths(monthsPerPeriod);
        }
        
        return nextCouponDate;
    }
    
    /**
     * Create zero accrued interest record (for matured trades)
     */
    private TradeAccruedInterest createZeroAccrued(CDSTrade trade, LocalDate calculationDate, String jobId) {
        return TradeAccruedInterest.builder()
            .calculationDate(calculationDate)
            .trade(trade)
            .accruedInterest(BigDecimal.ZERO)
            .accrualDays(0)
            .notionalAmount(trade.getNotionalAmount())
            .spread(trade.getSpread())
            .dayCountConvention(trade.getDayCountConvention())
            .accrualStartDate(calculationDate)
            .accrualEndDate(calculationDate)
            .numeratorDays(0)
            .denominatorDays(1)
            .dayCountFraction(BigDecimal.ZERO)
            .currency(trade.getCurrency())
            .calculationStatus(TradeAccruedInterest.CalculationStatus.SUCCESS)
            .jobId(jobId)
            .build();
    }
    
    /**
     * Get accrued interest for a trade on a specific date
     */
    public Optional<TradeAccruedInterest> getAccruedInterest(Long tradeId, LocalDate calculationDate) {
        return accruedRepository.findByCalculationDateAndTradeId(calculationDate, tradeId);
    }
    
    /**
     * Get all accrued interest calculations for a specific date
     */
    public List<TradeAccruedInterest> getAccruedByDate(LocalDate calculationDate) {
        return accruedRepository.findByCalculationDate(calculationDate);
    }
    
    /**
     * Get accrued interest history for a trade
     */
    public List<TradeAccruedInterest> getAccruedHistory(Long tradeId) {
        return accruedRepository.findByTradeIdOrderByCalculationDateDesc(tradeId);
    }
    
    /**
     * Get latest accrued interest for a trade
     */
    public Optional<TradeAccruedInterest> getLatestAccrued(Long tradeId) {
        return accruedRepository.findFirstByTradeIdOrderByCalculationDateDesc(tradeId);
    }
    
    /**
     * Helper class for day count calculation results
     */
    public static class DayCountResult {
        public final int numerator;
        public final int denominator;
        public final BigDecimal fraction;
        
        public DayCountResult(int numerator, int denominator, BigDecimal fraction) {
            this.numerator = numerator;
            this.denominator = denominator;
            this.fraction = fraction;
        }
    }
}
