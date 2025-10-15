package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cds_trades")
public class CDSTrade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reference_entity", nullable = false, length = 50)
    private String referenceEntity;
    
    @Column(name = "notional_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal notionalAmount;
    
    @Column(name = "spread", nullable = false, precision = 10, scale = 4)
    private BigDecimal spread;
    
    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "counterparty", nullable = false, length = 50)
    private String counterparty;
    
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "premium_frequency", nullable = false, length = 20)
    private String premiumFrequency;
    
    @Column(name = "day_count_convention", nullable = false, length = 20)
    private String dayCountConvention;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "buy_sell_protection", nullable = false)
    private ProtectionDirection buySellProtection;
    
    @Column(name = "restructuring_clause", length = 50)
    private String restructuringClause;
    
    @Column(name = "payment_calendar", nullable = false, length = 10)
    private String paymentCalendar;
    
    @Column(name = "accrual_start_date", nullable = false)
    private LocalDate accrualStartDate;
    
    @Column(name = "recovery_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal recoveryRate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_status", nullable = false)
    private TradeStatus tradeStatus;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
    // Enum for Buy/Sell Protection
    public enum ProtectionDirection {
        BUY, SELL
    }
    
    // Constructors
    public CDSTrade() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getReferenceEntity() {
        return referenceEntity;
    }
    
    public void setReferenceEntity(String referenceEntity) {
        this.referenceEntity = referenceEntity;
    }
    
    public BigDecimal getNotionalAmount() {
        return notionalAmount;
    }
    
    public void setNotionalAmount(BigDecimal notionalAmount) {
        this.notionalAmount = notionalAmount;
    }
    
    public BigDecimal getSpread() {
        return spread;
    }
    
    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }
    
    public LocalDate getMaturityDate() {
        return maturityDate;
    }
    
    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }
    
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public String getCounterparty() {
        return counterparty;
    }
    
    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }
    
    public LocalDate getTradeDate() {
        return tradeDate;
    }
    
    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getPremiumFrequency() {
        return premiumFrequency;
    }
    
    public void setPremiumFrequency(String premiumFrequency) {
        this.premiumFrequency = premiumFrequency;
    }
    
    public String getDayCountConvention() {
        return dayCountConvention;
    }
    
    public void setDayCountConvention(String dayCountConvention) {
        this.dayCountConvention = dayCountConvention;
    }
    
    public ProtectionDirection getBuySellProtection() {
        return buySellProtection;
    }
    
    public void setBuySellProtection(ProtectionDirection buySellProtection) {
        this.buySellProtection = buySellProtection;
    }
    
    public String getRestructuringClause() {
        return restructuringClause;
    }
    
    public void setRestructuringClause(String restructuringClause) {
        this.restructuringClause = restructuringClause;
    }
    
    public String getPaymentCalendar() {
        return paymentCalendar;
    }
    
    public void setPaymentCalendar(String paymentCalendar) {
        this.paymentCalendar = paymentCalendar;
    }
    
    public LocalDate getAccrualStartDate() {
        return accrualStartDate;
    }
    
    public void setAccrualStartDate(LocalDate accrualStartDate) {
        this.accrualStartDate = accrualStartDate;
    }
    
    public BigDecimal getRecoveryRate() {
        return recoveryRate;
    }
    
    public void setRecoveryRate(BigDecimal recoveryRate) {
        this.recoveryRate = recoveryRate;
    }
    
    public TradeStatus getTradeStatus() {
        return tradeStatus;
    }
    
    public void setTradeStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
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
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.updatedAt = lastUpdated;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}