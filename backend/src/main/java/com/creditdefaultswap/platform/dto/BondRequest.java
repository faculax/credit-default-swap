package com.creditdefaultswap.platform.dto;

import com.creditdefaultswap.platform.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Bond creation/update requests
 * Epic 14: Credit Bonds Enablement
 */
public class BondRequest {
    
    private String isin;
    private String issuer;
    private Seniority seniority;
    private String sector;
    private String currency;
    private BigDecimal notional;
    private BigDecimal couponRate;
    private CouponFrequency couponFrequency;
    private DayCount dayCount;
    private LocalDate issueDate;
    private LocalDate maturityDate;
    private Integer settlementDays;
    private BigDecimal faceValue;
    private PriceConvention priceConvention;
    
    // Getters and Setters
    public String getIsin() {
        return isin;
    }
    
    public void setIsin(String isin) {
        this.isin = isin;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public Seniority getSeniority() {
        return seniority;
    }
    
    public void setSeniority(Seniority seniority) {
        this.seniority = seniority;
    }
    
    public String getSector() {
        return sector;
    }
    
    public void setSector(String sector) {
        this.sector = sector;
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
    
    public BigDecimal getCouponRate() {
        return couponRate;
    }
    
    public void setCouponRate(BigDecimal couponRate) {
        this.couponRate = couponRate;
    }
    
    public CouponFrequency getCouponFrequency() {
        return couponFrequency;
    }
    
    public void setCouponFrequency(CouponFrequency couponFrequency) {
        this.couponFrequency = couponFrequency;
    }
    
    public DayCount getDayCount() {
        return dayCount;
    }
    
    public void setDayCount(DayCount dayCount) {
        this.dayCount = dayCount;
    }
    
    public LocalDate getIssueDate() {
        return issueDate;
    }
    
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
    
    public LocalDate getMaturityDate() {
        return maturityDate;
    }
    
    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }
    
    public Integer getSettlementDays() {
        return settlementDays;
    }
    
    public void setSettlementDays(Integer settlementDays) {
        this.settlementDays = settlementDays;
    }
    
    public BigDecimal getFaceValue() {
        return faceValue;
    }
    
    public void setFaceValue(BigDecimal faceValue) {
        this.faceValue = faceValue;
    }
    
    public PriceConvention getPriceConvention() {
        return priceConvention;
    }
    
    public void setPriceConvention(PriceConvention priceConvention) {
        this.priceConvention = priceConvention;
    }
}
