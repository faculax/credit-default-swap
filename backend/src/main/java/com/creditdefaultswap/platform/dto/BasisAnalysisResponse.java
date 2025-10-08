package com.creditdefaultswap.platform.dto;

import java.math.BigDecimal;

/**
 * Basis analysis comparing bond spreads vs CDS spreads for same issuer
 */
public class BasisAnalysisResponse {
    
    private String issuer;
    private String sector;
    
    // Bond metrics
    private Double bondZSpread;          // Bond Z-spread in bps
    private Double bondYield;            // Bond YTM
    private String bondMaturity;
    private String bondSeniority;
    
    // CDS metrics
    private BigDecimal cdsSpread;        // CDS spread in bps
    private String cdsMaturity;
    
    // Basis calculation
    private Double basisSpread;          // Bond Z-spread - CDS spread (in bps)
    private String basisDirection;       // POSITIVE (bond wider), NEGATIVE (CDS wider), NEUTRAL
    private String arbitrageOpportunity; // NONE, BUY_BOND_BUY_CDS, SELL_BOND_SELL_CDS
    private Double expectedProfit;       // Expected profit in bps if arb executed
    
    // Risk considerations
    private String maturityMismatch;     // MATCHED, SLIGHT_MISMATCH, LARGE_MISMATCH
    private Integer daysDifference;      // Days difference between maturities
    private String seniorityNote;        // Notes on seniority differences
    
    // Recommendation
    private String recommendation;
    private String rationale;
    
    // Constructors
    public BasisAnalysisResponse() {}
    
    // Getters and setters
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    
    public Double getBondZSpread() { return bondZSpread; }
    public void setBondZSpread(Double bondZSpread) { this.bondZSpread = bondZSpread; }
    
    public Double getBondYield() { return bondYield; }
    public void setBondYield(Double bondYield) { this.bondYield = bondYield; }
    
    public String getBondMaturity() { return bondMaturity; }
    public void setBondMaturity(String bondMaturity) { this.bondMaturity = bondMaturity; }
    
    public String getBondSeniority() { return bondSeniority; }
    public void setBondSeniority(String bondSeniority) { this.bondSeniority = bondSeniority; }
    
    public BigDecimal getCdsSpread() { return cdsSpread; }
    public void setCdsSpread(BigDecimal cdsSpread) { this.cdsSpread = cdsSpread; }
    
    public String getCdsMaturity() { return cdsMaturity; }
    public void setCdsMaturity(String cdsMaturity) { this.cdsMaturity = cdsMaturity; }
    
    public Double getBasisSpread() { return basisSpread; }
    public void setBasisSpread(Double basisSpread) { this.basisSpread = basisSpread; }
    
    public String getBasisDirection() { return basisDirection; }
    public void setBasisDirection(String basisDirection) { this.basisDirection = basisDirection; }
    
    public String getArbitrageOpportunity() { return arbitrageOpportunity; }
    public void setArbitrageOpportunity(String arbitrageOpportunity) { this.arbitrageOpportunity = arbitrageOpportunity; }
    
    public Double getExpectedProfit() { return expectedProfit; }
    public void setExpectedProfit(Double expectedProfit) { this.expectedProfit = expectedProfit; }
    
    public String getMaturityMismatch() { return maturityMismatch; }
    public void setMaturityMismatch(String maturityMismatch) { this.maturityMismatch = maturityMismatch; }
    
    public Integer getDaysDifference() { return daysDifference; }
    public void setDaysDifference(Integer daysDifference) { this.daysDifference = daysDifference; }
    
    public String getSeniorityNote() { return seniorityNote; }
    public void setSeniorityNote(String seniorityNote) { this.seniorityNote = seniorityNote; }
    
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
