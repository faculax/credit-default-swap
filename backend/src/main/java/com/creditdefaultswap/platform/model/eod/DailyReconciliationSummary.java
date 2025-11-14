package com.creditdefaultswap.platform.model.eod;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Daily reconciliation summary
 */
@Entity
@Table(name = "daily_reconciliation_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReconciliationSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reconciliation_date", nullable = false, unique = true)
    private LocalDate reconciliationDate;
    
    @Column(name = "total_valuations", nullable = false)
    private Integer totalValuations;
    
    @Column(name = "total_exceptions", nullable = false)
    private Integer totalExceptions;
    
    // Exception breakdown by severity
    @Column(name = "info_count")
    @Builder.Default
    private Integer infoCount = 0;
    
    @Column(name = "warning_count")
    @Builder.Default
    private Integer warningCount = 0;
    
    @Column(name = "error_count")
    @Builder.Default
    private Integer errorCount = 0;
    
    @Column(name = "critical_count")
    @Builder.Default
    private Integer criticalCount = 0;
    
    // Exception breakdown by type
    @Column(name = "large_npv_change_count")
    @Builder.Default
    private Integer largeNpvChangeCount = 0;
    
    @Column(name = "large_pnl_count")
    @Builder.Default
    private Integer largePnlCount = 0;
    
    @Column(name = "missing_valuation_count")
    @Builder.Default
    private Integer missingValuationCount = 0;
    
    @Column(name = "calculation_error_count")
    @Builder.Default
    private Integer calculationErrorCount = 0;
    
    @Column(name = "stale_market_data_count")
    @Builder.Default
    private Integer staleMarketDataCount = 0;
    
    @Column(name = "negative_accrued_count")
    @Builder.Default
    private Integer negativeAccruedCount = 0;
    
    // Status breakdown
    @Column(name = "open_exceptions")
    @Builder.Default
    private Integer openExceptions = 0;
    
    @Column(name = "under_review_exceptions")
    @Builder.Default
    private Integer underReviewExceptions = 0;
    
    @Column(name = "resolved_exceptions")
    @Builder.Default
    private Integer resolvedExceptions = 0;
    
    @Column(name = "reconciliation_status", nullable = false, length = 20)
    @Builder.Default
    private String reconciliationStatus = "IN_PROGRESS";
    
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    
    @Column(name = "job_id", length = 100)
    private String jobId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (reconciliationStatus == null) {
            reconciliationStatus = "IN_PROGRESS";
        }
        // Initialize counts to 0 if null
        if (infoCount == null) infoCount = 0;
        if (warningCount == null) warningCount = 0;
        if (errorCount == null) errorCount = 0;
        if (criticalCount == null) criticalCount = 0;
        if (largeNpvChangeCount == null) largeNpvChangeCount = 0;
        if (largePnlCount == null) largePnlCount = 0;
        if (missingValuationCount == null) missingValuationCount = 0;
        if (calculationErrorCount == null) calculationErrorCount = 0;
        if (staleMarketDataCount == null) staleMarketDataCount = 0;
        if (negativeAccruedCount == null) negativeAccruedCount = 0;
        if (openExceptions == null) openExceptions = 0;
        if (underReviewExceptions == null) underReviewExceptions = 0;
        if (resolvedExceptions == null) resolvedExceptions = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
