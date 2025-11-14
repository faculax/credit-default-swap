package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily P&L Result with full attribution
 * 
 * Calculates P&L as: Total P&L = V(T) - V(T-1)
 * Attribution breaks down P&L into:
 * - Market P&L: from spread, rate, FX, recovery moves
 * - Theta P&L: time decay / carry
 * - Accrued P&L: change in accrued interest
 * - Credit Event P&L: from defaults, credit events
 * - Trade P&L: from new trades or terminations
 */
@Entity
@Table(name = "daily_pnl_results",
    uniqueConstraints = @UniqueConstraint(columnNames = {"pnl_date", "trade_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPnlResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pnl_date", nullable = false)
    private LocalDate pnlDate;
    
    @Column(name = "trade_id", nullable = false)
    private Long tradeId;
    
    // Removed @ManyToOne relationship to avoid Hibernate cascade issues
    // Use tradeId directly instead
    
    // Current day valuation (T)
    @Column(name = "current_total_value", precision = 20, scale = 4, nullable = false)
    private BigDecimal currentTotalValue;
    
    @Column(name = "current_npv", precision = 20, scale = 4, nullable = false)
    private BigDecimal currentNpv;
    
    @Column(name = "current_accrued", precision = 20, scale = 4, nullable = false)
    private BigDecimal currentAccrued;
    
    // Previous day valuation (T-1)
    @Column(name = "previous_total_value", precision = 20, scale = 4)
    private BigDecimal previousTotalValue;
    
    @Column(name = "previous_npv", precision = 20, scale = 4)
    private BigDecimal previousNpv;
    
    @Column(name = "previous_accrued", precision = 20, scale = 4)
    private BigDecimal previousAccrued;
    
    // Total P&L
    @Column(name = "total_pnl", precision = 20, scale = 4, nullable = false)
    private BigDecimal totalPnl;
    
    @Column(name = "pnl_percentage", precision = 10, scale = 6)
    private BigDecimal pnlPercentage;
    
    // P&L Attribution Components
    @Column(name = "market_pnl", precision = 20, scale = 4)
    private BigDecimal marketPnl;
    
    @Column(name = "theta_pnl", precision = 20, scale = 4)
    private BigDecimal thetaPnl;
    
    @Column(name = "accrued_pnl", precision = 20, scale = 4)
    private BigDecimal accruedPnl;
    
    @Column(name = "credit_event_pnl", precision = 20, scale = 4)
    private BigDecimal creditEventPnl;
    
    @Column(name = "trade_pnl", precision = 20, scale = 4)
    private BigDecimal tradePnl;
    
    @Column(name = "unexplained_pnl", precision = 20, scale = 4)
    private BigDecimal unexplainedPnl;
    
    // Market moves that drove P&L
    @Column(name = "spread_move_bps", precision = 10, scale = 4)
    private BigDecimal spreadMoveBps;
    
    @Column(name = "rate_move_bps", precision = 10, scale = 4)
    private BigDecimal rateMoveBps;
    
    @Column(name = "fx_move_pct", precision = 10, scale = 6)
    private BigDecimal fxMovePct;
    
    @Column(name = "recovery_move_pct", precision = 10, scale = 6)
    private BigDecimal recoveryMovePct;
    
    // Risk attribution
    @Column(name = "cs01_pnl", precision = 20, scale = 4)
    private BigDecimal cs01Pnl;
    
    @Column(name = "ir01_pnl", precision = 20, scale = 4)
    private BigDecimal ir01Pnl;
    
    // Trade details (snapshot)
    @Column(name = "notional_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal notionalAmount;
    
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;
    
    @Column(name = "reference_entity", length = 100, nullable = false)
    private String referenceEntity;
    
    @Column(name = "buy_sell_protection", length = 10, nullable = false)
    private String buySellProtection;
    
    // P&L Flags
    @Column(name = "large_pnl_flag")
    private Boolean largePnlFlag;
    
    @Column(name = "unexplained_pnl_flag")
    private Boolean unexplainedPnlFlag;
    
    @Column(name = "credit_event_flag")
    private Boolean creditEventFlag;
    
    @Column(name = "new_trade_flag")
    private Boolean newTradeFlag;
    
    @Column(name = "terminated_trade_flag")
    private Boolean terminatedTradeFlag;
    
    // Calculation Metadata
    @Column(name = "job_id", length = 100)
    private String jobId;
    
    @Column(name = "calculation_timestamp")
    private LocalDateTime calculationTimestamp;
    
    @Column(name = "calculation_method", length = 50)
    private String calculationMethod;
    
    // Audit
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (calculationTimestamp == null) {
            calculationTimestamp = LocalDateTime.now();
        }
        if (calculationMethod == null) {
            calculationMethod = "STANDARD";
        }
        if (largePnlFlag == null) {
            largePnlFlag = false;
        }
        if (unexplainedPnlFlag == null) {
            unexplainedPnlFlag = false;
        }
        if (creditEventFlag == null) {
            creditEventFlag = false;
        }
        if (newTradeFlag == null) {
            newTradeFlag = false;
        }
        if (terminatedTradeFlag == null) {
            terminatedTradeFlag = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
