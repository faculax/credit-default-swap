package com.creditdefaultswap.platform.dto;

import com.creditdefaultswap.platform.model.BasketType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating/updating baskets
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public class BasketRequest {
    
    private String name;
    private BasketType type;
    private Integer nth;
    private BigDecimal attachmentPoint;
    private BigDecimal detachmentPoint;
    private String premiumFrequency;
    private String dayCount;
    private String currency;
    private BigDecimal notional;
    private LocalDate maturityDate;
    private List<BasketConstituentRequest> constituents;
    
    // Constructors
    public BasketRequest() {}
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BasketType getType() {
        return type;
    }
    
    public void setType(BasketType type) {
        this.type = type;
    }
    
    public Integer getNth() {
        return nth;
    }
    
    public void setNth(Integer nth) {
        this.nth = nth;
    }
    
    public BigDecimal getAttachmentPoint() {
        return attachmentPoint;
    }
    
    public void setAttachmentPoint(BigDecimal attachmentPoint) {
        this.attachmentPoint = attachmentPoint;
    }
    
    public BigDecimal getDetachmentPoint() {
        return detachmentPoint;
    }
    
    public void setDetachmentPoint(BigDecimal detachmentPoint) {
        this.detachmentPoint = detachmentPoint;
    }
    
    public String getPremiumFrequency() {
        return premiumFrequency;
    }
    
    public void setPremiumFrequency(String premiumFrequency) {
        this.premiumFrequency = premiumFrequency;
    }
    
    public String getDayCount() {
        return dayCount;
    }
    
    public void setDayCount(String dayCount) {
        this.dayCount = dayCount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public BigDecimal getNotional() {
        return notional;
    }
    
    public void setNotional(BigDecimal notional) {
        this.notional = notional;
    }
    
    public LocalDate getMaturityDate() {
        return maturityDate;
    }
    
    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }
    
    public List<BasketConstituentRequest> getConstituents() {
        return constituents;
    }
    
    public void setConstituents(List<BasketConstituentRequest> constituents) {
        this.constituents = constituents;
    }
}
