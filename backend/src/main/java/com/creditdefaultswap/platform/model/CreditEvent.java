package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import jakarta.persistence.EntityListeners;
import com.creditdefaultswap.platform.lineage.LineageEntityListener;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(LineageEntityListener.class)
@Table(name = "cds_credit_events", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"trade_id", "event_type", "event_date"}))
public class CreditEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotNull
    @Column(name = "trade_id", nullable = false)
    private Long tradeId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private CreditEventType eventType;
    
    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
    
    @NotNull
    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_method", nullable = false, length = 20)
    private SettlementMethod settlementMethod;
    
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationship to trade
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", insertable = false, updatable = false)
    @JsonIgnore
    private CDSTrade trade;
    
    // Constructors
    public CreditEvent() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Long getTradeId() {
        return tradeId;
    }
    
    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }
    
    public CreditEventType getEventType() {
        return eventType;
    }
    
    public void setEventType(CreditEventType eventType) {
        this.eventType = eventType;
    }
    
    public LocalDate getEventDate() {
        return eventDate;
    }
    
    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }
    
    public LocalDate getNoticeDate() {
        return noticeDate;
    }
    
    public void setNoticeDate(LocalDate noticeDate) {
        this.noticeDate = noticeDate;
    }
    
    public SettlementMethod getSettlementMethod() {
        return settlementMethod;
    }
    
    public void setSettlementMethod(SettlementMethod settlementMethod) {
        this.settlementMethod = settlementMethod;
    }
    
    public String getComments() {
        return comments;
    }
    
    public void setComments(String comments) {
        this.comments = comments;
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