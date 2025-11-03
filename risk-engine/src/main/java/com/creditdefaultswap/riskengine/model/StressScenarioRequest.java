package com.creditdefaultswap.riskengine.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request model for stress scenario analysis
 */
public class StressScenarioRequest {
    
    /**
     * The trade ID to analyze
     */
    private Long tradeId;
    
    /**
     * List of recovery rates to stress test (in percentage, e.g., 40 for 40%)
     */
    private List<BigDecimal> recoveryRates;
    
    /**
     * List of spread shifts to stress test (in basis points, e.g., 50 for +50bp)
     */
    private List<BigDecimal> spreadShifts;
    
    /**
     * List of yield curve shifts to stress test (in basis points, e.g., 25 for +25bp)
     */
    private List<BigDecimal> yieldCurveShifts;
    
    /**
     * Whether to run combined scenarios (e.g., recovery 30% + spread +100bp together)
     * If false, runs independent scenarios only
     */
    private boolean combined;
    
    /**
     * Optional valuation date (defaults to today)
     */
    private LocalDate valuationDate;
    
    // Getters and setters
    public Long getTradeId() {
        return tradeId;
    }
    
    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }
    
    public List<BigDecimal> getRecoveryRates() {
        return recoveryRates;
    }
    
    public void setRecoveryRates(List<BigDecimal> recoveryRates) {
        this.recoveryRates = recoveryRates;
    }
    
    public List<BigDecimal> getSpreadShifts() {
        return spreadShifts;
    }
    
    public void setSpreadShifts(List<BigDecimal> spreadShifts) {
        this.spreadShifts = spreadShifts;
    }
    
    public List<BigDecimal> getYieldCurveShifts() {
        return yieldCurveShifts;
    }
    
    public void setYieldCurveShifts(List<BigDecimal> yieldCurveShifts) {
        this.yieldCurveShifts = yieldCurveShifts;
    }
    
    public boolean isCombined() {
        return combined;
    }
    
    public void setCombined(boolean combined) {
        this.combined = combined;
    }
    
    public LocalDate getValuationDate() {
        return valuationDate;
    }
    
    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }
}
