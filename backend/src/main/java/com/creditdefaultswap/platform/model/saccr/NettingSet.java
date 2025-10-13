package com.creditdefaultswap.platform.model.saccr;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Netting Set entity for SA-CCR calculations
 * Represents legal netting agreements for trade aggregation
 */
@Entity
@Table(name = "netting_sets")
public class NettingSet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "netting_set_id", unique = true, nullable = false, length = 100)
    private String nettingSetId;
    
    @Column(name = "counterparty_id", nullable = false, length = 100)
    private String counterpartyId;
    
    @Column(name = "legal_agreement_type", nullable = false, length = 50)
    private String legalAgreementType;
    
    @Column(name = "agreement_date")
    private LocalDate agreementDate;
    
    @Column(name = "governing_law", length = 10)
    private String governingLaw;
    
    @Column(name = "netting_eligible", nullable = false)
    private Boolean nettingEligible = true;
    
    @Column(name = "collateral_agreement", nullable = false)
    private Boolean collateralAgreement = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public NettingSet() {}
    
    public NettingSet(String nettingSetId, String counterpartyId, String legalAgreementType) {
        this.nettingSetId = nettingSetId;
        this.counterpartyId = counterpartyId;
        this.legalAgreementType = legalAgreementType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNettingSetId() { return nettingSetId; }
    public void setNettingSetId(String nettingSetId) { this.nettingSetId = nettingSetId; }
    
    public String getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }
    
    public String getLegalAgreementType() { return legalAgreementType; }
    public void setLegalAgreementType(String legalAgreementType) { this.legalAgreementType = legalAgreementType; }
    
    public LocalDate getAgreementDate() { return agreementDate; }
    public void setAgreementDate(LocalDate agreementDate) { this.agreementDate = agreementDate; }
    
    public String getGoverningLaw() { return governingLaw; }
    public void setGoverningLaw(String governingLaw) { this.governingLaw = governingLaw; }
    
    public Boolean getNettingEligible() { return nettingEligible; }
    public void setNettingEligible(Boolean nettingEligible) { this.nettingEligible = nettingEligible; }
    
    public Boolean getCollateralAgreement() { return collateralAgreement; }
    public void setCollateralAgreement(Boolean collateralAgreement) { this.collateralAgreement = collateralAgreement; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "NettingSet{" +
                "id=" + id +
                ", nettingSetId='" + nettingSetId + '\'' +
                ", counterpartyId='" + counterpartyId + '\'' +
                ", legalAgreementType='" + legalAgreementType + '\'' +
                ", nettingEligible=" + nettingEligible +
                ", collateralAgreement=" + collateralAgreement +
                '}';
    }
}