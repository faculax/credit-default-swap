package com.creditdefaultswap.platform.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cds_portfolios")
public class CdsPortfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 60)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<CdsPortfolioConstituent> constituents = new ArrayList<>();
    
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<BondPortfolioConstituent> bondConstituents = new ArrayList<>();
    
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<BasketPortfolioConstituent> basketConstituents = new ArrayList<>();
    
    // Constructors
    public CdsPortfolio() {
        this.createdAt = LocalDateTime.now();
    }
    
    public CdsPortfolio(String name, String description) {
        this();
        this.name = name;
        this.description = description;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public List<CdsPortfolioConstituent> getConstituents() {
        return constituents;
    }
    
    public void setConstituents(List<CdsPortfolioConstituent> constituents) {
        this.constituents = constituents;
    }
    
    public List<BondPortfolioConstituent> getBondConstituents() {
        return bondConstituents;
    }
    
    public void setBondConstituents(List<BondPortfolioConstituent> bondConstituents) {
        this.bondConstituents = bondConstituents;
    }
    
    public List<BasketPortfolioConstituent> getBasketConstituents() {
        return basketConstituents;
    }
    
    public void setBasketConstituents(List<BasketPortfolioConstituent> basketConstituents) {
        this.basketConstituents = basketConstituents;
    }
    
    // Helper methods
    public void addConstituent(CdsPortfolioConstituent constituent) {
        constituents.add(constituent);
        constituent.setPortfolio(this);
    }
    
    public void removeConstituent(CdsPortfolioConstituent constituent) {
        constituents.remove(constituent);
        constituent.setPortfolio(null);
    }
    
    public void addBondConstituent(BondPortfolioConstituent constituent) {
        bondConstituents.add(constituent);
        constituent.setPortfolio(this);
    }
    
    public void removeBondConstituent(BondPortfolioConstituent constituent) {
        bondConstituents.remove(constituent);
        constituent.setPortfolio(null);
    }
    
    public void addBasketConstituent(BasketPortfolioConstituent constituent) {
        basketConstituents.add(constituent);
        constituent.setPortfolio(this);
    }
    
    public void removeBasketConstituent(BasketPortfolioConstituent constituent) {
        basketConstituents.remove(constituent);
        constituent.setPortfolio(null);
    }
    
    public int getTotalConstituentCount() {
        return constituents.size() + bondConstituents.size() + basketConstituents.size();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
