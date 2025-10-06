package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.CouponPeriod;
import com.creditdefaultswap.platform.repository.CouponPeriodRepository;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating and managing IMM coupon schedules for CDS trades.
 * Implements standard IMM dates (20th of March, June, September, December)
 * with ACT/360 day count convention.
 */
@Service
@Transactional
public class CouponScheduleService {

    @Autowired
    private CouponPeriodRepository couponPeriodRepository;

    @Autowired
    private CDSTradeRepository cdsTradeRepository;

    /**
     * Generate IMM-based coupon schedule for a CDS trade.
     * Respects the trade's premium frequency (QUARTERLY, SEMI_ANNUAL, ANNUAL, MONTHLY).
     * 
     * @param tradeId The trade ID
     * @return List of generated coupon periods
     */
    public List<CouponPeriod> generateImmSchedule(Long tradeId) {
        CDSTrade trade = cdsTradeRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));

        // Check if schedule already exists
        List<CouponPeriod> existingPeriods = couponPeriodRepository.findByTradeIdOrderByPeriodStartDate(tradeId);
        if (!existingPeriods.isEmpty()) {
            return existingPeriods;
        }

        LocalDate startDate = trade.getTradeDate();
        LocalDate maturityDate = trade.getMaturityDate();
        BigDecimal notional = trade.getNotionalAmount();
        String frequency = trade.getPremiumFrequency();

        List<CouponPeriod> periods = new ArrayList<>();
        LocalDate periodStart = startDate;

        while (periodStart.isBefore(maturityDate)) {
            LocalDate periodEnd = findNextPaymentDate(periodStart, frequency);
            if (periodEnd.isAfter(maturityDate)) {
                periodEnd = maturityDate;
            }

            LocalDate paymentDate = adjustForBusinessDay(periodEnd);
            int accrualDays = (int) ChronoUnit.DAYS.between(periodStart, periodEnd);

            CouponPeriod period = new CouponPeriod(
                tradeId,
                periodStart,
                periodEnd,
                paymentDate,
                accrualDays,
                notional
            );

            periods.add(period);
            periodStart = periodEnd;
        }

        return couponPeriodRepository.saveAll(periods);
    }    /**
     * Update coupon schedule when notional changes.
     */
    public void updateScheduleForNotionalChange(Long tradeId, BigDecimal newNotional, LocalDate effectiveDate) {
        List<CouponPeriod> futurePeriods = couponPeriodRepository
                .findByTradeIdAndPeriodStartDateGreaterThanEqualOrderByPeriodStartDate(tradeId, effectiveDate);

        for (CouponPeriod period : futurePeriods) {
            period.setNotionalAmount(newNotional);
        }

        couponPeriodRepository.saveAll(futurePeriods);
    }

    /**
     * Get all coupon periods for a trade.
     */
    public List<CouponPeriod> getCouponPeriods(Long tradeId) {
        List<CouponPeriod> periods = couponPeriodRepository.findByTradeIdOrderByPeriodStartDate(tradeId);
        
        // Calculate coupon amounts
        CDSTrade trade = cdsTradeRepository.findById(tradeId).orElse(null);
        if (trade != null && trade.getSpread() != null) {
            for (CouponPeriod period : periods) {
                period.calculateCouponAmount(trade.getSpread());
            }
        }
        
        return periods;
    }

    /**
     * Get coupon periods within a date range.
     */
    public List<CouponPeriod> getCouponPeriodsInRange(Long tradeId, LocalDate startDate, LocalDate endDate) {
        return couponPeriodRepository.findByTradeIdAndPeriodStartDateBetweenOrderByPeriodStartDate(
                tradeId, startDate, endDate);
    }

    /**
     * Mark a coupon period as paid.
     * 
     * @param periodId The coupon period ID
     * @return The updated coupon period
     */
    public CouponPeriod payCoupon(Long periodId) {
        return payCoupon(periodId, LocalDateTime.now());
    }

    /**
     * Mark a coupon period as paid with a specific payment timestamp.
     * 
     * @param periodId The coupon period ID
     * @param paidAtTimestamp The timestamp when the payment was made
     * @return The updated coupon period
     */
    public CouponPeriod payCoupon(Long periodId, LocalDateTime paidAtTimestamp) {
        CouponPeriod period = couponPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon period not found: " + periodId));
        
        if (Boolean.TRUE.equals(period.getPaid())) {
            throw new IllegalStateException("Coupon period " + periodId + " has already been paid");
        }
        
        // Validate that the payment timestamp is not in the future
        LocalDateTime now = LocalDateTime.now();
        if (paidAtTimestamp.isAfter(now)) {
            throw new IllegalStateException("Cannot pay coupon with future timestamp: " + paidAtTimestamp);
        }
        
        // For "pay on time" scenario, validate we're not before the scheduled payment date
        LocalDate paidAtDate = paidAtTimestamp.toLocalDate();
        if (paidAtDate.isBefore(period.getPaymentDate())) {
            throw new IllegalStateException("Cannot pay coupon period " + periodId + 
                " before its payment date " + period.getPaymentDate() + " (paid at: " + paidAtDate + ")");
        }
        
        period.setPaid(true);
        period.setPaidAt(paidAtTimestamp);
        
        return couponPeriodRepository.save(period);
    }

    /**
     * Find the next payment date based on the premium frequency.
     * For CDS, uses IMM dates (20th of month) for standard frequencies.
     * 
     * @param date The current date
     * @param frequency The premium frequency (QUARTERLY, SEMI_ANNUAL, ANNUAL, MONTHLY)
     * @return The next payment date
     */
    private LocalDate findNextPaymentDate(LocalDate date, String frequency) {
        if (frequency == null) {
            frequency = "QUARTERLY"; // Default to quarterly for CDS
        }
        
        switch (frequency.toUpperCase()) {
            case "MONTHLY":
                return findNextMonthlyImmDate(date);
            case "QUARTERLY":
                return findNextQuarterlyImmDate(date);
            case "SEMI_ANNUAL":
                return findNextSemiAnnualImmDate(date);
            case "ANNUAL":
                return findNextAnnualImmDate(date);
            default:
                return findNextQuarterlyImmDate(date); // Default to quarterly
        }
    }

    /**
     * Find the next monthly IMM date (20th of next month).
     */
    private LocalDate findNextMonthlyImmDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        
        LocalDate immDate = LocalDate.of(year, month, 20);
        
        if (immDate.isBefore(date) || immDate.equals(date)) {
            if (month == 12) {
                immDate = LocalDate.of(year + 1, 1, 20);
            } else {
                immDate = LocalDate.of(year, month + 1, 20);
            }
        }
        
        return immDate;
    }

    /**
     * Find the next quarterly IMM date after the given date.
     * IMM dates are the 20th of March, June, September, and December.
     */
    private LocalDate findNextQuarterlyImmDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        // IMM months: 3, 6, 9, 12
        int nextImmMonth;
        if (month < 3 || (month == 3 && date.getDayOfMonth() < 20)) {
            nextImmMonth = 3;
        } else if (month < 6 || (month == 6 && date.getDayOfMonth() < 20)) {
            nextImmMonth = 6;
        } else if (month < 9 || (month == 9 && date.getDayOfMonth() < 20)) {
            nextImmMonth = 9;
        } else if (month < 12 || (month == 12 && date.getDayOfMonth() < 20)) {
            nextImmMonth = 12;
        } else {
            nextImmMonth = 3;
            year++;
        }

        LocalDate immDate = LocalDate.of(year, nextImmMonth, 20);
        
        // If the date is exactly on or past the 20th, move to next quarter
        if (immDate.isBefore(date) || immDate.equals(date)) {
            if (nextImmMonth == 12) {
                immDate = LocalDate.of(year + 1, 3, 20);
            } else {
                immDate = LocalDate.of(year, nextImmMonth + 3, 20);
            }
        }

        return immDate;
    }
    
    /**
     * Find the next semi-annual IMM date (20th of Jun/Dec).
     */
    private LocalDate findNextSemiAnnualImmDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        
        // Semi-annual IMM months: 6, 12
        int nextImmMonth;
        if (month < 6 || (month == 6 && date.getDayOfMonth() < 20)) {
            nextImmMonth = 6;
        } else if (month < 12 || (month == 12 && date.getDayOfMonth() < 20)) {
            nextImmMonth = 12;
        } else {
            nextImmMonth = 6;
            year++;
        }
        
        LocalDate immDate = LocalDate.of(year, nextImmMonth, 20);
        
        if (immDate.isBefore(date) || immDate.equals(date)) {
            if (nextImmMonth == 12) {
                immDate = LocalDate.of(year + 1, 6, 20);
            } else {
                immDate = LocalDate.of(year, 12, 20);
            }
        }
        
        return immDate;
    }
    
    /**
     * Find the next annual IMM date (20th of December).
     */
    private LocalDate findNextAnnualImmDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        
        LocalDate immDate = LocalDate.of(year, 12, 20);
        
        // If we're past December 20th, move to next year
        if (immDate.isBefore(date) || immDate.equals(date)) {
            immDate = LocalDate.of(year + 1, 12, 20);
        }
        
        return immDate;
    }

    /**
     * Find the next IMM date after the given date.
     * IMM dates are the 20th of March, June, September, and December.
     * @deprecated Use findNextPaymentDate with frequency parameter instead
     */
    @Deprecated
    private LocalDate findNextImmDate(LocalDate date) {
        return findNextQuarterlyImmDate(date);
    }

    /**
     * Adjust date for business day convention (simplified - no holiday calendar).
     */
    private LocalDate adjustForBusinessDay(LocalDate date) {
        // Modified following convention: if weekend, move to next Monday
        while (date.getDayOfWeek().getValue() > 5) { // Saturday=6, Sunday=7
            date = date.plusDays(1);
        }
        return date;
    }

    /**
     * Calculate the current accrued premium for a trade.
     * This is the premium that has accrued from the last paid coupon date (or trade start) to today.
     *  
     * @param tradeId The trade ID
     * @return The accrued premium amount, or zero if no accrual
     */
    public BigDecimal calculateCurrentAccruedPremium(Long tradeId) {
        CDSTrade trade = cdsTradeRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));
        
        LocalDate today = LocalDate.now();
        
        // Find the last paid coupon
        List<CouponPeriod> allPeriods = couponPeriodRepository.findByTradeIdOrderByPaymentDateAsc(tradeId);
        
        LocalDate accrualStartDate = trade.getTradeDate(); // Default to trade start
        
        // Find the most recent paid coupon
        for (int i = allPeriods.size() - 1; i >= 0; i--) {
            CouponPeriod period = allPeriods.get(i);
            if (Boolean.TRUE.equals(period.getPaid())) {
                accrualStartDate = period.getPaymentDate();
                break;
            }
        }
        
        // If today is before or on the accrual start, no accrual
        if (!today.isAfter(accrualStartDate)) {
            return BigDecimal.ZERO;
        }
        
        // Calculate accrued days using ACT/360
        long accrualDays = ChronoUnit.DAYS.between(accrualStartDate, today);
        
        // Calculate accrued premium
        // Formula: Notional * Spread * (Days / 360)
        BigDecimal notional = trade.getNotionalAmount();
        BigDecimal spread = trade.getSpread().divide(new BigDecimal("10000"), 10, RoundingMode.HALF_UP); // Convert bps to decimal
        BigDecimal dayCountFraction = new BigDecimal(accrualDays).divide(new BigDecimal("360"), 10, RoundingMode.HALF_UP);
        
        BigDecimal accruedPremium = notional
                .multiply(spread)
                .multiply(dayCountFraction)
                .setScale(2, RoundingMode.HALF_UP);
        
        return accruedPremium;
    }
}
