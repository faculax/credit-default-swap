package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.service.dashboard.DashboardAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Margin & Exposure Reconciliation Dashboard Controller
 * Provides unified view of margin, SA-CCR, and SIMM data for reconciliation operations
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private DashboardAggregationService dashboardAggregationService;
    
    /**
     * Get comprehensive dashboard data for margin and exposure reconciliation
     */
    @GetMapping("/reconciliation")
    public ResponseEntity<?> getReconciliationDashboard(
            @RequestParam(value = "asOfDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        try {
            // Default to current date if not specified
            if (asOfDate == null) {
                asOfDate = LocalDate.now();
            }
            
            logger.info("Retrieving reconciliation dashboard data for date: {}", asOfDate);
            
            DashboardAggregationService.DashboardData dashboardData = 
                    dashboardAggregationService.getDashboardData(asOfDate);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Dashboard data retrieved successfully",
                    "data", dashboardData
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving dashboard data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Failed to retrieve dashboard data: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Get margin summary data only
     */
    @GetMapping("/margin-summary")
    public ResponseEntity<?> getMarginSummary(
            @RequestParam(value = "asOfDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        try {
            if (asOfDate == null) {
                asOfDate = LocalDate.now();
            }
            
            DashboardAggregationService.DashboardData dashboardData = 
                    dashboardAggregationService.getDashboardData(asOfDate);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "data", dashboardData.marginSummary
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving margin summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Failed to retrieve margin summary: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Get SA-CCR exposure summary data only
     */
    @GetMapping("/saccr-summary")
    public ResponseEntity<?> getSaCcrSummary(
            @RequestParam(value = "asOfDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        try {
            if (asOfDate == null) {
                asOfDate = LocalDate.now();
            }
            
            DashboardAggregationService.DashboardData dashboardData = 
                    dashboardAggregationService.getDashboardData(asOfDate);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS", 
                    "data", dashboardData.saCcrSummary
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving SA-CCR summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Failed to retrieve SA-CCR summary: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Get reconciliation status and exceptions
     */
    @GetMapping("/reconciliation-status")
    public ResponseEntity<?> getReconciliationStatus(
            @RequestParam(value = "asOfDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        try {
            if (asOfDate == null) {
                asOfDate = LocalDate.now();
            }
            
            DashboardAggregationService.DashboardData dashboardData = 
                    dashboardAggregationService.getDashboardData(asOfDate);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "data", dashboardData.reconciliationStatus
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving reconciliation status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Failed to retrieve reconciliation status: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Get overall system health and metrics
     */
    @GetMapping("/health-metrics")
    public ResponseEntity<?> getHealthMetrics(
            @RequestParam(value = "asOfDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        try {
            if (asOfDate == null) {
                asOfDate = LocalDate.now();
            }
            
            DashboardAggregationService.DashboardData dashboardData = 
                    dashboardAggregationService.getDashboardData(asOfDate);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "data", Map.of(
                            "overallMetrics", dashboardData.overallMetrics,
                            "reconciliationStatus", dashboardData.reconciliationStatus,
                            "asOfDate", dashboardData.asOfDate,
                            "generatedAt", dashboardData.generatedAt
                    )
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving health metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Failed to retrieve health metrics: " + e.getMessage()
                    ));
        }
    }
    

    
    /**
     * Get data freshness indicators
     */
    @GetMapping("/data-freshness")
    public ResponseEntity<?> getDataFreshness() {
        try {
            LocalDate asOfDate = LocalDate.now();
            
            DashboardAggregationService.DashboardData dashboardData = 
                    dashboardAggregationService.getDashboardData(asOfDate);
            
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "data", dashboardData.reconciliationStatus.dataFreshness,
                    "checkTime", dashboardData.generatedAt
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving data freshness: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Failed to retrieve data freshness: " + e.getMessage()
                    ));
        }
    }
}