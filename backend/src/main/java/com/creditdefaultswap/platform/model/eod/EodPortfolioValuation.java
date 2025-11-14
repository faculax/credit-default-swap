package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * EOD Portfolio Valuation - aggregated valuations at portfolio/book level
 * 
 * Provides consolidated view of risk and P&L across trading portfolios
 */
@Entity
@Table(name = "eod_portfolio_valuations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"valuation_date", "portfolio_id", "book"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EodPortfolioValuation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    // Portfolio Identification
    @Column(name = "portfolio_id", length = 50)
    private String portfolioId;
    
    @Column(name = "portfolio_name", length = 200)
    private String portfolioName;
    
    @Column(name = "book", length = 100)
    private String book;
    
    @Column(name = "desk", length = 100)
    private String desk;
    
    @Column(name = "business_unit", length = 100)
    private String businessUnit;
    
    // Aggregated Values
    @Column(name = "total_npv", precision = 20, scale = 4, nullable = false)
    private BigDecimal totalNpv;
    
    @Column(name = "total_accrued", precision = 20, scale = 4, nullable = false)
    private BigDecimal totalAccrued;
    
    @Column(name = "total_value", precision = 20, scale = 4, nullable = false)
    private BigDecimal totalValue;
    
    @Column(name = "total_notional", precision = 20, scale = 2, nullable = false)
    private BigDecimal totalNotional;
    
    // Trade Counts
    @Column(name = "num_trades", nullable = false)
    private Integer numTrades;
    
    @Column(name = "num_buy_protection")
    private Integer numBuyProtection;
    
    @Column(name = "num_sell_protection")
    private Integer numSellProtection;
    
    // Risk Metrics (Aggregated)
    @Column(name = "total_cs01", precision = 18, scale = 4)
    private BigDecimal totalCs01;
    
    @Column(name = "total_ir01", precision = 18, scale = 4)
    private BigDecimal totalIr01;
    
    @Column(name = "total_jtd", precision = 18, scale = 4)
    private BigDecimal totalJtd;
    
    @Column(name = "total_rec01", precision = 18, scale = 4)
    private BigDecimal totalRec01;
    
    @Column(name = "net_delta", precision = 18, scale = 4)
    private BigDecimal netDelta;
    
    @Column(name = "net_gamma", precision = 18, scale = 4)
    private BigDecimal netGamma;
    
    // P&L (vs Previous Day)
    @Column(name = "daily_pnl", precision = 20, scale = 4)
    private BigDecimal dailyPnl;
    
    @Column(name = "pnl_mtd", precision = 20, scale = 4)
    private BigDecimal pnlMtd;
    
    @Column(name = "pnl_ytd", precision = 20, scale = 4)
    private BigDecimal pnlYtd;
    
    // Currency Breakdown (JSON)
    @Column(name = "currency_breakdown", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> currencyBreakdown;
    
    // Top Exposures (JSON)
    @Column(name = "top_exposures", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> topExposures;
    
    // Calculation Metadata
    @Column(name = "job_id", length = 100)
    private String jobId;
    
    @Column(name = "calculation_timestamp")
    private LocalDateTime calculationTimestamp;
    
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
        if (numTrades == null) {
            numTrades = 0;
        }
        if (numBuyProtection == null) {
            numBuyProtection = 0;
        }
        if (numSellProtection == null) {
            numSellProtection = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
