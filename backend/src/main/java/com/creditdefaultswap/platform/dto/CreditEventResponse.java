package com.creditdefaultswap.platform.dto;

import com.creditdefaultswap.platform.model.CreditEvent;

import java.util.List;

/**
 * Response object for credit event creation that includes 
 * information about all affected trades (for propagated events)
 */
public class CreditEventResponse {
    
    private CreditEvent creditEvent;
    private List<Long> affectedTradeIds;
    
    public CreditEventResponse() {
    }
    
    public CreditEventResponse(CreditEvent creditEvent, List<Long> affectedTradeIds) {
        this.creditEvent = creditEvent;
        this.affectedTradeIds = affectedTradeIds;
    }
    
    public CreditEvent getCreditEvent() {
        return creditEvent;
    }
    
    public void setCreditEvent(CreditEvent creditEvent) {
        this.creditEvent = creditEvent;
    }
    
    public List<Long> getAffectedTradeIds() {
        return affectedTradeIds;
    }
    
    public void setAffectedTradeIds(List<Long> affectedTradeIds) {
        this.affectedTradeIds = affectedTradeIds;
    }
}
