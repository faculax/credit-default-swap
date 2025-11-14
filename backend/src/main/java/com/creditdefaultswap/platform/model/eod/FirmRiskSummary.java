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
 * Firm-wide risk summary with aggregated sensitivities and VaR metrics
 */
@Entity
@Table(name = "firm_risk_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirmRiskSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "calculation_date", nullable = false, unique = true)
    private LocalDate calculationDate;
    
    // Aggregated sensitivities
    @Column(name = "total_cs01", precision = 20, scale = 4)
    private BigDecimal totalCs01;
    
    @Column(name = "total_cs01_long", precision = 20, scale = 4)
    private BigDecimal totalCs01Long;
    
    @Column(name = "total_cs01_short", precision = 20, scale = 4)
    private BigDecimal totalCs01Short;
    
    @Column(name = "total_ir01", precision = 20, scale = 4)
    private BigDecimal totalIr01;
    
    @Column(name = "total_ir01_usd", precision = 20, scale = 4)
    private BigDecimal totalIr01Usd;
    
    @Column(name = "total_ir01_eur", precision = 20, scale = 4)
    private BigDecimal totalIr01Eur;
    
    @Column(name = "total_ir01_gbp", precision = 20, scale = 4)
    private BigDecimal totalIr01Gbp;
    
    @Column(name = "total_jtd", precision = 20, scale = 4)
    private BigDecimal totalJtd;
    
    @Column(name = "total_jtd_long", precision = 20, scale = 4)
    private BigDecimal totalJtdLong;
    
    @Column(name = "total_jtd_short", precision = 20, scale = 4)
    private BigDecimal totalJtdShort;
    
    @Column(name = "total_rec01", precision = 20, scale = 4)
    private BigDecimal totalRec01;
    
    // Notional exposures
    @Column(name = "total_gross_notional", precision = 20, scale = 4)
    private BigDecimal totalGrossNotional;
    
    @Column(name = "total_net_notional", precision = 20, scale = 4)
    private BigDecimal totalNetNotional;
    
    @Column(name = "total_long_notional", precision = 20, scale = 4)
    private BigDecimal totalLongNotional;
    
    @Column(name = "total_short_notional", precision = 20, scale = 4)
    private BigDecimal totalShortNotional;
    
    // Risk metrics
    @Column(name = "var_95", precision = 20, scale = 4)
    private BigDecimal var95; // 1-day 95% VaR
    
    @Column(name = "var_99", precision = 20, scale = 4)
    private BigDecimal var99; // 1-day 99% VaR
    
    @Column(name = "expected_shortfall", precision = 20, scale = 4)
    private BigDecimal expectedShortfall; // CVaR/Expected Shortfall
    
    // Counts
    @Column(name = "total_trade_count")
    private Integer totalTradeCount;
    
    @Column(name = "total_portfolio_count")
    private Integer totalPortfolioCount;
    
    @Column(name = "total_counterparty_count")
    private Integer totalCounterpartyCount;
    
    @Column(name = "total_reference_entity_count")
    private Integer totalReferenceEntityCount;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
    
    @Column(name = "job_id", length = 100)
    private String jobId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (currency == null) {
            currency = "USD";
        }
        // Initialize nulls to zero
        if (totalCs01 == null) totalCs01 = BigDecimal.ZERO;
        if (totalCs01Long == null) totalCs01Long = BigDecimal.ZERO;
        if (totalCs01Short == null) totalCs01Short = BigDecimal.ZERO;
        if (totalIr01 == null) totalIr01 = BigDecimal.ZERO;
        if (totalJtd == null) totalJtd = BigDecimal.ZERO;
        if (totalJtdLong == null) totalJtdLong = BigDecimal.ZERO;
        if (totalJtdShort == null) totalJtdShort = BigDecimal.ZERO;
        if (totalRec01 == null) totalRec01 = BigDecimal.ZERO;
        if (totalGrossNotional == null) totalGrossNotional = BigDecimal.ZERO;
        if (totalNetNotional == null) totalNetNotional = BigDecimal.ZERO;
        if (totalLongNotional == null) totalLongNotional = BigDecimal.ZERO;
        if (totalShortNotional == null) totalShortNotional = BigDecimal.ZERO;
        if (var95 == null) var95 = BigDecimal.ZERO;
        if (var99 == null) var99 = BigDecimal.ZERO;
        if (expectedShortfall == null) expectedShortfall = BigDecimal.ZERO;
        if (totalTradeCount == null) totalTradeCount = 0;
        if (totalPortfolioCount == null) totalPortfolioCount = 0;
        if (totalCounterpartyCount == null) totalCounterpartyCount = 0;
        if (totalReferenceEntityCount == null) totalReferenceEntityCount = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
