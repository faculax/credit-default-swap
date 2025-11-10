package com.creditdefaultswap.platform.annotation;

/**
 * Enumeration of operation types for lineage tracking.
 * Each type corresponds to a specific tracking method in LineageService.
 */
public enum LineageOperationType {
    /**
     * CDS Trade operations
     */
    TRADE,
    
    /**
     * Bond operations
     */
    BOND,
    
    /**
     * Portfolio operations (aggregation, pricing)
     */
    PORTFOLIO,
    
    /**
     * Basket/Index operations
     */
    BASKET,
    
    /**
     * Margin calculations (SIMM, SA-CCR)
     */
    MARGIN,
    
    /**
     * Lifecycle events (coupon, maturity, amendments)
     */
    LIFECYCLE,
    
    /**
     * Trade novation operations
     */
    NOVATION,
    
    /**
     * Pricing/valuation calculations
     */
    PRICING,
    
    /**
     * Credit event processing
     */
    CREDIT_EVENT,
    
    /**
     * Generic operation (custom tracking)
     */
    GENERIC
}
