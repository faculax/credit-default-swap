package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_risk_cache")
public class PortfolioRiskCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private CdsPortfolio portfolio;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    @Column(name = "aggregate_pv", precision = 18, scale = 4)
    private BigDecimal aggregatePv;
    
    @Column(name = "aggregate_accrued", precision = 18, scale = 4)
    private BigDecimal aggregateAccrued;
    
    @Column(name = "premium_leg_pv", precision = 18, scale = 4)
    private BigDecimal premiumLegPv;
    
    @Column(name = "protection_leg_pv", precision = 18, scale = 4)
    private BigDecimal protectionLegPv;
    
    @Column(name = "fair_spread_bps_weighted", precision = 10, scale = 4)
    private BigDecimal fairSpreadBpsWeighted;
    
    @Column(name = "cs01", precision = 18, scale = 4)
    private BigDecimal cs01;
    
    @Column(name = "rec01", precision = 18, scale = 4)
    private BigDecimal rec01;
    
    @Column(name = "jtd", precision = 18, scale = 4)
    private BigDecimal jtd;
    
    @Column(name = "top_5_pct_cs01", precision = 10, scale = 4)
    private BigDecimal top5PctCs01;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sector_breakdown", columnDefinition = "JSONB")
    private String sectorBreakdown;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "by_trade_breakdown", columnDefinition = "JSONB")
    private String byTradeBreakdown;
    
    @Column(name = "completeness_constituents")
    private Integer completenessConstituents;
    
    @Column(name = "completeness_priced")
    private Integer completenessPriced;
    
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
    
    // Constructors
    public PortfolioRiskCache() {
        this.calculatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public CdsPortfolio getPortfolio() {
        return portfolio;
    }
    
    public void setPortfolio(CdsPortfolio portfolio) {
        this.portfolio = portfolio;
    }
    
    public LocalDate getValuationDate() {
        return valuationDate;
    }
    
    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }
    
    public BigDecimal getAggregatePv() {
        return aggregatePv;
    }
    
    public void setAggregatePv(BigDecimal aggregatePv) {
        this.aggregatePv = aggregatePv;
    }
    
    public BigDecimal getAggregateAccrued() {
        return aggregateAccrued;
    }
    
    public void setAggregateAccrued(BigDecimal aggregateAccrued) {
        this.aggregateAccrued = aggregateAccrued;
    }
    
    public BigDecimal getPremiumLegPv() {
        return premiumLegPv;
    }
    
    public void setPremiumLegPv(BigDecimal premiumLegPv) {
        this.premiumLegPv = premiumLegPv;
    }
    
    public BigDecimal getProtectionLegPv() {
        return protectionLegPv;
    }
    
    public void setProtectionLegPv(BigDecimal protectionLegPv) {
        this.protectionLegPv = protectionLegPv;
    }
    
    public BigDecimal getFairSpreadBpsWeighted() {
        return fairSpreadBpsWeighted;
    }
    
    public void setFairSpreadBpsWeighted(BigDecimal fairSpreadBpsWeighted) {
        this.fairSpreadBpsWeighted = fairSpreadBpsWeighted;
    }
    
    public BigDecimal getCs01() {
        return cs01;
    }
    
    public void setCs01(BigDecimal cs01) {
        this.cs01 = cs01;
    }
    
    public BigDecimal getRec01() {
        return rec01;
    }
    
    public void setRec01(BigDecimal rec01) {
        this.rec01 = rec01;
    }
    
    public BigDecimal getJtd() {
        return jtd;
    }
    
    public void setJtd(BigDecimal jtd) {
        this.jtd = jtd;
    }
    
    public BigDecimal getTop5PctCs01() {
        return top5PctCs01;
    }
    
    public void setTop5PctCs01(BigDecimal top5PctCs01) {
        this.top5PctCs01 = top5PctCs01;
    }
    
    public String getSectorBreakdown() {
        return sectorBreakdown;
    }
    
    public void setSectorBreakdown(String sectorBreakdown) {
        this.sectorBreakdown = sectorBreakdown;
    }
    
    public String getByTradeBreakdown() {
        return byTradeBreakdown;
    }
    
    public void setByTradeBreakdown(String byTradeBreakdown) {
        this.byTradeBreakdown = byTradeBreakdown;
    }
    
    public Integer getCompletenessConstituents() {
        return completenessConstituents;
    }
    
    public void setCompletenessConstituents(Integer completenessConstituents) {
        this.completenessConstituents = completenessConstituents;
    }
    
    public Integer getCompletenessPriced() {
        return completenessPriced;
    }
    
    public void setCompletenessPriced(Integer completenessPriced) {
        this.completenessPriced = completenessPriced;
    }
    
    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
    
    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
