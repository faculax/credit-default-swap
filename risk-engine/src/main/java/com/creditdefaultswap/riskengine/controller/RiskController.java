package com.creditdefaultswap.riskengine.controller;

import com.creditdefaultswap.riskengine.model.RiskMeasures;
import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.model.StressScenarioRequest;
import com.creditdefaultswap.riskengine.model.StressImpactResult;
import com.creditdefaultswap.riskengine.service.RiskCalculationService;
import com.creditdefaultswap.riskengine.service.RiskEnrichmentClient;
import com.creditdefaultswap.riskengine.service.StressTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskController.class);

    private final RiskCalculationService calcService;
    private final RiskEnrichmentClient enrichmentClient;
    private final StressTestService stressTestService;

    @Autowired
    public RiskController(RiskCalculationService calcService, RiskEnrichmentClient enrichmentClient, StressTestService stressTestService) {
        this.calcService = calcService;
        this.enrichmentClient = enrichmentClient;
        this.stressTestService = stressTestService;
    }

    /**
     * Calculate risk measures for multiple trades with scenario analysis
     */
    @PostMapping("/scenario/calculate")
    public CompletableFuture<ResponseEntity<List<RiskMeasures>>> calculateScenario(
            @RequestBody ScenarioRequest request) {
        logger.info("Risk Calculation Request - Scenario: {}, Trades: {}, Valuation Date: {}", 
            request.getScenarioId(), request.getTradeIds(), request.getValuationDate());
        if (request.getScenarios() != null && !request.getScenarios().isEmpty()) {
            logger.info("Market Scenarios: {}", request.getScenarios());
        }
        
        return calcService.calculateRiskMeasures(request)
            .handle((riskMeasuresList, throwable) -> {
                if (throwable != null) {
                    logger.error("Scenario calculation failed", throwable);
                    return ResponseEntity.<List<RiskMeasures>>internalServerError().build();
                }
                
                // Enrich each risk measure with platform-specific data from backend
                for (int i = 0; i < riskMeasuresList.size(); i++) {
                    RiskMeasures rm = riskMeasuresList.get(i);
                    Long tradeId = request.getTradeIds().get(i);
                    enrichmentClient.enrichRiskMeasures(rm, tradeId);
                }
                
                return ResponseEntity.ok(riskMeasuresList);
            });
    }
    
    /**
     * Stress test analysis - tests recovery rate and spread scenarios
     */
    @PostMapping("/stress/analyze")
    public CompletableFuture<ResponseEntity<StressImpactResult>> analyzeStress(
            @RequestBody StressScenarioRequest request) {
        
        int expectedScenarios = 0;
        if (request.getRecoveryRates() != null) expectedScenarios += request.getRecoveryRates().size();
        if (request.getSpreadShifts() != null) expectedScenarios += request.getSpreadShifts().size();
        if (request.isCombined() && request.getRecoveryRates() != null && request.getSpreadShifts() != null) {
            expectedScenarios += request.getRecoveryRates().size() * request.getSpreadShifts().size();
        }
        
        logger.info("Stress test request: trade={}, scenarios={}", 
            request.getTradeId(), expectedScenarios);
        
        long startTime = System.currentTimeMillis();
        
        return stressTestService.runStressAnalysis(request)
            .handle((result, throwable) -> {
                long totalTime = System.currentTimeMillis() - startTime;
                
                if (throwable != null) {
                    logger.error("Stress test FAILED for trade {} after {} ms", 
                        request.getTradeId(), totalTime, throwable);
                    return ResponseEntity.<StressImpactResult>internalServerError().build();
                }
                
                logger.info("Stress test COMPLETED: trade={}, scenarios={}, time={}ms", 
                    request.getTradeId(), result.getScenarios().size(), totalTime);
                
                return ResponseEntity.ok(result);
            });
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEngineStatus() {
        logger.debug("Getting risk engine status");
        Map<String, Object> status = calcService.getEngineStatus();
        status.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String,String>> health(){
        Map<String, String> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("engineVersion", "ore-only-1.0");
        healthInfo.put("implementation", "ORE");
        healthInfo.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(healthInfo);
    }
}
