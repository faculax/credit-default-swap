package com.creditdefaultswap.platform.controller.eod;

import com.creditdefaultswap.platform.dto.eod.*;
import com.creditdefaultswap.platform.model.eod.*;
import com.creditdefaultswap.platform.service.eod.MarketDataSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for market data snapshot management
 */
@RestController
@RequestMapping("/api/eod/market-data-snapshots")
@RequiredArgsConstructor
@Slf4j
public class MarketDataSnapshotController {
    
    private final MarketDataSnapshotService snapshotService;
    
    /**
     * Create a new market data snapshot with all data
     */
    @PostMapping
    public ResponseEntity<MarketDataSnapshot> createSnapshot(
        @RequestBody CreateSnapshotRequest request
    ) {
        try {
            MarketDataSnapshot snapshot = snapshotService.createSnapshot(
                request.getSnapshotDate(),
                request.getCapturedBy()
            );
            
            // Add CDS spreads
            if (request.getCdsSpreads() != null) {
                for (CdsSpreadDto dto : request.getCdsSpreads()) {
                    snapshotService.addCdsSpread(
                        snapshot.getId(),
                        dto.getReferenceEntityName(),
                        dto.getTenor(),
                        dto.getCurrency(),
                        dto.getSeniority(),
                        dto.getSpread(),
                        dto.getDataSource()
                    );
                }
            }
            
            // Add IR curves
            if (request.getIrCurves() != null) {
                for (IrCurveDto dto : request.getIrCurves()) {
                    snapshotService.addIrCurve(
                        snapshot.getId(),
                        dto.getCurrency(),
                        dto.getCurveType(),
                        dto.getTenor(),
                        dto.getRate(),
                        dto.getDataSource()
                    );
                }
            }
            
            // Add FX rates
            if (request.getFxRates() != null) {
                for (FxRateDto dto : request.getFxRates()) {
                    snapshotService.addFxRate(
                        snapshot.getId(),
                        dto.getBaseCurrency(),
                        dto.getQuoteCurrency(),
                        dto.getRate(),
                        dto.getDataSource()
                    );
                }
            }
            
            // Add recovery rates
            if (request.getRecoveryRates() != null) {
                for (RecoveryRateDto dto : request.getRecoveryRates()) {
                    snapshotService.addRecoveryRate(
                        snapshot.getId(),
                        dto.getReferenceEntityName(),
                        dto.getSeniority(),
                        dto.getRecoveryRate(),
                        dto.getDataSource()
                    );
                }
            }
            
            // Complete the snapshot
            snapshotService.completeSnapshot(snapshot.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(snapshotService.getSnapshotByDate(request.getSnapshotDate()).orElse(snapshot));
            
        } catch (IllegalStateException e) {
            log.error("Error creating snapshot: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    /**
     * Get snapshot by date
     */
    @GetMapping("/{date}")
    public ResponseEntity<MarketDataSnapshot> getSnapshot(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return snapshotService.getSnapshotByDate(date)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get latest complete snapshot
     */
    @GetMapping("/latest")
    public ResponseEntity<MarketDataSnapshot> getLatestSnapshot() {
        return snapshotService.getLatestCompleteSnapshot()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get CDS spreads for a date
     */
    @GetMapping("/{date}/cds-spreads")
    public ResponseEntity<List<SnapshotCdsSpread>> getCdsSpreads(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return snapshotService.getSnapshotByDate(date)
            .map(snapshot -> ResponseEntity.ok(snapshotService.getCdsSpreads(snapshot.getId())))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get specific CDS spread
     */
    @GetMapping("/{date}/cds-spreads/{entityName}/{tenor}")
    public ResponseEntity<SnapshotCdsSpread> getCdsSpread(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @PathVariable String entityName,
        @PathVariable String tenor
    ) {
        return snapshotService.getSnapshotByDate(date)
            .flatMap(snapshot -> snapshotService.getCdsSpread(snapshot.getId(), entityName, tenor))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get IR curves for a date
     */
    @GetMapping("/{date}/ir-curves")
    public ResponseEntity<List<SnapshotIrCurve>> getIrCurves(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String currency
    ) {
        if (currency != null) {
            return ResponseEntity.ok(snapshotService.getIrCurves(date, currency));
        }
        
        return snapshotService.getSnapshotByDate(date)
            .map(snapshot -> ResponseEntity.ok(snapshotService.getIrCurves(snapshot.getId())))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get FX rates for a date
     */
    @GetMapping("/{date}/fx-rates")
    public ResponseEntity<List<SnapshotFxRate>> getFxRates(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return snapshotService.getSnapshotByDate(date)
            .map(snapshot -> ResponseEntity.ok(snapshotService.getFxRates(snapshot.getId())))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get recovery rates for a date
     */
    @GetMapping("/{date}/recovery-rates")
    public ResponseEntity<List<SnapshotRecoveryRate>> getRecoveryRates(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return snapshotService.getSnapshotByDate(date)
            .map(snapshot -> ResponseEntity.ok(snapshotService.getRecoveryRates(snapshot.getId())))
            .orElse(ResponseEntity.notFound().build());
    }
}
