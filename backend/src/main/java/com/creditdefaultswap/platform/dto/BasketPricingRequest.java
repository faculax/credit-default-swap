package com.creditdefaultswap.platform.dto;

import java.time.LocalDate;

/**
 * Request DTO for basket pricing
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public class BasketPricingRequest {
    
    private LocalDate valuationDate;
    private Integer paths = 50000;
    private Long seed;
    private Boolean includeSensitivities = true;
    private Boolean includeEtlTimeline = false;
    
    // Constructors
    public BasketPricingRequest() {}
    
    public BasketPricingRequest(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }
    
    // Getters and Setters
    public LocalDate getValuationDate() {
        return valuationDate;
    }
    
    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }
    
    public Integer getPaths() {
        return paths;
    }
    
    public void setPaths(Integer paths) {
        this.paths = paths;
    }
    
    public Long getSeed() {
        return seed;
    }
    
    public void setSeed(Long seed) {
        this.seed = seed;
    }
    
    public Boolean getIncludeSensitivities() {
        return includeSensitivities;
    }
    
    public void setIncludeSensitivities(Boolean includeSensitivities) {
        this.includeSensitivities = includeSensitivities;
    }
    
    public Boolean getIncludeEtlTimeline() {
        return includeEtlTimeline;
    }
    
    public void setIncludeEtlTimeline(Boolean includeEtlTimeline) {
        this.includeEtlTimeline = includeEtlTimeline;
    }
}
