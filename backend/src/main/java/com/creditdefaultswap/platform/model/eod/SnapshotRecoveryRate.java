package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Recovery rate assumption captured at snapshot time
 */
@Entity
@Table(name = "snapshot_recovery_rates",
       uniqueConstraints = @UniqueConstraint(columnNames = {
           "snapshot_id", "reference_entity_name", "seniority"
       }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotRecoveryRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    @ToString.Exclude
    private MarketDataSnapshot snapshot;
    
    @Column(name = "reference_entity_id")
    private Long referenceEntityId;
    
    @Column(name = "reference_entity_name", nullable = false)
    private String referenceEntityName;
    
    @Column(name = "seniority", nullable = false, length = 50)
    private String seniority;
    
    @Column(name = "recovery_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal recoveryRate; // Decimal format, e.g., 0.4000 for 40%
    
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
