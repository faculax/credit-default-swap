package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Market Data Snapshot header entity for EOD valuation
 * Captures a complete snapshot of market data at a specific date
 */
@Entity
@Table(name = "market_data_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketDataSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;
    
    @Column(name = "snapshot_time")
    private LocalDateTime snapshotTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SnapshotStatus status = SnapshotStatus.PENDING;
    
    @Column(name = "data_sources", columnDefinition = "TEXT[]")
    private String[] dataSources;
    
    // Counts
    @Column(name = "cds_spread_count")
    @Builder.Default
    private Integer cdsSpreadCount = 0;
    
    @Column(name = "ir_curve_count")
    @Builder.Default
    private Integer irCurveCount = 0;
    
    @Column(name = "fx_rate_count")
    @Builder.Default
    private Integer fxRateCount = 0;
    
    @Column(name = "recovery_rate_count")
    @Builder.Default
    private Integer recoveryRateCount = 0;
    
    // Validation
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
    
    @Column(name = "missing_data_points", columnDefinition = "TEXT")
    private String missingDataPoints;
    
    @Column(name = "captured_by")
    private String capturedBy;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Relationships
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SnapshotCdsSpread> cdsSpreads = new ArrayList<>();
    
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SnapshotIrCurve> irCurves = new ArrayList<>();
    
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SnapshotFxRate> fxRates = new ArrayList<>();
    
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SnapshotRecoveryRate> recoveryRates = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        if (snapshotTime == null) {
            snapshotTime = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        // Update counts before save
        this.cdsSpreadCount = cdsSpreads.size();
        this.irCurveCount = irCurves.size();
        this.fxRateCount = fxRates.size();
        this.recoveryRateCount = recoveryRates.size();
    }
    
    public void addCdsSpread(SnapshotCdsSpread spread) {
        cdsSpreads.add(spread);
        spread.setSnapshot(this);
    }
    
    public void addIrCurve(SnapshotIrCurve curve) {
        irCurves.add(curve);
        curve.setSnapshot(this);
    }
    
    public void addFxRate(SnapshotFxRate rate) {
        fxRates.add(rate);
        rate.setSnapshot(this);
    }
    
    public void addRecoveryRate(SnapshotRecoveryRate rate) {
        recoveryRates.add(rate);
        rate.setSnapshot(this);
    }
    
    public enum SnapshotStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETE,
        PARTIAL,
        FAILED
    }
}
