package com.creditdefaultswap.platform.dto;

import java.time.LocalDateTime;

/**
 * Request DTO for paying coupons.
 * Supports both "pay now" (using current timestamp) and "pay on time" (using scheduled payment date).
 */
public class PayCouponRequest {
    private Boolean payOnTime; // If true, use the scheduled payment date; if false/null, use current timestamp
    private LocalDateTime customPaymentTimestamp; // Optional: for specific backdated payments

    public PayCouponRequest() {}

    public PayCouponRequest(Boolean payOnTime) {
        this.payOnTime = payOnTime;
    }

    public Boolean getPayOnTime() {
        return payOnTime;
    }

    public void setPayOnTime(Boolean payOnTime) {
        this.payOnTime = payOnTime;
    }

    public LocalDateTime getCustomPaymentTimestamp() {
        return customPaymentTimestamp;
    }

    public void setCustomPaymentTimestamp(LocalDateTime customPaymentTimestamp) {
        this.customPaymentTimestamp = customPaymentTimestamp;
    }
}
