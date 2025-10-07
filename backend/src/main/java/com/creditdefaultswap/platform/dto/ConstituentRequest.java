package com.creditdefaultswap.platform.dto;

import com.creditdefaultswap.platform.model.WeightType;

import java.math.BigDecimal;

public class ConstituentRequest {
    
    private Long tradeId;
    private WeightType weightType;
    private BigDecimal weightValue;
    
    // Constructors
    public ConstituentRequest() {}
    
    public ConstituentRequest(Long tradeId, WeightType weightType, BigDecimal weightValue) {
        this.tradeId = tradeId;
        this.weightType = weightType;
        this.weightValue = weightValue;
    }
    
    // Getters and Setters
    public Long getTradeId() {
        return tradeId;
    }
    
    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }
    
    public WeightType getWeightType() {
        return weightType;
    }
    
    public void setWeightType(WeightType weightType) {
        this.weightType = weightType;
    }
    
    public BigDecimal getWeightValue() {
        return weightValue;
    }
    
    public void setWeightValue(BigDecimal weightValue) {
        this.weightValue = weightValue;
    }
}
