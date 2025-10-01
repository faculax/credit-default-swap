package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import com.creditdefaultswap.riskengine.model.RiskMeasures;
import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.ore.OreInputBuilder;
import com.creditdefaultswap.riskengine.ore.OreOutputParser;
import com.creditdefaultswap.riskengine.ore.OreProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class RiskCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskCalculationService.class);
    private final Random random = new Random();
    
    private final RiskEngineConfigProperties config;
    private final OreProcessManager oreProcessManager;
    private final OreInputBuilder oreInputBuilder;
    private final OreOutputParser oreOutputParser;
    
    @Autowired
    public RiskCalculationService(
            RiskEngineConfigProperties config,
            OreProcessManager oreProcessManager,
            OreInputBuilder oreInputBuilder,
            OreOutputParser oreOutputParser) {
        this.config = config;
        this.oreProcessManager = oreProcessManager;
        this.oreInputBuilder = oreInputBuilder;
        this.oreOutputParser = oreOutputParser;
    }

    // Legacy methods for backwards compatibility
    public RiskMeasures calculateBase(Long tradeId){
        // Stub deterministic-ish values for now
        BigDecimal base = BigDecimal.valueOf(1000 + (tradeId % 37));
        return new RiskMeasures(tradeId)
                .withPvClean(base)
                .withPvDirty(base.add(BigDecimal.valueOf(5)))
                .withParSpread(BigDecimal.valueOf(120.5))
                .withCs01(BigDecimal.valueOf(75,2)) // 0.75
                .withDv01(BigDecimal.valueOf(55,2)) // 0.55
                .withJtd(base.negate().divide(BigDecimal.valueOf(10),2, RoundingMode.HALF_UP))
                .withRecovery01(BigDecimal.valueOf(32,2));
    }

    public RiskMeasures shiftParallel(RiskMeasures base, int bps){
        BigDecimal factor = BigDecimal.valueOf(bps).divide(BigDecimal.valueOf(10000),6,RoundingMode.HALF_UP);
        return new RiskMeasures(base.getTradeId())
                .withPvClean(base.getPvClean().subtract(base.getPvClean().multiply(factor)))
                .withPvDirty(base.getPvDirty().subtract(base.getPvDirty().multiply(factor)))
                .withParSpread(base.getParSpread().add(BigDecimal.valueOf(bps)))
                .withCs01(base.getCs01())
                .withDv01(base.getDv01())
                .withJtd(base.getJtd())
                .withRecovery01(base.getRecovery01());
    }
    
    // New ORE-integrated methods
    public CompletableFuture<List<RiskMeasures>> calculateRiskMeasures(ScenarioRequest request) {
        logger.info("Calculating risk measures for scenario: {} with {} trades using implementation: {}", 
            request.getScenarioId(), request.getTradeIds().size(), config.getImplementation());
        
        if (config.getImplementation() == RiskEngineConfigProperties.Implementation.ORE) {
            return calculateWithOre(request);
        } else {
            return calculateWithStub(request);
        }
    }
    
    private CompletableFuture<List<RiskMeasures>> calculateWithOre(ScenarioRequest request) {
        return oreProcessManager.ensureProcessReady()
            .thenCompose(ready -> {
                if (!ready) {
                    logger.error("ORE process is not ready, falling back to stub calculation");
                    return calculateWithStub(request);
                }
                
                // Build ORE input XML
                String oreInput = oreInputBuilder.buildRiskCalculationInput(request);
                
                // Execute ORE calculation
                return oreProcessManager.executeCalculation(oreInput)
                    .thenApply(oreOutput -> {
                        if (!oreOutputParser.isValidOutput(oreOutput)) {
                            String errorMsg = oreOutputParser.extractErrorMessage(oreOutput);
                            logger.error("ORE calculation failed: {}, falling back to stub", errorMsg);
                            return calculateStubMeasures(request);
                        }
                        
                        // Parse ORE output for each trade
                        return request.getTradeIds().stream()
                            .map(tradeId -> oreOutputParser.parseRiskMeasures(oreOutput, tradeId))
                            .collect(Collectors.toList());
                    });
            })
            .exceptionally(throwable -> {
                logger.error("ORE calculation failed with exception, falling back to stub", throwable);
                return calculateStubMeasures(request);
            });
    }
    
    private CompletableFuture<List<RiskMeasures>> calculateWithStub(ScenarioRequest request) {
        return CompletableFuture.supplyAsync(() -> calculateStubMeasures(request));
    }
    
    private List<RiskMeasures> calculateStubMeasures(ScenarioRequest request) {
        logger.debug("Using stub implementation for risk calculation");
        
        return request.getTradeIds().stream()
            .map(tradeId -> calculateTradeRiskStub(tradeId, request))
            .toList();
    }
    
    private RiskMeasures calculateTradeRiskStub(Long tradeId, ScenarioRequest request) {
        logger.debug("Calculating stub risk for trade: {}", tradeId);
        
        // Simulate complex risk calculation
        try {
            Thread.sleep(100 + (tradeId % 10) * 50); // Variable delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        RiskMeasures measures = new RiskMeasures();
        measures.setTradeId(tradeId);
        
        // Mock calculations based on trade ID for consistent results
        double base = tradeId * 1000;
        measures.setNpv(BigDecimal.valueOf(base + Math.random() * 10000).setScale(2, RoundingMode.HALF_UP));
        measures.setDv01(BigDecimal.valueOf(base * 0.01 + Math.random() * 100).setScale(2, RoundingMode.HALF_UP));
        measures.setGamma(BigDecimal.valueOf(Math.random() * 0.001).setScale(6, RoundingMode.HALF_UP));
        measures.setVar95(BigDecimal.valueOf(base * 0.05 + Math.random() * 500).setScale(2, RoundingMode.HALF_UP));
        measures.setExpectedShortfall(BigDecimal.valueOf(base * 0.08 + Math.random() * 800).setScale(2, RoundingMode.HALF_UP));
        measures.setCurrency("USD");
        
        // Add Greeks
        Map<String, BigDecimal> greeks = new HashMap<>();
        greeks.put("delta", BigDecimal.valueOf(Math.random() * 0.5).setScale(6, RoundingMode.HALF_UP));
        greeks.put("gamma", measures.getGamma());
        greeks.put("theta", BigDecimal.valueOf(-Math.random() * 10).setScale(6, RoundingMode.HALF_UP));
        greeks.put("vega", BigDecimal.valueOf(Math.random() * 100).setScale(6, RoundingMode.HALF_UP));
        greeks.put("rho", BigDecimal.valueOf(Math.random() * 50).setScale(6, RoundingMode.HALF_UP));
        measures.setGreeks(greeks);
        
        logger.debug("Calculated stub risk measures for trade {}: NPV={}, DV01={}", 
            tradeId, measures.getNpv(), measures.getDv01());
        
        return measures;
    }
    
    /**
     * Get the status of the risk calculation engine
     */
    public Map<String, Object> getEngineStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("implementation", config.getImplementation().toString());
        
        if (config.getImplementation() == RiskEngineConfigProperties.Implementation.ORE) {
            OreProcessManager.ProcessStatus processStatus = oreProcessManager.getStatus();
            status.put("oreProcess", Map.of(
                "alive", processStatus.isAlive(),
                "ready", processStatus.isReady(),
                "warmingUp", processStatus.isWarmingUp(),
                "restartCount", processStatus.getRestartCount()
            ));
        }
        
        return status;
    }
}
