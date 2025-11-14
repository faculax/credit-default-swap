package com.creditdefaultswap.platform.controller.marketdata;

import com.creditdefaultswap.platform.model.eod.MarketDataSnapshot;
import com.creditdefaultswap.platform.service.eod.MarketDataSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API for market data ingestion
 * 
 * Provides endpoints to:
 * - Upload CDS spreads
 * - Upload interest rate curves
 * - Upload FX rates
 * - Upload recovery rates
 * - Capture daily market data snapshots
 */
@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
@Slf4j
public class MarketDataIngestionController {
    
    private final MarketDataSnapshotService snapshotService;
    
    /**
     * Capture market data snapshot for a date
     */
    @PostMapping("/snapshots")
    public ResponseEntity<Map<String, Object>> captureSnapshot(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
        
        log.info("Capturing market data snapshot for {}", snapshotDate);
        MarketDataSnapshot snapshot = snapshotService.createSnapshot(snapshotDate, "API");
        
        return ResponseEntity.ok(Map.of(
            "snapshotId", snapshot.getId(),
            "snapshotDate", snapshot.getSnapshotDate(),
            "status", snapshot.getStatus(),
            "message", "Market data snapshot created - now populate with data"
        ));
    }
    
    /**
     * Get latest market data snapshot
     */
    @GetMapping("/snapshots/latest")
    public ResponseEntity<MarketDataSnapshot> getLatestSnapshot() {
        return snapshotService.getLatestCompleteSnapshot()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Upload CDS spreads (bulk)
     * 
     * Request body format:
     * {
     *   "ACME": 150.5,
     *   "TECHCORP": 320.25,
     *   "BANKCO": 180.0
     * }
     */
    @PostMapping("/spreads")
    public ResponseEntity<Map<String, String>> uploadSpreads(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestBody Map<String, BigDecimal> spreads) {
        
        log.info("Uploading {} CDS spreads for {}", spreads.size(), effectiveDate);
        
        // TODO: Store spreads in cds_spread_curves table
        // For now, just acknowledge receipt
        
        return ResponseEntity.ok(Map.of(
            "message", "Received " + spreads.size() + " spreads for " + effectiveDate,
            "status", "PENDING_IMPLEMENTATION"
        ));
    }
    
    /**
     * Upload recovery rates (bulk)
     * 
     * Request body format:
     * {
     *   "ACME": 40.0,
     *   "TECHCORP": 35.5,
     *   "BANKCO": 42.0
     * }
     */
    @PostMapping("/recovery-rates")
    public ResponseEntity<Map<String, String>> uploadRecoveryRates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestBody Map<String, BigDecimal> recoveryRates) {
        
        log.info("Uploading {} recovery rates for {}", recoveryRates.size(), effectiveDate);
        
        // TODO: Store recovery rates in reference_data table or dedicated recovery_rates table
        
        return ResponseEntity.ok(Map.of(
            "message", "Received " + recoveryRates.size() + " recovery rates for " + effectiveDate,
            "status", "PENDING_IMPLEMENTATION"
        ));
    }
    
    /**
     * Upload interest rate curve (bulk)
     * 
     * Request body format:
     * {
     *   "currency": "USD",
     *   "curveType": "DISCOUNT",
     *   "tenors": ["1M", "3M", "6M", "1Y", "2Y", "5Y", "10Y"],
     *   "rates": [5.25, 5.30, 5.35, 5.40, 5.50, 5.75, 6.00]
     * }
     */
    @PostMapping("/interest-rates")
    public ResponseEntity<Map<String, String>> uploadInterestRates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestBody InterestRateCurveUpload curveData) {
        
        log.info("Uploading {} interest rate curve for {} on {}", 
            curveData.curveType, curveData.currency, effectiveDate);
        
        // TODO: Store in interest_rate_curves table
        
        return ResponseEntity.ok(Map.of(
            "message", "Received " + curveData.curveType + " curve for " + curveData.currency,
            "tenor_count", String.valueOf(curveData.tenors.size()),
            "status", "PENDING_IMPLEMENTATION"
        ));
    }
    
    /**
     * Upload FX rates (bulk)
     * 
     * Request body format:
     * {
     *   "USD/EUR": 0.92,
     *   "USD/GBP": 0.79,
     *   "USD/JPY": 150.25
     * }
     */
    @PostMapping("/fx-rates")
    public ResponseEntity<Map<String, String>> uploadFxRates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestBody Map<String, BigDecimal> fxRates) {
        
        log.info("Uploading {} FX rates for {}", fxRates.size(), effectiveDate);
        
        // TODO: Store in fx_rates table
        
        return ResponseEntity.ok(Map.of(
            "message", "Received " + fxRates.size() + " FX rates for " + effectiveDate,
            "status", "PENDING_IMPLEMENTATION"
        ));
    }
    
    /**
     * Health check - verify market data availability
     */
    @GetMapping("/health")
    public ResponseEntity<MarketDataHealthResponse> checkHealth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate checkDate = date != null ? date : LocalDate.now();
        
        // TODO: Check if we have fresh data for the date
        // Check spreads, rates, FX, recovery rates
        
        return ResponseEntity.ok(new MarketDataHealthResponse(
            checkDate,
            "OK",
            "Using placeholder market data - real data ingestion not yet implemented",
            Map.of(
                "spreads_available", "false",
                "rates_available", "false",
                "fx_available", "false",
                "recovery_rates_available", "false"
            )
        ));
    }
    
    // DTOs
    
    public record InterestRateCurveUpload(
        String currency,
        String curveType,
        List<String> tenors,
        List<BigDecimal> rates
    ) {}
    
    public record MarketDataHealthResponse(
        LocalDate checkDate,
        String status,
        String message,
        Map<String, String> dataAvailability
    ) {}
}
