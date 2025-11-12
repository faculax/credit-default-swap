package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.annotation.TrackLineage;
import com.creditdefaultswap.platform.annotation.LineageOperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for automated margin statement generation
 */
@RestController
@RequestMapping("/api/automated-margin")
@CrossOrigin(origins = "*")
public class AutomatedMarginController {
    
    private static final Logger logger = LoggerFactory.getLogger(AutomatedMarginController.class);
    
    /**
     * Generate automated VM/IM statements using existing CCP data and netting sets
     */
    @PostMapping("/generate")
    @TrackLineage(
        operationType = LineageOperationType.MARGIN,
        operation = "AUTO_MARGIN_GENERATE",
        autoExtractDetails = true
    )
    public ResponseEntity<?> generateAutomatedStatements(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate statementDate) {
        
        try {
            if (statementDate == null) {
                statementDate = LocalDate.now();
            }
            
            logger.info("Generating automated margin statements for date: {}", statementDate);
            
            // Simple mock response for now demonstrating automated generation capability
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Automated margin statements generated successfully");
            response.put("statementDate", statementDate);
            response.put("generatedStatements", List.of(createMockGeneratedStatement(statementDate)));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating automated statements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to generate automated statements: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get summary of automated margin generation capabilities
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getGenerationSummary(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate statementDate) {
        
        try {
            if (statementDate == null) {
                statementDate = LocalDate.now();
            }
            
            logger.info("Getting automated margin generation summary for date: {}", statementDate);
            
            // Mock response showing available netting sets for generation
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("service", "AutomatedMarginStatementGeneration");
            response.put("status", "AVAILABLE");
            response.put("statementDate", statementDate);
            response.put("availableNettingSets", 4);
            response.put("totalTradeCount", 7);
            response.put("totalNotional", "111,000,000 USD equivalent");
            response.put("ccps", List.of("LCH", "CME"));
            response.put("description", "Generates VM/IM statements using existing CCP and netting set data");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting generation summary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to get generation summary: " + e.getMessage()
            ));
        }
    }
    
    private Map<String, Object> createMockGeneratedStatement(LocalDate statementDate) {
        Map<String, Object> statement = new java.util.HashMap<>();
        statement.put("nettingSetId", "LCH-HOUSE-001-USD");
        statement.put("ccpName", "LCH");
        statement.put("accountId", "HOUSE-001");
        statement.put("statementDate", statementDate);
        statement.put("variationMarginNet", -15000.00);
        statement.put("initialMarginRequired", 250000.00);
        statement.put("excessMargin", 50000.00);
        statement.put("currency", "USD");
        statement.put("tradeCount", 6);
        statement.put("totalNotional", 92000000.00);
        statement.put("calculationStatus", "COMPLETED");
        statement.put("processingTime", "< 1 second");
        return statement;
    }
}