package com.creditdefaultswap.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.persistence.EntityListeners;
import com.creditdefaultswap.platform.lineage.LineageEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Constituent reference entity in a basket
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
@Entity
@EntityListeners(LineageEntityListener.class)
@Table(name = "basket_constituents", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"basket_id", "issuer"}))
public class BasketConstituent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "basket_id", nullable = false)
    @JsonBackReference
    private BasketDefinition basket;
    
    @Column(name = "issuer", nullable = false, length = 40)
    private String issuer;
    
    @Column(name = "weight", precision = 18, scale = 10)
    private BigDecimal weight;
    
    @Column(name = "recovery_override", precision = 5, scale = 4)
    private BigDecimal recoveryOverride;
    
    @Column(name = "seniority", length = 20)
    private String seniority;
    
    @Column(name = "sector", length = 30)
    private String sector;
    
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public BasketConstituent() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BasketDefinition getBasket() {
        return basket;
    }
    
    public void setBasket(BasketDefinition basket) {
        this.basket = basket;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public BigDecimal getRecoveryOverride() {
        return recoveryOverride;
    }
    
    public void setRecoveryOverride(BigDecimal recoveryOverride) {
        this.recoveryOverride = recoveryOverride;
    }
    
    public String getSeniority() {
        return seniority;
    }
    
    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }
    
    public String getSector() {
        return sector;
    }
    
    public void setSector(String sector) {
        this.sector = sector;
    }
    
    public Integer getSequenceOrder() {
        return sequenceOrder;
    }
    
    public void setSequenceOrder(Integer sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
