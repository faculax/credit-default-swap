package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FX rate captured at snapshot time
 */
@Entity
@Table(name = "snapshot_fx_rates",
       uniqueConstraints = @UniqueConstraint(columnNames = {
           "snapshot_id", "base_currency", "quote_currency"
       }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotFxRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    @ToString.Exclude
    private MarketDataSnapshot snapshot;
    
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;
    
    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;
    
    @Column(name = "rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal rate; // e.g., 1.0850 for EUR/USD
    
    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource;
    
    @Column(name = "quote_time")
    private LocalDateTime quoteTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
