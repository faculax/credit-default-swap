package com.creditdefaultswap.platform.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for basket sensitivities
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public class SensitivitiesResponse {
    
    private BigDecimal spreadDv01;
    private BigDecimal correlationBeta;
    private BigDecimal recovery01;
    private Map<String, BigDecimal> bumpSizes;
    
    // Constructors
    public SensitivitiesResponse() {}
    
    // Getters and Setters
    public BigDecimal getSpreadDv01() {
        return spreadDv01;
    }
    
    public void setSpreadDv01(BigDecimal spreadDv01) {
        this.spreadDv01 = spreadDv01;
    }
    
    public BigDecimal getCorrelationBeta() {
        return correlationBeta;
    }
    
    public void setCorrelationBeta(BigDecimal correlationBeta) {
        this.correlationBeta = correlationBeta;
    }
    
    public BigDecimal getRecovery01() {
        return recovery01;
    }
    
    public void setRecovery01(BigDecimal recovery01) {
        this.recovery01 = recovery01;
    }
    
    public Map<String, BigDecimal> getBumpSizes() {
        return bumpSizes;
    }
    
    public void setBumpSizes(Map<String, BigDecimal> bumpSizes) {
        this.bumpSizes = bumpSizes;
    }
}
