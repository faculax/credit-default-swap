package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a SIMM calculation request and its overall results
 */
@Entity
@Table(name = "simm_calculations")
public class SimmCalculation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "calculation_id", unique = true, nullable = false, length = 100)
    private String calculationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private CrifUpload upload;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_set_id", nullable = false)
    private SimmParameterSet parameterSet;
    
    @Column(name = "portfolio_id", length = 100)
    private String portfolioId;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    @Column(name = "reporting_currency", nullable = false, length = 3)
    private String reportingCurrency = "USD";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_status", nullable = false, length = 20)
    private CalculationStatus calculationStatus = CalculationStatus.PENDING;
    
    @Column(name = "total_im", precision = 20, scale = 8)
    private BigDecimal totalIm;
    
    @Column(name = "total_im_usd", precision = 20, scale = 8)
    private BigDecimal totalImUsd;
    
    @Column(name = "diversification_benefit", precision = 20, scale = 8)
    private BigDecimal diversificationBenefit;
    
    @Column(name = "calculation_time_ms")
    private Long calculationTimeMs;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "calculation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimmCalculationResult> results = new ArrayList<>();
    
    @OneToMany(mappedBy = "calculation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimmCalculationAudit> auditTrail = new ArrayList<>();
    
    // Calculation status enumeration
    public enum CalculationStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
    
    // Constructors
    public SimmCalculation() {}
    
    public SimmCalculation(String calculationId, CrifUpload upload, 
                          SimmParameterSet parameterSet, LocalDate calculationDate) {
        this.calculationId = calculationId;
        this.upload = upload;
        this.parameterSet = parameterSet;
        this.calculationDate = calculationDate;
        this.portfolioId = upload.getPortfolioId();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCalculationId() { return calculationId; }
    public void setCalculationId(String calculationId) { this.calculationId = calculationId; }
    
    public CrifUpload getUpload() { return upload; }
    public void setUpload(CrifUpload upload) { this.upload = upload; }
    
    public SimmParameterSet getParameterSet() { return parameterSet; }
    public void setParameterSet(SimmParameterSet parameterSet) { this.parameterSet = parameterSet; }
    
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    
    public LocalDate getCalculationDate() { return calculationDate; }
    public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }
    
    public String getReportingCurrency() { return reportingCurrency; }
    public void setReportingCurrency(String reportingCurrency) { 
        this.reportingCurrency = reportingCurrency; 
    }
    
    public CalculationStatus getCalculationStatus() { return calculationStatus; }
    public void setCalculationStatus(CalculationStatus calculationStatus) { 
        this.calculationStatus = calculationStatus; 
    }
    
    public BigDecimal getTotalIm() { return totalIm; }
    public void setTotalIm(BigDecimal totalIm) { this.totalIm = totalIm; }
    
    public BigDecimal getTotalImUsd() { return totalImUsd; }
    public void setTotalImUsd(BigDecimal totalImUsd) { this.totalImUsd = totalImUsd; }
    
    public BigDecimal getDiversificationBenefit() { return diversificationBenefit; }
    public void setDiversificationBenefit(BigDecimal diversificationBenefit) { 
        this.diversificationBenefit = diversificationBenefit; 
    }
    
    public Long getCalculationTimeMs() { return calculationTimeMs; }
    public void setCalculationTimeMs(Long calculationTimeMs) { 
        this.calculationTimeMs = calculationTimeMs; 
    }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<SimmCalculationResult> getResults() { return results; }
    public void setResults(List<SimmCalculationResult> results) { this.results = results; }
    
    public List<SimmCalculationAudit> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<SimmCalculationAudit> auditTrail) { this.auditTrail = auditTrail; }
    
    // Helper methods
    public void addResult(SimmCalculationResult result) {
        results.add(result);
        result.setCalculation(this);
    }
    
    public void addAuditEntry(SimmCalculationAudit auditEntry) {
        auditTrail.add(auditEntry);
        auditEntry.setCalculation(this);
    }
    
    public boolean isCalculationComplete() {
        return calculationStatus == CalculationStatus.COMPLETED || 
               calculationStatus == CalculationStatus.FAILED;
    }
    
    public boolean isSuccessful() {
        return calculationStatus == CalculationStatus.COMPLETED && totalIm != null;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "SimmCalculation{" +
                "id=" + id +
                ", calculationId='" + calculationId + '\'' +
                ", portfolioId='" + portfolioId + '\'' +
                ", calculationDate=" + calculationDate +
                ", calculationStatus=" + calculationStatus +
                ", totalIm=" + totalIm +
                ", reportingCurrency='" + reportingCurrency + '\'' +
                '}';
    }
}