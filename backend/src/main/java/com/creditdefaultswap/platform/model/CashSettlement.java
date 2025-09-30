package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cds_cash_settlements")
public class CashSettlement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotNull
    @Column(name = "credit_event_id", nullable = false, unique = true)
    private UUID creditEventId;
    
    @NotNull
    @Column(name = "trade_id", nullable = false)
    private Long tradeId;
    
    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "notional", nullable = false, precision = 15, scale = 2)
    private BigDecimal notional;
    
    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Column(name = "recovery_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal recoveryRate;
    
    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "payout_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal payoutAmount;
    
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
    
    // Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_event_id", insertable = false, updatable = false)
    private CreditEvent creditEvent;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", insertable = false, updatable = false)
    private CDSTrade trade;
    
    // Constructors
    public CashSettlement() {
        this.calculatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getCreditEventId() {
        return creditEventId;
    }
    
    public void setCreditEventId(UUID creditEventId) {
        this.creditEventId = creditEventId;
    }
    
    public Long getTradeId() {
        return tradeId;
    }
    
    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }
    
    public BigDecimal getNotional() {
        return notional;
    }
    
    public void setNotional(BigDecimal notional) {
        this.notional = notional;
    }
    
    public BigDecimal getRecoveryRate() {
        return recoveryRate;
    }
    
    public void setRecoveryRate(BigDecimal recoveryRate) {
        this.recoveryRate = recoveryRate;
    }
    
    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }
    
    public void setPayoutAmount(BigDecimal payoutAmount) {
        this.payoutAmount = payoutAmount;
    }
    
    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
    
    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
    
    public CreditEvent getCreditEvent() {
        return creditEvent;
    }
    
    public void setCreditEvent(CreditEvent creditEvent) {
        this.creditEvent = creditEvent;
    }
    
    public CDSTrade getTrade() {
        return trade;
    }
    
    public void setTrade(CDSTrade trade) {
        this.trade = trade;
    }
}