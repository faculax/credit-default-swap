package com.creditdefaultswap.platform.model.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Accrued interest calculation for CDS trades
 */
@Entity
@Table(name = "trade_accrued_interest",
       uniqueConstraints = @UniqueConstraint(columnNames = {"calculation_date", "trade_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeAccruedInterest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    @ToString.Exclude
    private CDSTrade trade;
    
    // Accrued interest calculation
    @Column(name = "accrued_interest", nullable = false, precision = 20, scale = 4)
    private BigDecimal accruedInterest;
    
    @Column(name = "accrual_days", nullable = false)
    private Integer accrualDays;
    
    // Calculation inputs
    @Column(name = "notional_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal notionalAmount;
    
    @Column(name = "spread", nullable = false, precision = 10, scale = 6)
    private BigDecimal spread;
    
    @Column(name = "day_count_convention", nullable = false, length = 20)
    private String dayCountConvention;
    
    // Date range for accrual period
    @Column(name = "accrual_start_date", nullable = false)
    private LocalDate accrualStartDate;
    
    @Column(name = "accrual_end_date", nullable = false)
    private LocalDate accrualEndDate;
    
    // Days calculation details
    @Column(name = "numerator_days", nullable = false)
    private Integer numeratorDays;
    
    @Column(name = "denominator_days", nullable = false)
    private Integer denominatorDays;
    
    @Column(name = "day_count_fraction", nullable = false, precision = 10, scale = 8)
    private BigDecimal dayCountFraction;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_status", nullable = false)
    @Builder.Default
    private CalculationStatus calculationStatus = CalculationStatus.SUCCESS;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "job_id", length = 100)
    private String jobId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    public enum CalculationStatus {
        SUCCESS,
        FAILED
    }
}
