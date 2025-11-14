package com.creditdefaultswap.platform.model.eod;

import com.creditdefaultswap.platform.model.CdsPortfolio;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Tolerance rules for valuation exception detection
 */
@Entity
@Table(name = "valuation_tolerance_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValuationToleranceRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;
    
    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType; // NPV_CHANGE, SPREAD_CHANGE, PNL_THRESHOLD
    
    @Column(name = "absolute_threshold", precision = 20, scale = 4)
    private BigDecimal absoluteThreshold;
    
    @Column(name = "percentage_threshold", precision = 5, scale = 2)
    private BigDecimal percentageThreshold;
    
    @Column(name = "applies_to", length = 50)
    private String appliesTo; // ALL, PORTFOLIO, TRADE_TYPE
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private CdsPortfolio portfolio;
    
    @Column(name = "trade_type", length = 50)
    private String tradeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ValuationException.ExceptionSeverity severity;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
