package com.creditdefaultswap.platform.model.saccr;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SA-CCR Supervisory Parameters entity
 * Stores jurisdiction-specific regulatory parameters for SA-CCR calculations
 */
@Entity
@Table(name = "sa_ccr_supervisory_parameters")
public class SaCcrSupervisoryParameter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "jurisdiction", nullable = false, length = 10)
    private String jurisdiction;
    
    @Column(name = "asset_class", nullable = false, length = 50)
    private String assetClass;
    
    @Column(name = "parameter_type", nullable = false, length = 50)
    private String parameterType;
    
    @Column(name = "tenor_bucket", length = 20)
    private String tenorBucket;
    
    @Column(name = "credit_quality", length = 20)
    private String creditQuality;
    
    @Column(name = "parameter_value", nullable = false, precision = 10, scale = 6)
    private BigDecimal parameterValue;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Enums for standardized values
    public enum AssetClass {
        CREDIT, IR, FX, EQUITY, COMMODITY, ALL
    }
    
    public enum ParameterType {
        ALPHA_FACTOR, SUPERVISORY_FACTOR, CORRELATION_CROSS_ASSET, CORRELATION_SAME_BUCKET, CORRELATION_DIFF_BUCKET
    }
    
    public enum Jurisdiction {
        US, EU, UK, CA, JP, AU, SG, HK
    }
    
    public enum CreditQuality {
        IG, HY, UNRATED
    }
    
    public enum TenorBucket {
        LESS_THAN_1Y, Y1_TO_2Y, Y2_TO_5Y, GREATER_THAN_5Y
    }
    
    // Constructors
    public SaCcrSupervisoryParameter() {}
    
    public SaCcrSupervisoryParameter(String jurisdiction, String assetClass, String parameterType, 
                                   BigDecimal parameterValue, LocalDate effectiveDate) {
        this.jurisdiction = jurisdiction;
        this.assetClass = assetClass;
        this.parameterType = parameterType;
        this.parameterValue = parameterValue;
        this.effectiveDate = effectiveDate;
    }
    
    // Business methods
    
    /**
     * Check if parameter is currently effective
     */
    public boolean isEffective(LocalDate asOfDate) {
        if (asOfDate.isBefore(effectiveDate)) {
            return false;
        }
        return expiryDate == null || !asOfDate.isAfter(expiryDate);
    }
    
    /**
     * Get standard supervisory factor for Credit asset class by quality
     */
    public static BigDecimal getStandardCreditSupervisoryFactor(CreditQuality quality) {
        switch (quality) {
            case IG:
                return new BigDecimal("0.0050"); // 0.5%
            case HY:
                return new BigDecimal("0.0130"); // 1.3%
            case UNRATED:
                return new BigDecimal("0.0130"); // Treat as HY
            default:
                return new BigDecimal("0.0050");
        }
    }
    
    /**
     * Get standard supervisory factor for Interest Rate by tenor
     */
    public static BigDecimal getStandardIrSupervisoryFactor(TenorBucket tenor) {
        switch (tenor) {
            case LESS_THAN_1Y:
            case Y1_TO_2Y:
                return new BigDecimal("0.0050"); // 0.5%
            case Y2_TO_5Y:
                return new BigDecimal("0.0075"); // 0.75%
            case GREATER_THAN_5Y:
                return new BigDecimal("0.0150"); // 1.5%
            default:
                return new BigDecimal("0.0050");
        }
    }
    
    /**
     * Get standard alpha factor for jurisdiction
     */
    public static BigDecimal getStandardAlphaFactor(Jurisdiction jurisdiction) {
        // Most jurisdictions use 1.4 as per Basel III
        return new BigDecimal("1.4");
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    
    public String getAssetClass() { return assetClass; }
    public void setAssetClass(String assetClass) { this.assetClass = assetClass; }
    
    public String getParameterType() { return parameterType; }
    public void setParameterType(String parameterType) { this.parameterType = parameterType; }
    
    public String getTenorBucket() { return tenorBucket; }
    public void setTenorBucket(String tenorBucket) { this.tenorBucket = tenorBucket; }
    
    public String getCreditQuality() { return creditQuality; }
    public void setCreditQuality(String creditQuality) { this.creditQuality = creditQuality; }
    
    public BigDecimal getParameterValue() { return parameterValue; }
    public void setParameterValue(BigDecimal parameterValue) { this.parameterValue = parameterValue; }
    
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "SaCcrSupervisoryParameter{" +
                "id=" + id +
                ", jurisdiction='" + jurisdiction + '\'' +
                ", assetClass='" + assetClass + '\'' +
                ", parameterType='" + parameterType + '\'' +
                ", parameterValue=" + parameterValue +
                ", effectiveDate=" + effectiveDate +
                '}';
    }
}