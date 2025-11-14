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
 * Risk limit breaches and alerts
 */
@Entity
@Table(name = "risk_limit_breaches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskLimitBreach {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "breach_date", nullable = false)
    private LocalDate breachDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_limit_id", nullable = false)
    private RiskLimit riskLimit;
    
    @Column(name = "current_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal currentValue;
    
    @Column(name = "limit_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal limitValue;
    
    @Column(name = "breach_percentage", precision = 5, scale = 2)
    private BigDecimal breachPercentage;
    
    @Column(name = "breach_amount", precision = 20, scale = 4)
    private BigDecimal breachAmount;
    
    @Column(name = "breach_severity", nullable = false, length = 20)
    private String breachSeverity; // WARNING, BREACH, CRITICAL
    
    @Column(name = "is_resolved")
    @Builder.Default
    private Boolean isResolved = false;
    
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (isResolved == null) {
            isResolved = false;
        }
    }
}
