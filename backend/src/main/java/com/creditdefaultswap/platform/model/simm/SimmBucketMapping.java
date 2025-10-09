package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing SIMM bucket mappings for risk factor classification
 */
@Entity
@Table(name = "simm_bucket_mappings")
public class SimmBucketMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_set_id", nullable = false)
    private SimmParameterSet parameterSet;
    
    @Column(name = "risk_class", nullable = false, length = 20)
    private String riskClass; // IR, FX, EQ, CO, CR_Q, CR_NQ
    
    @Column(name = "risk_factor", nullable = false, length = 100)
    private String riskFactor;
    
    @Column(name = "bucket", nullable = false, length = 10)
    private String bucket;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public SimmBucketMapping() {}
    
    public SimmBucketMapping(String riskClass, String riskFactor, String bucket) {
        this.riskClass = riskClass;
        this.riskFactor = riskFactor;
        this.bucket = bucket;
    }
    
    public SimmBucketMapping(String riskClass, String riskFactor, String bucket, String description) {
        this.riskClass = riskClass;
        this.riskFactor = riskFactor;
        this.bucket = bucket;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SimmParameterSet getParameterSet() { return parameterSet; }
    public void setParameterSet(SimmParameterSet parameterSet) { this.parameterSet = parameterSet; }
    
    public String getRiskClass() { return riskClass; }
    public void setRiskClass(String riskClass) { this.riskClass = riskClass; }
    
    public String getRiskFactor() { return riskFactor; }
    public void setRiskFactor(String riskFactor) { this.riskFactor = riskFactor; }
    
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "SimmBucketMapping{" +
                "id=" + id +
                ", riskClass='" + riskClass + '\'' +
                ", riskFactor='" + riskFactor + '\'' +
                ", bucket='" + bucket + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}