package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a coupon period for a CDS trade as part of the IMM schedule.
 * Supports ACT/360 day count convention and modified following business day convention.
 */
@Entity
@Table(name = "coupon_periods", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"trade_id", "period_start_date"})
})
public class CouponPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "accrual_days", nullable = false)
    private Integer accrualDays;

    @Column(name = "notional_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal notionalAmount;

    @Column(name = "day_count_convention", nullable = false)
    @Enumerated(EnumType.STRING)
    private DayCountConvention dayCountConvention = DayCountConvention.ACT_360;

    @Column(name = "business_day_convention", nullable = false)
    @Enumerated(EnumType.STRING)
    private BusinessDayConvention businessDayConvention = BusinessDayConvention.MODIFIED_FOLLOWING;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid", nullable = false)
    private Boolean paid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Transient
    private BigDecimal couponAmount;  // Calculated field: notional × spread × (days/360)

    // Constructors
    public CouponPeriod() {}

    public CouponPeriod(Long tradeId, LocalDate periodStartDate, LocalDate periodEndDate, 
                       LocalDate paymentDate, Integer accrualDays, BigDecimal notionalAmount) {
        this.tradeId = tradeId;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.paymentDate = paymentDate;
        this.accrualDays = accrualDays;
        this.notionalAmount = notionalAmount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTradeId() { return tradeId; }
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }

    public LocalDate getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(LocalDate periodStartDate) { this.periodStartDate = periodStartDate; }

    public LocalDate getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(LocalDate periodEndDate) { this.periodEndDate = periodEndDate; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public Integer getAccrualDays() { return accrualDays; }
    public void setAccrualDays(Integer accrualDays) { this.accrualDays = accrualDays; }

    public BigDecimal getNotionalAmount() { return notionalAmount; }
    public void setNotionalAmount(BigDecimal notionalAmount) { this.notionalAmount = notionalAmount; }

    public DayCountConvention getDayCountConvention() { return dayCountConvention; }
    public void setDayCountConvention(DayCountConvention dayCountConvention) { this.dayCountConvention = dayCountConvention; }

    public BusinessDayConvention getBusinessDayConvention() { return businessDayConvention; }
    public void setBusinessDayConvention(BusinessDayConvention businessDayConvention) { this.businessDayConvention = businessDayConvention; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public BigDecimal getCouponAmount() { return couponAmount; }
    public void setCouponAmount(BigDecimal couponAmount) { this.couponAmount = couponAmount; }

    /**
     * Calculate coupon amount: notional × spread × (accrualDays / 360)
     * 
     * @param spread The CDS spread (in basis points, e.g., 500 for 500bps or 5%)
     * @return The coupon amount
     */
    public BigDecimal calculateCouponAmount(BigDecimal spread) {
        if (notionalAmount == null || spread == null || accrualDays == null) {
            return BigDecimal.ZERO;
        }
        
        // Convert spread from basis points to decimal (e.g., 500 bps -> 0.05)
        BigDecimal spreadDecimal = spread.compareTo(BigDecimal.ONE) > 0 
            ? spread.divide(new BigDecimal("10000"), 10, java.math.RoundingMode.HALF_UP)
            : spread;
        
        // For ACT/360: coupon = notional × spread × (days / 360)
        BigDecimal dayCountFactor = new BigDecimal(accrualDays).divide(new BigDecimal("360"), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal amount = notionalAmount.multiply(spreadDecimal).multiply(dayCountFactor);
        
        this.couponAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        return this.couponAmount;
    }

    public enum DayCountConvention {
        ACT_360, ACT_365, THIRTY_360
    }

    public enum BusinessDayConvention {
        MODIFIED_FOLLOWING, FOLLOWING, PRECEDING
    }
}