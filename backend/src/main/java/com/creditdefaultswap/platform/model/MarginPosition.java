package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "margin_positions")
public class MarginPosition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false)
    private MarginStatement statement;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "position_type", nullable = false, columnDefinition = "VARCHAR")
    private PositionType positionType;
    
    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "account_number", nullable = false)
    private String accountNumber;
    
    @Column(name = "portfolio_code")
    private String portfolioCode;
    
    @Column(name = "product_class")
    private String productClass;
    
    @Column(name = "netting_set_id")
    private String nettingSetId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum PositionType {
        VARIATION_MARGIN, INITIAL_MARGIN, EXCESS_COLLATERAL
    }
    
    // Constructors
    public MarginPosition() {}
    
    public MarginPosition(MarginStatement statement, PositionType positionType, 
                         BigDecimal amount, String currency, LocalDate effectiveDate,
                         String accountNumber) {
        this.statement = statement;
        this.positionType = positionType;
        this.amount = amount;
        this.currency = currency;
        this.effectiveDate = effectiveDate;
        this.accountNumber = accountNumber;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public MarginStatement getStatement() {
        return statement;
    }
    
    public void setStatement(MarginStatement statement) {
        this.statement = statement;
    }
    
    public PositionType getPositionType() {
        return positionType;
    }
    
    public void setPositionType(PositionType positionType) {
        this.positionType = positionType;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getPortfolioCode() {
        return portfolioCode;
    }
    
    public void setPortfolioCode(String portfolioCode) {
        this.portfolioCode = portfolioCode;
    }
    
    public String getProductClass() {
        return productClass;
    }
    
    public void setProductClass(String productClass) {
        this.productClass = productClass;
    }
    
    public String getNettingSetId() {
        return nettingSetId;
    }
    
    public void setNettingSetId(String nettingSetId) {
        this.nettingSetId = nettingSetId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}