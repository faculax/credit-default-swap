package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import jakarta.persistence.EntityListeners;
import com.creditdefaultswap.platform.lineage.LineageEntityListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing notional adjustments including partial terminations.
 * Supports reduction and termination scenarios with cash unwind calculations.
 */
@Entity
@EntityListeners(LineageEntityListener.class)
@Table(name = "notional_adjustments")
public class NotionalAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDate adjustmentDate;

    @Column(name = "adjustment_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustmentType;

    @Column(name = "original_notional", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalNotional;

    @Column(name = "adjustment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal adjustmentAmount;

    @Column(name = "remaining_notional", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingNotional;

    @Column(name = "unwind_cash_amount", precision = 15, scale = 2)
    private BigDecimal unwindCashAmount;

    @Column(name = "adjustment_reason", columnDefinition = "TEXT")
    private String adjustmentReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public NotionalAdjustment() {}

    public NotionalAdjustment(Long tradeId, LocalDate adjustmentDate, AdjustmentType adjustmentType,
                             BigDecimal originalNotional, BigDecimal adjustmentAmount, 
                             BigDecimal remainingNotional) {
        this.tradeId = tradeId;
        this.adjustmentDate = adjustmentDate;
        this.adjustmentType = adjustmentType;
        this.originalNotional = originalNotional;
        this.adjustmentAmount = adjustmentAmount;
        this.remainingNotional = remainingNotional;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTradeId() { return tradeId; }
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }

    public LocalDate getAdjustmentDate() { return adjustmentDate; }
    public void setAdjustmentDate(LocalDate adjustmentDate) { this.adjustmentDate = adjustmentDate; }

    public AdjustmentType getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(AdjustmentType adjustmentType) { this.adjustmentType = adjustmentType; }

    public BigDecimal getOriginalNotional() { return originalNotional; }
    public void setOriginalNotional(BigDecimal originalNotional) { this.originalNotional = originalNotional; }

    public BigDecimal getAdjustmentAmount() { return adjustmentAmount; }
    public void setAdjustmentAmount(BigDecimal adjustmentAmount) { this.adjustmentAmount = adjustmentAmount; }

    public BigDecimal getRemainingNotional() { return remainingNotional; }
    public void setRemainingNotional(BigDecimal remainingNotional) { this.remainingNotional = remainingNotional; }

    public BigDecimal getUnwindCashAmount() { return unwindCashAmount; }
    public void setUnwindCashAmount(BigDecimal unwindCashAmount) { this.unwindCashAmount = unwindCashAmount; }

    public String getAdjustmentReason() { return adjustmentReason; }
    public void setAdjustmentReason(String adjustmentReason) { this.adjustmentReason = adjustmentReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum AdjustmentType {
        PARTIAL_TERMINATION,
        FULL_TERMINATION,
        REDUCTION
    }
}