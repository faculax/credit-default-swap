package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Interest rate curve point captured at snapshot time
 */
@Entity
@Table(name = "snapshot_ir_curves",
       uniqueConstraints = @UniqueConstraint(columnNames = {
           "snapshot_id", "currency", "curve_type", "tenor"
       }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotIrCurve {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    @ToString.Exclude
    private MarketDataSnapshot snapshot;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "curve_type", nullable = false, length = 50)
    private String curveType; // LIBOR, SOFR, OIS, GOVERNMENT
    
    @Column(name = "tenor", nullable = false, length = 10)
    private String tenor; // 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, 30Y
    
    @Column(name = "rate", nullable = false, precision = 10, scale = 8)
    private BigDecimal rate; // Decimal format, e.g., 0.0525 for 5.25%
    
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
