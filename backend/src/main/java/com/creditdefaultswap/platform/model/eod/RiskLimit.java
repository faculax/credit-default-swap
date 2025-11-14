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
 * Risk limits and thresholds for monitoring
 */
@Entity
@Table(name = "risk_limits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskLimit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private CdsPortfolio portfolio;
    
    @Column(name = "counterparty_id")
    private Long counterpartyId;
    
    @Column(name = "sector", length = 100)
    private String sector;
    
    @Column(name = "firm_wide")
    @Builder.Default
    private Boolean firmWide = false;
    
    @Column(name = "limit_type", nullable = false, length = 50)
    private String limitType; // CS01, IR01, JTD, NOTIONAL, VAR_95, VAR_99
    
    @Column(name = "limit_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal limitValue;
    
    @Column(name = "warning_threshold", precision = 20, scale = 4)
    private BigDecimal warningThreshold;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (currency == null) {
            currency = "USD";
        }
        if (isActive == null) {
            isActive = true;
        }
        if (firmWide == null) {
            firmWide = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
