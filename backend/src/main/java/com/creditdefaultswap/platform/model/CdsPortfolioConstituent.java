package com.creditdefaultswap.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.persistence.EntityListeners;
import com.creditdefaultswap.platform.lineage.LineageEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners(LineageEntityListener.class)
@Table(name = "cds_portfolio_constituents")
public class CdsPortfolioConstituent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonBackReference
    private CdsPortfolio portfolio;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trade_id", nullable = false)
    private CDSTrade trade;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "weight_type", nullable = false, length = 20)
    private WeightType weightType;
    
    @Column(name = "weight_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal weightValue;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;
    
    // Constructors
    public CdsPortfolioConstituent() {
        this.addedAt = LocalDateTime.now();
        this.active = true;
    }
    
    public CdsPortfolioConstituent(CdsPortfolio portfolio, CDSTrade trade, WeightType weightType, BigDecimal weightValue) {
        this();
        this.portfolio = portfolio;
        this.trade = trade;
        this.weightType = weightType;
        this.weightValue = weightValue;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public CdsPortfolio getPortfolio() {
        return portfolio;
    }
    
    public void setPortfolio(CdsPortfolio portfolio) {
        this.portfolio = portfolio;
    }
    
    public CDSTrade getTrade() {
        return trade;
    }
    
    public void setTrade(CDSTrade trade) {
        this.trade = trade;
    }
    
    public WeightType getWeightType() {
        return weightType;
    }
    
    public void setWeightType(WeightType weightType) {
        this.weightType = weightType;
    }
    
    public BigDecimal getWeightValue() {
        return weightValue;
    }
    
    public void setWeightValue(BigDecimal weightValue) {
        this.weightValue = weightValue;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public LocalDateTime getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
