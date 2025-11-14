package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.eod.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.eod.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for storing and retrieving EOD valuation results
 * 
 * Combines NPV and accrued interest calculations into consolidated
 * valuation records with full historical tracking
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ValuationStorageService {
    
    private final EodValuationResultRepository valuationResultRepository;
    private final EodPortfolioValuationRepository portfolioValuationRepository;
    private final TradeValuationRepository tradeValuationRepository;
    private final TradeAccruedInterestRepository accruedInterestRepository;
    private final TradeValuationSensitivityRepository sensitivityRepository;
    private final CDSTradeRepository tradeRepository;
    
    /**
     * Store EOD valuation result for a trade
     * Combines NPV, accrued interest, and risk metrics into single record
     */
    @Transactional
    public EodValuationResult storeValuationResult(
        Long tradeId,
        LocalDate valuationDate,
        String jobId
    ) {
        // Get trade
        CDSTrade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));
        
        // Get NPV calculation
        TradeValuation npvCalc = tradeValuationRepository
            .findByValuationDateAndTradeId(valuationDate, tradeId)
            .orElseThrow(() -> new IllegalStateException(
                "NPV calculation not found for trade " + tradeId + " on " + valuationDate));
        
        // Get accrued interest calculation
        TradeAccruedInterest accruedCalc = accruedInterestRepository
            .findByCalculationDateAndTradeId(valuationDate, tradeId)
            .orElseThrow(() -> new IllegalStateException(
                "Accrued interest not found for trade " + tradeId + " on " + valuationDate));
        
        // Calculate total value
        BigDecimal totalValue = npvCalc.getNpv().add(accruedCalc.getAccruedInterest());
        
        // Get sensitivities (query separately now that relationship is removed)
        TradeValuationSensitivity sensitivities = sensitivityRepository
            .findByTradeValuationId(npvCalc.getId())
            .orElse(null);
        
        // Build result
        EodValuationResult result = EodValuationResult.builder()
            .valuationDate(valuationDate)
            .trade(trade)
            // NPV components
            .npv(npvCalc.getNpv())
            .premiumLegPv(npvCalc.getPremiumLegPv())
            .protectionLegPv(npvCalc.getProtectionLegPv())
            // Accrued
            .accruedInterest(accruedCalc.getAccruedInterest())
            .accrualDays(accruedCalc.getAccrualDays())
            // Total
            .totalValue(totalValue)
            // Market data
            .creditSpread(npvCalc.getCreditSpread())
            .recoveryRate(npvCalc.getRecoveryRate())
            .discountRate(npvCalc.getDiscountFactor()) // Discount factor used as rate
            // Risk metrics
            .cs01(sensitivities != null ? sensitivities.getCs01() : null)
            .ir01(sensitivities != null ? sensitivities.getIr01() : null)
            .jtd(sensitivities != null ? sensitivities.getJtd() : null)
            .rec01(sensitivities != null ? sensitivities.getRec01() : null)
            // Trade details (snapshot)
            .notionalAmount(trade.getNotionalAmount())
            .spread(trade.getSpread())
            .maturityDate(trade.getMaturityDate())
            .referenceEntity(trade.getReferenceEntity())
            .currency(trade.getCurrency())
            .buySellProtection(trade.getBuySellProtection().name())
            // Metadata
            .jobId(jobId)
            .calculationTimestamp(LocalDateTime.now())
            .valuationMethod(npvCalc.getCalculationMethod())
            .calculationTimeMs(npvCalc.getCalculationTimeMs())
            .status(EodValuationResult.ValuationStatus.VALID)
            .build();
        
        result = valuationResultRepository.save(result);
        
        log.debug("Stored valuation result for trade {}: total value = {}",
            tradeId, totalValue);
        
        return result;
    }
    
    /**
     * Store valuation results for multiple trades in batch
     */
    @Transactional
    public List<EodValuationResult> storeValuationResultsBatch(
        List<Long> tradeIds,
        LocalDate valuationDate,
        String jobId
    ) {
        log.info("Storing valuation results for {} trades on {}",
            tradeIds.size(), valuationDate);
        
        List<EodValuationResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (Long tradeId : tradeIds) {
            try {
                EodValuationResult result = storeValuationResult(tradeId, valuationDate, jobId);
                results.add(result);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to store valuation for trade {}: {}",
                    tradeId, e.getMessage());
                failCount++;
            }
        }
        
        log.info("Stored {} valuations successfully, {} failed", successCount, failCount);
        
        return results;
    }
    
    /**
     * Aggregate valuations at portfolio level
     */
    @Transactional
    public EodPortfolioValuation aggregatePortfolioValuation(
        LocalDate valuationDate,
        String portfolioId,
        String book,
        String jobId
    ) {
        // Get all valuations for this portfolio/book
        // Note: This requires portfolio_id to be added to trades or passed in
        // For now, we'll aggregate all trades
        List<EodValuationResult> valuations = valuationResultRepository
            .findByValuationDate(valuationDate);
        
        if (valuations.isEmpty()) {
            throw new IllegalStateException(
                "No valuations found for date " + valuationDate);
        }
        
        // Filter by portfolio if needed (would require portfolio field on trades)
        // For now, aggregate all
        
        // Calculate aggregates
        BigDecimal totalNpv = BigDecimal.ZERO;
        BigDecimal totalAccrued = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalNotional = BigDecimal.ZERO;
        BigDecimal totalCs01 = BigDecimal.ZERO;
        BigDecimal totalIr01 = BigDecimal.ZERO;
        BigDecimal totalJtd = BigDecimal.ZERO;
        BigDecimal totalRec01 = BigDecimal.ZERO;
        
        int numTrades = valuations.size();
        int numBuyProtection = 0;
        int numSellProtection = 0;
        
        Map<String, BigDecimal> currencyTotals = new HashMap<>();
        
        for (EodValuationResult val : valuations) {
            totalNpv = totalNpv.add(val.getNpv());
            totalAccrued = totalAccrued.add(val.getAccruedInterest());
            totalValue = totalValue.add(val.getTotalValue());
            totalNotional = totalNotional.add(val.getNotionalAmount());
            
            if (val.getCs01() != null) totalCs01 = totalCs01.add(val.getCs01());
            if (val.getIr01() != null) totalIr01 = totalIr01.add(val.getIr01());
            if (val.getJtd() != null) totalJtd = totalJtd.add(val.getJtd());
            if (val.getRec01() != null) totalRec01 = totalRec01.add(val.getRec01());
            
            if ("BUY".equals(val.getBuySellProtection())) {
                numBuyProtection++;
            } else {
                numSellProtection++;
            }
            
            // Currency breakdown
            currencyTotals.merge(val.getCurrency(), val.getTotalValue(), BigDecimal::add);
        }
        
        // Build currency breakdown JSON
        Map<String, Object> currencyBreakdown = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : currencyTotals.entrySet()) {
            Map<String, Object> currencyData = new HashMap<>();
            currencyData.put("totalValue", entry.getValue());
            currencyData.put("count", valuations.stream()
                .filter(v -> v.getCurrency().equals(entry.getKey()))
                .count());
            currencyBreakdown.put(entry.getKey(), currencyData);
        }
        
        // Top exposures by reference entity
        Map<String, Object> topExposures = valuations.stream()
            .collect(Collectors.groupingBy(
                EodValuationResult::getReferenceEntity,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    EodValuationResult::getTotalValue,
                    BigDecimal::add
                )
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("exposure", e.getValue());
                    return data;
                },
                (a, b) -> a,
                LinkedHashMap::new
            ));
        
        // Build portfolio valuation
        EodPortfolioValuation portfolioVal = EodPortfolioValuation.builder()
            .valuationDate(valuationDate)
            .portfolioId(portfolioId)
            .portfolioName(portfolioId) // Would come from portfolio master
            .book(book)
            .desk("CREDIT") // Would come from portfolio hierarchy
            .businessUnit("TRADING") // Would come from portfolio hierarchy
            .totalNpv(totalNpv)
            .totalAccrued(totalAccrued)
            .totalValue(totalValue)
            .totalNotional(totalNotional)
            .numTrades(numTrades)
            .numBuyProtection(numBuyProtection)
            .numSellProtection(numSellProtection)
            .totalCs01(totalCs01)
            .totalIr01(totalIr01)
            .totalJtd(totalJtd)
            .totalRec01(totalRec01)
            .currencyBreakdown(currencyBreakdown)
            .topExposures(topExposures)
            .jobId(jobId)
            .build();
        
        portfolioVal = portfolioValuationRepository.save(portfolioVal);
        
        log.info("Aggregated portfolio valuation: {} trades, total value = {}",
            numTrades, totalValue);
        
        return portfolioVal;
    }
    
    /**
     * Get valuation for a specific trade on a date
     */
    public Optional<EodValuationResult> getValuation(Long tradeId, LocalDate valuationDate) {
        return valuationResultRepository.findByValuationDateAndTradeId(valuationDate, tradeId);
    }
    
    /**
     * Get all valuations for a date
     */
    public List<EodValuationResult> getValuationsByDate(LocalDate valuationDate) {
        return valuationResultRepository.findByValuationDate(valuationDate);
    }
    
    /**
     * Get valuation history for a trade
     */
    public List<EodValuationResult> getValuationHistory(Long tradeId) {
        return valuationResultRepository.findByTradeIdOrderByValuationDateDesc(tradeId);
    }
    
    /**
     * Get portfolio valuation
     */
    public Optional<EodPortfolioValuation> getPortfolioValuation(
        LocalDate valuationDate,
        String portfolioId,
        String book
    ) {
        return portfolioValuationRepository
            .findByValuationDateAndPortfolioIdAndBook(valuationDate, portfolioId, book);
    }
    
    /**
     * Get all portfolio valuations for a date
     */
    public List<EodPortfolioValuation> getPortfolioValuationsByDate(LocalDate valuationDate) {
        return portfolioValuationRepository.findByValuationDate(valuationDate);
    }
}
