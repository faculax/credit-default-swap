package com.creditdefaultswap.platform.controller.saccr;

import com.creditdefaultswap.platform.model.saccr.NettingSet;
import com.creditdefaultswap.platform.model.saccr.SaCcrCalculation;
import com.creditdefaultswap.platform.model.saccr.SaCcrSupervisoryParameter;
import com.creditdefaultswap.platform.service.saccr.SaCcrCalculationService;
import com.creditdefaultswap.platform.service.saccr.SaCcrJurisdictionService;
import com.creditdefaultswap.platform.service.LineageService;
import com.creditdefaultswap.platform.repository.saccr.NettingSetRepository;
import com.creditdefaultswap.platform.repository.saccr.SaCcrCalculationRepository;
import com.creditdefaultswap.platform.repository.saccr.SaCcrSupervisoryParameterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SA-CCR REST Controller
 * Provides endpoints for Basel III Standardized Approach for Counterparty Credit Risk
 */
@RestController
@RequestMapping("/api/v1/sa-ccr")
public class SaCcrController {
    
    private static final Logger logger = LoggerFactory.getLogger(SaCcrController.class);
    
    @Autowired
    private SaCcrCalculationService calculationService;
    
    @Autowired
    private NettingSetRepository nettingSetRepository;
    
    @Autowired
    private SaCcrCalculationRepository calculationRepository;
    
    @Autowired
    private SaCcrSupervisoryParameterRepository supervisoryParameterRepository;
    
    @Autowired
    private SaCcrJurisdictionService jurisdictionService;
    
    @Autowired
    private LineageService lineageService;
    
    // ========== Calculation Endpoints ==========
    
