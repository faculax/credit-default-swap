package com.creditdefaultswap.platform.model.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Trade valuation result with NPV calculation
 */
@Entity
@Table(name = "trade_valuations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"valuation_date", "trade_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeValuation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    // TEMPORARILY SIMPLIFIED: Store trade_id directly instead of @ManyToOne relationship
    // This is to debug Hibernate "delayed insert actions" error
    @Column(name = "trade_id", nullable = false)
    private Long tradeId;
    
    // Removed @ManyToOne relationship temporarily for debugging
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "trade_id", nullable = false)
    // @ToString.Exclude
    // private CDSTrade trade;
    
    // NPV breakdown
    @Column(name = "npv", nullable = false, precision = 20, scale = 4)
    private BigDecimal npv;
    
    @Column(name = "premium_leg_pv", precision = 20, scale = 4)
    private BigDecimal premiumLegPv;
    
    @Column(name = "protection_leg_pv", precision = 20, scale = 4)
    private BigDecimal protectionLegPv;
    
    // Valuation inputs
    @Column(name = "credit_spread", precision = 10, scale = 6)
    private BigDecimal creditSpread;
    
    @Column(name = "recovery_rate", precision = 5, scale = 4)
    private BigDecimal recoveryRate;
    
    @Column(name = "discount_factor", precision = 10, scale = 8)
    private BigDecimal discountFactor;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    // Calculation details
    @Column(name = "calculation_method", length = 50)
    @Builder.Default
    private String calculationMethod = "ORE";
    
    @Column(name = "calculation_time_ms")
    private Integer calculationTimeMs;
    
    @Column(name = "ore_scenario_id", length = 100)
    private String oreScenarioId;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "valuation_status", nullable = false)
    @Builder.Default
    private ValuationStatus valuationStatus = ValuationStatus.SUCCESS;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "job_id", length = 100)
    private String jobId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Relationship to sensitivities REMOVED to bypass Hibernate cascade issues
    // Use TradeValuationSensitivityRepository queries instead
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    /**
     * Temporary helper method for backward compatibility
     * TODO: Remove this once all services are updated to use tradeId directly
     */
    @Transient
    public CDSTrade getTrade() {
        // Returns null - services need to fetch trade separately using tradeId
        return null;
    }
    
    /**
     * Temporary setter for backward compatibility
     */
    public void setTrade(CDSTrade trade) {
        if (trade != null) {
            this.tradeId = trade.getId();
        }
    }
    
    public enum ValuationStatus {
        SUCCESS,
        FAILED,
        PENDING
    }
}
