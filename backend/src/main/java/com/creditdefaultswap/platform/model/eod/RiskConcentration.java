package com.creditdefaultswap.platform.model.eod;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Risk concentration analysis - tracks top entities/sectors/counterparties by risk
 */
@Entity
@Table(name = "risk_concentration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskConcentration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    @Column(name = "concentration_type", nullable = false, length = 50)
    private String concentrationType; // TOP_10_NAMES, TOP_5_SECTORS, TOP_10_COUNTERPARTIES
    
    @Column(name = "reference_entity_id")
    private Long referenceEntityId;
    
    @Column(name = "reference_entity_name", length = 255)
    private String referenceEntityName;
    
    @Column(name = "sector", length = 100)
    private String sector;
    
    @Column(name = "counterparty_id")
    private Long counterpartyId;
    
    @Column(name = "cs01", precision = 20, scale = 4)
    private BigDecimal cs01;
    
    @Column(name = "jtd", precision = 20, scale = 4)
    private BigDecimal jtd;
    
    @Column(name = "gross_notional", precision = 20, scale = 4)
    private BigDecimal grossNotional;
    
    @Column(name = "net_notional", precision = 20, scale = 4)
    private BigDecimal netNotional;
    
    @Column(name = "percentage_of_total", precision = 5, scale = 2)
    private BigDecimal percentageOfTotal;
    
    @Column(name = "percentage_of_total_cs01", precision = 5, scale = 2)
    private BigDecimal percentageOfTotalCs01;
    
    @Column(name = "percentage_of_total_jtd", precision = 5, scale = 2)
    private BigDecimal percentageOfTotalJtd;
    
    @Column(name = "ranking")
    private Integer ranking;
    
    @Column(name = "trade_count")
    private Integer tradeCount;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (currency == null) {
            currency = "USD";
        }
    }
}
