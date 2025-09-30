package com.creditdefaultswap.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class SettlementView {
    
    private String type; // "cash" or "physical"
    private Long tradeId;
    private UUID creditEventId;
    
    // Common fields
    private LocalDateTime createdAt;
    
    // Cash settlement fields
    private BigDecimal notional;
    private BigDecimal recoveryRate;
    private BigDecimal payoutAmount;
    private LocalDateTime calculatedAt;
    
    // Physical settlement fields
    private String referenceObligationIsin;
    private LocalDate proposedDeliveryDate;
    private String notes;
    private String status;
    
    // Constructors
    public SettlementView() {}
    
    // Factory methods for cash settlement
    public static SettlementView fromCashSettlement(Long tradeId, UUID creditEventId, 
                                                   BigDecimal notional, BigDecimal recoveryRate, 
                                                   BigDecimal payoutAmount, LocalDateTime calculatedAt) {
        SettlementView view = new SettlementView();
        view.type = "cash";
        view.tradeId = tradeId;
        view.creditEventId = creditEventId;
        view.notional = notional;
        view.recoveryRate = recoveryRate;
        view.payoutAmount = payoutAmount;
        view.calculatedAt = calculatedAt;
        view.createdAt = calculatedAt;
        return view;
    }
    
    // Factory methods for physical settlement
    public static SettlementView fromPhysicalSettlement(Long tradeId, UUID creditEventId,
                                                       String referenceObligationIsin, LocalDate proposedDeliveryDate,
                                                       String notes, String status, LocalDateTime createdAt) {
        SettlementView view = new SettlementView();
        view.type = "physical";
        view.tradeId = tradeId;
        view.creditEventId = creditEventId;
        view.referenceObligationIsin = referenceObligationIsin;
        view.proposedDeliveryDate = proposedDeliveryDate;
        view.notes = notes;
        view.status = status;
        view.createdAt = createdAt;
        return view;
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getTradeId() {
        return tradeId;
    }
    
    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }
    
    public UUID getCreditEventId() {
        return creditEventId;
    }
    
    public void setCreditEventId(UUID creditEventId) {
        this.creditEventId = creditEventId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
    
    public String getReferenceObligationIsin() {
        return referenceObligationIsin;
    }
    
    public void setReferenceObligationIsin(String referenceObligationIsin) {
        this.referenceObligationIsin = referenceObligationIsin;
    }
    
    public LocalDate getProposedDeliveryDate() {
        return proposedDeliveryDate;
    }
    
    public void setProposedDeliveryDate(LocalDate proposedDeliveryDate) {
        this.proposedDeliveryDate = proposedDeliveryDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}