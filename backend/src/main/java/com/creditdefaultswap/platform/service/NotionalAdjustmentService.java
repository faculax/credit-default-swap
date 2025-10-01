package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.NotionalAdjustment;
import com.creditdefaultswap.platform.model.TradeStatus;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.NotionalAdjustmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing notional adjustments including partial and full terminations.
 */
@Service
@Transactional
public class NotionalAdjustmentService {

    @Autowired
    private NotionalAdjustmentRepository notionalAdjustmentRepository;

    @Autowired
    private CDSTradeRepository cdsTradeRepository;

    @Autowired
    private CouponScheduleService couponScheduleService;

    @Autowired
    private AccrualService accrualService;

    /**
     * Perform a notional adjustment.
     */
    public NotionalAdjustment adjustNotional(Long tradeId, LocalDate adjustmentDate,
                                           NotionalAdjustment.AdjustmentType adjustmentType,
                                           BigDecimal adjustmentAmount, String adjustmentReason) {
        CDSTrade trade = cdsTradeRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));

        BigDecimal originalNotional = trade.getNotionalAmount();
        BigDecimal remainingNotional;

        // Calculate remaining notional based on adjustment type
        switch (adjustmentType) {
            case PARTIAL_TERMINATION:
            case REDUCTION:
                remainingNotional = originalNotional.subtract(adjustmentAmount);
                if (remainingNotional.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Adjustment amount exceeds current notional");
                }
                break;
            case FULL_TERMINATION:
                remainingNotional = BigDecimal.ZERO;
                adjustmentAmount = originalNotional;
                break;
            default:
                throw new IllegalArgumentException("Unsupported adjustment type: " + adjustmentType);
        }

        // Calculate unwind cash amount (simplified - would use market data in real system)
        BigDecimal unwindCashAmount = calculateUnwindCashAmount(trade, adjustmentAmount, adjustmentDate);

        // Create adjustment record
        NotionalAdjustment adjustment = new NotionalAdjustment(
                tradeId, adjustmentDate, adjustmentType, originalNotional, 
                adjustmentAmount, remainingNotional
        );
        adjustment.setUnwindCashAmount(unwindCashAmount);
        adjustment.setAdjustmentReason(adjustmentReason);

        // Update trade
        trade.setNotionalAmount(remainingNotional);
        
        // Update trade status based on adjustment
        if (adjustmentType == NotionalAdjustment.AdjustmentType.FULL_TERMINATION) {
            trade.setTradeStatus(TradeStatus.TERMINATED);
        } else if (adjustmentType == NotionalAdjustment.AdjustmentType.PARTIAL_TERMINATION) {
            trade.setTradeStatus(TradeStatus.PARTIALLY_TERMINATED);
        }

        cdsTradeRepository.save(trade);

        // Update coupon schedule for remaining notional
        if (remainingNotional.compareTo(BigDecimal.ZERO) > 0) {
            couponScheduleService.updateScheduleForNotionalChange(tradeId, remainingNotional, adjustmentDate);
        }

        return notionalAdjustmentRepository.save(adjustment);
    }

    /**
     * Perform partial termination.
     */
    public NotionalAdjustment partiallyTerminate(Long tradeId, LocalDate terminationDate, 
                                               BigDecimal terminationAmount, String reason) {
        return adjustNotional(tradeId, terminationDate, 
                NotionalAdjustment.AdjustmentType.PARTIAL_TERMINATION, 
                terminationAmount, reason);
    }

    /**
     * Perform full termination.
     */
    public NotionalAdjustment fullyTerminate(Long tradeId, LocalDate terminationDate, String reason) {
        return adjustNotional(tradeId, terminationDate,
                NotionalAdjustment.AdjustmentType.FULL_TERMINATION,
                BigDecimal.ZERO, reason);
    }

    /**
     * Get all notional adjustments for a trade.
     */
    public List<NotionalAdjustment> getNotionalAdjustments(Long tradeId) {
        return notionalAdjustmentRepository.findByTradeIdOrderByAdjustmentDateDesc(tradeId);
    }

    /**
     * Get effective notional amount as of a specific date.
     */
    public BigDecimal getEffectiveNotional(Long tradeId, LocalDate asOfDate) {
        CDSTrade trade = cdsTradeRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));

        BigDecimal effectiveNotional = trade.getNotionalAmount();

        // Apply adjustments that occurred on or before the as-of date
        List<NotionalAdjustment> adjustments = notionalAdjustmentRepository
                .findByTradeIdOrderByAdjustmentDateDesc(tradeId);

        for (NotionalAdjustment adjustment : adjustments) {
            if (adjustment.getAdjustmentDate().isAfter(asOfDate)) {
                // This adjustment happened after our as-of date, so reverse it
                switch (adjustment.getAdjustmentType()) {
                    case PARTIAL_TERMINATION:
                    case REDUCTION:
                        effectiveNotional = effectiveNotional.add(adjustment.getAdjustmentAmount());
                        break;
                    case FULL_TERMINATION:
                        effectiveNotional = adjustment.getOriginalNotional();
                        break;
                }
            }
        }

        return effectiveNotional;
    }

    /**
     * Calculate unwind cash amount for the adjustment.
     * In a real system, this would use current market spreads and yield curves.
     */
    private BigDecimal calculateUnwindCashAmount(CDSTrade trade, BigDecimal adjustmentAmount, LocalDate adjustmentDate) {
        // Simplified calculation: adjustment amount * spread * time remaining
        BigDecimal spread = trade.getSpread();
        long daysRemaining = adjustmentDate.until(trade.getMaturityDate()).getDays();
        BigDecimal yearsRemaining = new BigDecimal(daysRemaining).divide(new BigDecimal(365), 4, BigDecimal.ROUND_HALF_UP);
        
        return adjustmentAmount.multiply(spread).multiply(yearsRemaining);
    }

    /**
     * Validate that adjustment is allowed for the current trade state.
     */
    private void validateAdjustment(CDSTrade trade, NotionalAdjustment.AdjustmentType adjustmentType) {
        TradeStatus status = trade.getTradeStatus();
        
        if (status == TradeStatus.TERMINATED || status == TradeStatus.CANCELLED) {
            throw new IllegalStateException("Cannot adjust notional for terminated or cancelled trade");
        }
        
        if (status == TradeStatus.SETTLED_CASH || status == TradeStatus.SETTLED_PHYSICAL) {
            throw new IllegalStateException("Cannot adjust notional for settled trade");
        }
    }
}