package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "margin_statements")
public class MarginStatement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "statement_id", nullable = false)
    private String statementId;
    
    @Column(name = "ccp_name", nullable = false)
    private String ccpName;
    
    @Column(name = "member_firm", nullable = false)
    private String memberFirm;
    
    @Column(name = "account_number", nullable = false)
    private String accountNumber;
    
    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "statement_format", nullable = false, columnDefinition = "VARCHAR")
    private StatementFormat statementFormat;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR")
    private StatementStatus status = StatementStatus.PENDING;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "variation_margin", precision = 15, scale = 2)
    private BigDecimal variationMargin;
    
    @Column(name = "initial_margin", precision = 15, scale = 2)
    private BigDecimal initialMargin;
    
    public enum StatementFormat {
        CSV, XML, JSON, PROPRIETARY
    }
    
    public enum StatementStatus {
        PENDING, PROCESSING, PROCESSED, FAILED, DISPUTED, RETRYING
    }
    
    // Constructors
    public MarginStatement() {}
    
    public MarginStatement(String statementId, String ccpName, String memberFirm, 
                          String accountNumber, LocalDate statementDate, String currency,
                          StatementFormat statementFormat, String fileName) {
        this.statementId = statementId;
        this.ccpName = ccpName;
        this.memberFirm = memberFirm;
        this.accountNumber = accountNumber;
        this.statementDate = statementDate;
        this.currency = currency;
        this.statementFormat = statementFormat;
        this.fileName = fileName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getStatementId() {
        return statementId;
    }
    
    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }
    
    public String getCcpName() {
        return ccpName;
    }
    
    public void setCcpName(String ccpName) {
        this.ccpName = ccpName;
    }
    
    public String getMemberFirm() {
        return memberFirm;
    }
    
    public void setMemberFirm(String memberFirm) {
        this.memberFirm = memberFirm;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public LocalDate getStatementDate() {
        return statementDate;
    }
    
    public void setStatementDate(LocalDate statementDate) {
        this.statementDate = statementDate;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public StatementFormat getStatementFormat() {
        return statementFormat;
    }
    
    public void setStatementFormat(StatementFormat statementFormat) {
        this.statementFormat = statementFormat;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getRawContent() {
        return rawContent;
    }
    
    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }
    
    public StatementStatus getStatus() {
        return status;
    }
    
    public void setStatus(StatementStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == StatementStatus.PROCESSED) {
            this.processedAt = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
    }
    
    public BigDecimal getVariationMargin() {
        return variationMargin;
    }
    
    public void setVariationMargin(BigDecimal variationMargin) {
        this.variationMargin = variationMargin;
    }
    
    public BigDecimal getInitialMargin() {
        return initialMargin;
    }
    
    public void setInitialMargin(BigDecimal initialMargin) {
        this.initialMargin = initialMargin;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}