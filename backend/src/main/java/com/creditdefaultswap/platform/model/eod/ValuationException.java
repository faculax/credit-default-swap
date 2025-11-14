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
 * Valuation exception - anomalies requiring review
 */
@Entity
@Table(name = "valuation_exceptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValuationException {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;
    
    @Column(name = "trade_id", nullable = false)
    private Long tradeId;
    
    // Removed @ManyToOne relationship to avoid Hibernate cascade issues
    // Use tradeId directly instead
    
    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 50)
    private ExceptionType exceptionType;
    
    @Column(name = "current_value", precision = 20, scale = 4)
    private BigDecimal currentValue;
    
    @Column(name = "previous_value", precision = 20, scale = 4)
    private BigDecimal previousValue;
    
    @Column(name = "value_change", precision = 20, scale = 4)
    private BigDecimal valueChange;
    
    @Column(name = "percentage_change", precision = 5, scale = 2)
    private BigDecimal percentageChange;
    
    @Column(name = "threshold_value", precision = 20, scale = 4)
    private BigDecimal thresholdValue;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private ValuationToleranceRule rule;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ExceptionSeverity severity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExceptionStatus status = ExceptionStatus.OPEN;
    
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;
    
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;
    
    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "valuation_result_id")
    private Long valuationResultId;
    
    @Column(name = "revaluation_result_id")
    private Long revaluationResultId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) {
            status = ExceptionStatus.OPEN;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
    
    public enum ExceptionType {
        LARGE_NPV_CHANGE,
        LARGE_PNL,
        MISSING_VALUATION,
        STALE_MARKET_DATA,
        NEGATIVE_ACCRUED,
        CALCULATION_ERROR,
        EXTERNAL_VALUATION_MISMATCH
    }
    
    public enum ExceptionStatus {
        OPEN,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        REVALUED
    }
    
    public enum ExceptionSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}
