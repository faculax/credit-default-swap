package com.creditdefaultswap.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type", nullable = false)
    private SettlementMethod settlementType;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obligation_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Bond obligation;
    
    // CCP and Novation related fields
    @Column(name = "ccp_name", length = 50)
    private String ccpName;
    
    @Column(name = "ccp_member_id", length = 50)
    private String ccpMemberId;
    
    @Column(name = "clearing_account", length = 50)
    private String clearingAccount;
    
    @Column(name = "netting_set_id", length = 50)
    private String nettingSetId;
    
    @Column(name = "original_trade_id")
    private Long originalTradeId;
    
    @Column(name = "novation_timestamp")
    private LocalDateTime novationTimestamp;
    
    @Column(name = "novation_reference", length = 100)
    private String novationReference;
    
    @Column(name = "uti", length = 100)
    private String uti; // Unique Transaction Identifier
    
    @Column(name = "usi", length = 100)
    private String usi; // Unique Swap Identifier
    
    @Column(name = "is_cleared", nullable = false)
    private Boolean isCleared = false;
    
    @Column(name = "upfront_amount", precision = 15, scale = 2)
    private BigDecimal upfrontAmount = BigDecimal.ZERO;
    
    @Column(name = "mark_to_market_value", precision = 15, scale = 2)
    private BigDecimal markToMarketValue = BigDecimal.ZERO;
    
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
    
    public SettlementMethod getSettlementType() {
        return settlementType;
    }
    
    public void setSettlementType(SettlementMethod settlementType) {
        this.settlementType = settlementType;
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
    
    public Bond getObligation() {
        return obligation;
    }
    
    public void setObligation(Bond obligation) {
        this.obligation = obligation;
    }
    
    public String getCcpName() {
        return ccpName;
    }
    
    public void setCcpName(String ccpName) {
        this.ccpName = ccpName;
    }
    
    public String getCcpMemberId() {
        return ccpMemberId;
    }
    
    public void setCcpMemberId(String ccpMemberId) {
        this.ccpMemberId = ccpMemberId;
    }
    
    public String getClearingAccount() {
        return clearingAccount;
    }
    
    public void setClearingAccount(String clearingAccount) {
        this.clearingAccount = clearingAccount;
    }
    
    public String getNettingSetId() {
        return nettingSetId;
    }
    
    public void setNettingSetId(String nettingSetId) {
        this.nettingSetId = nettingSetId;
    }
    
    public Long getOriginalTradeId() {
        return originalTradeId;
    }
    
    public void setOriginalTradeId(Long originalTradeId) {
        this.originalTradeId = originalTradeId;
    }
    
    public LocalDateTime getNovationTimestamp() {
        return novationTimestamp;
    }
    
    public void setNovationTimestamp(LocalDateTime novationTimestamp) {
        this.novationTimestamp = novationTimestamp;
    }
    
    public String getNovationReference() {
        return novationReference;
    }
    
    public void setNovationReference(String novationReference) {
        this.novationReference = novationReference;
    }
    
    public String getUti() {
        return uti;
    }
    
    public void setUti(String uti) {
        this.uti = uti;
    }
    
    public String getUsi() {
        return usi;
    }
    
    public void setUsi(String usi) {
        this.usi = usi;
    }
    
    public Boolean getIsCleared() {
        return isCleared;
    }
    
    public void setIsCleared(Boolean isCleared) {
        this.isCleared = isCleared;
    }
    
    public BigDecimal getUpfrontAmount() {
        return upfrontAmount;
    }
    
    public void setUpfrontAmount(BigDecimal upfrontAmount) {
        this.upfrontAmount = upfrontAmount;
    }
    
    public BigDecimal getMarkToMarketValue() {
        return markToMarketValue;
    }
    
    public void setMarkToMarketValue(BigDecimal markToMarketValue) {
        this.markToMarketValue = markToMarketValue;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.updatedAt = lastUpdated;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}