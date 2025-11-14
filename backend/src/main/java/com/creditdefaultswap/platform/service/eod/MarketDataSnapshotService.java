package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.eod.*;
import com.creditdefaultswap.platform.repository.eod.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for capturing and managing market data snapshots for EOD valuation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataSnapshotService {
    
    private final MarketDataSnapshotRepository snapshotRepository;
    private final SnapshotCdsSpreadRepository cdsSpreadRepository;
    private final SnapshotIrCurveRepository irCurveRepository;
    private final SnapshotFxRateRepository fxRateRepository;
    private final SnapshotRecoveryRateRepository recoveryRateRepository;
    
    /**
     * Create a new market data snapshot for a specific date
     */
    @Transactional
    public MarketDataSnapshot createSnapshot(LocalDate snapshotDate, String capturedBy) {
        // Check if snapshot already exists
        if (snapshotRepository.existsBySnapshotDate(snapshotDate)) {
            throw new IllegalStateException("Snapshot already exists for date: " + snapshotDate);
        }
        
        MarketDataSnapshot snapshot = MarketDataSnapshot.builder()
            .snapshotDate(snapshotDate)
            .snapshotTime(LocalDateTime.now())
            .status(MarketDataSnapshot.SnapshotStatus.PENDING)
            .capturedBy(capturedBy)
            .build();
        
        snapshot = snapshotRepository.save(snapshot);
        log.info("Created market data snapshot for date: {}", snapshotDate);
        
        return snapshot;
    }
    
    /**
     * Add CDS spread to snapshot
     */
    @Transactional
    public void addCdsSpread(Long snapshotId, String referenceEntityName, String tenor,
                            String currency, String seniority, BigDecimal spread, 
                            String dataSource) {
        MarketDataSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        
        SnapshotCdsSpread cdsSpread = SnapshotCdsSpread.builder()
            .snapshot(snapshot)
            .referenceEntityName(referenceEntityName)
            .tenor(tenor)
            .currency(currency)
            .seniority(seniority)
            .spread(spread)
            .dataSource(dataSource)
            .quoteTime(LocalDateTime.now())
            .build();
        
        snapshot.addCdsSpread(cdsSpread);
        cdsSpreadRepository.save(cdsSpread);
    }
    
    /**
     * Add interest rate curve point to snapshot
     */
    @Transactional
    public void addIrCurve(Long snapshotId, String currency, String curveType,
                          String tenor, BigDecimal rate, String dataSource) {
        MarketDataSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        
        SnapshotIrCurve irCurve = SnapshotIrCurve.builder()
            .snapshot(snapshot)
            .currency(currency)
            .curveType(curveType)
            .tenor(tenor)
            .rate(rate)
            .dataSource(dataSource)
            .quoteTime(LocalDateTime.now())
            .build();
        
        snapshot.addIrCurve(irCurve);
        irCurveRepository.save(irCurve);
    }
    
    /**
     * Add FX rate to snapshot
     */
    @Transactional
    public void addFxRate(Long snapshotId, String baseCurrency, String quoteCurrency,
                         BigDecimal rate, String dataSource) {
        MarketDataSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        
        SnapshotFxRate fxRate = SnapshotFxRate.builder()
            .snapshot(snapshot)
            .baseCurrency(baseCurrency)
            .quoteCurrency(quoteCurrency)
            .rate(rate)
            .dataSource(dataSource)
            .quoteTime(LocalDateTime.now())
            .build();
        
        snapshot.addFxRate(fxRate);
        fxRateRepository.save(fxRate);
    }
    
    /**
     * Add recovery rate to snapshot
     */
    @Transactional
    public void addRecoveryRate(Long snapshotId, String referenceEntityName,
                               String seniority, BigDecimal recoveryRate, 
                               String dataSource) {
        MarketDataSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        
        SnapshotRecoveryRate rate = SnapshotRecoveryRate.builder()
            .snapshot(snapshot)
            .referenceEntityName(referenceEntityName)
            .seniority(seniority)
            .recoveryRate(recoveryRate)
            .dataSource(dataSource)
            .quoteTime(LocalDateTime.now())
            .build();
        
        snapshot.addRecoveryRate(rate);
        recoveryRateRepository.save(rate);
    }
    
    /**
     * Complete snapshot capture
     */
    @Transactional
    public void completeSnapshot(Long snapshotId) {
        MarketDataSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        
        snapshot.setStatus(MarketDataSnapshot.SnapshotStatus.COMPLETE);
        snapshot.setCompletedAt(LocalDateTime.now());
        
        // Update counts
        snapshot.setCdsSpreadCount(snapshot.getCdsSpreads().size());
        snapshot.setIrCurveCount(snapshot.getIrCurves().size());
        snapshot.setFxRateCount(snapshot.getFxRates().size());
        snapshot.setRecoveryRateCount(snapshot.getRecoveryRates().size());
        
        snapshotRepository.save(snapshot);
        log.info("Completed snapshot for date: {} with {} CDS spreads, {} IR curves, {} FX rates, {} recovery rates",
            snapshot.getSnapshotDate(),
            snapshot.getCdsSpreadCount(),
            snapshot.getIrCurveCount(),
            snapshot.getFxRateCount(),
            snapshot.getRecoveryRateCount());
    }
    
    /**
     * Mark snapshot as failed
     */
    @Transactional
    public void failSnapshot(Long snapshotId, String error) {
        MarketDataSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        
        snapshot.setStatus(MarketDataSnapshot.SnapshotStatus.FAILED);
        snapshot.setValidationErrors(error);
        snapshotRepository.save(snapshot);
        
        log.error("Snapshot failed for date: {} - {}", snapshot.getSnapshotDate(), error);
    }
    
    /**
     * Get snapshot by date
     */
    public Optional<MarketDataSnapshot> getSnapshotByDate(LocalDate date) {
        return snapshotRepository.findBySnapshotDate(date);
    }
    
    /**
     * Get latest complete snapshot
     */
    public Optional<MarketDataSnapshot> getLatestCompleteSnapshot() {
        return snapshotRepository.findFirstByStatusOrderBySnapshotDateDesc(
            MarketDataSnapshot.SnapshotStatus.COMPLETE
        );
    }
    
    /**
     * Get CDS spreads for a snapshot
     */
    public List<SnapshotCdsSpread> getCdsSpreads(Long snapshotId) {
        return cdsSpreadRepository.findBySnapshotId(snapshotId);
    }
    
    /**
     * Get CDS spread for specific entity and tenor
     */
    public Optional<SnapshotCdsSpread> getCdsSpread(Long snapshotId, String entityName, String tenor) {
        return cdsSpreadRepository.findBySnapshotAndEntityAndTenor(snapshotId, entityName, tenor);
    }
    
    /**
     * Get IR curves for a snapshot
     */
    public List<SnapshotIrCurve> getIrCurves(Long snapshotId) {
        return irCurveRepository.findBySnapshotId(snapshotId);
    }
    
    /**
     * Get IR curves for specific date and currency
     */
    public List<SnapshotIrCurve> getIrCurves(LocalDate date, String currency) {
        return irCurveRepository.findBySnapshotDateAndCurrency(date, currency);
    }
    
    /**
     * Get FX rates for a snapshot
     */
    public List<SnapshotFxRate> getFxRates(Long snapshotId) {
        return fxRateRepository.findBySnapshotId(snapshotId);
    }
    
    /**
     * Get recovery rates for a snapshot
     */
    public List<SnapshotRecoveryRate> getRecoveryRates(Long snapshotId) {
        return recoveryRateRepository.findBySnapshotId(snapshotId);
    }
    
    /**
     * Validate snapshot completeness
     */
    @Transactional
    public void validateSnapshot(Long snapshotId, List<String> requiredEntities, 
                                 List<String> requiredCurrencies) {
        MarketDataSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        
        StringBuilder missingData = new StringBuilder();
        
        // Check for required CDS spreads
        List<SnapshotCdsSpread> cdsSpreads = cdsSpreadRepository.findBySnapshotId(snapshotId);
        for (String entity : requiredEntities) {
            boolean found = cdsSpreads.stream()
                .anyMatch(s -> s.getReferenceEntityName().equals(entity));
            if (!found) {
                missingData.append("Missing CDS spread for: ").append(entity).append("; ");
            }
        }
        
        // Check for required IR curves
        List<SnapshotIrCurve> irCurves = irCurveRepository.findBySnapshotId(snapshotId);
        for (String currency : requiredCurrencies) {
            boolean found = irCurves.stream()
                .anyMatch(c -> c.getCurrency().equals(currency));
            if (!found) {
                missingData.append("Missing IR curve for: ").append(currency).append("; ");
            }
        }
        
        if (missingData.length() > 0) {
            snapshot.setStatus(MarketDataSnapshot.SnapshotStatus.PARTIAL);
            snapshot.setMissingDataPoints(missingData.toString());
            log.warn("Snapshot validation found missing data: {}", missingData);
        } else {
            snapshot.setStatus(MarketDataSnapshot.SnapshotStatus.COMPLETE);
        }
        
        snapshotRepository.save(snapshot);
    }
}
