package com.creditdefaultswap.platform.model;

/**
 * Coupon payment frequency for fixed-rate bonds
 */
public enum CouponFrequency {
    ANNUAL(1),
    SEMI_ANNUAL(2),
    QUARTERLY(4);
    
    private final int paymentsPerYear;
    
    CouponFrequency(int paymentsPerYear) {
        this.paymentsPerYear = paymentsPerYear;
    }
    
    public int getPaymentsPerYear() {
        return paymentsPerYear;
    }
}
