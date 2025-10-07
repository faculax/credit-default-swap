package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.PortfolioPricingResponse;
import com.creditdefaultswap.platform.dto.PortfolioPricingResponse.*;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CdsPortfolioConstituentRepository;
import com.creditdefaultswap.platform.repository.CdsPortfolioRepository;
import com.creditdefaultswap.platform.repository.PortfolioRiskCacheRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioPricingService {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioPricingService.class);
    private static final String DEFAULT_SECTOR = "UNCLASSIFIED";
    
    private final CdsPortfolioRepository portfolioRepository;
    private final CdsPortfolioConstituentRepository constituentRepository;
    private final PortfolioRiskCacheRepository riskCacheRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${risk.engine.url:http://risk-engine:8082}")
    private String riskEngineUrl;
    
    @Autowired
    public PortfolioPricingService(
            CdsPortfolioRepository portfolioRepository,
            CdsPortfolioConstituentRepository constituentRepository,
            PortfolioRiskCacheRepository riskCacheRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.portfolioRepository = portfolioRepository;
        this.constituentRepository = constituentRepository;
        this.riskCacheRepository = riskCacheRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public PortfolioPricingResponse pricePortfolio(Long portfolioId, LocalDate valuationDate) {
        logger.info("Pricing portfolio {} for valuation date {}", portfolioId, valuationDate);
        
        CdsPortfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        
        List<CdsPortfolioConstituent> constituents = constituentRepository.findActiveByPortfolioId(portfolioId);
        
        if (constituents.isEmpty()) {
            throw new IllegalArgumentException("Portfolio has no active constituents");
        }
        
        // Extract trade IDs for risk calculation
        List<Long> tradeIds = constituents.stream()
                .map(c -> c.getTrade().getId())
                .collect(Collectors.toList());
        
        // Call risk engine for multi-trade pricing
        List<Map<String, Object>> riskMeasures = callRiskEngine(tradeIds, valuationDate);
        
        // Build response
        PortfolioPricingResponse response = new PortfolioPricingResponse();
        response.setPortfolioId(portfolioId);
        response.setValuationDate(valuationDate.toString());
        
        // Calculate weights
        Map<Long, BigDecimal> normalizedWeights = calculateNormalizedWeights(constituents);
        
        // Aggregate metrics
        AggregateMetrics aggregate = new AggregateMetrics();
        List<TradeBreakdown> tradeBreakdowns = new ArrayList<>();
        
        BigDecimal totalPv = BigDecimal.ZERO;
        BigDecimal totalAccrued = BigDecimal.ZERO;
        BigDecimal totalPremiumLegPv = BigDecimal.ZERO;
        BigDecimal totalProtectionLegPv = BigDecimal.ZERO;
        BigDecimal totalCs01 = BigDecimal.ZERO;
        BigDecimal totalRec01 = BigDecimal.ZERO;
        BigDecimal totalJtd = BigDecimal.ZERO;
        BigDecimal weightedFairSpread = BigDecimal.ZERO;
        
        for (int i = 0; i < constituents.size(); i++) {
            CdsPortfolioConstituent constituent = constituents.get(i);
            Map<String, Object> measures = riskMeasures.get(i);
            
            BigDecimal pv = new BigDecimal(measures.getOrDefault("npv", "0").toString());
            BigDecimal accrued = new BigDecimal(measures.getOrDefault("accruedPremium", "0").toString());
            BigDecimal protectionLegPv = new BigDecimal(measures.getOrDefault("protectionLegNPV", "0").toString());
            BigDecimal premiumLegPv = new BigDecimal(measures.getOrDefault("premiumLegNPVClean", "0").toString());
            
            // Use fairSpreadClean and convert to basis points (* 10000)
            BigDecimal fairSpreadClean = new BigDecimal(measures.getOrDefault("fairSpreadClean", "0").toString());
            BigDecimal fairSpread = fairSpreadClean.multiply(new BigDecimal("10000"));
            
            // CS01: Use couponLegBPS if available, otherwise estimate from notional
            BigDecimal cs01 = measures.containsKey("couponLegBPS") 
                ? new BigDecimal(measures.get("couponLegBPS").toString())
                : constituent.getTrade().getNotionalAmount().multiply(new BigDecimal("0.0001"));
            
            // REC01 and JTD: Not available from ORE, set to zero for now
            BigDecimal rec01 = BigDecimal.ZERO;
            BigDecimal jtd = BigDecimal.ZERO;
            
            totalPv = totalPv.add(pv);
            totalAccrued = totalAccrued.add(accrued);
            totalPremiumLegPv = totalPremiumLegPv.add(premiumLegPv);
            totalProtectionLegPv = totalProtectionLegPv.add(protectionLegPv);
            totalCs01 = totalCs01.add(cs01);
            totalRec01 = totalRec01.add(rec01);
            totalJtd = totalJtd.add(jtd);
            
            BigDecimal weight = normalizedWeights.get(constituent.getTrade().getId());
            weightedFairSpread = weightedFairSpread.add(fairSpread.multiply(weight));
            
            // Trade breakdown
            TradeBreakdown breakdown = new TradeBreakdown();
            breakdown.setTradeId(constituent.getTrade().getId());
            breakdown.setReferenceEntity(constituent.getTrade().getReferenceEntity());
            breakdown.setNotional(constituent.getTrade().getNotionalAmount());
            breakdown.setPv(pv);
            breakdown.setCs01(cs01);
            breakdown.setRec01(rec01);
            breakdown.setWeight(weight);
            breakdown.setSector(extractSector(constituent.getTrade()));
            tradeBreakdowns.add(breakdown);
        }
        
        aggregate.setPv(totalPv);
        aggregate.setAccrued(totalAccrued);
        aggregate.setPremiumLegPv(totalPremiumLegPv);
        aggregate.setProtectionLegPv(totalProtectionLegPv);
        aggregate.setFairSpreadBpsWeighted(weightedFairSpread);
        aggregate.setCs01(totalCs01);
        aggregate.setRec01(totalRec01);
        aggregate.setJtd(totalJtd);
        
        response.setAggregate(aggregate);
        response.setByTrade(tradeBreakdowns);
        
        // Concentration metrics
        ConcentrationMetrics concentration = calculateConcentration(tradeBreakdowns, totalCs01);
        response.setConcentration(concentration);
        
        // Completeness
        CompletenessMetrics completeness = new CompletenessMetrics();
        completeness.setConstituents(constituents.size());
        completeness.setPriced(riskMeasures.size());
        response.setCompleteness(completeness);
        
        // Cache the result
        cacheRiskResult(portfolioId, valuationDate, response);
        
        return response;
    }
    
    @Transactional(readOnly = true)
    public Optional<PortfolioPricingResponse> getCachedRiskSummary(Long portfolioId) {
        Optional<PortfolioRiskCache> cache = riskCacheRepository.findLatestByPortfolioId(portfolioId);
        return cache.map(this::convertCacheToResponse);
    }
    
    private List<Map<String, Object>> callRiskEngine(List<Long> tradeIds, LocalDate valuationDate) {
        String url = riskEngineUrl + "/api/risk/scenario/calculate";
        
        Map<String, Object> request = new HashMap<>();
        request.put("tradeIds", tradeIds);
        request.put("valuationDate", valuationDate.toString());
        request.put("scenarioId", "portfolio-pricing");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<List> response = restTemplate.postForEntity(url, entity, List.class);
            return (List<Map<String, Object>>) response.getBody();
        } catch (Exception e) {
            logger.error("Failed to call risk engine", e);
            throw new RuntimeException("Risk engine calculation failed: " + e.getMessage());
        }
    }
    
    private Map<Long, BigDecimal> calculateNormalizedWeights(List<CdsPortfolioConstituent> constituents) {
        Map<Long, BigDecimal> weights = new HashMap<>();
        
        BigDecimal totalNotional = constituents.stream()
                .map(c -> c.getTrade().getNotionalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        for (CdsPortfolioConstituent c : constituents) {
            BigDecimal weight;
            if (c.getWeightType() == WeightType.PERCENT) {
                weight = c.getWeightValue();
            } else {
                // NOTIONAL: normalize by total
                weight = c.getTrade().getNotionalAmount().divide(totalNotional, 8, RoundingMode.HALF_UP);
            }
            weights.put(c.getTrade().getId(), weight);
        }
        
        return weights;
    }
    
    private ConcentrationMetrics calculateConcentration(List<TradeBreakdown> breakdowns, BigDecimal totalCs01) {
        ConcentrationMetrics metrics = new ConcentrationMetrics();
        
        // Top 5 contributors by CS01
        List<TradeBreakdown> sorted = breakdowns.stream()
                .sorted((a, b) -> b.getCs01().compareTo(a.getCs01()))
                .limit(5)
                .collect(Collectors.toList());
        
        BigDecimal top5Cs01 = sorted.stream()
                .map(TradeBreakdown::getCs01)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal top5Pct = totalCs01.compareTo(BigDecimal.ZERO) > 0
                ? top5Cs01.divide(totalCs01, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        metrics.setTop5PctCs01(top5Pct);
        
        // Sector breakdown
        Map<String, BigDecimal> sectorCs01 = breakdowns.stream()
                .collect(Collectors.groupingBy(
                        TradeBreakdown::getSector,
                        Collectors.reducing(BigDecimal.ZERO, TradeBreakdown::getCs01, BigDecimal::add)
                ));
        
        List<SectorBreakdown> sectorBreakdowns = sectorCs01.entrySet().stream()
                .map(e -> {
                    BigDecimal pct = totalCs01.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().divide(totalCs01, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : BigDecimal.ZERO;
                    return new SectorBreakdown(e.getKey(), pct);
                })
                .sorted((a, b) -> b.getCs01Pct().compareTo(a.getCs01Pct()))
                .collect(Collectors.toList());
        
        metrics.setSectorBreakdown(sectorBreakdowns);
        
        return metrics;
    }
    
    private String extractSector(CDSTrade trade) {
        // For now, derive from reference entity or default
        // In Epic 11 this should be a proper field
        String entity = trade.getReferenceEntity().toUpperCase();
        if (entity.contains("TECH") || entity.matches("(AAPL|MSFT|GOOGL|META|AMZN)")) {
            return "TECH";
        } else if (entity.matches("(JPM|GS|BAC|C|WFC)")) {
            return "FINANCIALS";
        } else if (entity.matches("(XOM|CVX|BP)")) {
            return "ENERGY";
        }
        return DEFAULT_SECTOR;
    }
    
    private void cacheRiskResult(Long portfolioId, LocalDate valuationDate, PortfolioPricingResponse response) {
        try {
            // Check if cache entry already exists and update it, otherwise create new
            PortfolioRiskCache cache = riskCacheRepository
                    .findByPortfolioIdAndValuationDate(portfolioId, valuationDate)
                    .orElseGet(() -> {
                        PortfolioRiskCache newCache = new PortfolioRiskCache();
                        newCache.setPortfolio(portfolioRepository.findById(portfolioId).orElseThrow());
                        newCache.setValuationDate(valuationDate);
                        return newCache;
                    });
            
            // Update all values
            cache.setAggregatePv(response.getAggregate().getPv());
            cache.setAggregateAccrued(response.getAggregate().getAccrued());
            cache.setPremiumLegPv(response.getAggregate().getPremiumLegPv());
            cache.setProtectionLegPv(response.getAggregate().getProtectionLegPv());
            cache.setFairSpreadBpsWeighted(response.getAggregate().getFairSpreadBpsWeighted());
            cache.setCs01(response.getAggregate().getCs01());
            cache.setRec01(response.getAggregate().getRec01());
            cache.setJtd(response.getAggregate().getJtd());
            cache.setTop5PctCs01(response.getConcentration().getTop5PctCs01());
            cache.setSectorBreakdown(objectMapper.writeValueAsString(response.getConcentration().getSectorBreakdown()));
            cache.setByTradeBreakdown(objectMapper.writeValueAsString(response.getByTrade()));
            cache.setCompletenessConstituents(response.getCompleteness().getConstituents());
            cache.setCompletenessPriced(response.getCompleteness().getPriced());
            cache.setCalculatedAt(LocalDateTime.now()); // Update timestamp
            
            riskCacheRepository.save(cache);
        } catch (JsonProcessingException e) {
            logger.error("Failed to cache risk result", e);
        }
    }
    
    private PortfolioPricingResponse convertCacheToResponse(PortfolioRiskCache cache) {
        PortfolioPricingResponse response = new PortfolioPricingResponse();
        response.setPortfolioId(cache.getPortfolio().getId());
        response.setValuationDate(cache.getValuationDate().toString());
        
        AggregateMetrics aggregate = new AggregateMetrics();
        aggregate.setPv(cache.getAggregatePv());
        aggregate.setAccrued(cache.getAggregateAccrued());
        aggregate.setPremiumLegPv(cache.getPremiumLegPv());
        aggregate.setProtectionLegPv(cache.getProtectionLegPv());
        aggregate.setFairSpreadBpsWeighted(cache.getFairSpreadBpsWeighted());
        aggregate.setCs01(cache.getCs01());
        aggregate.setRec01(cache.getRec01());
        aggregate.setJtd(cache.getJtd());
        response.setAggregate(aggregate);
        
        try {
            if (cache.getByTradeBreakdown() != null) {
                List<TradeBreakdown> byTrade = objectMapper.readValue(
                        cache.getByTradeBreakdown(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, TradeBreakdown.class)
                );
                response.setByTrade(byTrade);
            }
            
            if (cache.getSectorBreakdown() != null) {
                ConcentrationMetrics concentration = new ConcentrationMetrics();
                concentration.setTop5PctCs01(cache.getTop5PctCs01());
                List<SectorBreakdown> sectorBreakdown = objectMapper.readValue(
                        cache.getSectorBreakdown(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SectorBreakdown.class)
                );
                concentration.setSectorBreakdown(sectorBreakdown);
                response.setConcentration(concentration);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize cached data", e);
        }
        
        CompletenessMetrics completeness = new CompletenessMetrics();
        completeness.setConstituents(cache.getCompletenessConstituents());
        completeness.setPriced(cache.getCompletenessPriced());
        response.setCompleteness(completeness);
        
        return response;
    }
}
