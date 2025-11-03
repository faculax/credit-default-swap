package com.creditdefaultswap.riskengine.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result model for stress scenario impact analysis
 */
public class StressImpactResult {
    
    /**
     * Trade ID being analyzed
     */
    private Long tradeId;
    
    /**
     * Base case NPV (mark-to-market)
     */
    private BigDecimal baseNpv;
    
    /**
     * Base case JTD (jump-to-default)
     */
    private BigDecimal baseJtd;
    
    /**
     * Currency of the trade
     */
    private String currency;
    
    /**
     * Total number of scenarios executed
     */
    private int scenarioCount;
    
    /**
     * Base yield curve (tenor -> rate) for the trade currency
     */
    private Map<String, Double> baseYieldCurve;
    
    /**
     * Shifted yield curves (shift label -> {tenor -> rate})
     * e.g., "+50bp" -> {"1Y": 0.047, "3Y": 0.050, ...}
     */
    private Map<String, Map<String, Double>> shiftedYieldCurves;
    
    /**
     * List of stress scenario results
     */
    private List<ScenarioResult> scenarios;
    
    public StressImpactResult() {
        this.scenarios = new ArrayList<>();
        this.baseYieldCurve = new HashMap<>();
        this.shiftedYieldCurves = new HashMap<>();
    }
    
    /**
     * Individual scenario result
     */
    public static class ScenarioResult {
        private String scenarioName;
        private BigDecimal npv;
        private BigDecimal jtd;
        private BigDecimal deltaNpv; // Change from base case
        private BigDecimal deltaJtd; // Change from base case
        private boolean severe; // Flag for severe stress (large move)
        
        // Getters and setters
        public String getScenarioName() {
            return scenarioName;
        }
        
        public void setScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
        }
        
        public BigDecimal getNpv() {
            return npv;
        }
        
        public void setNpv(BigDecimal npv) {
            this.npv = npv;
        }
        
        public BigDecimal getJtd() {
            return jtd;
        }
        
        public void setJtd(BigDecimal jtd) {
            this.jtd = jtd;
        }
        
        public BigDecimal getDeltaNpv() {
            return deltaNpv;
        }
        
        public void setDeltaNpv(BigDecimal deltaNpv) {
            this.deltaNpv = deltaNpv;
        }
        
        public BigDecimal getDeltaJtd() {
            return deltaJtd;
        }
        
        public void setDeltaJtd(BigDecimal deltaJtd) {
            this.deltaJtd = deltaJtd;
        }
        
        public boolean isSevere() {
            return severe;
        }
        
        public void setSevere(boolean severe) {
            this.severe = severe;
        }
    }
    
    // Getters and setters
    public Long getTradeId() {
        return tradeId;
    }
    
    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }
    
    public BigDecimal getBaseNpv() {
        return baseNpv;
    }
    
    public void setBaseNpv(BigDecimal baseNpv) {
        this.baseNpv = baseNpv;
    }
    
    public BigDecimal getBaseJtd() {
        return baseJtd;
    }
    
    public void setBaseJtd(BigDecimal baseJtd) {
        this.baseJtd = baseJtd;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public int getScenarioCount() {
        return scenarioCount;
    }
    
    public void setScenarioCount(int scenarioCount) {
        this.scenarioCount = scenarioCount;
    }
    
    public Map<String, Double> getBaseYieldCurve() {
        return baseYieldCurve;
    }
    
    public void setBaseYieldCurve(Map<String, Double> baseYieldCurve) {
        this.baseYieldCurve = baseYieldCurve;
    }
    
    public Map<String, Map<String, Double>> getShiftedYieldCurves() {
        return shiftedYieldCurves;
    }
    
    public void setShiftedYieldCurves(Map<String, Map<String, Double>> shiftedYieldCurves) {
        this.shiftedYieldCurves = shiftedYieldCurves;
    }
    
    public List<ScenarioResult> getScenarios() {
        return scenarios;
    }
    
    public void setScenarios(List<ScenarioResult> scenarios) {
        this.scenarios = scenarios;
    }
    
    public void addScenario(ScenarioResult scenario) {
        this.scenarios.add(scenario);
    }
}