    /**
     * Calculate SA-CCR exposures for all active netting sets
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateAllExposures(
            @RequestParam("valuationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valuationDate,
            @RequestParam(value = "jurisdiction", defaultValue = "US") String jurisdiction) {
        
        try {
            logger.info("Received SA-CCR calculation request for jurisdiction: {} as of {}", jurisdiction, valuationDate);
            
            List<SaCcrCalculation> calculations = calculationService.calculateAllExposures(valuationDate, jurisdiction);
            
            // Track lineage for SA-CCR calculation
            for (SaCcrCalculation calculation : calculations) {
                Map<String, Object> marginDetails = new HashMap<>();
                marginDetails.put("nettingSetId", calculation.getNettingSetId());
                marginDetails.put("ead", calculation.getExposureAtDefault() != null ? calculation.getExposureAtDefault().doubleValue() : 0.0);
                marginDetails.put("replacementCost", calculation.getReplacementCost() != null ? calculation.getReplacementCost().doubleValue() : 0.0);
                marginDetails.put("pfe", calculation.getPotentialFutureExposure() != null ? calculation.getPotentialFutureExposure().doubleValue() : 0.0);
                marginDetails.put("valuationDate", valuationDate.toString());
                marginDetails.put("jurisdiction", jurisdiction);
                marginDetails.put("status", calculation.getCalculationStatus().name());
                lineageService.trackMarginOperation("SA-CCR", String.valueOf(calculation.getNettingSetId()), "system", marginDetails);
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "SA-CCR calculations completed successfully",
                "calculationCount", calculations.size(),
                "valuationDate", valuationDate,
                "jurisdiction", jurisdiction,
                "calculations", calculations
            ));
            
        } catch (Exception e) {
            logger.error("Error during SA-CCR calculation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "ERROR",
                        "message", "Failed to calculate SA-CCR exposures: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Calculate SA-CCR exposure for specific netting set
     */
    @PostMapping("/calculate/{nettingSetId}")
    public ResponseEntity<?> calculateExposureForNettingSet(
            @PathVariable Long nettingSetId,
            @RequestParam("valuationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valuationDate,
            @RequestParam(value = "jurisdiction", defaultValue = "US") String jurisdiction) {
        
        try {
            Optional<NettingSet> nettingSetOpt = nettingSetRepository.findById(nettingSetId);
            if (!nettingSetOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "ERROR", "message", "Netting set not found: " + nettingSetId));
            }
            
            SaCcrCalculation calculation = calculationService.calculateExposure(
                    nettingSetOpt.get(), valuationDate, jurisdiction);
            
            // Save the calculation to database
            SaCcrCalculation savedCalculation = calculationRepository.save(calculation);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "SA-CCR calculation completed",
                "calculation", savedCalculation
            ));
            
        } catch (Exception e) {
            logger.error("Error calculating exposure for netting set {}: {}", nettingSetId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    // ========== Exposure Retrieval Endpoints ==========
    
    /**
     * Get latest SA-CCR calculation for a netting set
     */
    @GetMapping("/exposures/netting-set/{nettingSetId}/latest")
    public ResponseEntity<?> getLatestExposure(@PathVariable Long nettingSetId) {
        try {
            // TODO: Implement getLatestCalculation method
            // SaCcrCalculation calculation = calculationService.getLatestCalculation(nettingSetId);
            
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("status", "ERROR", "message", "Method not yet implemented"));
            
        } catch (Exception e) {
            logger.error("Error retrieving latest exposure for netting set {}: {}", nettingSetId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    /**
     * Get SA-CCR calculation history for a netting set
     */
    @GetMapping("/exposures/netting-set/{nettingSetId}/history")
    public ResponseEntity<?> getCalculationHistory(
            @PathVariable Long nettingSetId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        try {
            // TODO: Implement getCalculationHistory method
            // List<SaCcrCalculation> calculations = calculationService.getCalculationHistory(nettingSetId, fromDate, toDate);
            
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("status", "ERROR", "message", "Method not yet implemented"));
            
        } catch (Exception e) {
            logger.error("Error retrieving calculation history for netting set {}: {}", nettingSetId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    /**
     * Get SA-CCR calculations by date and jurisdiction
     */
    @GetMapping("/exposures")
    public ResponseEntity<?> getCalculationsByDateAndJurisdiction(
            @RequestParam("valuationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valuationDate,
            @RequestParam(value = "jurisdiction", defaultValue = "US") String jurisdiction) {
        
        try {
            logger.info("Retrieving SA-CCR calculations for date: {} and jurisdiction: {}", valuationDate, jurisdiction);
            
            List<SaCcrCalculation> calculations = calculationRepository
                .findByCalculationDateAndJurisdictionOrderByNettingSetId(valuationDate, jurisdiction);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "SA-CCR calculations retrieved successfully",
                "calculationCount", calculations.size(),
                "valuationDate", valuationDate,
                "jurisdiction", jurisdiction,
                "calculations", calculations
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving SA-CCR calculations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    // ========== Netting Set Management Endpoints ==========
    
    /**
     * Get all netting sets
     */
    @GetMapping("/netting-sets")
    public ResponseEntity<?> getAllNettingSets() {
        try {
            List<NettingSet> nettingSets = nettingSetRepository.findAll();
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "nettingSetCount", nettingSets.size(),
                "nettingSets", nettingSets
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving netting sets: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    /**
     * Get netting set by ID
     */
    @GetMapping("/netting-sets/{id}")
    public ResponseEntity<?> getNettingSet(@PathVariable Long id) {
        try {
            Optional<NettingSet> nettingSetOpt = nettingSetRepository.findById(id);
            
            if (!nettingSetOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "ERROR", "message", "Netting set not found: " + id));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "nettingSet", nettingSetOpt.get()
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving netting set {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    /**
     * Create new netting set
     */
    @PostMapping("/netting-sets")
    public ResponseEntity<?> createNettingSet(@RequestBody NettingSet nettingSet) {
        try {
            NettingSet savedNettingSet = nettingSetRepository.save(nettingSet);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "status", "SUCCESS",
                        "message", "Netting set created successfully",
                        "nettingSet", savedNettingSet
                    ));
            
        } catch (Exception e) {
            logger.error("Error creating netting set: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    // ========== Supervisory Parameter Management Endpoints ==========
    
    /**
     * Get supervisory parameters by jurisdiction
     */
    @GetMapping("/supervisory-parameters")
    public ResponseEntity<?> getSupervisoryParameters(
            @RequestParam(value = "jurisdiction", required = false) String jurisdiction,
            @RequestParam(value = "assetClass", required = false) String assetClass) {
        
        try {
            List<SaCcrSupervisoryParameter> parameters;
            
            if (jurisdiction != null && assetClass != null) {
                parameters = supervisoryParameterRepository.findByJurisdictionAndAssetClassOrderByEffectiveDateDesc(
                        jurisdiction, assetClass);
            } else if (jurisdiction != null) {
                parameters = supervisoryParameterRepository.findByJurisdictionOrderByEffectiveDateDesc(jurisdiction);
            } else if (assetClass != null) {
                parameters = supervisoryParameterRepository.findByAssetClassOrderByEffectiveDateDesc(assetClass);
            } else {
                parameters = supervisoryParameterRepository.findAll();
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "parameterCount", parameters.size(),
                "parameters", parameters
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving supervisory parameters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    /**
     * Create new supervisory parameter
     */
    @PostMapping("/supervisory-parameters")
    public ResponseEntity<?> createSupervisoryParameter(@RequestBody SaCcrSupervisoryParameter parameter) {
        try {
            SaCcrSupervisoryParameter savedParameter = supervisoryParameterRepository.save(parameter);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "status", "SUCCESS",
                        "message", "Supervisory parameter created successfully",
                        "parameter", savedParameter
                    ));
            
        } catch (Exception e) {
            logger.error("Error creating supervisory parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    // ========== System Status Endpoints ==========
    
    /**
     * Get SA-CCR system status and configuration
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSystemStatus() {
        try {
            long totalNettingSets = nettingSetRepository.count();
            long totalParameters = supervisoryParameterRepository.count();
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "systemStatus", "OPERATIONAL",
                "activeNettingSets", totalNettingSets,
                "supervisoryParameters", totalParameters,
                "supportedJurisdictions", List.of("US", "EU", "UK", "CA", "JP", "AU"),
                "supportedAssetClasses", List.of("CREDIT", "IR", "FX", "EQUITY", "COMMODITY"),
                "baseLimitAlphaFactor", "1.4"
            ));
            
        } catch (Exception e) {
            logger.error("Error getting system status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    // ========== Jurisdiction-Specific Endpoints ==========
    
    /**
     * Get all supported jurisdictions with their configurations
     */
    @GetMapping("/jurisdictions")
    public ResponseEntity<?> getSupportedJurisdictions() {
        try {
            List<SaCcrJurisdictionService.JurisdictionConfig> jurisdictions = 
                jurisdictionService.getSupportedJurisdictions();
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "jurisdictionCount", jurisdictions.size(),
                "jurisdictions", jurisdictions
            ));
            
        } catch (Exception e) {
            logger.error("Error getting supported jurisdictions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    /**
     * Get jurisdiction-specific configuration and parameters
     */
    @GetMapping("/jurisdictions/{jurisdiction}")
    public ResponseEntity<?> getJurisdictionDetails(
            @PathVariable String jurisdiction,
            @RequestParam(value = "asOfDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        try {
            if (asOfDate == null) {
                asOfDate = LocalDate.now();
            }
            
            if (!jurisdictionService.isJurisdictionSupported(jurisdiction)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "ERROR", "message", "Unsupported jurisdiction: " + jurisdiction));
            }
            
            SaCcrJurisdictionService.JurisdictionConfig config = 
                jurisdictionService.getJurisdictionConfig(jurisdiction);
            
            // Get jurisdiction-specific parameters
            List<SaCcrSupervisoryParameter> parameters = 
                supervisoryParameterRepository.findByJurisdictionOrderByEffectiveDateDesc(jurisdiction);
            
            // Get alpha factor for this jurisdiction
            BigDecimal alphaFactor = jurisdictionService.getAlphaFactor(jurisdiction, asOfDate);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "jurisdiction", jurisdiction,
                "asOfDate", asOfDate,
                "config", config,
                "alphaFactor", alphaFactor,
                "parameterCount", parameters.size(),
                "parameters", parameters
            ));
            
        } catch (Exception e) {
            logger.error("Error getting jurisdiction details for {}: {}", jurisdiction, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    /**
     * Compare jurisdictional differences in SA-CCR implementation
     */
    @PostMapping("/jurisdictions/compare")
    public ResponseEntity<?> compareJurisdictions(
            @RequestBody Map<String, Object> request) {
        
        try {
            @SuppressWarnings("unchecked")
            List<String> jurisdictions = (List<String>) request.get("jurisdictions");
            LocalDate asOfDate = request.containsKey("asOfDate") ? 
                LocalDate.parse((String) request.get("asOfDate")) : LocalDate.now();
            
            if (jurisdictions == null || jurisdictions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "ERROR", "message", "At least one jurisdiction must be specified"));
            }
            
            // Validate all jurisdictions are supported
            for (String jurisdiction : jurisdictions) {
                if (!jurisdictionService.isJurisdictionSupported(jurisdiction)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("status", "ERROR", "message", "Unsupported jurisdiction: " + jurisdiction));
                }
            }
            
            Map<String, Object> comparison = jurisdictionService.getJurisdictionComparison(jurisdictions, asOfDate);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "asOfDate", asOfDate,
                "comparedJurisdictions", jurisdictions,
                "comparison", comparison
            ));
            
        } catch (Exception e) {
            logger.error("Error comparing jurisdictions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
    
    /**
     * Calculate SA-CCR exposure for specific jurisdiction with detailed breakdown
     */
    @PostMapping("/calculate/jurisdiction/{jurisdiction}")
    public ResponseEntity<?> calculateExposureByJurisdiction(
            @PathVariable String jurisdiction,
            @RequestParam("valuationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valuationDate,
            @RequestParam(value = "nettingSetId", required = false) String nettingSetId) {
        
        try {
            if (!jurisdictionService.isJurisdictionSupported(jurisdiction)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "ERROR", "message", "Unsupported jurisdiction: " + jurisdiction));
            }
            
            logger.info("Calculating SA-CCR exposure for jurisdiction: {} as of {}", jurisdiction, valuationDate);
            
            List<SaCcrCalculation> calculations;
            if (nettingSetId != null) {
                // Calculate for specific netting set
                // Implementation would filter by netting set
                calculations = calculationService.calculateAllExposures(valuationDate, jurisdiction);
                calculations = calculations.stream()
                    .filter(calc -> nettingSetId.equals(calc.getNettingSetId()))
                    .toList();
            } else {
                // Calculate for all netting sets
                calculations = calculationService.calculateAllExposures(valuationDate, jurisdiction);
            }
            
            // Get jurisdiction configuration for context
            SaCcrJurisdictionService.JurisdictionConfig config = 
                jurisdictionService.getJurisdictionConfig(jurisdiction);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "SA-CCR calculations completed for jurisdiction: " + jurisdiction,
                "jurisdiction", jurisdiction,
                "jurisdictionConfig", config,
                "calculationCount", calculations.size(),
                "valuationDate", valuationDate,
                "calculations", calculations
            ));
            
        } catch (Exception e) {
            logger.error("Error calculating SA-CCR exposure for jurisdiction {}: {}", jurisdiction, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "ERROR",
                        "message", "Failed to calculate SA-CCR exposures for jurisdiction " + jurisdiction + ": " + e.getMessage()
                    ));
        }
    }
}