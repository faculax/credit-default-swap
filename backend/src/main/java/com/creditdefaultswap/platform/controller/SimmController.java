package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.service.simm.CrifParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for SIMM (Standard Initial Margin Model) functionality.
 * 
 * Provides endpoints for:
 * - CRIF file upload and processing
 * - SIMM calculation execution
 * - Results retrieval and audit trail access
 * - Parameter set management
 */
@RestController
@RequestMapping("/api/simm")
@CrossOrigin(origins = "*")
public class SimmController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimmController.class);
    
    @Autowired
    private CrifParserService crifParserService;
    
    /**
     * Upload and process a CRIF file
     */
    @PostMapping("/crif/upload")
    public ResponseEntity<?> uploadCrifFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "portfolioId", required = false) String portfolioId,
            @RequestParam(value = "valuationDate", required = false) String valuationDateStr,
            @RequestParam(value = "currency", defaultValue = "USD") String currency) {
        
        try {
            logger.info("Received CRIF file upload: {}", file.getOriginalFilename());
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
            }
            
            if (portfolioId == null || portfolioId.trim().isEmpty()) {
                portfolioId = "DEFAULT_PORTFOLIO";
            }
            
            LocalDate valuationDate = LocalDate.now();
            if (valuationDateStr != null && !valuationDateStr.trim().isEmpty()) {
                try {
                    valuationDate = LocalDate.parse(valuationDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid valuation date format. Use YYYY-MM-DD"));
                }
            }
            
            // Parse CRIF file
            CrifParserService.CrifParsingResult result = crifParserService.parseCrifFile(
                file, portfolioId, valuationDate, currency);
            
            Map<String, Object> response = new HashMap<>();
            response.put("filename", result.getFilename());
            response.put("portfolioId", result.getPortfolioId());
            response.put("valuationDate", result.getValuationDate().toString());
            response.put("currency", result.getCurrency());
            response.put("totalRecords", result.getTotalRecords());
            response.put("validRecords", result.getValidRecords());
            response.put("errorRecords", result.getErrorRecords());
            response.put("successRate", result.getSuccessRate());
            response.put("hasErrors", result.hasErrors());
            
            if (result.hasErrors()) {
                response.put("errors", result.getErrors().stream()
                    .map(error -> Map.of(
                        "lineNumber", error.getLineNumber(),
                        "message", error.getMessage()
                    ))
                    .limit(10) // Limit to first 10 errors
                    .toList());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("CRIF file upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
    
    /**
     * Execute SIMM calculation for a portfolio
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> executeCalculation(@RequestBody SimmCalculationRequest request) {
        try {
            logger.info("Executing SIMM calculation for portfolio: {}", request.getPortfolioId());
            
            // For now, return a mock calculation result
            // TODO: Implement actual SIMM calculation once calculation engine is integrated
            
            Map<String, Object> response = new HashMap<>();
            response.put("calculationId", "CALC-" + System.currentTimeMillis());
            response.put("portfolioId", request.getPortfolioId());
            response.put("status", "COMPLETED");
            response.put("totalInitialMargin", 15750000.00);
            response.put("calculationDate", request.getCalculationDate() != null ? 
                request.getCalculationDate() : LocalDate.now());
            response.put("completedAt", LocalDate.now());
            
            // Mock breakdown by product class
            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("Credit", 8500000.00);
            breakdown.put("RatesFX", 4750000.00);
            breakdown.put("Equity", 2000000.00);
            breakdown.put("Commodity", 500000.00);
            response.put("marginByProductClass", breakdown);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("SIMM calculation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Calculation failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get SIMM calculations for a portfolio
     */
    @GetMapping("/portfolio/{portfolioId}/calculations")
    public ResponseEntity<?> getCalculationsForPortfolio(@PathVariable String portfolioId) {
        try {
            logger.info("Getting SIMM calculations for portfolio: {}", portfolioId);
            
            // For now, return mock calculation history
            // TODO: Implement actual calculation retrieval from database
            
            List<Map<String, Object>> calculations = new ArrayList<>();
            
            // Add some mock calculation history
            if ("ALL".equals(portfolioId) || "TEST_PORTFOLIO".equals(portfolioId)) {
                Map<String, Object> calc1 = new HashMap<>();
                calc1.put("calculationId", "CALC-" + (System.currentTimeMillis() - 86400000)); // Yesterday
                calc1.put("portfolioId", "TEST_PORTFOLIO");
                calc1.put("status", "COMPLETED");
                calc1.put("totalInitialMargin", 15750000.00);
                calc1.put("calculationDate", "2024-10-08");
                calc1.put("completedAt", "2024-10-08T10:30:00");
                calculations.add(calc1);
                
                Map<String, Object> calc2 = new HashMap<>();
                calc2.put("calculationId", "CALC-" + (System.currentTimeMillis() - 172800000)); // 2 days ago
                calc2.put("portfolioId", "PORTFOLIO_001");
                calc2.put("status", "COMPLETED");
                calc2.put("totalInitialMargin", 8250000.00);
                calc2.put("calculationDate", "2024-10-07");
                calc2.put("completedAt", "2024-10-07T15:45:00");
                calculations.add(calc2);
            }
            
            return ResponseEntity.ok(calculations);
            
        } catch (Exception e) {
            logger.error("Failed to get calculations for portfolio: " + portfolioId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve calculations: " + e.getMessage()));
        }
    }
    
    /**
     * Get calculation results for a specific calculation
     */
    @GetMapping("/calculation/{calculationId}/results")
    public ResponseEntity<?> getCalculationResults(@PathVariable String calculationId) {
        try {
            logger.info("Getting results for calculation: {}", calculationId);
            
            // Mock detailed results breakdown
            List<Map<String, Object>> results = new ArrayList<>();
            
            // Credit results
            Map<String, Object> creditResult = new HashMap<>();
            creditResult.put("resultId", 1);
            creditResult.put("productClass", "Credit");
            creditResult.put("riskClass", "Credit_Q");
            creditResult.put("bucket", "1");
            creditResult.put("initialMargin", 8500000.00);
            creditResult.put("calculationStep", "Delta Risk Weight");
            results.add(creditResult);
            
            // RatesFX results
            Map<String, Object> ratesFxResult = new HashMap<>();
            ratesFxResult.put("resultId", 2);
            ratesFxResult.put("productClass", "RatesFX");
            ratesFxResult.put("riskClass", "Interest Rate");
            ratesFxResult.put("bucket", "USD");
            ratesFxResult.put("initialMargin", 4750000.00);
            ratesFxResult.put("calculationStep", "Vega Risk Weight");
            results.add(ratesFxResult);
            
            // Equity results
            Map<String, Object> equityResult = new HashMap<>();
            equityResult.put("resultId", 3);
            equityResult.put("productClass", "Equity");
            equityResult.put("riskClass", "Equity");
            equityResult.put("bucket", "1");
            equityResult.put("initialMargin", 2000000.00);
            equityResult.put("calculationStep", "Delta Risk Weight");
            results.add(equityResult);
            
            // Commodity results
            Map<String, Object> commodityResult = new HashMap<>();
            commodityResult.put("resultId", 4);
            commodityResult.put("productClass", "Commodity");
            commodityResult.put("riskClass", "Commodity");
            commodityResult.put("bucket", "1");
            commodityResult.put("initialMargin", 500000.00);
            commodityResult.put("calculationStep", "Delta Risk Weight");
            results.add(commodityResult);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Failed to get results for calculation: " + calculationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve results: " + e.getMessage()));
        }
    }
    
    /**
     * Get audit trail for a specific calculation
     */
    @GetMapping("/calculation/{calculationId}/audit")
    public ResponseEntity<?> getCalculationAudit(@PathVariable String calculationId) {
        try {
            logger.info("Getting audit trail for calculation: {}", calculationId);
            
            // Mock audit trail
            List<Map<String, Object>> auditTrail = new ArrayList<>();
            
            Map<String, Object> step1 = new HashMap<>();
            step1.put("step", "Initialization");
            step1.put("timestamp", "2024-10-08T10:25:00");
            step1.put("description", "SIMM calculation started for portfolio");
            auditTrail.add(step1);
            
            Map<String, Object> step2 = new HashMap<>();
            step2.put("step", "CRIF Processing");
            step2.put("timestamp", "2024-10-08T10:26:15");
            step2.put("description", "Processed 1,247 CRIF records across 4 product classes");
            auditTrail.add(step2);
            
            Map<String, Object> step3 = new HashMap<>();
            step3.put("step", "Risk Weight Application");
            step3.put("timestamp", "2024-10-08T10:27:30");
            step3.put("description", "Applied ISDA SIMM 2.6 risk weights by asset class");
            auditTrail.add(step3);
            
            Map<String, Object> step4 = new HashMap<>();
            step4.put("step", "Correlation Adjustment");
            step4.put("timestamp", "2024-10-08T10:28:45");
            step4.put("description", "Applied cross-bucket and cross-risk correlations");
            auditTrail.add(step4);
            
            Map<String, Object> step5 = new HashMap<>();
            step5.put("step", "Portfolio Aggregation");
            step5.put("timestamp", "2024-10-08T10:29:30");
            step5.put("description", "Aggregated margin requirements across product classes");
            auditTrail.add(step5);
            
            Map<String, Object> step6 = new HashMap<>();
            step6.put("step", "Calculation Complete");
            step6.put("timestamp", "2024-10-08T10:30:00");
            step6.put("description", "Total Initial Margin: $15,750,000 calculated successfully");
            auditTrail.add(step6);
            
            return ResponseEntity.ok(auditTrail);
            
        } catch (Exception e) {
            logger.error("Failed to get audit trail for calculation: " + calculationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve audit trail: " + e.getMessage()));
        }
    }

    /**
     * Get SIMM parameters
     */
    @GetMapping("/parameters")
    public ResponseEntity<?> getActiveParameterSets() {
        try {
            // Mock SIMM parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("version", "ISDA SIMM 2.6");
            parameters.put("effectiveDate", "2023-04-01");
            parameters.put("currency", "USD");
            parameters.put("regulatoryRegime", "EMIR");
            
            Map<String, Object> riskWeights = new HashMap<>();
            riskWeights.put("Credit_IG_1Y", 0.0038);
            riskWeights.put("Credit_IG_2Y", 0.0040);
            riskWeights.put("Credit_IG_3Y", 0.0043);
            riskWeights.put("Credit_IG_5Y", 0.0048);
            riskWeights.put("Credit_IG_10Y", 0.0055);
            parameters.put("riskWeights", riskWeights);
            
            return ResponseEntity.ok(parameters);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve parameter sets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve parameter sets: " + e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDate.now());
        health.put("service", "SIMM");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Request DTO for SIMM calculations
     */
    public static class SimmCalculationRequest {
        private String portfolioId;
        private LocalDate calculationDate;
        private String createdBy;
        private String regulatoryRegime = "EMIR";
        
        // Getters and setters
        public String getPortfolioId() {
            return portfolioId;
        }
        
        public void setPortfolioId(String portfolioId) {
            this.portfolioId = portfolioId;
        }
        
        public LocalDate getCalculationDate() {
            return calculationDate;
        }
        
        public void setCalculationDate(LocalDate calculationDate) {
            this.calculationDate = calculationDate;
        }
        
        public String getCreatedBy() {
            return createdBy;
        }
        
        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
        
        public String getRegulatoryRegime() {
            return regulatoryRegime;
        }
        
        public void setRegulatoryRegime(String regulatoryRegime) {
            this.regulatoryRegime = regulatoryRegime;
        }
    }
}