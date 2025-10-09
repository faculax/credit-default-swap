package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing SIMM risk weights by bucket and risk class
 */
@Entity
@Table(name = "simm_risk_weights")
public class SimmRiskWeight {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_set_id", nullable = false)
    private SimmParameterSet parameterSet;
    
    @Column(name = "risk_class", nullable = false, length = 20)
    private String riskClass; // IR, FX, EQ, CO, CR_Q, CR_NQ
    
    @Column(name = "bucket", nullable = false, length = 10)
    private String bucket;
    
    @Column(name = "risk_weight", nullable = false, precision = 10, scale = 6)
    private BigDecimal riskWeight;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public SimmRiskWeight() {}
    
    public SimmRiskWeight(String riskClass, String bucket, BigDecimal riskWeight) {
        this.riskClass = riskClass;
        this.bucket = bucket;
        this.riskWeight = riskWeight;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SimmParameterSet getParameterSet() { return parameterSet; }
    public void setParameterSet(SimmParameterSet parameterSet) { this.parameterSet = parameterSet; }
    
    public String getRiskClass() { return riskClass; }
    public void setRiskClass(String riskClass) { this.riskClass = riskClass; }
    
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    
    public BigDecimal getRiskWeight() { return riskWeight; }
    public void setRiskWeight(BigDecimal riskWeight) { this.riskWeight = riskWeight; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "SimmRiskWeight{" +
                "id=" + id +
                ", riskClass='" + riskClass + '\'' +
                ", bucket='" + bucket + '\'' +
                ", riskWeight=" + riskWeight +
                '}';
    }
}