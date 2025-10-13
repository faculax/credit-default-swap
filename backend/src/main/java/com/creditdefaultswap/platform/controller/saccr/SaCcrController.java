package com.creditdefaultswap.platform.controller.saccr;

import com.creditdefaultswap.platform.model.saccr.NettingSet;
import com.creditdefaultswap.platform.model.saccr.SaCcrCalculation;
import com.creditdefaultswap.platform.model.saccr.SaCcrSupervisoryParameter;
import com.creditdefaultswap.platform.service.saccr.SaCcrCalculationService;
import com.creditdefaultswap.platform.repository.saccr.NettingSetRepository;
import com.creditdefaultswap.platform.repository.saccr.SaCcrSupervisoryParameterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SA-CCR REST Controller
 * Provides endpoints for Basel III Standardized Approach for Counterparty Credit Risk
 */
@RestController
@RequestMapping("/api/v1/sa-ccr")
@CrossOrigin(origins = "http://localhost:3000")
public class SaCcrController {
    
    private static final Logger logger = LoggerFactory.getLogger(SaCcrController.class);
    
    @Autowired
    private SaCcrCalculationService calculationService;
    
    @Autowired
    private NettingSetRepository nettingSetRepository;
    
    @Autowired
    private SaCcrSupervisoryParameterRepository supervisoryParameterRepository;
    
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
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "SA-CCR calculation completed",
                "calculation", calculation
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
}