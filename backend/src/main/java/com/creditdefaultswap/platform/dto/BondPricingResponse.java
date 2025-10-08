package com.creditdefaultswap.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Bond pricing response
 * Epic 14: Credit Bonds Enablement (Story 14.10)
 */
public class BondPricingResponse {
    
    private Long bondId;
    private LocalDate valuationDate;
    
    // Prices
    private Double cleanPrice;
    private Double dirtyPrice;
    private Double accruedInterest;
    
    // Yield & Spread
    private Double yieldToMaturity;
    private Double zSpread;
    
    // Present Values
    private Double pv;
    private Double pvRisky;
    
    // Sensitivities
    private Sensitivities sensitivities;
    
    // Input echo
    private Inputs inputs;
    
    public static class Sensitivities {
        private Double irDv01;
        private Double spreadDv01;
        private Double jtd;
        private Double modifiedDuration;
        
        public Double getIrDv01() {
            return irDv01;
        }
        
        public void setIrDv01(Double irDv01) {
            this.irDv01 = irDv01;
        }
        
        public Double getSpreadDv01() {
            return spreadDv01;
        }
        
        public void setSpreadDv01(Double spreadDv01) {
            this.spreadDv01 = spreadDv01;
        }
        
        public Double getJtd() {
            return jtd;
        }
        
        public void setJtd(Double jtd) {
            this.jtd = jtd;
        }
        
        public Double getModifiedDuration() {
            return modifiedDuration;
        }
        
        public void setModifiedDuration(Double modifiedDuration) {
            this.modifiedDuration = modifiedDuration;
        }
    }
    
    public static class Inputs {
        private BigDecimal couponRate;
        private String couponFrequency;
        private String dayCount;
        private BigDecimal recoveryRate;
        private String creditCurveId;
        
        public BigDecimal getCouponRate() {
            return couponRate;
        }
        
        public void setCouponRate(BigDecimal couponRate) {
            this.couponRate = couponRate;
        }
        
        public String getCouponFrequency() {
            return couponFrequency;
        }
        
        public void setCouponFrequency(String couponFrequency) {
            this.couponFrequency = couponFrequency;
        }
        
        public String getDayCount() {
            return dayCount;
        }
        
        public void setDayCount(String dayCount) {
            this.dayCount = dayCount;
        }
        
        public BigDecimal getRecoveryRate() {
            return recoveryRate;
        }
        
        public void setRecoveryRate(BigDecimal recoveryRate) {
            this.recoveryRate = recoveryRate;
        }
        
        public String getCreditCurveId() {
            return creditCurveId;
        }
        
        public void setCreditCurveId(String creditCurveId) {
            this.creditCurveId = creditCurveId;
        }
    }
    
    // Getters and Setters
    public Long getBondId() {
        return bondId;
    }
    
    public void setBondId(Long bondId) {
        this.bondId = bondId;
    }
    
    public LocalDate getValuationDate() {
        return valuationDate;
    }
    
    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }
    
    public Double getCleanPrice() {
        return cleanPrice;
    }
    
    public void setCleanPrice(Double cleanPrice) {
        this.cleanPrice = cleanPrice;
    }
    
    public Double getDirtyPrice() {
        return dirtyPrice;
    }
    
    public void setDirtyPrice(Double dirtyPrice) {
        this.dirtyPrice = dirtyPrice;
    }
    
    public Double getAccruedInterest() {
        return accruedInterest;
    }
    
    public void setAccruedInterest(Double accruedInterest) {
        this.accruedInterest = accruedInterest;
    }
    
    public Double getYieldToMaturity() {
        return yieldToMaturity;
    }
    
    public void setYieldToMaturity(Double yieldToMaturity) {
        this.yieldToMaturity = yieldToMaturity;
    }
    
    public Double getzSpread() {
        return zSpread;
    }
    
    public void setzSpread(Double zSpread) {
        this.zSpread = zSpread;
    }
    
    public Double getPv() {
        return pv;
    }
    
    public void setPv(Double pv) {
        this.pv = pv;
    }
    
    public Double getPvRisky() {
        return pvRisky;
    }
    
    public void setPvRisky(Double pvRisky) {
        this.pvRisky = pvRisky;
    }
    
    public Sensitivities getSensitivities() {
        return sensitivities;
    }
    
    public void setSensitivities(Sensitivities sensitivities) {
        this.sensitivities = sensitivities;
    }
    
    public Inputs getInputs() {
        return inputs;
    }
    
    public void setInputs(Inputs inputs) {
        this.inputs = inputs;
    }
}
