package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.AccrualEvent;
import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.CouponPeriod;
import com.creditdefaultswap.platform.repository.AccrualEventRepository;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CouponPeriodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for calculating and posting daily accruals using ACT/360 day count convention.
 * Supports premium accrual tracking with cumulative amounts and versioning.
 */
@Service
@Transactional
public class AccrualService {

    @Autowired
    private AccrualEventRepository accrualEventRepository;

    @Autowired
    private CouponPeriodRepository couponPeriodRepository;

    @Autowired
    private CDSTradeRepository cdsTradeRepository;

    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("360");

    /**
     * Post daily accrual for a specific trade and date.
     */
    public AccrualEvent postDailyAccrual(Long tradeId, LocalDate accrualDate) {
        CDSTrade trade = cdsTradeRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));

        // Find the relevant coupon period
        Optional<CouponPeriod> periodOpt = couponPeriodRepository
                .findByTradeIdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThan(
                        tradeId, accrualDate, accrualDate);

        if (periodOpt.isEmpty()) {
            throw new IllegalArgumentException("No coupon period found for date: " + accrualDate);
        }

        CouponPeriod period = periodOpt.get();

        // Check if accrual already exists for this date and version
        Optional<AccrualEvent> existingAccrual = accrualEventRepository
                .findByTradeIdAndAccrualDateAndTradeVersion(tradeId, accrualDate, trade.getVersion());

        if (existingAccrual.isPresent()) {
            return existingAccrual.get();
        }

        // Calculate day count fraction (ACT/360)
        BigDecimal dayCountFraction = BigDecimal.ONE.divide(DAYS_IN_YEAR, 8, RoundingMode.HALF_UP);

        // Calculate accrual amount: notional * spread * day_count_fraction
        BigDecimal accrualAmount = period.getNotionalAmount()
                .multiply(trade.getSpread())
                .multiply(dayCountFraction)
                .setScale(2, RoundingMode.HALF_UP);

        // Get cumulative accrual from previous day
        BigDecimal cumulativeAccrual = calculateCumulativeAccrual(tradeId, accrualDate, trade.getVersion());
        cumulativeAccrual = cumulativeAccrual.add(accrualAmount);

        AccrualEvent accrualEvent = new AccrualEvent(
                tradeId,
                period.getId(),
                accrualDate,
                accrualAmount,
                cumulativeAccrual,
                dayCountFraction,
                period.getNotionalAmount()
        );
        accrualEvent.setTradeVersion(trade.getVersion());

        return accrualEventRepository.save(accrualEvent);
    }

    /**
     * Post accruals for a date range.
     */
    public List<AccrualEvent> postAccrualsForPeriod(Long tradeId, LocalDate startDate, LocalDate endDate) {
        List<AccrualEvent> accruals = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            try {
                AccrualEvent accrual = postDailyAccrual(tradeId, currentDate);
                accruals.add(accrual);
            } catch (IllegalArgumentException e) {
                // Skip dates outside coupon periods (e.g., weekends, holidays)
            }
            currentDate = currentDate.plusDays(1);
        }

        return accruals;
    }

    /**
     * Get accrual events for a trade within a date range.
     */
    public List<AccrualEvent> getAccrualEvents(Long tradeId, LocalDate startDate, LocalDate endDate) {
        return accrualEventRepository.findByTradeIdAndAccrualDateBetweenOrderByAccrualDate(
                tradeId, startDate, endDate);
    }

    /**
     * Get current cumulative accrual for a trade.
     */
    public BigDecimal getCurrentCumulativeAccrual(Long tradeId) {
        Optional<AccrualEvent> latestAccrual = accrualEventRepository
                .findTopByTradeIdOrderByAccrualDateDescPostedAtDesc(tradeId);
        
        return latestAccrual.map(AccrualEvent::getCumulativeAccrual).orElse(BigDecimal.ZERO);
    }

    /**
     * Reset accruals for a new trade version (after amendment).
     */
    public void resetAccrualsForNewVersion(Long tradeId, Integer newVersion, LocalDate effectiveDate) {
        // Get the cumulative accrual up to the effective date from the previous version
        BigDecimal baseAccrual = BigDecimal.ZERO;
        
        Optional<AccrualEvent> lastAccrualBeforeAmendment = accrualEventRepository
                .findTopByTradeIdAndAccrualDateLessThanOrderByAccrualDateDescPostedAtDesc(tradeId, effectiveDate);
        
        if (lastAccrualBeforeAmendment.isPresent()) {
            baseAccrual = lastAccrualBeforeAmendment.get().getCumulativeAccrual();
        }

        // Create a base accrual event for the new version
        if (baseAccrual.compareTo(BigDecimal.ZERO) > 0) {
            AccrualEvent baseEvent = new AccrualEvent();
            baseEvent.setTradeId(tradeId);
            baseEvent.setAccrualDate(effectiveDate.minusDays(1));
            baseEvent.setAccrualAmount(BigDecimal.ZERO);
            baseEvent.setCumulativeAccrual(baseAccrual);
            baseEvent.setDayCountFraction(BigDecimal.ZERO);
            baseEvent.setNotionalAmount(BigDecimal.ZERO);
            baseEvent.setTradeVersion(newVersion);
            
            accrualEventRepository.save(baseEvent);
        }
    }

    /**
     * Calculate cumulative accrual up to a specific date for a trade version.
     */
    private BigDecimal calculateCumulativeAccrual(Long tradeId, LocalDate accrualDate, Integer tradeVersion) {
        Optional<AccrualEvent> previousAccrual = accrualEventRepository
                .findTopByTradeIdAndAccrualDateLessThanAndTradeVersionOrderByAccrualDateDescPostedAtDesc(
                        tradeId, accrualDate, tradeVersion);
        
        return previousAccrual.map(AccrualEvent::getCumulativeAccrual).orElse(BigDecimal.ZERO);
    }

    /**
     * Get net cash position for payment date.
     */
    public BigDecimal calculateNetCashForPayment(Long tradeId, LocalDate paymentDate) {
        // Find coupon period ending on this payment date
        Optional<CouponPeriod> periodOpt = couponPeriodRepository
                .findByTradeIdAndPaymentDate(tradeId, paymentDate);
        
        if (periodOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        CouponPeriod period = periodOpt.get();
        
        // Sum all accruals in this period
        List<AccrualEvent> periodAccruals = accrualEventRepository
                .findByTradeIdAndAccrualDateBetweenOrderByAccrualDate(
                        tradeId, period.getPeriodStartDate(), period.getPeriodEndDate());
        
        return periodAccruals.stream()
                .map(AccrualEvent::getAccrualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}