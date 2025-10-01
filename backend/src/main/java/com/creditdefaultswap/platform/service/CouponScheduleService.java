package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.CouponPeriod;
import com.creditdefaultswap.platform.repository.CouponPeriodRepository;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * Generate IMM coupon schedule for a CDS trade.
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

        List<CouponPeriod> periods = new ArrayList<>();
        LocalDate periodStart = startDate;

        while (periodStart.isBefore(maturityDate)) {
            LocalDate periodEnd = findNextImmDate(periodStart);
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
    }

    /**
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
        return couponPeriodRepository.findByTradeIdOrderByPeriodStartDate(tradeId);
    }

    /**
     * Get coupon periods within a date range.
     */
    public List<CouponPeriod> getCouponPeriodsInRange(Long tradeId, LocalDate startDate, LocalDate endDate) {
        return couponPeriodRepository.findByTradeIdAndPeriodStartDateBetweenOrderByPeriodStartDate(
                tradeId, startDate, endDate);
    }

    /**
     * Find the next IMM date after the given date.
     * IMM dates are the 20th of March, June, September, and December.
     */
    private LocalDate findNextImmDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        // IMM months: 3, 6, 9, 12
        int nextImmMonth;
        if (month <= 3) {
            nextImmMonth = 3;
        } else if (month <= 6) {
            nextImmMonth = 6;
        } else if (month <= 9) {
            nextImmMonth = 9;
        } else if (month <= 12) {
            nextImmMonth = 12;
        } else {
            nextImmMonth = 3;
            year++;
        }

        LocalDate immDate = LocalDate.of(year, nextImmMonth, 20);
        
        // If the date is exactly on the 20th and we're past it, move to next quarter
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
     * Adjust date for business day convention (simplified - no holiday calendar).
     */
    private LocalDate adjustForBusinessDay(LocalDate date) {
        // Modified following convention: if weekend, move to next Monday
        while (date.getDayOfWeek().getValue() > 5) { // Saturday=6, Sunday=7
            date = date.plusDays(1);
        }
        return date;
    }
}