package com.creditdefaultswap.platform.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Basket credit derivative definition
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
@Entity
@Table(name = "basket_definitions")
public class BasketDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true, length = 80)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private BasketType type;
    
    @Column(name = "nth")
    private Integer nth;
    
    @Column(name = "attachment_point", precision = 9, scale = 6)
    private BigDecimal attachmentPoint;
    
    @Column(name = "detachment_point", precision = 9, scale = 6)
    private BigDecimal detachmentPoint;
    
    @Column(name = "premium_frequency", nullable = false, length = 20)
    private String premiumFrequency;
    
    @Column(name = "day_count", nullable = false, length = 20)
    private String dayCount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";
    
    @Column(name = "notional", nullable = false, precision = 18, scale = 2)
    private BigDecimal notional;
    
    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "basket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<BasketConstituent> constituents = new ArrayList<>();
    
    // Constructors
    public BasketDefinition() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BasketType getType() {
        return type;
    }
    
    public void setType(BasketType type) {
        this.type = type;
    }
    
    public Integer getNth() {
        return nth;
    }
    
    public void setNth(Integer nth) {
        this.nth = nth;
    }
    
    public BigDecimal getAttachmentPoint() {
        return attachmentPoint;
    }
    
    public void setAttachmentPoint(BigDecimal attachmentPoint) {
        this.attachmentPoint = attachmentPoint;
    }
    
    public BigDecimal getDetachmentPoint() {
        return detachmentPoint;
    }
    
    public void setDetachmentPoint(BigDecimal detachmentPoint) {
        this.detachmentPoint = detachmentPoint;
    }
    
    public String getPremiumFrequency() {
        return premiumFrequency;
    }
    
    public void setPremiumFrequency(String premiumFrequency) {
        this.premiumFrequency = premiumFrequency;
    }
    
    public String getDayCount() {
        return dayCount;
    }
    
    public void setDayCount(String dayCount) {
        this.dayCount = dayCount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public BigDecimal getNotional() {
        return notional;
    }
    
    public void setNotional(BigDecimal notional) {
        this.notional = notional;
    }
    
    public LocalDate getMaturityDate() {
        return maturityDate;
    }
    
    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<BasketConstituent> getConstituents() {
        return constituents;
    }
    
    public void setConstituents(List<BasketConstituent> constituents) {
        this.constituents = constituents;
    }
    
    // Helper methods
    public void addConstituent(BasketConstituent constituent) {
        constituents.add(constituent);
        constituent.setBasket(this);
    }
    
    public void removeConstituent(BasketConstituent constituent) {
        constituents.remove(constituent);
        constituent.setBasket(null);
    }
    
    public int getConstituentCount() {
        return constituents.size();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
