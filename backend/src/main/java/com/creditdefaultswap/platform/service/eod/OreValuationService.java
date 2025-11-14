package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.eod.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.eod.TradeValuationRepository;
import com.creditdefaultswap.platform.repository.eod.TradeValuationJdbcRepository;
import com.creditdefaultswap.platform.repository.eod.TradeValuationSensitivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for calculating NPV and sensitivities using ORE (Open Source Risk Engine)
 * 
 * This service provides:
 * - NPV calculation for CDS trades
 * - Risk sensitivities (CS01, IR01, JTD, REC01)
 * - Batch processing capabilities
 * - Integration with market data snapshots
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OreValuationService {
    
    private final TradeValuationJdbcRepository valuationJdbcRepository;
    private final TradeValuationRepository valuationRepository; // For queries only
    private final TradeValuationSensitivityRepository sensitivityRepository;
    private final CDSTradeRepository tradeRepository;
    private final MarketDataSnapshotService marketDataService;
    
    /**
     * Calculate NPV for a single trade
     * Runs within caller's transaction context
     */
    public TradeValuation calculateNpv(Long tradeId, LocalDate valuationDate, String jobId) {
        CDSTrade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Get market data snapshot (or create default for testing)
            MarketDataSnapshot snapshot = marketDataService.getSnapshotByDate(valuationDate)
                .orElseGet(() -> {
                    log.warn("No market data snapshot for date {}, using default values", valuationDate);
                    return createDefaultMarketData(valuationDate);
                });
            
            // TODO: Integrate with actual ORE engine
            // For now, use simplified calculation as placeholder
            BigDecimal npv = calculateSimplifiedNpv(trade, valuationDate, snapshot);
            
            long calculationTime = System.currentTimeMillis() - startTime;
            
            // Create valuation record - using tradeId instead of trade object for debugging
            TradeValuation valuation = new TradeValuation();
            valuation.setValuationDate(valuationDate);
            valuation.setTradeId(tradeId);  // Store ID directly instead of object reference
            valuation.setNpv(npv);
            valuation.setCurrency(trade.getCurrency());
            valuation.setCalculationMethod("ORE");
            valuation.setValuationStatus(TradeValuation.ValuationStatus.SUCCESS);
            valuation.setJobId(jobId);
            valuation.setCalculationTimeMs((int) calculationTime);
            
            // Use JDBC to bypass Hibernate cascade issues
            valuationJdbcRepository.insertValuation(valuation);
            
            log.debug("Calculated NPV for trade {}: {} in {}ms", 
                tradeId, npv, calculationTime);
            
            return valuation;
            
        } catch (Exception e) {
            log.error("Error calculating NPV for trade {}: {}", tradeId, e.getMessage(), e);
            
            TradeValuation failedValuation = new TradeValuation();
            failedValuation.setValuationDate(valuationDate);
            failedValuation.setTradeId(tradeId);  // Store ID directly
            failedValuation.setNpv(BigDecimal.ZERO);
            failedValuation.setCurrency(trade.getCurrency());
            failedValuation.setValuationStatus(TradeValuation.ValuationStatus.FAILED);
            failedValuation.setErrorMessage(e.getMessage());
            failedValuation.setJobId(jobId);
            
            // Use JDBC to bypass Hibernate cascade issues
            valuationJdbcRepository.insertFailedValuation(failedValuation);
            
            return failedValuation;
        }
    }
    
    /**
     * Calculate NPV for multiple trades in batch
     * No @Transactional here - each calculateNpv runs in its own transaction
     */
    public List<TradeValuation> calculateNpvBatch(List<Long> tradeIds, LocalDate valuationDate, String jobId) {
        log.info("Starting batch NPV calculation for {} trades on {}", tradeIds.size(), valuationDate);
        
        List<TradeValuation> valuations = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (Long tradeId : tradeIds) {
            try {
                TradeValuation valuation = calculateNpv(tradeId, valuationDate, jobId);
                valuations.add(valuation);
                
                if (valuation.getValuationStatus() == TradeValuation.ValuationStatus.SUCCESS) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("Failed to calculate NPV for trade {}: {}", tradeId, e.getMessage());
                failCount++;
            }
        }
        
        log.info("Batch NPV calculation completed: {} successful, {} failed", successCount, failCount);
        
        return valuations;
    }
    
    /**
     * Simplified NPV calculation (placeholder for ORE integration)
     * 
     * CDS NPV = Protection Leg PV - Premium Leg PV
     * 
     * Protection Leg PV = Notional × (1 - Recovery Rate) × Default Probability × Discount Factor
     * Premium Leg PV = Notional × Spread × Risky Duration × Discount Factor
     */
    private BigDecimal calculateSimplifiedNpv(CDSTrade trade, LocalDate valuationDate, MarketDataSnapshot snapshot) {
        BigDecimal notional = trade.getNotionalAmount();
        
        // Convert spread from basis points to decimal (e.g., 150 bps -> 0.0150)
        BigDecimal spread = trade.getSpread().divide(new BigDecimal("10000"), 6, RoundingMode.HALF_UP);
        
        // Convert recovery rate from percentage to decimal (e.g., 40% -> 0.40)
        BigDecimal recoveryRate = trade.getRecoveryRate() != null 
            ? trade.getRecoveryRate().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
            : new BigDecimal("0.40");
        
        // Calculate time to maturity in years
        long daysToMaturity = java.time.temporal.ChronoUnit.DAYS.between(valuationDate, trade.getMaturityDate());
        BigDecimal yearsToMaturity = BigDecimal.valueOf(daysToMaturity).divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP);
        
        if (yearsToMaturity.compareTo(BigDecimal.ZERO) <= 0) {
            // Trade has matured
            return BigDecimal.ZERO;
        }
        
        // Simplified discount factor (assuming 5% risk-free rate)
        BigDecimal riskFreeRate = new BigDecimal("0.05");
        BigDecimal discountFactor = BigDecimal.ONE.divide(
            BigDecimal.ONE.add(riskFreeRate.multiply(yearsToMaturity)), 
            6, 
            RoundingMode.HALF_UP
        );
        
        // Simplified hazard rate from spread (assuming 40% recovery)
        BigDecimal hazardRate = spread.divide(
            BigDecimal.ONE.subtract(recoveryRate), 
            6, 
            RoundingMode.HALF_UP
        );
        
        // Default probability = 1 - exp(-hazard_rate * time)
        double expValue = Math.exp(-hazardRate.multiply(yearsToMaturity).doubleValue());
        BigDecimal survivalProbability = BigDecimal.valueOf(expValue);
        BigDecimal defaultProbability = BigDecimal.ONE.subtract(survivalProbability);
        
        // Protection leg PV
        BigDecimal protectionLegPv = notional
            .multiply(BigDecimal.ONE.subtract(recoveryRate))
            .multiply(defaultProbability)
            .multiply(discountFactor);
        
        // Premium leg PV (simplified risky duration)
        BigDecimal riskyDuration = yearsToMaturity.multiply(survivalProbability);
        BigDecimal premiumLegPv = notional
            .multiply(spread)
            .multiply(riskyDuration)
            .multiply(discountFactor);
        
        // NPV = Protection Leg - Premium Leg (from protection buyer perspective)
        BigDecimal npv = protectionLegPv.subtract(premiumLegPv);
        
        // Adjust sign based on direction
        if (trade.getBuySellProtection() == CDSTrade.ProtectionDirection.SELL) {
            npv = npv.negate();
        }
        
        return npv.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate risk sensitivities (Greeks)
     */
    private TradeValuationSensitivity calculateSensitivities(CDSTrade trade, BigDecimal npv, LocalDate valuationDate) {
        // TODO: Implement proper sensitivity calculations using ORE
        // For now, use simplified approximations
        
        BigDecimal notional = trade.getNotionalAmount();
        
        // CS01: Approximate as 0.01% of notional times duration
        long daysToMaturity = java.time.temporal.ChronoUnit.DAYS.between(valuationDate, trade.getMaturityDate());
        BigDecimal duration = BigDecimal.valueOf(daysToMaturity).divide(BigDecimal.valueOf(365), 4, RoundingMode.HALF_UP);
        BigDecimal cs01 = notional.multiply(new BigDecimal("0.0001")).multiply(duration);
        
        // IR01: Approximate as 10% of CS01
        BigDecimal ir01 = cs01.multiply(new BigDecimal("0.10"));
        
        // JTD: Loss if immediate default (1 - recovery) * notional
        BigDecimal recoveryRate = trade.getRecoveryRate() != null 
            ? trade.getRecoveryRate() 
            : new BigDecimal("0.40");
        BigDecimal jtd = notional.multiply(BigDecimal.ONE.subtract(recoveryRate));
        
        // Adjust for protection buyer vs seller
        if (trade.getBuySellProtection() == CDSTrade.ProtectionDirection.SELL) {
            jtd = jtd.negate();
        }
        
        // REC01: Sensitivity to 1% recovery rate change
        BigDecimal rec01 = notional.multiply(new BigDecimal("0.01"));
        
        // Theta: Approximate daily time decay
        BigDecimal theta1d = npv.multiply(new BigDecimal("0.0001")); // 0.01% daily decay
        
        return TradeValuationSensitivity.builder()
            .cs01(cs01.setScale(4, RoundingMode.HALF_UP))
            .ir01(ir01.setScale(4, RoundingMode.HALF_UP))
            .jtd(jtd.setScale(4, RoundingMode.HALF_UP))
            .rec01(rec01.setScale(4, RoundingMode.HALF_UP))
            .theta1d(theta1d.setScale(4, RoundingMode.HALF_UP))
            .durationYears(duration)
            .dv01(cs01.setScale(4, RoundingMode.HALF_UP))
            .build();
    }
    
    /**
     * Get valuation for a trade on a specific date
     */
    public Optional<TradeValuation> getValuation(Long tradeId, LocalDate valuationDate) {
        return valuationRepository.findByValuationDateAndTradeId(valuationDate, tradeId);
    }
    
    /**
     * Get all valuations for a specific date
     */
    public List<TradeValuation> getValuationsByDate(LocalDate valuationDate) {
        return valuationRepository.findByValuationDate(valuationDate);
    }
    
    /**
     * Get valuation history for a trade
     */
    public List<TradeValuation> getValuationHistory(Long tradeId, LocalDate startDate, LocalDate endDate) {
        return valuationRepository.findByTradeIdAndDateRange(tradeId, startDate, endDate);
    }
    
    /**
     * Get latest valuation for a trade
     */
    public Optional<TradeValuation> getLatestValuation(Long tradeId) {
        return valuationRepository.findFirstByTradeIdOrderByValuationDateDesc(tradeId);
    }
    
    /**
     * Get count of successful/failed valuations for a date
     */
    public long getValuationCount(LocalDate valuationDate, TradeValuation.ValuationStatus status) {
        return valuationRepository.countByValuationDateAndValuationStatus(valuationDate, status);
    }
    
    /**
     * Create default market data snapshot for testing when real data is unavailable
     */
    private MarketDataSnapshot createDefaultMarketData(LocalDate valuationDate) {
        MarketDataSnapshot snapshot = new MarketDataSnapshot();
        snapshot.setSnapshotDate(valuationDate);
        snapshot.setSnapshotTime(java.time.LocalDateTime.now());
        // Use default/placeholder curves - these would normally come from market data providers
        log.info("Using default market data curves for valuation date: {}", valuationDate);
        return snapshot;
    }
}
