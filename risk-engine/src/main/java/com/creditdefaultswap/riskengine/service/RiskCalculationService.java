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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class RiskCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskCalculationService.class);
    
    private final RiskEngineConfigProperties config;
    private final OreProcessManager oreProcessManager;
    private final OreInputBuilder oreInputBuilder;
    private final OreOutputParser oreOutputParser;
    private final TradeDataService tradeDataService;
    
    @Autowired
    public RiskCalculationService(
            RiskEngineConfigProperties config,
            OreProcessManager oreProcessManager,
            OreInputBuilder oreInputBuilder,
            OreOutputParser oreOutputParser,
            TradeDataService tradeDataService) {
        this.config = config;
        this.oreProcessManager = oreProcessManager;
        this.oreInputBuilder = oreInputBuilder;
        this.oreOutputParser = oreOutputParser;
        this.tradeDataService = tradeDataService;
    }

    /**
     * Calculate risk measures using ORE. Either succeeds or throws an exception.
     * No fallback to stub data.
     */
    public CompletableFuture<List<RiskMeasures>> calculateRiskMeasures(ScenarioRequest request) {
        logger.info("Calculating risk measures for scenario: {} with {} trades using ORE", 
            request.getScenarioId(), request.getTradeIds().size());
        
        // Only ORE implementation is supported
        return calculateWithOre(request);
    }
    
    private CompletableFuture<List<RiskMeasures>> calculateWithOre(ScenarioRequest request) {
        // Execute ORE in batch mode
        logger.info("Executing ORE batch calculation for scenario: {}", request.getScenarioId());
        
        // Build ORE input XML and get working directory path
        String workingDirPath = oreInputBuilder.buildRiskCalculationInput(request);
        
        // Get valuation date from request (defaults to today if not provided)
        java.time.LocalDate valuationDate = request.getValuationDate() != null ? 
            request.getValuationDate() : java.time.LocalDate.now();
        
        // Execute ORE calculation in batch mode - throw exception on failure
        return oreProcessManager.executeCalculation(workingDirPath)
            .thenApply(oreOutput -> {
                if (!oreOutputParser.isValidOutput(oreOutput)) {
                    String errorMsg = oreOutputParser.extractErrorMessage(oreOutput);
                    logger.error("ORE calculation failed: {}", errorMsg);
                    throw new RuntimeException("ORE calculation failed: " + errorMsg);
                }
                
                // Parse ORE output for each trade with correct currency
                return request.getTradeIds().stream()
                    .map(tradeId -> {
                        // Get trade data to extract currency, passing valuation date for proper effective date adjustment
                        var tradeData = tradeDataService.fetchCDSTradeData(tradeId, valuationDate);
                        String tradeCurrency = tradeData.getCurrency();
                        return oreOutputParser.parseRiskMeasures(oreOutput, tradeId, tradeCurrency, workingDirPath);
                    })
                    .toList();
            })
            .exceptionally(throwable -> {
                logger.error("ORE calculation failed with exception", throwable);
                throw new RuntimeException("ORE calculation failed", throwable);
            });
    }
    
    /**
     * Get the status of the risk calculation engine
     */
    public Map<String, Object> getEngineStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("implementation", "ORE");
        status.put("mode", "batch");
        status.put("ore", Map.of(
            "binaryPath", config.getOre().getBinaryPath(),
            "configPath", config.getOre().getConfigPath(),
            "timeoutSeconds", config.getOre().getTimeoutSeconds(),
            "mode", "batch-execution"
        ));
        
        return status;
    }
}
