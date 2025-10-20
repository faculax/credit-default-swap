package com.creditdefaultswap.riskengine.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Captures the exact market data and curve configurations used for an ORE calculation.
 * This allows audit trail and transparency into what inputs were used for pricing.
 */
public class MarketDataSnapshot {
    private LocalDate valuationDate;
    private String baseCurrency;
    
    // Discount curves used
    private List<DiscountCurveData> discountCurves;
    
    // Credit/default curves used
    private List<DefaultCurveData> defaultCurves;
    
    // FX rates used (if any)
    private Map<String, Double> fxRates;
    
    // Raw market data file content (for full transparency)
    private String marketDataFileContent;
    private String todaysMarketFileContent;
    private String curveConfigFileContent;
    
    public static class DiscountCurveData {
        private String currency;
        private String curveId;
        private List<QuoteData> quotes;
        
        public DiscountCurveData() {}
        
        public DiscountCurveData(String currency, String curveId, List<QuoteData> quotes) {
            this.currency = currency;
            this.curveId = curveId;
            this.quotes = quotes;
        }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getCurveId() { return curveId; }
        public void setCurveId(String curveId) { this.curveId = curveId; }
        
        public List<QuoteData> getQuotes() { return quotes; }
        public void setQuotes(List<QuoteData> quotes) { this.quotes = quotes; }
    }
    
    public static class DefaultCurveData {
        private String referenceEntity;
        private String currency;
        private String curveId;
        private Double recoveryRate;
        private List<QuoteData> spreadQuotes;
        
        public DefaultCurveData() {}
        
        public DefaultCurveData(String referenceEntity, String currency, String curveId, Double recoveryRate, List<QuoteData> spreadQuotes) {
            this.referenceEntity = referenceEntity;
            this.currency = currency;
            this.curveId = curveId;
            this.recoveryRate = recoveryRate;
            this.spreadQuotes = spreadQuotes;
        }
        
        public String getReferenceEntity() { return referenceEntity; }
        public void setReferenceEntity(String referenceEntity) { this.referenceEntity = referenceEntity; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getCurveId() { return curveId; }
        public void setCurveId(String curveId) { this.curveId = curveId; }
        
        public Double getRecoveryRate() { return recoveryRate; }
        public void setRecoveryRate(Double recoveryRate) { this.recoveryRate = recoveryRate; }
        
        public List<QuoteData> getSpreadQuotes() { return spreadQuotes; }
        public void setSpreadQuotes(List<QuoteData> spreadQuotes) { this.spreadQuotes = spreadQuotes; }
    }
    
    public static class QuoteData {
        private String tenor;
        private String quoteName;
        private Double value;
        private String type; // e.g., "DEPOSIT", "SWAP", "CDS_SPREAD", "RECOVERY_RATE"
        
        public QuoteData() {}
        
        public QuoteData(String tenor, String quoteName, Double value, String type) {
            this.tenor = tenor;
            this.quoteName = quoteName;
            this.value = value;
            this.type = type;
        }
        
        public String getTenor() { return tenor; }
        public void setTenor(String tenor) { this.tenor = tenor; }
        
        public String getQuoteName() { return quoteName; }
        public void setQuoteName(String quoteName) { this.quoteName = quoteName; }
        
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
    
    // Default constructor
    public MarketDataSnapshot() {}
    
    // Getters and setters
    public LocalDate getValuationDate() { return valuationDate; }
    public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }
    
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    
    public List<DiscountCurveData> getDiscountCurves() { return discountCurves; }
    public void setDiscountCurves(List<DiscountCurveData> discountCurves) { this.discountCurves = discountCurves; }
    
    public List<DefaultCurveData> getDefaultCurves() { return defaultCurves; }
    public void setDefaultCurves(List<DefaultCurveData> defaultCurves) { this.defaultCurves = defaultCurves; }
    
    public Map<String, Double> getFxRates() { return fxRates; }
    public void setFxRates(Map<String, Double> fxRates) { this.fxRates = fxRates; }
    
    public String getMarketDataFileContent() { return marketDataFileContent; }
    public void setMarketDataFileContent(String marketDataFileContent) { this.marketDataFileContent = marketDataFileContent; }
    
    public String getTodaysMarketFileContent() { return todaysMarketFileContent; }
    public void setTodaysMarketFileContent(String todaysMarketFileContent) { this.todaysMarketFileContent = todaysMarketFileContent; }
    
    public String getCurveConfigFileContent() { return curveConfigFileContent; }
    public void setCurveConfigFileContent(String curveConfigFileContent) { this.curveConfigFileContent = curveConfigFileContent; }
}
