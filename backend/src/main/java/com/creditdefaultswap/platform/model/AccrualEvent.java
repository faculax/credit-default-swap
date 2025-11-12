package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import jakarta.persistence.EntityListeners;
import com.creditdefaultswap.platform.lineage.LineageEntityListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing daily accrual events for a CDS trade.
 * Tracks accrual amounts using ACT/360 day count convention.
 */
@Entity
@EntityListeners(LineageEntityListener.class)
@Table(name = "accrual_events", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"trade_id", "accrual_date", "trade_version"})
})
public class AccrualEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "coupon_period_id", nullable = false)
    private Long couponPeriodId;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "accrual_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal accrualAmount;

    @Column(name = "cumulative_accrual", nullable = false, precision = 15, scale = 2)
    private BigDecimal cumulativeAccrual;

    @Column(name = "day_count_fraction", nullable = false, precision = 10, scale = 8)
    private BigDecimal dayCountFraction;

    @Column(name = "notional_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal notionalAmount;

    @Column(name = "trade_version", nullable = false)
    private Integer tradeVersion = 1;

    @Column(name = "posted_at")
    private LocalDateTime postedAt = LocalDateTime.now();

    // Constructors
    public AccrualEvent() {}

    public AccrualEvent(Long tradeId, Long couponPeriodId, LocalDate accrualDate, 
                       BigDecimal accrualAmount, BigDecimal cumulativeAccrual, 
                       BigDecimal dayCountFraction, BigDecimal notionalAmount) {
        this.tradeId = tradeId;
        this.couponPeriodId = couponPeriodId;
        this.accrualDate = accrualDate;
        this.accrualAmount = accrualAmount;
        this.cumulativeAccrual = cumulativeAccrual;
        this.dayCountFraction = dayCountFraction;
        this.notionalAmount = notionalAmount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTradeId() { return tradeId; }
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }

    public Long getCouponPeriodId() { return couponPeriodId; }
    public void setCouponPeriodId(Long couponPeriodId) { this.couponPeriodId = couponPeriodId; }

    public LocalDate getAccrualDate() { return accrualDate; }
    public void setAccrualDate(LocalDate accrualDate) { this.accrualDate = accrualDate; }

    public BigDecimal getAccrualAmount() { return accrualAmount; }
    public void setAccrualAmount(BigDecimal accrualAmount) { this.accrualAmount = accrualAmount; }

    public BigDecimal getCumulativeAccrual() { return cumulativeAccrual; }
    public void setCumulativeAccrual(BigDecimal cumulativeAccrual) { this.cumulativeAccrual = cumulativeAccrual; }

    public BigDecimal getDayCountFraction() { return dayCountFraction; }
    public void setDayCountFraction(BigDecimal dayCountFraction) { this.dayCountFraction = dayCountFraction; }

    public BigDecimal getNotionalAmount() { return notionalAmount; }
    public void setNotionalAmount(BigDecimal notionalAmount) { this.notionalAmount = notionalAmount; }

    public Integer getTradeVersion() { return tradeVersion; }
    public void setTradeVersion(Integer tradeVersion) { this.tradeVersion = tradeVersion; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}