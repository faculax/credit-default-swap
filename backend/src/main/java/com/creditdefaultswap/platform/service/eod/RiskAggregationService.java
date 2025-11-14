package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.CdsPortfolio;
import com.creditdefaultswap.platform.model.eod.*;
import com.creditdefaultswap.platform.model.eod.RiskConcentration;
import com.creditdefaultswap.platform.model.eod.RiskLimit;
import com.creditdefaultswap.platform.model.eod.RiskLimitBreach;
import com.creditdefaultswap.platform.repository.CdsPortfolioRepository;
import com.creditdefaultswap.platform.repository.eod.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating risk metrics at different levels:
 * - Portfolio level
 * - Firm-wide level
 * 
 * Calculates sensitivities (CS01, IR01, JTD, REC01) and notional exposures
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RiskAggregationService {
    
    private final TradeValuationRepository valuationRepo;
    private final TradeValuationSensitivityRepository sensitivityRepo;
    private final PortfolioRiskMetricsRepository portfolioRiskRepo;
    private final FirmRiskSummaryRepository firmRiskRepo;
    private final CdsPortfolioRepository portfolioRepo;
    private final RiskConcentrationRepository concentrationRepo;
    private final RiskLimitRepository limitRepo;
    private final RiskLimitBreachRepository breachRepo;
    
    /**
     * Aggregate risk for a specific portfolio
     * 
     * NOTE: This is a simplified implementation. In production, would aggregate from
     * TradeValuation + TradeValuationSensitivity joins with proper portfolio relationships.
     */
    @Transactional
    public PortfolioRiskMetrics aggregatePortfolioRisk(LocalDate date, Long portfolioId, String jobId) {
        log.info("Aggregating risk for portfolio {} on {}", portfolioId, date);
        
        // Get all trade valuations for this date
        List<TradeValuation> valuations = valuationRepo.findByValuationDate(date);
        
        if (valuations.isEmpty()) {
            log.warn("No valuations found for {}", date);
            return null;
        }
        
        // Get portfolio
        CdsPortfolio portfolio = portfolioRepo.findById(portfolioId)
            .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));
        
        PortfolioRiskMetrics metrics = PortfolioRiskMetrics.builder()
            .calculationDate(date)
            .portfolio(portfolio)
            .jobId(jobId)
            .currency("USD")
            .build();
        
        // Aggregate CS01, IR01, JTD, REC01 from sensitivities
        BigDecimal totalCs01 = BigDecimal.ZERO;
        BigDecimal cs01Long = BigDecimal.ZERO;
        BigDecimal cs01Short = BigDecimal.ZERO;
        BigDecimal totalIr01 = BigDecimal.ZERO;
        BigDecimal totalJtd = BigDecimal.ZERO;
        BigDecimal jtdLong = BigDecimal.ZERO;
        BigDecimal jtdShort = BigDecimal.ZERO;
        BigDecimal totalRec01 = BigDecimal.ZERO;
        BigDecimal grossNotional = BigDecimal.ZERO;
        BigDecimal longNotional = BigDecimal.ZERO;
        BigDecimal shortNotional = BigDecimal.ZERO;
        
        for (TradeValuation val : valuations) {
            CDSTrade trade = val.getTrade();
            
            // Get sensitivity for this valuation
            Optional<TradeValuationSensitivity> sensOpt = 
                sensitivityRepo.findByTradeValuationId(val.getId());
            
            if (sensOpt.isEmpty()) {
                continue; // Skip if no sensitivity data
            }
            
            TradeValuationSensitivity sens = sensOpt.get();
            
            // Aggregate CS01
            BigDecimal cs01 = sens.getCs01() != null ? sens.getCs01() : BigDecimal.ZERO;
            totalCs01 = totalCs01.add(cs01);
            
            if (trade.getBuySellProtection() == CDSTrade.ProtectionDirection.BUY) {
                cs01Long = cs01Long.add(cs01);
            } else {
                cs01Short = cs01Short.add(cs01);
            }
            
            // Aggregate IR01
            BigDecimal ir01 = sens.getIr01() != null ? sens.getIr01() : BigDecimal.ZERO;
            totalIr01 = totalIr01.add(ir01);
            
            // Aggregate JTD
            BigDecimal jtd = sens.getJtd() != null ? sens.getJtd() : BigDecimal.ZERO;
            totalJtd = totalJtd.add(jtd);
            
            if (trade.getBuySellProtection() == CDSTrade.ProtectionDirection.BUY) {
                jtdLong = jtdLong.add(jtd);
            } else {
                jtdShort = jtdShort.add(jtd);
            }
            
            // Aggregate REC01
            BigDecimal rec01 = sens.getRec01() != null ? sens.getRec01() : BigDecimal.ZERO;
            totalRec01 = totalRec01.add(rec01);
            
            // Calculate notional exposures
            BigDecimal notional = trade.getNotionalAmount();
            grossNotional = grossNotional.add(notional.abs());
            
            if (trade.getBuySellProtection() == CDSTrade.ProtectionDirection.BUY) {
                longNotional = longNotional.add(notional);
            } else {
                shortNotional = shortNotional.add(notional);
            }
        }
        
        metrics.setCs01(totalCs01);
        metrics.setCs01Long(cs01Long);
        metrics.setCs01Short(cs01Short);
        metrics.setIr01(totalIr01);
        metrics.setJtd(totalJtd);
        metrics.setJtdLong(jtdLong);
        metrics.setJtdShort(jtdShort);
        metrics.setRec01(totalRec01);
        metrics.setGrossNotional(grossNotional);
        metrics.setLongNotional(longNotional);
        metrics.setShortNotional(shortNotional);
        metrics.setNetNotional(longNotional.add(shortNotional));
        
        // Save
        PortfolioRiskMetrics saved = portfolioRiskRepo.save(metrics);
        
        log.info("Portfolio {} risk: CS01={}, IR01={}, JTD={}, GrossNotional={}", 
            portfolioId, totalCs01, totalIr01, totalJtd, grossNotional);
        
        return saved;
    }
    
    /**
     * Aggregate risk across all portfolios to firm-wide level
     */
    @Transactional
    public FirmRiskSummary aggregateFirmRisk(LocalDate date, String jobId) {
        log.info("Aggregating firm-wide risk for {}", date);
        
        List<PortfolioRiskMetrics> allPortfolios = portfolioRiskRepo.findByCalculationDate(date);
        
        if (allPortfolios.isEmpty()) {
            log.warn("No portfolio risk metrics found for {}", date);
            return null;
        }
        
        FirmRiskSummary summary = FirmRiskSummary.builder()
            .calculationDate(date)
            .jobId(jobId)
            .currency("USD")
            .build();
        
        // Sum CS01 across all portfolios
        BigDecimal firmCs01 = allPortfolios.stream()
            .map(p -> p.getCs01() != null ? p.getCs01() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalCs01(firmCs01);
        
        BigDecimal firmCs01Long = allPortfolios.stream()
            .map(p -> p.getCs01Long() != null ? p.getCs01Long() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalCs01Long(firmCs01Long);
        
        BigDecimal firmCs01Short = allPortfolios.stream()
            .map(p -> p.getCs01Short() != null ? p.getCs01Short() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalCs01Short(firmCs01Short);
        
        // Sum IR01 across all portfolios
        BigDecimal firmIr01 = allPortfolios.stream()
            .map(p -> p.getIr01() != null ? p.getIr01() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalIr01(firmIr01);
        
        BigDecimal firmIr01Usd = allPortfolios.stream()
            .map(p -> p.getIr01Usd() != null ? p.getIr01Usd() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalIr01Usd(firmIr01Usd);
        
        BigDecimal firmIr01Eur = allPortfolios.stream()
            .map(p -> p.getIr01Eur() != null ? p.getIr01Eur() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalIr01Eur(firmIr01Eur);
        
        BigDecimal firmIr01Gbp = allPortfolios.stream()
            .map(p -> p.getIr01Gbp() != null ? p.getIr01Gbp() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalIr01Gbp(firmIr01Gbp);
        
        // Sum JTD across all portfolios
        BigDecimal firmJtd = allPortfolios.stream()
            .map(p -> p.getJtd() != null ? p.getJtd() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalJtd(firmJtd);
        
        BigDecimal firmJtdLong = allPortfolios.stream()
            .map(p -> p.getJtdLong() != null ? p.getJtdLong() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalJtdLong(firmJtdLong);
        
        BigDecimal firmJtdShort = allPortfolios.stream()
            .map(p -> p.getJtdShort() != null ? p.getJtdShort() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalJtdShort(firmJtdShort);
        
        // Sum REC01
        BigDecimal firmRec01 = allPortfolios.stream()
            .map(p -> p.getRec01() != null ? p.getRec01() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalRec01(firmRec01);
        
        // Sum notional exposures
        BigDecimal firmGross = allPortfolios.stream()
            .map(p -> p.getGrossNotional() != null ? p.getGrossNotional() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalGrossNotional(firmGross);
        
        BigDecimal firmNet = allPortfolios.stream()
            .map(p -> p.getNetNotional() != null ? p.getNetNotional() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalNetNotional(firmNet);
        
        BigDecimal firmLong = allPortfolios.stream()
            .map(p -> p.getLongNotional() != null ? p.getLongNotional() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalLongNotional(firmLong);
        
        BigDecimal firmShort = allPortfolios.stream()
            .map(p -> p.getShortNotional() != null ? p.getShortNotional() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalShortNotional(firmShort);
        
        // Calculate VaR (simplified parametric VaR based on CS01 and volatility)
        BigDecimal var95 = calculateVaR(date, firmCs01, 1.65); // 95% confidence = 1.65 std devs
        BigDecimal var99 = calculateVaR(date, firmCs01, 2.33); // 99% confidence = 2.33 std devs
        summary.setVar95(var95);
        summary.setVar99(var99);
        
        // Expected Shortfall (CVaR) - simplified as 1.2x VaR99
        summary.setExpectedShortfall(var99.multiply(new BigDecimal("1.2")));
        
        // Counts
        summary.setTotalPortfolioCount(allPortfolios.size());
        
        // Count distinct trades, counterparties, reference entities
        List<TradeValuation> allValuations = valuationRepo.findByValuationDate(date);
        
        Set<Long> distinctTrades = allValuations.stream()
            .map(v -> v.getTrade().getId())
            .collect(Collectors.toSet());
        summary.setTotalTradeCount(distinctTrades.size());
        
        Set<String> distinctCounterparties = allValuations.stream()
            .map(v -> v.getTrade().getCounterparty())
            .collect(Collectors.toSet());
        summary.setTotalCounterpartyCount(distinctCounterparties.size());
        
        Set<String> distinctRefEntities = allValuations.stream()
            .map(v -> v.getTrade().getReferenceEntity())
            .collect(Collectors.toSet());
        summary.setTotalReferenceEntityCount(distinctRefEntities.size());
        
        // Save
        FirmRiskSummary saved = firmRiskRepo.save(summary);
        
        log.info("Firm-wide risk: CS01={}, IR01={}, JTD={}, VaR95={}, VaR99={}", 
            firmCs01, firmIr01, firmJtd, var95, var99);
        
        return saved;
    }
    
    /**
     * Calculate parametric VaR based on CS01 and historical spread volatility
     * 
     * Simplified formula: VaR = CS01 × spread_volatility × confidence_multiplier
     */
    private BigDecimal calculateVaR(LocalDate date, BigDecimal cs01, double confidenceMultiplier) {
        // In production, would calculate historical spread volatility from market data
        // For now, use a typical spread volatility of 20 bps per day
        BigDecimal spreadVolatility = new BigDecimal("0.20"); // 20 bps daily volatility
        
        // VaR = |CS01| × spread_vol × z-score
        BigDecimal var = cs01.abs()
            .multiply(spreadVolatility)
            .multiply(new BigDecimal(confidenceMultiplier))
            .setScale(2, RoundingMode.HALF_UP);
        
        return var;
    }
    
    /**
     * Batch aggregation: all portfolios + firm-wide
     */
    @Transactional
    public void aggregateAllRisk(LocalDate date, String jobId) {
        log.info("Starting risk aggregation for {}", date);
        
        // Get all portfolios
        List<CdsPortfolio> allPortfolios = portfolioRepo.findAll();
        
        log.info("Found {} portfolios to aggregate", allPortfolios.size());
        
        // Aggregate each portfolio
        int successCount = 0;
        for (CdsPortfolio portfolio : allPortfolios) {
            try {
                aggregatePortfolioRisk(date, portfolio.getId(), jobId);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to aggregate portfolio {}: {}", portfolio.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Successfully aggregated {} / {} portfolios", successCount, allPortfolios.size());
        
        // Aggregate firm-wide
        if (successCount > 0) {
            aggregateFirmRisk(date, jobId);
        } else {
            log.warn("No portfolios successfully aggregated, skipping firm-wide aggregation");
        }
    }
    
    /**
     * Get portfolio risk for a specific date and portfolio
     */
    public Optional<PortfolioRiskMetrics> getPortfolioRisk(LocalDate date, Long portfolioId) {
        return portfolioRiskRepo.findByCalculationDateAndPortfolioId(date, portfolioId);
    }
    
    /**
     * Get firm risk for a specific date
     */
    public Optional<FirmRiskSummary> getFirmRisk(LocalDate date) {
        return firmRiskRepo.findByCalculationDate(date);
    }
    
    /**
     * Get risk time series for a portfolio
     */
    public List<PortfolioRiskMetrics> getPortfolioRiskHistory(Long portfolioId, LocalDate startDate, LocalDate endDate) {
        return portfolioRiskRepo.findByPortfolioIdAndCalculationDateBetweenOrderByCalculationDate(
            portfolioId, startDate, endDate);
    }
    
    /**
     * Get firm risk time series
     */
    public List<FirmRiskSummary> getFirmRiskHistory(LocalDate startDate, LocalDate endDate) {
        return firmRiskRepo.findByCalculationDateBetweenOrderByCalculationDate(startDate, endDate);
    }
    
    /**
     * Calculate risk concentration - identifies top entities, sectors, counterparties by risk
     */
    @Transactional
    public void calculateRiskConcentration(LocalDate date) {
        log.info("Calculating risk concentration for {}", date);
        
        // Get firm risk summary to calculate percentages
        FirmRiskSummary firmRisk = firmRiskRepo.findByCalculationDate(date)
            .orElse(null);
        
        if (firmRisk == null) {
            log.warn("No firm risk summary found for {}, skipping concentration analysis", date);
            return;
        }
        
        BigDecimal totalCs01 = firmRisk.getTotalCs01();
        BigDecimal totalJtd = firmRisk.getTotalJtd();
        
        // Get all valuations for the date
        List<TradeValuation> valuations = valuationRepo.findByValuationDate(date);
        
        // Group by reference entity
        Map<String, EntityRiskData> entityRiskMap = new HashMap<>();
        
        for (TradeValuation val : valuations) {
            String refEntity = val.getTrade().getReferenceEntity();
            
            // Get sensitivity
            Optional<TradeValuationSensitivity> sensOpt = 
                sensitivityRepo.findByTradeValuationId(val.getId());
            
            if (sensOpt.isEmpty()) {
                continue;
            }
            
            TradeValuationSensitivity sens = sensOpt.get();
            
            EntityRiskData data = entityRiskMap.computeIfAbsent(refEntity, 
                k -> new EntityRiskData(refEntity));
            
            data.cs01 = data.cs01.add(sens.getCs01() != null ? sens.getCs01() : BigDecimal.ZERO);
            data.jtd = data.jtd.add(sens.getJtd() != null ? sens.getJtd() : BigDecimal.ZERO);
            data.grossNotional = data.grossNotional.add(val.getTrade().getNotionalAmount().abs());
            data.tradeCount++;
        }
        
        // Sort by JTD and take top 10
        List<EntityRiskData> topEntities = entityRiskMap.values().stream()
            .sorted((a, b) -> b.jtd.abs().compareTo(a.jtd.abs()))
            .limit(10)
            .collect(Collectors.toList());
        
        // Save concentration records
        int ranking = 1;
        for (EntityRiskData data : topEntities) {
            RiskConcentration concentration = RiskConcentration.builder()
                .calculationDate(date)
                .concentrationType("TOP_10_NAMES")
                .referenceEntityName(data.entityName)
                .cs01(data.cs01)
                .jtd(data.jtd)
                .grossNotional(data.grossNotional)
                .ranking(ranking++)
                .tradeCount(data.tradeCount)
                .currency("USD")
                .build();
            
            // Calculate percentages
            if (totalJtd.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal pctJtd = data.jtd.abs()
                    .divide(totalJtd.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                concentration.setPercentageOfTotalJtd(pctJtd);
            }
            
            if (totalCs01.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal pctCs01 = data.cs01.abs()
                    .divide(totalCs01.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                concentration.setPercentageOfTotalCs01(pctCs01);
            }
            
            concentrationRepo.save(concentration);
        }
        
        log.info("Calculated risk concentration for {} entities", topEntities.size());
    }
    
    /**
     * Check risk limits and create breach records
     */
    @Transactional
    public void checkRiskLimits(LocalDate date) {
        log.info("Checking risk limits for {}", date);
        
        List<RiskLimit> activeLimits = limitRepo.findByIsActiveTrue();
        
        if (activeLimits.isEmpty()) {
            log.info("No active risk limits to check");
            return;
        }
        
        int breachCount = 0;
        int warningCount = 0;
        
        for (RiskLimit limit : activeLimits) {
            try {
                BigDecimal currentValue = getCurrentRiskValue(date, limit);
                
                if (currentValue == null) {
                    continue; // Skip if unable to get current value
                }
                
                // Check for breach
                if (currentValue.abs().compareTo(limit.getLimitValue()) > 0) {
                    createBreach(date, limit, currentValue, "BREACH");
                    breachCount++;
                } else if (limit.getWarningThreshold() != null &&
                           currentValue.abs().compareTo(limit.getWarningThreshold()) > 0) {
                    createBreach(date, limit, currentValue, "WARNING");
                    warningCount++;
                }
            } catch (Exception e) {
                log.error("Error checking limit {}: {}", limit.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Risk limit check complete: {} breaches, {} warnings", breachCount, warningCount);
    }
    
    private BigDecimal getCurrentRiskValue(LocalDate date, RiskLimit limit) {
        if (limit.getFirmWide()) {
            // Firm-wide limit
            FirmRiskSummary firmRisk = firmRiskRepo.findByCalculationDate(date).orElse(null);
            if (firmRisk == null) {
                return null;
            }
            
            return switch (limit.getLimitType()) {
                case "CS01" -> firmRisk.getTotalCs01();
                case "IR01" -> firmRisk.getTotalIr01();
                case "JTD" -> firmRisk.getTotalJtd();
                case "NOTIONAL" -> firmRisk.getTotalGrossNotional();
                case "VAR_95" -> firmRisk.getVar95();
                case "VAR_99" -> firmRisk.getVar99();
                default -> null;
            };
        } else if (limit.getPortfolio() != null) {
            // Portfolio limit
            PortfolioRiskMetrics portfolioRisk = portfolioRiskRepo
                .findByCalculationDateAndPortfolioId(date, limit.getPortfolio().getId())
                .orElse(null);
            
            if (portfolioRisk == null) {
                return null;
            }
            
            return switch (limit.getLimitType()) {
                case "CS01" -> portfolioRisk.getCs01();
                case "IR01" -> portfolioRisk.getIr01();
                case "JTD" -> portfolioRisk.getJtd();
                case "NOTIONAL" -> portfolioRisk.getGrossNotional();
                default -> null;
            };
        }
        
        return null;
    }
    
    private void createBreach(LocalDate date, RiskLimit limit, BigDecimal currentValue, String severity) {
        // Check if breach already exists
        List<RiskLimitBreach> existing = breachRepo.findByRiskLimitIdAndIsResolvedFalse(limit.getId());
        if (!existing.isEmpty()) {
            log.debug("Breach already exists for limit {}", limit.getId());
            return;
        }
        
        BigDecimal limitValue = limit.getLimitValue();
        BigDecimal breachAmount = currentValue.abs().subtract(limitValue);
        BigDecimal breachPercentage = breachAmount
            .divide(limitValue, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        
        RiskLimitBreach breach = RiskLimitBreach.builder()
            .breachDate(date)
            .riskLimit(limit)
            .currentValue(currentValue)
            .limitValue(limitValue)
            .breachAmount(breachAmount)
            .breachPercentage(breachPercentage)
            .breachSeverity(severity)
            .isResolved(false)
            .build();
        
        breachRepo.save(breach);
        
        log.warn("Risk limit breach: {} limit={} current={} ({}% over)", 
            severity, limitValue, currentValue, breachPercentage);
    }
    
    /**
     * Get unresolved limit breaches
     */
    public List<RiskLimitBreach> getUnresolvedBreaches() {
        return breachRepo.findByIsResolvedFalseOrderByBreachDateDesc();
    }
    
    /**
     * Get risk concentration for a date
     */
    public List<RiskConcentration> getRiskConcentration(LocalDate date, String type) {
        return concentrationRepo.findByCalculationDateAndConcentrationType(date, type);
    }
    
    // Helper class for grouping entity risk data
    private static class EntityRiskData {
        String entityName;
        BigDecimal cs01 = BigDecimal.ZERO;
        BigDecimal jtd = BigDecimal.ZERO;
        BigDecimal grossNotional = BigDecimal.ZERO;
        int tradeCount = 0;
        
        EntityRiskData(String entityName) {
            this.entityName = entityName;
        }
    }
}
