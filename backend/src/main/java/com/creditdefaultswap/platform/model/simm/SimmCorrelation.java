package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing SIMM correlation coefficients
 * Supports both within-bucket and cross-bucket correlations
 */
@Entity
@Table(name = "simm_correlations")
public class SimmCorrelation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_set_id", nullable = false)
    private SimmParameterSet parameterSet;
    
    @Column(name = "risk_class", nullable = false, length = 20)
    private String riskClass; // IR, FX, EQ, CO, CR_Q, CR_NQ
    
    @Enumerated(EnumType.STRING)
    @Column(name = "correlation_type", nullable = false, length = 20)
    private CorrelationType correlationType;
    
    @Column(name = "bucket_from", length = 10)
    private String bucketFrom;
    
    @Column(name = "bucket_to", length = 10)
    private String bucketTo;
    
    @Column(name = "risk_factor_from", length = 100)
    private String riskFactorFrom;
    
    @Column(name = "risk_factor_to", length = 100)
    private String riskFactorTo;
    
    @Column(name = "correlation", nullable = false, precision = 8, scale = 6)
    private BigDecimal correlation;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Correlation type enumeration
    public enum CorrelationType {
        WITHIN_BUCKET, CROSS_BUCKET
    }
    
    // Constructors
    public SimmCorrelation() {}
    
    public SimmCorrelation(String riskClass, CorrelationType correlationType, 
                          BigDecimal correlation) {
        this.riskClass = riskClass;
        this.correlationType = correlationType;
        this.correlation = correlation;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SimmParameterSet getParameterSet() { return parameterSet; }
    public void setParameterSet(SimmParameterSet parameterSet) { this.parameterSet = parameterSet; }
    
    public String getRiskClass() { return riskClass; }
    public void setRiskClass(String riskClass) { this.riskClass = riskClass; }
    
    public CorrelationType getCorrelationType() { return correlationType; }
    public void setCorrelationType(CorrelationType correlationType) { 
        this.correlationType = correlationType; 
    }
    
    public String getBucketFrom() { return bucketFrom; }
    public void setBucketFrom(String bucketFrom) { this.bucketFrom = bucketFrom; }
    
    public String getBucketTo() { return bucketTo; }
    public void setBucketTo(String bucketTo) { this.bucketTo = bucketTo; }
    
    public String getRiskFactorFrom() { return riskFactorFrom; }
    public void setRiskFactorFrom(String riskFactorFrom) { this.riskFactorFrom = riskFactorFrom; }
    
    public String getRiskFactorTo() { return riskFactorTo; }
    public void setRiskFactorTo(String riskFactorTo) { this.riskFactorTo = riskFactorTo; }
    
    public BigDecimal getCorrelation() { return correlation; }
    public void setCorrelation(BigDecimal correlation) { this.correlation = correlation; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * Check if this correlation applies to the given bucket pair
     */
    public boolean appliesToBuckets(String bucket1, String bucket2) {
        if (correlationType == CorrelationType.WITHIN_BUCKET) {
            return bucket1.equals(bucket2) && bucket1.equals(bucketFrom);
        } else {
            return (bucket1.equals(bucketFrom) && bucket2.equals(bucketTo)) ||
                   (bucket1.equals(bucketTo) && bucket2.equals(bucketFrom));
        }
    }
    
    /**
     * Check if this correlation applies to the given risk factor pair
     */
    public boolean appliesToRiskFactors(String factor1, String factor2) {
        if (riskFactorFrom == null || riskFactorTo == null) {
            return true; // Bucket-level correlation
        }
        
        return (factor1.equals(riskFactorFrom) && factor2.equals(riskFactorTo)) ||
               (factor1.equals(riskFactorTo) && factor2.equals(riskFactorFrom));
    }
    
    @Override
    public String toString() {
        return "SimmCorrelation{" +
                "id=" + id +
                ", riskClass='" + riskClass + '\'' +
                ", correlationType=" + correlationType +
                ", bucketFrom='" + bucketFrom + '\'' +
                ", bucketTo='" + bucketTo + '\'' +
                ", correlation=" + correlation +
                '}';
    }
}