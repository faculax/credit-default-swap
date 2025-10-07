package com.creditdefaultswap.platform.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class PortfolioPricingResponse {
    
    private Long portfolioId;
    private String valuationDate;
    private AggregateMetrics aggregate;
    private List<TradeBreakdown> byTrade;
    private ConcentrationMetrics concentration;
    private CompletenessMetrics completeness;
    
    // Nested classes
    public static class AggregateMetrics {
        private BigDecimal pv;
        private BigDecimal accrued;
        private BigDecimal premiumLegPv;
        private BigDecimal protectionLegPv;
        private BigDecimal fairSpreadBpsWeighted;
        private BigDecimal cs01;
        private BigDecimal rec01;
        private BigDecimal jtd;
        
        // Getters and Setters
        public BigDecimal getPv() { return pv; }
        public void setPv(BigDecimal pv) { this.pv = pv; }
        
        public BigDecimal getAccrued() { return accrued; }
        public void setAccrued(BigDecimal accrued) { this.accrued = accrued; }
        
        public BigDecimal getPremiumLegPv() { return premiumLegPv; }
        public void setPremiumLegPv(BigDecimal premiumLegPv) { this.premiumLegPv = premiumLegPv; }
        
        public BigDecimal getProtectionLegPv() { return protectionLegPv; }
        public void setProtectionLegPv(BigDecimal protectionLegPv) { this.protectionLegPv = protectionLegPv; }
        
        public BigDecimal getFairSpreadBpsWeighted() { return fairSpreadBpsWeighted; }
        public void setFairSpreadBpsWeighted(BigDecimal fairSpreadBpsWeighted) { 
            this.fairSpreadBpsWeighted = fairSpreadBpsWeighted; 
        }
        
        public BigDecimal getCs01() { return cs01; }
        public void setCs01(BigDecimal cs01) { this.cs01 = cs01; }
        
        public BigDecimal getRec01() { return rec01; }
        public void setRec01(BigDecimal rec01) { this.rec01 = rec01; }
        
        public BigDecimal getJtd() { return jtd; }
        public void setJtd(BigDecimal jtd) { this.jtd = jtd; }
    }
    
    public static class TradeBreakdown {
        private Long tradeId;
        private String referenceEntity;
        private BigDecimal notional;
        private BigDecimal pv;
        private BigDecimal cs01;
        private BigDecimal rec01;
        private BigDecimal weight;
        private String sector;
        
        // Getters and Setters
        public Long getTradeId() { return tradeId; }
        public void setTradeId(Long tradeId) { this.tradeId = tradeId; }
        
        public String getReferenceEntity() { return referenceEntity; }
        public void setReferenceEntity(String referenceEntity) { this.referenceEntity = referenceEntity; }
        
        public BigDecimal getNotional() { return notional; }
        public void setNotional(BigDecimal notional) { this.notional = notional; }
        
        public BigDecimal getPv() { return pv; }
        public void setPv(BigDecimal pv) { this.pv = pv; }
        
        public BigDecimal getCs01() { return cs01; }
        public void setCs01(BigDecimal cs01) { this.cs01 = cs01; }
        
        public BigDecimal getRec01() { return rec01; }
        public void setRec01(BigDecimal rec01) { this.rec01 = rec01; }
        
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
    }
    
    public static class ConcentrationMetrics {
        private BigDecimal top5PctCs01;
        private List<SectorBreakdown> sectorBreakdown;
        
        public BigDecimal getTop5PctCs01() { return top5PctCs01; }
        public void setTop5PctCs01(BigDecimal top5PctCs01) { this.top5PctCs01 = top5PctCs01; }
        
        public List<SectorBreakdown> getSectorBreakdown() { return sectorBreakdown; }
        public void setSectorBreakdown(List<SectorBreakdown> sectorBreakdown) { 
            this.sectorBreakdown = sectorBreakdown; 
        }
    }
    
    public static class SectorBreakdown {
        private String sector;
        private BigDecimal cs01Pct;
        
        public SectorBreakdown() {}
        
        public SectorBreakdown(String sector, BigDecimal cs01Pct) {
            this.sector = sector;
            this.cs01Pct = cs01Pct;
        }
        
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        
        public BigDecimal getCs01Pct() { return cs01Pct; }
        public void setCs01Pct(BigDecimal cs01Pct) { this.cs01Pct = cs01Pct; }
    }
    
    public static class CompletenessMetrics {
        private int constituents;
        private int priced;
        
        public int getConstituents() { return constituents; }
        public void setConstituents(int constituents) { this.constituents = constituents; }
        
        public int getPriced() { return priced; }
        public void setPriced(int priced) { this.priced = priced; }
    }
    
    // Getters and Setters for main class
    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
    
    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }
    
    public AggregateMetrics getAggregate() { return aggregate; }
    public void setAggregate(AggregateMetrics aggregate) { this.aggregate = aggregate; }
    
    public List<TradeBreakdown> getByTrade() { return byTrade; }
    public void setByTrade(List<TradeBreakdown> byTrade) { this.byTrade = byTrade; }
    
    public ConcentrationMetrics getConcentration() { return concentration; }
    public void setConcentration(ConcentrationMetrics concentration) { this.concentration = concentration; }
    
    public CompletenessMetrics getCompleteness() { return completeness; }
    public void setCompleteness(CompletenessMetrics completeness) { this.completeness = completeness; }
}
