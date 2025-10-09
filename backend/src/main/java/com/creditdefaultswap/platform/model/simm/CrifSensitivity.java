package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a CRIF (Common Risk Interchange Format) sensitivity record
 * Compliant with ISDA SIMM 2.6+ specification
 */
@Entity
@Table(name = "crif_sensitivities")
public class CrifSensitivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private CrifUpload upload;
    
    @Column(name = "trade_id", length = 100)
    private String tradeId;
    
    @Column(name = "portfolio_id", length = 100)
    private String portfolioId;
    
    @Column(name = "product_class", nullable = false, length = 50)
    private String productClass; // RatesFX, Credit, Equity, Commodity
    
    @Column(name = "risk_type", nullable = false, length = 50)
    private String riskType; // Risk_IRCurve, Risk_CreditQ, Risk_Equity, etc.
    
    @Column(name = "risk_class", nullable = false, length = 20)
    private String riskClass; // IR, FX, EQ, CO, CR_Q, CR_NQ
    
    @Column(name = "bucket", length = 10)
    private String bucket;
    
    @Column(name = "label1", length = 100)
    private String label1; // Currency, Index Name, etc.
    
    @Column(name = "label2", length = 100)
    private String label2; // Tenor, Sub-curve, etc.
    
    @Column(name = "amount_base_currency", nullable = false, precision = 20, scale = 8)
    private BigDecimal amountBaseCurrency;
    
    @Column(name = "amount_usd", precision = 20, scale = 8)
    private BigDecimal amountUsd;
    
    @Column(name = "collect_regulations", length = 50)
    private String collectRegulations;
    
    @Column(name = "post_regulations", length = 50)
    private String postRegulations;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public CrifSensitivity() {}
    
    public CrifSensitivity(String productClass, String riskType, String riskClass,
                          BigDecimal amountBaseCurrency) {
        this.productClass = productClass;
        this.riskType = riskType;
        this.riskClass = riskClass;
        this.amountBaseCurrency = amountBaseCurrency;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public CrifUpload getUpload() { return upload; }
    public void setUpload(CrifUpload upload) { this.upload = upload; }
    
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    
    public String getProductClass() { return productClass; }
    public void setProductClass(String productClass) { this.productClass = productClass; }
    
    public String getRiskType() { return riskType; }
    public void setRiskType(String riskType) { this.riskType = riskType; }
    
    public String getRiskClass() { return riskClass; }
    public void setRiskClass(String riskClass) { this.riskClass = riskClass; }
    
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    
    public String getLabel1() { return label1; }
    public void setLabel1(String label1) { this.label1 = label1; }
    
    public String getLabel2() { return label2; }
    public void setLabel2(String label2) { this.label2 = label2; }
    
    public BigDecimal getAmountBaseCurrency() { return amountBaseCurrency; }
    public void setAmountBaseCurrency(BigDecimal amountBaseCurrency) { 
        this.amountBaseCurrency = amountBaseCurrency; 
    }
    
    public BigDecimal getAmountUsd() { return amountUsd; }
    public void setAmountUsd(BigDecimal amountUsd) { this.amountUsd = amountUsd; }
    
    public String getCollectRegulations() { return collectRegulations; }
    public void setCollectRegulations(String collectRegulations) { 
        this.collectRegulations = collectRegulations; 
    }
    
    public String getPostRegulations() { return postRegulations; }
    public void setPostRegulations(String postRegulations) { 
        this.postRegulations = postRegulations; 
    }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * Get risk factor key for bucket mapping and correlation lookups
     */
    public String getRiskFactorKey() {
        if (label1 != null && label2 != null) {
            return label1 + "_" + label2;
        } else if (label1 != null) {
            return label1;
        } else {
            return riskType;
        }
    }
    
    @Override
    public String toString() {
        return "CrifSensitivity{" +
                "id=" + id +
                ", tradeId='" + tradeId + '\'' +
                ", portfolioId='" + portfolioId + '\'' +
                ", riskClass='" + riskClass + '\'' +
                ", bucket='" + bucket + '\'' +
                ", amountBaseCurrency=" + amountBaseCurrency +
                '}';
    }
}