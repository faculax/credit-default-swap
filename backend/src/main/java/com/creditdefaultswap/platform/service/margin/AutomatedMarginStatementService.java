package com.creditdefaultswap.platform.service.margin;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.saccr.NettingSet;
import com.creditdefaultswap.platform.model.saccr.SaCcrCalculation;
import com.creditdefaultswap.platform.model.simm.SimmCalculationResult;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.saccr.NettingSetRepository;
import com.creditdefaultswap.platform.repository.saccr.SaCcrCalculationRepository;
import com.creditdefaultswap.platform.repository.simm.SimmCalculationResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Automated VM/IM Statement Generation Service
 * Generates margin statements using existing CCP relationships and netting sets
 * instead of requiring manual file uploads
 */
@Service
@Slf4j
public class AutomatedMarginStatementService {

    @Autowired
    private CDSTradeRepository tradeRepository;
    
    @Autowired
    private NettingSetRepository nettingSetRepository;
    
    @Autowired
    private SaCcrCalculationRepository saCcrRepository;
    
    @Autowired
    private SimmCalculationResultRepository simmResultRepository;

    /**
     * Generate VM/IM statements for all active netting sets
     */
    @Transactional(readOnly = true)
    public List<GeneratedMarginStatement> generateDailyStatements(LocalDate statementDate) {
        log.info("Generating automated VM/IM statements for date: {}", statementDate);
        
        // Get all active cleared trades grouped by netting set
        Map<String, List<CDSTrade>> tradesByNettingSet = getActiveTradesByNettingSet();
        
        List<GeneratedMarginStatement> statements = new ArrayList<>();
        
        for (Map.Entry<String, List<CDSTrade>> entry : tradesByNettingSet.entrySet()) {
            String nettingSetId = entry.getKey();
            List<CDSTrade> trades = entry.getValue();
            
            GeneratedMarginStatement statement = generateStatementForNettingSet(
                nettingSetId, trades, statementDate
            );
            statements.add(statement);
        }
        
        log.info("Generated {} VM/IM statements for {} netting sets", 
                 statements.size(), tradesByNettingSet.size());
        
        return statements;
    }

    /**
     * Generate statement for a specific netting set using existing data
     */
    private GeneratedMarginStatement generateStatementForNettingSet(
            String nettingSetId, List<CDSTrade> trades, LocalDate statementDate) {
        
        log.debug("Generating statement for netting set: {}", nettingSetId);
        
        // Extract CCP info from trades (all trades in netting set have same CCP and currency)
        CDSTrade sampleTrade = trades.get(0);
        String ccpName = sampleTrade.getCcpName();
        String ccpMemberId = sampleTrade.getCcpMemberId();
        String clearingAccount = sampleTrade.getClearingAccount();
        String currency = sampleTrade.getCurrency();
        
        // Calculate VM using trade MTM aggregation
        BigDecimal variationMargin = calculateVariationMargin(trades, statementDate);
        
        // Calculate IM using SA-CCR and SIMM data
        MarginComponents initialMargin = calculateInitialMargin(nettingSetId, statementDate);
        
        // Build statement
        GeneratedMarginStatement statement = GeneratedMarginStatement.builder()
                .statementId(generateStatementId(nettingSetId, statementDate))
                .statementDate(statementDate)
                .nettingSetId(nettingSetId)
                .ccpName(ccpName)
                .ccpMemberId(ccpMemberId)
                .clearingAccount(clearingAccount)
                .currency(currency)
                .tradeCount(trades.size())
                .totalNotional(calculateTotalNotional(trades))
                .variationMarginNet(variationMargin)
                .initialMarginRequired(initialMargin.getRequired())
                .initialMarginPosted(initialMargin.getPosted())
                .initialMarginExcess(initialMargin.getExcess())
                .generatedAt(LocalDate.now())
                .source("AUTOMATED_GENERATION")
                .underlyingTrades(trades.stream()
                    .map(CDSTrade::getId)
                    .collect(Collectors.toList()))
                .build();

        log.debug("Generated statement for {}: VM={}, IM={}", 
                  nettingSetId, variationMargin, initialMargin.getRequired());
                  
        return statement;
    }

