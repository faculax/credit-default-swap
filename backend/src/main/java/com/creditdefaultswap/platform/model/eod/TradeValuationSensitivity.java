package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Risk sensitivities (Greeks) for trade valuations
 */
@Entity
@Table(name = "trade_valuation_sensitivities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeValuationSensitivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Changed from @OneToOne to simple ID to avoid Hibernate cascade issues
    @Column(name = "trade_valuation_id", nullable = false)
    private Long tradeValuationId;
    
    // Credit spread sensitivity (CS01)
    @Column(name = "cs01", precision = 20, scale = 4)
    private BigDecimal cs01; // P&L impact of 1bp parallel spread move
    
    // Interest rate sensitivity (IR01)
    @Column(name = "ir01", precision = 20, scale = 4)
    private BigDecimal ir01; // P&L impact of 1bp parallel IR move
    
    @Column(name = "ir01_1y", precision = 20, scale = 4)
    private BigDecimal ir01_1y;
    
    @Column(name = "ir01_5y", precision = 20, scale = 4)
    private BigDecimal ir01_5y;
    
    @Column(name = "ir01_10y", precision = 20, scale = 4)
    private BigDecimal ir01_10y;
    
    // Jump-to-default (JTD)
    @Column(name = "jtd", precision = 20, scale = 4)
    private BigDecimal jtd; // Loss if reference entity defaults
    
    // Recovery rate sensitivity (REC01)
    @Column(name = "rec01", precision = 20, scale = 4)
    private BigDecimal rec01; // P&L impact of 1% recovery change
    
    // Time decay (Theta)
    @Column(name = "theta_1d", precision = 20, scale = 4)
    private BigDecimal theta1d; // P&L from 1 day time decay
    
    // Duration and DV01
    @Column(name = "duration_years", precision = 10, scale = 4)
    private BigDecimal durationYears;
    
    @Column(name = "dv01", precision = 20, scale = 4)
    private BigDecimal dv01;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
