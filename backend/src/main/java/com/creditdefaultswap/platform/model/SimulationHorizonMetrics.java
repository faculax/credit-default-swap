package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "simulation_horizon_metrics")
public class SimulationHorizonMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "run_id", nullable = false, length = 50)
    private String runId;
    
    @Column(name = "tenor", nullable = false, length = 10)
    private String tenor;
    
    @Column(name = "p_any_default", precision = 10, scale = 6)
    private BigDecimal pAnyDefault;
    
    @Column(name = "expected_defaults", precision = 10, scale = 4)
    private BigDecimal expectedDefaults;
    
    @Column(name = "loss_mean", precision = 18, scale = 4)
    private BigDecimal lossMean;
    
    @Column(name = "loss_var95", precision = 18, scale = 4)
    private BigDecimal lossVar95;
    
    @Column(name = "loss_var99", precision = 18, scale = 4)
    private BigDecimal lossVar99;
    
    @Column(name = "loss_es97_5", precision = 18, scale = 4)
    private BigDecimal lossEs975;
    
    @Column(name = "sum_standalone_el", precision = 18, scale = 4)
    private BigDecimal sumStandaloneEl;
    
    @Column(name = "portfolio_el", precision = 18, scale = 4)
    private BigDecimal portfolioEl;
    
    @Column(name = "diversification_benefit_pct", precision = 10, scale = 4)
    private BigDecimal diversificationBenefitPct;
    
    // Constructors
    public SimulationHorizonMetrics() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRunId() {
        return runId;
    }
    
    public void setRunId(String runId) {
        this.runId = runId;
    }
    
    public String getTenor() {
        return tenor;
    }
    
    public void setTenor(String tenor) {
        this.tenor = tenor;
    }
    
    public BigDecimal getPAnyDefault() {
        return pAnyDefault;
    }
    
    public void setPAnyDefault(BigDecimal pAnyDefault) {
        this.pAnyDefault = pAnyDefault;
    }
    
    public BigDecimal getExpectedDefaults() {
        return expectedDefaults;
    }
    
    public void setExpectedDefaults(BigDecimal expectedDefaults) {
        this.expectedDefaults = expectedDefaults;
    }
    
    public BigDecimal getLossMean() {
        return lossMean;
    }
    
    public void setLossMean(BigDecimal lossMean) {
        this.lossMean = lossMean;
    }
    
    public BigDecimal getLossVar95() {
        return lossVar95;
    }
    
    public void setLossVar95(BigDecimal lossVar95) {
        this.lossVar95 = lossVar95;
    }
    
    public BigDecimal getLossVar99() {
        return lossVar99;
    }
    
    public void setLossVar99(BigDecimal lossVar99) {
        this.lossVar99 = lossVar99;
    }
    
    public BigDecimal getLossEs975() {
        return lossEs975;
    }
    
    public void setLossEs975(BigDecimal lossEs975) {
        this.lossEs975 = lossEs975;
    }
    
    public BigDecimal getSumStandaloneEl() {
        return sumStandaloneEl;
    }
    
    public void setSumStandaloneEl(BigDecimal sumStandaloneEl) {
        this.sumStandaloneEl = sumStandaloneEl;
    }
    
    public BigDecimal getPortfolioEl() {
        return portfolioEl;
    }
    
    public void setPortfolioEl(BigDecimal portfolioEl) {
        this.portfolioEl = portfolioEl;
    }
    
    public BigDecimal getDiversificationBenefitPct() {
        return diversificationBenefitPct;
    }
    
    public void setDiversificationBenefitPct(BigDecimal diversificationBenefitPct) {
        this.diversificationBenefitPct = diversificationBenefitPct;
    }
}
