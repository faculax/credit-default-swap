package com.creditdefaultswap.platform.model.eod;

import com.creditdefaultswap.platform.model.CdsPortfolio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Portfolio-level risk metrics aggregation
 * Stores aggregated sensitivities (CS01, IR01, JTD, REC01) and notional exposures
 */
@Entity
@Table(name = "portfolio_risk_metrics", uniqueConstraints = {
    @UniqueConstraint(name = "uq_portfolio_risk_date_portfolio", 
                     columnNames = {"calculation_date", "portfolio_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioRiskMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CdsPortfolio portfolio;
    
    // Credit spread sensitivity (CS01)
    @Column(name = "cs01", precision = 20, scale = 4)
    private BigDecimal cs01; // P&L impact of 1bp parallel spread move
    
    @Column(name = "cs01_long", precision = 20, scale = 4)
    private BigDecimal cs01Long; // CS01 for protection bought
    
    @Column(name = "cs01_short", precision = 20, scale = 4)
    private BigDecimal cs01Short; // CS01 for protection sold
    
    // Interest rate sensitivity (IR01)
    @Column(name = "ir01", precision = 20, scale = 4)
    private BigDecimal ir01; // P&L impact of 1bp IR move
    
    @Column(name = "ir01_usd", precision = 20, scale = 4)
    private BigDecimal ir01Usd;
    
    @Column(name = "ir01_eur", precision = 20, scale = 4)
    private BigDecimal ir01Eur;
    
    @Column(name = "ir01_gbp", precision = 20, scale = 4)
    private BigDecimal ir01Gbp;
    
    // Jump-to-default risk (JTD)
    @Column(name = "jtd", precision = 20, scale = 4)
    private BigDecimal jtd; // Loss if all names default
    
    @Column(name = "jtd_long", precision = 20, scale = 4)
    private BigDecimal jtdLong;
    
    @Column(name = "jtd_short", precision = 20, scale = 4)
    private BigDecimal jtdShort;
    
    // Recovery rate sensitivity (REC01)
    @Column(name = "rec01", precision = 20, scale = 4)
    private BigDecimal rec01; // P&L impact of 1% recovery change
    
    // Notional exposures
    @Column(name = "gross_notional", precision = 20, scale = 4)
    private BigDecimal grossNotional;
    
    @Column(name = "net_notional", precision = 20, scale = 4)
    private BigDecimal netNotional;
    
    @Column(name = "long_notional", precision = 20, scale = 4)
    private BigDecimal longNotional;
    
    @Column(name = "short_notional", precision = 20, scale = 4)
    private BigDecimal shortNotional;
    
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
        // Initialize nulls to zero for arithmetic operations
        if (cs01 == null) cs01 = BigDecimal.ZERO;
        if (cs01Long == null) cs01Long = BigDecimal.ZERO;
        if (cs01Short == null) cs01Short = BigDecimal.ZERO;
        if (ir01 == null) ir01 = BigDecimal.ZERO;
        if (jtd == null) jtd = BigDecimal.ZERO;
        if (jtdLong == null) jtdLong = BigDecimal.ZERO;
        if (jtdShort == null) jtdShort = BigDecimal.ZERO;
        if (rec01 == null) rec01 = BigDecimal.ZERO;
        if (grossNotional == null) grossNotional = BigDecimal.ZERO;
        if (netNotional == null) netNotional = BigDecimal.ZERO;
        if (longNotional == null) longNotional = BigDecimal.ZERO;
        if (shortNotional == null) shortNotional = BigDecimal.ZERO;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
