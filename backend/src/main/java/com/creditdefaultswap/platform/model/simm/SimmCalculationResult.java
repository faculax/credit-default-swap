package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing SIMM calculation results by risk class and bucket
 */
@Entity
@Table(name = "simm_calculation_results")
public class SimmCalculationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculation_id", nullable = false)
    private SimmCalculation calculation;
    
    @Column(name = "risk_class", nullable = false, length = 20)
    private String riskClass; // IR, FX, EQ, CO, CR_Q, CR_NQ
    
    @Column(name = "bucket", length = 10)
    private String bucket;
    
    @Column(name = "weighted_sensitivity", precision = 20, scale = 8)
    private BigDecimal weightedSensitivity;
    
    @Column(name = "correlation_adjustment", precision = 20, scale = 8)
    private BigDecimal correlationAdjustment;
    
    @Column(name = "margin_component", precision = 20, scale = 8)
    private BigDecimal marginComponent;
    
    @Column(name = "margin_component_usd", precision = 20, scale = 8)
    private BigDecimal marginComponentUsd;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public SimmCalculationResult() {}
    
    public SimmCalculationResult(String riskClass, String bucket, 
                                BigDecimal marginComponent) {
        this.riskClass = riskClass;
        this.bucket = bucket;
        this.marginComponent = marginComponent;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SimmCalculation getCalculation() { return calculation; }
    public void setCalculation(SimmCalculation calculation) { this.calculation = calculation; }
    
    public String getRiskClass() { return riskClass; }
    public void setRiskClass(String riskClass) { this.riskClass = riskClass; }
    
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    
    public BigDecimal getWeightedSensitivity() { return weightedSensitivity; }
    public void setWeightedSensitivity(BigDecimal weightedSensitivity) { 
        this.weightedSensitivity = weightedSensitivity; 
    }
    
    public BigDecimal getCorrelationAdjustment() { return correlationAdjustment; }
    public void setCorrelationAdjustment(BigDecimal correlationAdjustment) { 
        this.correlationAdjustment = correlationAdjustment; 
    }
    
    public BigDecimal getMarginComponent() { return marginComponent; }
    public void setMarginComponent(BigDecimal marginComponent) { 
        this.marginComponent = marginComponent; 
    }
    
    public BigDecimal getMarginComponentUsd() { return marginComponentUsd; }
    public void setMarginComponentUsd(BigDecimal marginComponentUsd) { 
        this.marginComponentUsd = marginComponentUsd; 
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "SimmCalculationResult{" +
                "id=" + id +
                ", riskClass='" + riskClass + '\'' +
                ", bucket='" + bucket + '\'' +
                ", marginComponent=" + marginComponent +
                ", marginComponentUsd=" + marginComponentUsd +
                '}';
    }
}