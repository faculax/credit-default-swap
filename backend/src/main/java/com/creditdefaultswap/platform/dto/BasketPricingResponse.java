package com.creditdefaultswap.platform.dto;

import com.creditdefaultswap.platform.model.BasketType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for basket pricing
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public class BasketPricingResponse {
    
    private Long basketId;
    private LocalDate valuationDate;
    private BasketType type;
    private BigDecimal notional;
    
    // Pricing Results
    private BigDecimal fairSpreadBps;
    private BigDecimal premiumLegPv;
    private BigDecimal protectionLegPv;
    private BigDecimal pv;
    
    // Tranche-specific (nullable for FTD/Nth)
    private BigDecimal expectedTrancheLossPct;
    private List<TrancheLossPoint> etlTimeline;
    
    // Convergence Diagnostics
    private ConvergenceDiagnostics convergence;
    
    // Sensitivities (nullable if not requested)
    private SensitivitiesResponse sensitivities;
    
    // Constituents with effective values
    private List<BasketConstituentResponse> constituents;
    
    // Seed used (for reproducibility)
    private Long seedUsed;
    
    // Constructors
    public BasketPricingResponse() {}
    
    // Getters and Setters
    public Long getBasketId() {
        return basketId;
    }
    
    public void setBasketId(Long basketId) {
        this.basketId = basketId;
    }
    
    public LocalDate getValuationDate() {
        return valuationDate;
    }
    
    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }
    
    public BasketType getType() {
        return type;
    }
    
    public void setType(BasketType type) {
        this.type = type;
    }
    
    public BigDecimal getNotional() {
        return notional;
    }
    
    public void setNotional(BigDecimal notional) {
        this.notional = notional;
    }
    
    public BigDecimal getFairSpreadBps() {
        return fairSpreadBps;
    }
    
    public void setFairSpreadBps(BigDecimal fairSpreadBps) {
        this.fairSpreadBps = fairSpreadBps;
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
    
    public BigDecimal getPv() {
        return pv;
    }
    
    public void setPv(BigDecimal pv) {
        this.pv = pv;
    }
    
    public BigDecimal getExpectedTrancheLossPct() {
        return expectedTrancheLossPct;
    }
    
    public void setExpectedTrancheLossPct(BigDecimal expectedTrancheLossPct) {
        this.expectedTrancheLossPct = expectedTrancheLossPct;
    }
    
    public List<TrancheLossPoint> getEtlTimeline() {
        return etlTimeline;
    }
    
    public void setEtlTimeline(List<TrancheLossPoint> etlTimeline) {
        this.etlTimeline = etlTimeline;
    }
    
    public ConvergenceDiagnostics getConvergence() {
        return convergence;
    }
    
    public void setConvergence(ConvergenceDiagnostics convergence) {
        this.convergence = convergence;
    }
    
    public SensitivitiesResponse getSensitivities() {
        return sensitivities;
    }
    
    public void setSensitivities(SensitivitiesResponse sensitivities) {
        this.sensitivities = sensitivities;
    }
    
    public List<BasketConstituentResponse> getConstituents() {
        return constituents;
    }
    
    public void setConstituents(List<BasketConstituentResponse> constituents) {
        this.constituents = constituents;
    }
    
    public Long getSeedUsed() {
        return seedUsed;
    }
    
    public void setSeedUsed(Long seedUsed) {
        this.seedUsed = seedUsed;
    }
    
    /**
     * Inner class for tranche loss timeline points
     */
    public static class TrancheLossPoint {
        private String tenor;
        private BigDecimal etl;
        
        public TrancheLossPoint() {}
        
        public TrancheLossPoint(String tenor, BigDecimal etl) {
            this.tenor = tenor;
            this.etl = etl;
        }
        
        public String getTenor() {
            return tenor;
        }
        
        public void setTenor(String tenor) {
            this.tenor = tenor;
        }
        
        public BigDecimal getEtl() {
            return etl;
        }
        
        public void setEtl(BigDecimal etl) {
            this.etl = etl;
        }
    }
}
