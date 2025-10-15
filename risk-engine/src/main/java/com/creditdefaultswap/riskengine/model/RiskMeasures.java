package com.creditdefaultswap.riskengine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class RiskMeasures {
    private Long tradeId;
    private Instant valuationTimestamp;
    
    // Core ORE fields
    private BigDecimal npv; // Net Present Value
    private String currency; // Currency denomination
    
    // Real CDS-specific fields from additional_results.csv
    private BigDecimal fairSpreadClean; // Fair spread (clean)
    private BigDecimal fairSpreadDirty; // Fair spread (dirty)
    private BigDecimal protectionLegNPV; // Protection leg NPV
    private BigDecimal premiumLegNPVClean; // Premium leg NPV (clean)
    private BigDecimal premiumLegNPVDirty; // Premium leg NPV (dirty)
    private BigDecimal accruedPremium; // Accrued premium amount
    private BigDecimal upfrontPremium; // Upfront premium amount
    private BigDecimal couponLegBPS; // Coupon leg basis point value
    private BigDecimal currentNotional; // Current notional amount
    private BigDecimal originalNotional; // Original notional amount
    
    // Credit risk arrays
    private List<BigDecimal> defaultProbabilities; // Default probabilities by period
    private List<BigDecimal> expectedLosses; // Expected losses by period
    private List<LocalDate> accrualStartDates; // Accrual start dates by period
    private List<LocalDate> accrualEndDates; // Accrual end dates by period
    
    // Cashflow schedule
    private List<Cashflow> cashflows; // Complete cashflow schedule
    
    // Market data snapshot used for this calculation
    private MarketDataSnapshot marketDataSnapshot;
    
    // DEPRECATED - These are fake estimates, kept for backwards compatibility but will be removed
    @Deprecated private BigDecimal dv01;
    @Deprecated private BigDecimal gamma;
    @Deprecated private BigDecimal var95;
    @Deprecated private BigDecimal expectedShortfall;
    @Deprecated private Map<String, BigDecimal> greeks;

    // Default constructor
    public RiskMeasures() {
        this.valuationTimestamp = Instant.now();
    }

    // Constructor with trade ID
    public RiskMeasures(Long tradeId) {
        this.tradeId = tradeId;
        this.valuationTimestamp = Instant.now();
    }

    // Getters and setters
    public Long getTradeId() { return tradeId; }
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }
    
    public Instant getValuationTimestamp() { return valuationTimestamp; }
    public void setValuationTimestamp(Instant valuationTimestamp) { this.valuationTimestamp = valuationTimestamp; }
    
    public BigDecimal getNpv() { return npv; }
    public void setNpv(BigDecimal npv) { this.npv = npv; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    // Real CDS fields
    public BigDecimal getFairSpreadClean() { return fairSpreadClean; }
    public void setFairSpreadClean(BigDecimal fairSpreadClean) { this.fairSpreadClean = fairSpreadClean; }
    
    public BigDecimal getFairSpreadDirty() { return fairSpreadDirty; }
    public void setFairSpreadDirty(BigDecimal fairSpreadDirty) { this.fairSpreadDirty = fairSpreadDirty; }
    
    public BigDecimal getProtectionLegNPV() { return protectionLegNPV; }
    public void setProtectionLegNPV(BigDecimal protectionLegNPV) { this.protectionLegNPV = protectionLegNPV; }
    
    public BigDecimal getPremiumLegNPVClean() { return premiumLegNPVClean; }
    public void setPremiumLegNPVClean(BigDecimal premiumLegNPVClean) { this.premiumLegNPVClean = premiumLegNPVClean; }
    
    public BigDecimal getPremiumLegNPVDirty() { return premiumLegNPVDirty; }
    public void setPremiumLegNPVDirty(BigDecimal premiumLegNPVDirty) { this.premiumLegNPVDirty = premiumLegNPVDirty; }
    
    public BigDecimal getAccruedPremium() { return accruedPremium; }
    public void setAccruedPremium(BigDecimal accruedPremium) { this.accruedPremium = accruedPremium; }
    
    public BigDecimal getUpfrontPremium() { return upfrontPremium; }
    public void setUpfrontPremium(BigDecimal upfrontPremium) { this.upfrontPremium = upfrontPremium; }
    
    public BigDecimal getCouponLegBPS() { return couponLegBPS; }
    public void setCouponLegBPS(BigDecimal couponLegBPS) { this.couponLegBPS = couponLegBPS; }
    
    public BigDecimal getCurrentNotional() { return currentNotional; }
    public void setCurrentNotional(BigDecimal currentNotional) { this.currentNotional = currentNotional; }
    
    public BigDecimal getOriginalNotional() { return originalNotional; }
    public void setOriginalNotional(BigDecimal originalNotional) { this.originalNotional = originalNotional; }
    
    public List<BigDecimal> getDefaultProbabilities() { return defaultProbabilities; }
    public void setDefaultProbabilities(List<BigDecimal> defaultProbabilities) { this.defaultProbabilities = defaultProbabilities; }
    
    public List<BigDecimal> getExpectedLosses() { return expectedLosses; }
    public void setExpectedLosses(List<BigDecimal> expectedLosses) { this.expectedLosses = expectedLosses; }
    
    public List<LocalDate> getAccrualStartDates() { return accrualStartDates; }
    public void setAccrualStartDates(List<LocalDate> accrualStartDates) { this.accrualStartDates = accrualStartDates; }
    
    public List<LocalDate> getAccrualEndDates() { return accrualEndDates; }
    public void setAccrualEndDates(List<LocalDate> accrualEndDates) { this.accrualEndDates = accrualEndDates; }
    
    public List<Cashflow> getCashflows() { return cashflows; }
    public void setCashflows(List<Cashflow> cashflows) { this.cashflows = cashflows; }
    
    public MarketDataSnapshot getMarketDataSnapshot() { return marketDataSnapshot; }
    public void setMarketDataSnapshot(MarketDataSnapshot marketDataSnapshot) { this.marketDataSnapshot = marketDataSnapshot; }
    
    // DEPRECATED getters/setters
    @Deprecated public BigDecimal getDv01() { return dv01; }
    @Deprecated public void setDv01(BigDecimal dv01) { this.dv01 = dv01; }
    
    @Deprecated public BigDecimal getGamma() { return gamma; }
    @Deprecated public void setGamma(BigDecimal gamma) { this.gamma = gamma; }
    
    @Deprecated public BigDecimal getVar95() { return var95; }
    @Deprecated public void setVar95(BigDecimal var95) { this.var95 = var95; }
    
    @Deprecated public BigDecimal getExpectedShortfall() { return expectedShortfall; }
    @Deprecated public void setExpectedShortfall(BigDecimal expectedShortfall) { this.expectedShortfall = expectedShortfall; }
    
    @Deprecated public Map<String, BigDecimal> getGreeks() { return greeks; }
    @Deprecated public void setGreeks(Map<String, BigDecimal> greeks) { this.greeks = greeks; }
}
