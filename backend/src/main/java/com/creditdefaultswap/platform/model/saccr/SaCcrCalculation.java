package com.creditdefaultswap.platform.model.saccr;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SA-CCR Calculation entity representing a complete exposure calculation
 * Implements Basel III formula: EAD = α × (RC + PFE)
 */
@Entity
@Table(name = "sa_ccr_calculations")
public class SaCcrCalculation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "calculation_id", unique = true, nullable = false, length = 100)
    private String calculationId;
    
    @Column(name = "netting_set_id", nullable = false, length = 100)
    private String nettingSetId;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    @Column(name = "jurisdiction", nullable = false, length = 10)
    private String jurisdiction;
    
    @Column(name = "alpha_factor", nullable = false, precision = 5, scale = 3)
    private BigDecimal alphaFactor = new BigDecimal("1.4");
    
    // Replacement Cost (RC) components
    @Column(name = "gross_mtm", nullable = false, precision = 20, scale = 8)
    private BigDecimal grossMtm;
    
    @Column(name = "vm_received", nullable = false, precision = 20, scale = 8)
    private BigDecimal vmReceived = BigDecimal.ZERO;
    
    @Column(name = "vm_posted", nullable = false, precision = 20, scale = 8)
    private BigDecimal vmPosted = BigDecimal.ZERO;
    
    @Column(name = "im_received", nullable = false, precision = 20, scale = 8)
    private BigDecimal imReceived = BigDecimal.ZERO;
    
    @Column(name = "im_posted", nullable = false, precision = 20, scale = 8)
    private BigDecimal imPosted = BigDecimal.ZERO;
    
    @Column(name = "replacement_cost", nullable = false, precision = 20, scale = 8)
    private BigDecimal replacementCost;
    
    // Potential Future Exposure (PFE) components
    @Column(name = "effective_notional", nullable = false, precision = 20, scale = 8)
    private BigDecimal effectiveNotional;
    
    @Column(name = "supervisory_addon", nullable = false, precision = 20, scale = 8)
    private BigDecimal supervisoryAddon;
    
    @Column(name = "multiplier", nullable = false, precision = 10, scale = 6)
    private BigDecimal multiplier;
    
    @Column(name = "potential_future_exposure", nullable = false, precision = 20, scale = 8)
    private BigDecimal potentialFutureExposure;
    
    // Final result
    @Column(name = "exposure_at_default", nullable = false, precision = 20, scale = 8)
    private BigDecimal exposureAtDefault;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_status", nullable = false, length = 20)
    private CalculationStatus calculationStatus = CalculationStatus.PENDING;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "netting_set_id", referencedColumnName = "netting_set_id", insertable = false, updatable = false)
    private NettingSet nettingSet;
    
    // Enums
    public enum CalculationStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
    
    // Constructors
    public SaCcrCalculation() {}
    
    public SaCcrCalculation(String calculationId, String nettingSetId, LocalDate calculationDate, String jurisdiction) {
        this.calculationId = calculationId;
        this.nettingSetId = nettingSetId;
        this.calculationDate = calculationDate;
        this.jurisdiction = jurisdiction;
    }
    
    // Business methods
    
    /**
     * Calculate Replacement Cost: RC = max(V - C, 0)
     * Where V = gross MTM value, C = net collateral (VM + IM)
     */
    public BigDecimal calculateReplacementCost() {
        BigDecimal netCollateral = vmReceived.subtract(vmPosted).add(imReceived.subtract(imPosted));
        BigDecimal rc = grossMtm.subtract(netCollateral);
        this.replacementCost = rc.max(BigDecimal.ZERO);
        return this.replacementCost;
    }
    
    /**
     * Calculate Potential Future Exposure: PFE = Multiplier × Add-On
     */
    public BigDecimal calculatePotentialFutureExposure() {
        this.potentialFutureExposure = multiplier.multiply(supervisoryAddon);
        return this.potentialFutureExposure;
    }
    
    /**
     * Calculate final Exposure at Default: EAD = α × (RC + PFE)
     */
    public BigDecimal calculateExposureAtDefault() {
        this.exposureAtDefault = alphaFactor.multiply(replacementCost.add(potentialFutureExposure));
        return this.exposureAtDefault;
    }
    
    /**
     * Calculate multiplier: M = min(1, (Floor + (1-Floor) × exp(trades)))
     * Simplified version - actual implementation would consider trade count and floor
     */
    public BigDecimal calculateMultiplier(int tradeCount) {
        if (tradeCount <= 5) {
            this.multiplier = BigDecimal.ONE;
        } else {
            // Simplified: reduce multiplier for large portfolios
            BigDecimal reduction = new BigDecimal("0.95");
            this.multiplier = reduction;
        }
        return this.multiplier;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCalculationId() { return calculationId; }
    public void setCalculationId(String calculationId) { this.calculationId = calculationId; }
    
    public String getNettingSetId() { return nettingSetId; }
    public void setNettingSetId(String nettingSetId) { this.nettingSetId = nettingSetId; }
    
    public LocalDate getCalculationDate() { return calculationDate; }
    public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }
    
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    
    public BigDecimal getAlphaFactor() { return alphaFactor; }
    public void setAlphaFactor(BigDecimal alphaFactor) { this.alphaFactor = alphaFactor; }
    
    public BigDecimal getGrossMtm() { return grossMtm; }
    public void setGrossMtm(BigDecimal grossMtm) { this.grossMtm = grossMtm; }
    
    public BigDecimal getVmReceived() { return vmReceived; }
    public void setVmReceived(BigDecimal vmReceived) { this.vmReceived = vmReceived; }
    
    public BigDecimal getVmPosted() { return vmPosted; }
    public void setVmPosted(BigDecimal vmPosted) { this.vmPosted = vmPosted; }
    
    public BigDecimal getImReceived() { return imReceived; }
    public void setImReceived(BigDecimal imReceived) { this.imReceived = imReceived; }
    
    public BigDecimal getImPosted() { return imPosted; }
    public void setImPosted(BigDecimal imPosted) { this.imPosted = imPosted; }
    
    public BigDecimal getReplacementCost() { return replacementCost; }
    public void setReplacementCost(BigDecimal replacementCost) { this.replacementCost = replacementCost; }
    
    public BigDecimal getEffectiveNotional() { return effectiveNotional; }
    public void setEffectiveNotional(BigDecimal effectiveNotional) { this.effectiveNotional = effectiveNotional; }
    
    public BigDecimal getSupervisoryAddon() { return supervisoryAddon; }
    public void setSupervisoryAddon(BigDecimal supervisoryAddon) { this.supervisoryAddon = supervisoryAddon; }
    
    public void setSupervisoryDelta(BigDecimal supervisoryDelta) {
        // Store supervisory delta calculation in addon field for simplicity
        this.supervisoryAddon = supervisoryDelta;
    }
    
    public void setMaturityFactor(BigDecimal maturityFactor) {
        // Apply maturity factor to multiplier for simplicity
        if (this.multiplier != null) {
            this.multiplier = this.multiplier.multiply(maturityFactor);
        } else {
            this.multiplier = maturityFactor;
        }
    }
    
    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
    
    public BigDecimal getPotentialFutureExposure() { return potentialFutureExposure; }
    public void setPotentialFutureExposure(BigDecimal potentialFutureExposure) { this.potentialFutureExposure = potentialFutureExposure; }
    
    public BigDecimal getExposureAtDefault() { return exposureAtDefault; }
    public void setExposureAtDefault(BigDecimal exposureAtDefault) { this.exposureAtDefault = exposureAtDefault; }
    
    public CalculationStatus getCalculationStatus() { return calculationStatus; }
    public void setCalculationStatus(CalculationStatus calculationStatus) { this.calculationStatus = calculationStatus; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public NettingSet getNettingSet() { return nettingSet; }
    public void setNettingSet(NettingSet nettingSet) { this.nettingSet = nettingSet; }
    
    @Override
    public String toString() {
        return "SaCcrCalculation{" +
                "id=" + id +
                ", calculationId='" + calculationId + '\'' +
                ", nettingSetId='" + nettingSetId + '\'' +
                ", calculationDate=" + calculationDate +
                ", jurisdiction='" + jurisdiction + '\'' +
                ", exposureAtDefault=" + exposureAtDefault +
                ", calculationStatus=" + calculationStatus +
                '}';
    }
}