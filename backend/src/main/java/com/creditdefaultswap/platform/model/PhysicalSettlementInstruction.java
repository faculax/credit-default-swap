package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cds_physical_settlement_instructions")
public class PhysicalSettlementInstruction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotNull
    @Column(name = "credit_event_id", nullable = false, unique = true)
    private UUID creditEventId;
    
    @NotNull
    @Column(name = "trade_id", nullable = false)
    private Long tradeId;
    
    @Column(name = "reference_obligation_isin", length = 12)
    private String referenceObligationIsin;
    
    @Column(name = "proposed_delivery_date")
    private LocalDate proposedDeliveryDate;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstructionStatus status = InstructionStatus.DRAFT;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_event_id", insertable = false, updatable = false)
    private CreditEvent creditEvent;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", insertable = false, updatable = false)
    private CDSTrade trade;
    
    // Enum for instruction status
    public enum InstructionStatus {
        DRAFT,
        PENDING,
        CONFIRMED,
        COMPLETED
    }
    
    // Constructors
    public PhysicalSettlementInstruction() {
        this.createdAt = LocalDateTime.now();
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
    
    public InstructionStatus getStatus() {
        return status;
    }
    
    public void setStatus(InstructionStatus status) {
        this.status = status;
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
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}