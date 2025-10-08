package com.creditdefaultswap.platform.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response containing net credit exposure aggregated by issuer across all instruments (CDS + Bonds)
 */
public class NetExposureByIssuerResponse {
    
    private String issuer;
    private String sector;
    private BigDecimal bondNotional;        // Total long bond positions
    private BigDecimal cdsProtectionBought;  // Total CDS protection bought
    private BigDecimal cdsProtectionSold;    // Total CDS protection sold
    private BigDecimal netCreditExposure;    // Net exposure (positive = long credit, negative = short credit)
    private BigDecimal hedgeRatio;           // Protection bought / Bond notional
    private String hedgeStatus;              // OVER_HEDGED, UNDER_HEDGED, BALANCED, UNHEDGED
    
    // Risk metrics aggregated
    private BigDecimal netJtd;              // Net jump-to-default exposure
    private BigDecimal netSpreadDv01;       // Net spread sensitivity
    private BigDecimal totalIrDv01;         // IR DV01 from bonds only
    
    // Bond details
    private List<BondDetail> bonds;
    
    // CDS details
    private List<CdsDetail> cdsPositions;
    
    public static class BondDetail {
        private Long bondId;
        private String isin;
        private String seniority;
        private BigDecimal notional;
        private Double yieldToMaturity;
        private Double zSpread;
        private String maturityDate;
        
        // Constructor, getters, setters
        public BondDetail() {}
        
        public BondDetail(Long bondId, String isin, String seniority, BigDecimal notional, 
                         Double yieldToMaturity, Double zSpread, String maturityDate) {
            this.bondId = bondId;
            this.isin = isin;
            this.seniority = seniority;
            this.notional = notional;
            this.yieldToMaturity = yieldToMaturity;
            this.zSpread = zSpread;
            this.maturityDate = maturityDate;
        }
        
        // Getters and setters
        public Long getBondId() { return bondId; }
        public void setBondId(Long bondId) { this.bondId = bondId; }
        public String getIsin() { return isin; }
        public void setIsin(String isin) { this.isin = isin; }
        public String getSeniority() { return seniority; }
        public void setSeniority(String seniority) { this.seniority = seniority; }
        public BigDecimal getNotional() { return notional; }
        public void setNotional(BigDecimal notional) { this.notional = notional; }
        public Double getYieldToMaturity() { return yieldToMaturity; }
        public void setYieldToMaturity(Double yieldToMaturity) { this.yieldToMaturity = yieldToMaturity; }
        public Double getZSpread() { return zSpread; }
        public void setZSpread(Double zSpread) { this.zSpread = zSpread; }
        public String getMaturityDate() { return maturityDate; }
        public void setMaturityDate(String maturityDate) { this.maturityDate = maturityDate; }
    }
    
    public static class CdsDetail {
        private Long tradeId;
        private String buySellProtection;
        private BigDecimal notional;
        private BigDecimal spread;
        private String maturityDate;
        
        // Constructor, getters, setters
        public CdsDetail() {}
        
        public CdsDetail(Long tradeId, String buySellProtection, BigDecimal notional, 
                        BigDecimal spread, String maturityDate) {
            this.tradeId = tradeId;
            this.buySellProtection = buySellProtection;
            this.notional = notional;
            this.spread = spread;
            this.maturityDate = maturityDate;
        }
        
        // Getters and setters
        public Long getTradeId() { return tradeId; }
        public void setTradeId(Long tradeId) { this.tradeId = tradeId; }
        public String getBuySellProtection() { return buySellProtection; }
        public void setBuySellProtection(String buySellProtection) { this.buySellProtection = buySellProtection; }
        public BigDecimal getNotional() { return notional; }
        public void setNotional(BigDecimal notional) { this.notional = notional; }
        public BigDecimal getSpread() { return spread; }
        public void setSpread(BigDecimal spread) { this.spread = spread; }
        public String getMaturityDate() { return maturityDate; }
        public void setMaturityDate(String maturityDate) { this.maturityDate = maturityDate; }
    }
    
    // Constructors
    public NetExposureByIssuerResponse() {}
    
    // Getters and setters
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    
    public BigDecimal getBondNotional() { return bondNotional; }
    public void setBondNotional(BigDecimal bondNotional) { this.bondNotional = bondNotional; }
    
    public BigDecimal getCdsProtectionBought() { return cdsProtectionBought; }
    public void setCdsProtectionBought(BigDecimal cdsProtectionBought) { this.cdsProtectionBought = cdsProtectionBought; }
    
    public BigDecimal getCdsProtectionSold() { return cdsProtectionSold; }
    public void setCdsProtectionSold(BigDecimal cdsProtectionSold) { this.cdsProtectionSold = cdsProtectionSold; }
    
    public BigDecimal getNetCreditExposure() { return netCreditExposure; }
    public void setNetCreditExposure(BigDecimal netCreditExposure) { this.netCreditExposure = netCreditExposure; }
    
    public BigDecimal getHedgeRatio() { return hedgeRatio; }
    public void setHedgeRatio(BigDecimal hedgeRatio) { this.hedgeRatio = hedgeRatio; }
    
    public String getHedgeStatus() { return hedgeStatus; }
    public void setHedgeStatus(String hedgeStatus) { this.hedgeStatus = hedgeStatus; }
    
    public BigDecimal getNetJtd() { return netJtd; }
    public void setNetJtd(BigDecimal netJtd) { this.netJtd = netJtd; }
    
    public BigDecimal getNetSpreadDv01() { return netSpreadDv01; }
    public void setNetSpreadDv01(BigDecimal netSpreadDv01) { this.netSpreadDv01 = netSpreadDv01; }
    
    public BigDecimal getTotalIrDv01() { return totalIrDv01; }
    public void setTotalIrDv01(BigDecimal totalIrDv01) { this.totalIrDv01 = totalIrDv01; }
    
    public List<BondDetail> getBonds() { return bonds; }
    public void setBonds(List<BondDetail> bonds) { this.bonds = bonds; }
    
    public List<CdsDetail> getCdsPositions() { return cdsPositions; }
    public void setCdsPositions(List<CdsDetail> cdsPositions) { this.cdsPositions = cdsPositions; }
}
