package com.creditdefaultswap.platform.dto;

import java.math.BigDecimal;

/**
 * Request DTO for basket constituents
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public class BasketConstituentRequest {
    
    private String issuer;
    private BigDecimal weight;
    private BigDecimal recoveryOverride;
    private String seniority;
    private String sector;
    
    // Constructors
    public BasketConstituentRequest() {}
    
    public BasketConstituentRequest(String issuer) {
        this.issuer = issuer;
    }
    
    // Getters and Setters
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
    
    public BigDecimal getRecoveryOverride() {
        return recoveryOverride;
    }
    
    public void setRecoveryOverride(BigDecimal recoveryOverride) {
        this.recoveryOverride = recoveryOverride;
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
}
