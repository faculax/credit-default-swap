package com.creditdefaultswap.riskengine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class RiskMeasures {
    // Legacy fields for backwards compatibility
    private Long tradeId;
    private BigDecimal pvClean;
    private BigDecimal pvDirty;
    private BigDecimal parSpread;
    private BigDecimal cs01;
    private BigDecimal dv01;
    private BigDecimal jtd;
    private BigDecimal recovery01;
    private Instant valuationTimestamp;
    
    // New fields for ORE integration
    private BigDecimal npv; // Net Present Value
    private BigDecimal gamma; // Second-order sensitivity
    private BigDecimal var95; // Value at Risk 95%
    private BigDecimal expectedShortfall; // Expected Shortfall
    private String currency; // Currency denomination
    private Map<String, BigDecimal> greeks; // Greeks (delta, gamma, theta, vega, rho)

    // Constructor for legacy usage
    public RiskMeasures(Long tradeId) {
        this.tradeId = tradeId;
        this.valuationTimestamp = Instant.now();
    }
    
    // Default constructor for ORE usage
    public RiskMeasures() {
        this.valuationTimestamp = Instant.now();
    }

    // Legacy getters
    public Long getTradeId() { return tradeId; }
    public BigDecimal getPvClean() { return pvClean; }
    public BigDecimal getPvDirty() { return pvDirty; }
    public BigDecimal getParSpread() { return parSpread; }
    public BigDecimal getCs01() { return cs01; }
    public BigDecimal getDv01() { return dv01; }
    public BigDecimal getJtd() { return jtd; }
    public BigDecimal getRecovery01() { return recovery01; }
    public Instant getValuationTimestamp() { return valuationTimestamp; }

    // Legacy fluent setters for backwards compatibility
    public RiskMeasures withPvClean(BigDecimal v){ this.pvClean=v; return this; }
    public RiskMeasures withPvDirty(BigDecimal v){ this.pvDirty=v; return this; }
    public RiskMeasures withParSpread(BigDecimal v){ this.parSpread=v; return this; }
    public RiskMeasures withCs01(BigDecimal v){ this.cs01=v; return this; }
    public RiskMeasures withDv01(BigDecimal v){ this.dv01=v; return this; }
    public RiskMeasures withJtd(BigDecimal v){ this.jtd=v; return this; }
    public RiskMeasures withRecovery01(BigDecimal v){ this.recovery01=v; return this; }
    
    // New getters and setters for ORE integration
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }
    
    public BigDecimal getNpv() { return npv; }
    public void setNpv(BigDecimal npv) { this.npv = npv; }
    
    public BigDecimal getGamma() { return gamma; }
    public void setGamma(BigDecimal gamma) { this.gamma = gamma; }
    
    public BigDecimal getVar95() { return var95; }
    public void setVar95(BigDecimal var95) { this.var95 = var95; }
    
    public BigDecimal getExpectedShortfall() { return expectedShortfall; }
    public void setExpectedShortfall(BigDecimal expectedShortfall) { this.expectedShortfall = expectedShortfall; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public Map<String, BigDecimal> getGreeks() { return greeks; }
    public void setGreeks(Map<String, BigDecimal> greeks) { this.greeks = greeks; }
    
    // Setter for dv01 to support ORE integration
    public void setDv01(BigDecimal dv01) { this.dv01 = dv01; }
}
