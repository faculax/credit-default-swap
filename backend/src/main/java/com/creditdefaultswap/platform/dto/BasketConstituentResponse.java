package com.creditdefaultswap.platform.dto;

import java.math.BigDecimal;

/**
 * Response DTO for basket constituents
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public class BasketConstituentResponse {
    
    private Long id;
    private String issuer;
    private BigDecimal weight;
    private BigDecimal normalizedWeight;
    private BigDecimal recoveryOverride;
    private BigDecimal effectiveRecovery;
    private String seniority;
    private String sector;
    private String hazardCurveId;
    
    // Constructors
    public BasketConstituentResponse() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public BigDecimal getNormalizedWeight() {
        return normalizedWeight;
    }
    
    public void setNormalizedWeight(BigDecimal normalizedWeight) {
        this.normalizedWeight = normalizedWeight;
    }
    
    public BigDecimal getRecoveryOverride() {
        return recoveryOverride;
    }
    
    public void setRecoveryOverride(BigDecimal recoveryOverride) {
        this.recoveryOverride = recoveryOverride;
    }
    
    public BigDecimal getEffectiveRecovery() {
        return effectiveRecovery;
    }
    
    public void setEffectiveRecovery(BigDecimal effectiveRecovery) {
        this.effectiveRecovery = effectiveRecovery;
    }
    
    public String getSeniority() {
        return seniority;
    }
    
    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }
    
    public String getSector() {
        return sector;
    }
    
    public void setSector(String sector) {
        this.sector = sector;
    }
    
    public String getHazardCurveId() {
        return hazardCurveId;
    }
    
    public void setHazardCurveId(String hazardCurveId) {
        this.hazardCurveId = hazardCurveId;
    }
}
