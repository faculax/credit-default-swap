package com.creditdefaultswap.riskengine.controller;

import com.creditdefaultswap.riskengine.model.RiskMeasures;
import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.service.RiskCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskController.class);

    private final RiskCalculationService calcService;

    @Autowired
    public RiskController(RiskCalculationService calcService) {
        this.calcService = calcService;
    }

    // Legacy endpoints for backwards compatibility
    @GetMapping("/cds/{tradeId}")
    public ResponseEntity<RiskMeasures> getBase(@PathVariable Long tradeId){
        logger.info("Getting base CDS risk measures for trade: {}", tradeId);
        return ResponseEntity.ok(calcService.calculateBase(tradeId));
    }

    @PostMapping("/cds/{tradeId}/scenarios")
    public ResponseEntity<Map<String,Object>> runScenarios(@PathVariable Long tradeId,
                                                           @RequestBody ScenarioRequest request){
        logger.info("Running CDS scenarios for trade: {}", tradeId);
        RiskMeasures base = calcService.calculateBase(tradeId);
        List<Map<String,Object>> scenarios = new ArrayList<>();
        if(request.getParallelBpsShifts()!=null){
            for(BigDecimal shift : request.getParallelBpsShifts()){
                int bps = shift.intValue();
                RiskMeasures shifted = calcService.shiftParallel(base, bps);
                Map<String,Object> entry = new LinkedHashMap<>();
                entry.put("scenario","PARALLEL_"+bps+"BP");
                entry.put("measures", shifted);
                scenarios.add(entry);
            }
        }
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("base", base);
        response.put("scenarios", scenarios);
        return ResponseEntity.ok(response);
    }
    
    // Additional legacy endpoints
    @GetMapping("/measures/{tradeId}")
    public ResponseEntity<RiskMeasures> getRiskMeasures(@PathVariable Long tradeId) {
        logger.info("Getting base risk measures for trade: {}", tradeId);
        RiskMeasures measures = calcService.calculateBase(tradeId);
        return ResponseEntity.ok(measures);
    }
    
    @PostMapping("/measures/{tradeId}/shift")
    public ResponseEntity<RiskMeasures> getShiftedRiskMeasures(
            @PathVariable Long tradeId,
            @RequestParam int bps) {
        logger.info("Getting shifted risk measures for trade: {} with shift: {} bps", tradeId, bps);
        RiskMeasures baseMeasures = calcService.calculateBase(tradeId);
        RiskMeasures shiftedMeasures = calcService.shiftParallel(baseMeasures, bps);
        return ResponseEntity.ok(shiftedMeasures);
    }
    
    // New ORE-integrated endpoints
    @PostMapping("/scenario/calculate")
    public CompletableFuture<ResponseEntity<List<RiskMeasures>>> calculateScenario(
            @RequestBody ScenarioRequest request) {
        logger.info("Calculating scenario: {} for {} trades", 
            request.getScenarioId(), request.getTradeIds().size());
        
        return calcService.calculateRiskMeasures(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                logger.error("Scenario calculation failed", throwable);
                return ResponseEntity.internalServerError().build();
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
        healthInfo.put("engineVersion", "ore-integrated-0.1");
        healthInfo.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(healthInfo);
    }
}
