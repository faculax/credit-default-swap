package com.creditdefaultswap.platform.dto;

import com.creditdefaultswap.platform.model.CreditEventType;
import com.creditdefaultswap.platform.model.SettlementMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class CreateCreditEventRequest {
    
    @NotNull(message = "Event type is required")
    private CreditEventType eventType;
    
    @NotNull(message = "Event date is required")
    @PastOrPresent(message = "Event date cannot be in the future")
    private LocalDate eventDate;
    
    @NotNull(message = "Notice date is required")
    private LocalDate noticeDate;
    
    @NotNull(message = "Settlement method is required")
    private SettlementMethod settlementMethod;
    
    private String comments;
    
    // Constructors
    public CreateCreditEventRequest() {}
    
    public CreateCreditEventRequest(CreditEventType eventType, LocalDate eventDate, 
                                   LocalDate noticeDate, SettlementMethod settlementMethod, String comments) {
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.noticeDate = noticeDate;
        this.settlementMethod = settlementMethod;
        this.comments = comments;
    }
    
    // Getters and Setters
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
    
    // Custom validation method
    public boolean isValid() {
        return noticeDate != null && eventDate != null && !noticeDate.isBefore(eventDate);
    }
}