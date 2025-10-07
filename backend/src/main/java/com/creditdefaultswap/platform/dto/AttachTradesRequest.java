package com.creditdefaultswap.platform.dto;

import java.util.List;

public class AttachTradesRequest {
    
    private List<ConstituentRequest> trades;
    
    // Constructors
    public AttachTradesRequest() {}
    
    public AttachTradesRequest(List<ConstituentRequest> trades) {
        this.trades = trades;
    }
    
    // Getters and Setters
    public List<ConstituentRequest> getTrades() {
        return trades;
    }
    
    public void setTrades(List<ConstituentRequest> trades) {
        this.trades = trades;
    }
}
