package com.creditdefaultswap.platform.dto;

import com.creditdefaultswap.platform.model.NotionalAdjustment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for notional adjustments.
 */
public class NotionalAdjustmentRequest {
    private LocalDate adjustmentDate;
    private NotionalAdjustment.AdjustmentType adjustmentType;
    private BigDecimal adjustmentAmount;
    private String adjustmentReason;

    public NotionalAdjustmentRequest() {}

    public NotionalAdjustmentRequest(LocalDate adjustmentDate, 
                                   NotionalAdjustment.AdjustmentType adjustmentType,
                                   BigDecimal adjustmentAmount) {
        this.adjustmentDate = adjustmentDate;
        this.adjustmentType = adjustmentType;
        this.adjustmentAmount = adjustmentAmount;
    }

    public LocalDate getAdjustmentDate() {
        return adjustmentDate;
    }

    public void setAdjustmentDate(LocalDate adjustmentDate) {
        this.adjustmentDate = adjustmentDate;
    }

    public NotionalAdjustment.AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(NotionalAdjustment.AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public void setAdjustmentAmount(BigDecimal adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public void setAdjustmentReason(String adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }
}