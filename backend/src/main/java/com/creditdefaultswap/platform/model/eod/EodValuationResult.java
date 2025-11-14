package com.creditdefaultswap.platform.model.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EOD Valuation Result - consolidated daily valuation for a trade
 * 
 * Combines NPV calculation, accrued interest, and risk metrics
 * into a single record for historical tracking and reporting
 */
@Entity
@Table(name = "eod_valuation_results",
    uniqueConstraints = @UniqueConstraint(columnNames = {"valuation_date", "trade_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EodValuationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private CDSTrade trade;
    
    // NPV Components
    @Column(name = "npv", precision = 20, scale = 4, nullable = false)
    private BigDecimal npv;
    
    @Column(name = "premium_leg_pv", precision = 20, scale = 4)
    private BigDecimal premiumLegPv;
    
    @Column(name = "protection_leg_pv", precision = 20, scale = 4)
    private BigDecimal protectionLegPv;
    
    // Accrued Interest
    @Column(name = "accrued_interest", precision = 20, scale = 4, nullable = false)
    private BigDecimal accruedInterest;
    
    @Column(name = "accrual_days")
    private Integer accrualDays;
    
    // Total Value (NPV + Accrued)
    @Column(name = "total_value", precision = 20, scale = 4, nullable = false)
    private BigDecimal totalValue;
    
    // Market Data Used
    @Column(name = "credit_spread", precision = 10, scale = 6)
    private BigDecimal creditSpread;
    
    @Column(name = "recovery_rate", precision = 5, scale = 4)
    private BigDecimal recoveryRate;
    
    @Column(name = "discount_rate", precision = 10, scale = 6)
    private BigDecimal discountRate;
    
    // Risk Metrics
    @Column(name = "cs01", precision = 18, scale = 4)
    private BigDecimal cs01;
    
    @Column(name = "ir01", precision = 18, scale = 4)
    private BigDecimal ir01;
    
    @Column(name = "jtd", precision = 18, scale = 4)
    private BigDecimal jtd;
    
    @Column(name = "rec01", precision = 18, scale = 4)
    private BigDecimal rec01;
    
    // Trade Details (snapshot at valuation date)
    @Column(name = "notional_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal notionalAmount;
    
    @Column(name = "spread", precision = 10, scale = 6, nullable = false)
    private BigDecimal spread;
    
    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;
    
    @Column(name = "reference_entity", length = 100, nullable = false)
    private String referenceEntity;
    
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;
    
    @Column(name = "buy_sell_protection", length = 10, nullable = false)
    private String buySellProtection;
    
    // Calculation Metadata
    @Column(name = "job_id", length = 100)
    private String jobId;
    
    @Column(name = "calculation_timestamp")
    private LocalDateTime calculationTimestamp;
    
    @Column(name = "valuation_method", length = 50)
    private String valuationMethod;
    
    @Column(name = "calculation_time_ms")
    private Integer calculationTimeMs;
    
    // Status
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private ValuationStatus status;
    
    @Column(name = "validation_flags", columnDefinition = "text[]")
    private String[] validationFlags;
    
    // Audit
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (calculationTimestamp == null) {
            calculationTimestamp = LocalDateTime.now();
        }
        if (status == null) {
            status = ValuationStatus.VALID;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Valuation status enum
     */
    public enum ValuationStatus {
        VALID,      // Valuation is current and valid
        INVALID,    // Valuation failed validation checks
        STALE,      // Valuation is outdated
        REVALUED    // Valuation was recalculated
    }
}
