package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CDS spread quote captured at snapshot time
 */
@Entity
@Table(name = "snapshot_cds_spreads",
       uniqueConstraints = @UniqueConstraint(columnNames = {
           "snapshot_id", "reference_entity_name", "tenor", "currency", "seniority"
       }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotCdsSpread {
    
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
    
    @Column(name = "tenor", nullable = false, length = 10)
    private String tenor; // e.g., "1Y", "5Y", "10Y"
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "seniority", nullable = false, length = 50)
    private String seniority; // SENIOR_UNSECURED, SUBORDINATED
    
    @Column(name = "restructuring_clause", length = 50)
    private String restructuringClause; // CR, MR, XR, MM
    
    @Column(name = "spread", nullable = false, precision = 10, scale = 6)
    private BigDecimal spread; // In basis points
    
    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource;
    
    @Column(name = "quote_time")
    private LocalDateTime quoteTime;
    
    @Column(name = "is_composite")
    @Builder.Default
    private Boolean isComposite = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
