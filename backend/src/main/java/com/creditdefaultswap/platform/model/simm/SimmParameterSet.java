package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a SIMM parameter set with versioning information
 * Contains all parameters for a specific ISDA SIMM version
 */
@Entity
@Table(name = "simm_parameter_sets")
public class SimmParameterSet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "version_name", nullable = false, length = 50)
    private String versionName;
    
    @Column(name = "isda_version", nullable = false, length = 20)
    private String isdaVersion; // e.g., "2.6", "2.7"
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "parameterSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimmRiskWeight> riskWeights = new ArrayList<>();
    
    @OneToMany(mappedBy = "parameterSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimmCorrelation> correlations = new ArrayList<>();
    
    @OneToMany(mappedBy = "parameterSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimmBucketMapping> bucketMappings = new ArrayList<>();
    
    // Constructors
    public SimmParameterSet() {}
    
    public SimmParameterSet(String versionName, String isdaVersion, 
                           LocalDate effectiveDate, String description) {
        this.versionName = versionName;
        this.isdaVersion = isdaVersion;
        this.effectiveDate = effectiveDate;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }
    
    public String getIsdaVersion() { return isdaVersion; }
    public void setIsdaVersion(String isdaVersion) { this.isdaVersion = isdaVersion; }
    
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<SimmRiskWeight> getRiskWeights() { return riskWeights; }
    public void setRiskWeights(List<SimmRiskWeight> riskWeights) { this.riskWeights = riskWeights; }
    
    public List<SimmCorrelation> getCorrelations() { return correlations; }
    public void setCorrelations(List<SimmCorrelation> correlations) { this.correlations = correlations; }
    
    public List<SimmBucketMapping> getBucketMappings() { return bucketMappings; }
    public void setBucketMappings(List<SimmBucketMapping> bucketMappings) { 
        this.bucketMappings = bucketMappings; 
    }
    
    // Helper methods
    public void addRiskWeight(SimmRiskWeight riskWeight) {
        riskWeights.add(riskWeight);
        riskWeight.setParameterSet(this);
    }
    
    public void addCorrelation(SimmCorrelation correlation) {
        correlations.add(correlation);
        correlation.setParameterSet(this);
    }
    
    public void addBucketMapping(SimmBucketMapping bucketMapping) {
        bucketMappings.add(bucketMapping);
        bucketMapping.setParameterSet(this);
    }
    
    public boolean isEffectiveOn(LocalDate date) {
        if (date.isBefore(effectiveDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        return true;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "SimmParameterSet{" +
                "id=" + id +
                ", versionName='" + versionName + '\'' +
                ", isdaVersion='" + isdaVersion + '\'' +
                ", effectiveDate=" + effectiveDate +
                ", endDate=" + endDate +
                ", isActive=" + isActive +
                '}';
    }
}