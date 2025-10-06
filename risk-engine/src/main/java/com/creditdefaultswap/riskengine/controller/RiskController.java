package com.creditdefaultswap.riskengine.controller;

import com.creditdefaultswap.riskengine.model.RiskMeasures;
import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.service.RiskCalculationService;
import com.creditdefaultswap.riskengine.service.RiskEnrichmentClient;
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

    @Autowired
    public RiskController(RiskCalculationService calcService, RiskEnrichmentClient enrichmentClient) {
        this.calcService = calcService;
        this.enrichmentClient = enrichmentClient;
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
