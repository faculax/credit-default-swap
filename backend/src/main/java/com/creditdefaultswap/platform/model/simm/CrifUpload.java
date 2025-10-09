package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a CRIF file upload with metadata and processing status
 */
@Entity
@Table(name = "crif_uploads")
public class CrifUpload {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "upload_id", unique = true, nullable = false, length = 100)
    private String uploadId;
    
    @Column(name = "filename", nullable = false)
    private String filename;
    
    @Column(name = "portfolio_id", length = 100)
    private String portfolioId;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "upload_timestamp", nullable = false)
    private LocalDateTime uploadTimestamp = LocalDateTime.now();
    
    @Column(name = "total_records", nullable = false)
    private Integer totalRecords = 0;
    
    @Column(name = "valid_records", nullable = false)
    private Integer validRecords = 0;
    
    @Column(name = "error_records", nullable = false)
    private Integer errorRecords = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "upload", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CrifSensitivity> sensitivities = new ArrayList<>();
    
    // Processing status enumeration
    public enum ProcessingStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
    
    // Constructors
    public CrifUpload() {}
    
    public CrifUpload(String uploadId, String filename, String portfolioId, 
                     LocalDate valuationDate, String currency) {
        this.uploadId = uploadId;
        this.filename = filename;
        this.portfolioId = portfolioId;
        this.valuationDate = valuationDate;
        this.currency = currency;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    
    public LocalDate getValuationDate() { return valuationDate; }
    public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public LocalDateTime getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) { 
        this.uploadTimestamp = uploadTimestamp; 
    }
    
    public Integer getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Integer totalRecords) { this.totalRecords = totalRecords; }
    
    public Integer getValidRecords() { return validRecords; }
    public void setValidRecords(Integer validRecords) { this.validRecords = validRecords; }
    
    public Integer getErrorRecords() { return errorRecords; }
    public void setErrorRecords(Integer errorRecords) { this.errorRecords = errorRecords; }
    
    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { 
        this.processingStatus = processingStatus; 
    }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<CrifSensitivity> getSensitivities() { return sensitivities; }
    public void setSensitivities(List<CrifSensitivity> sensitivities) { 
        this.sensitivities = sensitivities; 
    }
    
    // Helper methods
    public void addSensitivity(CrifSensitivity sensitivity) {
        sensitivities.add(sensitivity);
        sensitivity.setUpload(this);
    }
    
    public void removeSensitivity(CrifSensitivity sensitivity) {
        sensitivities.remove(sensitivity);
        sensitivity.setUpload(null);
    }
    
    public void incrementValidRecords() {
        this.validRecords++;
        this.totalRecords++;
    }
    
    public void incrementErrorRecords() {
        this.errorRecords++;
        this.totalRecords++;
    }
    
    public double getSuccessRate() {
        if (totalRecords == 0) return 0.0;
        return (double) validRecords / totalRecords * 100.0;
    }
    
    public boolean isProcessingComplete() {
        return processingStatus == ProcessingStatus.COMPLETED || 
               processingStatus == ProcessingStatus.FAILED;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "CrifUpload{" +
                "id=" + id +
                ", uploadId='" + uploadId + '\'' +
                ", filename='" + filename + '\'' +
                ", portfolioId='" + portfolioId + '\'' +
                ", valuationDate=" + valuationDate +
                ", currency='" + currency + '\'' +
                ", processingStatus=" + processingStatus +
                ", totalRecords=" + totalRecords +
                ", validRecords=" + validRecords +
                ", errorRecords=" + errorRecords +
                '}';
    }
}