    /**
     * Get active cleared trades grouped by netting set
     */
    private Map<String, List<CDSTrade>> getActiveTradesByNettingSet() {
        List<CDSTrade> clearedTrades = tradeRepository.findByIsClearedOrderByCreatedAtDesc(true);
        
        return clearedTrades.stream()
                .filter(trade -> trade.getNettingSetId() != null)
                .filter(trade -> !"TERMINATED".equals(trade.getTradeStatus()))
                .collect(Collectors.groupingBy(CDSTrade::getNettingSetId));
    }

    /**
     * Calculate variation margin using daily P&L changes
     */
    private BigDecimal calculateVariationMargin(List<CDSTrade> trades, LocalDate statementDate) {
        // This would typically use daily MTM data
        // For now, using mark-to-market values from trades
        return trades.stream()
                .map(CDSTrade::getMarkToMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate initial margin using SA-CCR and SIMM results
     */
    private MarginComponents calculateInitialMargin(String nettingSetId, LocalDate statementDate) {
        // Get SA-CCR exposure for the netting set using string ID
        SaCcrCalculation saCcr = saCcrRepository
                .findTopByNettingSetIdOrderByCalculationDateDesc(nettingSetId);
        
        BigDecimal saCcrIM = BigDecimal.ZERO;
        if (saCcr != null) {
            log.debug("Found SA-CCR calculation for {}: EAD={}", nettingSetId, saCcr.getExposureAtDefault());
            // Use SA-CCR EAD * 8% capital multiplier as IM estimate
            // EAD already includes alpha factor, so we just apply capital ratio
            saCcrIM = saCcr.getExposureAtDefault()
                    .multiply(new BigDecimal("0.08")); // 8% capital multiplier
        } else {
            log.warn("No SA-CCR calculation found for netting set: {}", nettingSetId);
        }

        // Get SIMM IM if available (would need portfolio mapping)
        BigDecimal simmIM = getSimmInitialMargin(nettingSetId, statementDate);
        
        // Use maximum of SA-CCR based IM and SIMM IM
        BigDecimal requiredIM = saCcrIM.max(simmIM);
        
        // For demonstration, assume posted = required (perfect collateralization)
        BigDecimal postedIM = requiredIM;
        BigDecimal excessIM = postedIM.subtract(requiredIM);
        
        return MarginComponents.builder()
                .required(requiredIM)
                .posted(postedIM)
                .excess(excessIM)
                .build();
    }

    /**
     * Get SIMM-based initial margin for netting set
     */
    private BigDecimal getSimmInitialMargin(String nettingSetId, LocalDate statementDate) {
        // This would map netting set to SIMM portfolio and get latest results
        // Implementation would depend on portfolio-to-netting-set mapping
        return BigDecimal.ZERO; // Placeholder
    }

    /**
     * Calculate total notional for trades in netting set
     */
    private BigDecimal calculateTotalNotional(List<CDSTrade> trades) {
        return trades.stream()
                .map(CDSTrade::getNotionalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Generate unique statement ID
     */
    private String generateStatementId(String nettingSetId, LocalDate statementDate) {
        return String.format("AUTO-VM-IM-%s-%s", 
                            nettingSetId, 
                            statementDate.toString());
    }

    /**
     * Data classes for margin components
     */
    @lombok.Builder
    @lombok.Data
    public static class MarginComponents {
        private BigDecimal required;
        private BigDecimal posted;
        private BigDecimal excess;
    }

    /**
     * Generated margin statement data structure
     */
    @lombok.Builder
    @lombok.Data
    public static class GeneratedMarginStatement {
        private String statementId;
        private LocalDate statementDate;
        private String nettingSetId;
        private String ccpName;
        private String ccpMemberId;
        private String clearingAccount;
        private String currency;
        private Integer tradeCount;
        private BigDecimal totalNotional;
        private BigDecimal variationMarginNet;
        private BigDecimal initialMarginRequired;
        private BigDecimal initialMarginPosted;
        private BigDecimal initialMarginExcess;
        private LocalDate generatedAt;
        private String source;
        private List<Long> underlyingTrades;
    }
}