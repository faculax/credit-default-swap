package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.eod.DailyPnlResult;
import com.creditdefaultswap.platform.model.eod.TradeAccruedInterest;
import com.creditdefaultswap.platform.model.eod.TradeValuation;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.eod.DailyPnlResultRepository;
import com.creditdefaultswap.platform.repository.eod.TradeAccruedInterestRepository;
import com.creditdefaultswap.platform.repository.eod.TradeValuationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Daily P&L calculation service
 * 
 * Calculates P&L as: Total P&L = V(T) - V(T-1)
 * Where V = NPV + Accrued Interest
 * 
 * For new trades (no previous valuation), marks as new_trade_flag=true
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DailyPnlService {
    
    private final TradeValuationRepository valuationRepo;
    private final TradeAccruedInterestRepository accruedRepo;
    private final DailyPnlResultRepository pnlRepo;
    private final CDSTradeRepository tradeRepo;
    
    /**
     * Calculate P&L for all active trades on a specific date
     */
    @Transactional
    public List<DailyPnlResult> calculateDailyPnl(LocalDate pnlDate, String jobId) {
        log.info("Calculating daily P&L for date: {}", pnlDate);
        
        LocalDate previousDate = pnlDate.minusDays(1);
        List<DailyPnlResult> results = new ArrayList<>();
        
        // Get current day valuations
        List<TradeValuation> currentValuations = valuationRepo.findByValuationDate(pnlDate);
        
        log.info("Found {} valuations for {}", currentValuations.size(), pnlDate);
        
        for (TradeValuation currentValuation : currentValuations) {
            try {
                DailyPnlResult pnl = calculatePnlForTrade(
                    currentValuation, 
                    pnlDate, 
                    previousDate, 
                    jobId
                );
                
                if (pnl != null) {
                    results.add(pnl);
                }
            } catch (Exception e) {
                log.error("Failed to calculate P&L for trade {}: {}", 
                    currentValuation.getTradeId(), e.getMessage());
            }
        }
        
        // Save all results
        if (!results.isEmpty()) {
            results = pnlRepo.saveAll(results);
            log.info("Saved {} P&L results for {}", results.size(), pnlDate);
        }
        
        return results;
    }
    
    /**
     * Calculate P&L for a single trade
     */
    private DailyPnlResult calculatePnlForTrade(
            TradeValuation currentValuation,
            LocalDate pnlDate,
            LocalDate previousDate,
            String jobId) {
        
        Long tradeId = currentValuation.getTradeId();
        
        // Get current accrued interest
        BigDecimal currentAccrued = accruedRepo
            .findByCalculationDateAndTradeId(pnlDate, tradeId)
            .map(TradeAccruedInterest::getAccruedInterest)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal currentNpv = currentValuation.getNpv();
        BigDecimal currentTotalValue = currentNpv.add(currentAccrued);
        
        // Try to get previous day valuation
        Optional<TradeValuation> previousValuationOpt = valuationRepo
            .findByValuationDateAndTradeId(previousDate, tradeId);
        
        boolean isNewTrade = previousValuationOpt.isEmpty();
        
        BigDecimal previousNpv = null;
        BigDecimal previousAccrued = null;
        BigDecimal previousTotalValue = null;
        BigDecimal totalPnl;
        BigDecimal pnlPercentage = null;
        
        if (isNewTrade) {
            // New trade - P&L is the current total value
            totalPnl = currentTotalValue;
            log.debug("Trade {} is new - P&L = current value: {}", tradeId, totalPnl);
        } else {
            // Existing trade - calculate P&L delta
            TradeValuation previousValuation = previousValuationOpt.get();
            previousNpv = previousValuation.getNpv();
            
            previousAccrued = accruedRepo
                .findByCalculationDateAndTradeId(previousDate, tradeId)
                .map(TradeAccruedInterest::getAccruedInterest)
                .orElse(BigDecimal.ZERO);
            
            previousTotalValue = previousNpv.add(previousAccrued);
            
            // Total P&L = V(T) - V(T-1)
            totalPnl = currentTotalValue.subtract(previousTotalValue);
            
            // Calculate percentage change
            if (previousTotalValue.abs().compareTo(BigDecimal.ZERO) > 0) {
                pnlPercentage = totalPnl
                    .divide(previousTotalValue.abs(), 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }
            
            log.debug("Trade {} P&L: {} - {} = {}", 
                tradeId, currentTotalValue, previousTotalValue, totalPnl);
        }
        
        // Get trade details for enrichment
        Optional<CDSTrade> tradeOpt = tradeRepo.findById(tradeId);
        if (tradeOpt.isEmpty()) {
            log.warn("Trade {} not found - skipping P&L", tradeId);
            return null;
        }
        
        CDSTrade trade = tradeOpt.get();
        
        // Build P&L result
        DailyPnlResult.DailyPnlResultBuilder builder = DailyPnlResult.builder()
            .pnlDate(pnlDate)
            .tradeId(tradeId)
            .currentNpv(currentNpv)
            .currentAccrued(currentAccrued)
            .currentTotalValue(currentTotalValue)
            .previousNpv(previousNpv)
            .previousAccrued(previousAccrued)
            .previousTotalValue(previousTotalValue)
            .totalPnl(totalPnl)
            .pnlPercentage(pnlPercentage)
            .newTradeFlag(isNewTrade)
            .jobId(jobId)
            // Enrich with trade details
            .notionalAmount(trade.getNotionalAmount())
            .currency(trade.getCurrency())
            .referenceEntity(trade.getReferenceEntity())
            .buySellProtection(trade.getBuySellProtection().name())
            .calculationMethod("STANDARD");
        
        // Simple P&L attribution (can be enhanced later)
        if (!isNewTrade) {
            // Accrued P&L = change in accrued
            BigDecimal accruedPnl = currentAccrued.subtract(previousAccrued);
            
            // Market P&L = change in NPV (simplified - full attribution would break down by greeks)
            BigDecimal marketPnl = currentNpv.subtract(previousNpv);
            
            builder.accruedPnl(accruedPnl);
            builder.marketPnl(marketPnl);
        }
        
        return builder.build();
    }
    
    /**
     * Calculate P&L for a specific trade on a date
     */
    @Transactional(readOnly = true)
    public Optional<DailyPnlResult> getPnlForTrade(Long tradeId, LocalDate pnlDate) {
        return pnlRepo.findByPnlDateAndTradeId(pnlDate, tradeId);
    }
    
    /**
     * Get all P&L results for a date
     */
    @Transactional(readOnly = true)
    public List<DailyPnlResult> getPnlForDate(LocalDate pnlDate) {
        return pnlRepo.findByPnlDate(pnlDate);
    }
    
    /**
     * Get P&L summary for a date
     */
    @Transactional(readOnly = true)
    public PnlSummary getPnlSummary(LocalDate pnlDate) {
        List<DailyPnlResult> results = pnlRepo.findByPnlDate(pnlDate);
        
        BigDecimal totalPnl = results.stream()
            .map(DailyPnlResult::getTotalPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long newTradeCount = results.stream()
            .filter(r -> Boolean.TRUE.equals(r.getNewTradeFlag()))
            .count();
        
        long largePnlCount = results.stream()
            .filter(r -> Boolean.TRUE.equals(r.getLargePnlFlag()))
            .count();
        
        return new PnlSummary(
            pnlDate,
            results.size(),
            totalPnl,
            newTradeCount,
            largePnlCount
        );
    }
    
    /**
     * P&L Summary DTO
     */
    public record PnlSummary(
        LocalDate pnlDate,
        int tradeCount,
        BigDecimal totalPnl,
        long newTradeCount,
        long largePnlCount
    ) {}
}
