package com.creditdefaultswap.riskengine.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ScenarioRequest {
    // Legacy field for backwards compatibility
    private List<BigDecimal> parallelBpsShifts; // e.g. [10, -10]
    
    // New fields for ORE integration
    private String scenarioId;
    private List<Long> tradeIds;
    private LocalDate valuationDate;
    private Map<String, Double> scenarios; // e.g. {"USD_1Y": 0.0001, "USD_5Y": 0.0002}
    
    // Legacy getter/setter
    public List<BigDecimal> getParallelBpsShifts() { 
        return parallelBpsShifts; 
    }
    
    public void setParallelBpsShifts(List<BigDecimal> parallelBpsShifts) { 
        this.parallelBpsShifts = parallelBpsShifts; 
    }
    
    // New getters/setters
    public String getScenarioId() { 
        return scenarioId; 
    }
    
    public void setScenarioId(String scenarioId) { 
        this.scenarioId = scenarioId; 
    }
    
    public List<Long> getTradeIds() { 
        return tradeIds; 
    }
    
    public void setTradeIds(List<Long> tradeIds) { 
        this.tradeIds = tradeIds; 
    }
    
    public LocalDate getValuationDate() { 
        return valuationDate; 
    }
    
    public void setValuationDate(LocalDate valuationDate) { 
        this.valuationDate = valuationDate; 
    }
    
    public Map<String, Double> getScenarios() { 
        return scenarios; 
    }
    
    public void setScenarios(Map<String, Double> scenarios) { 
        this.scenarios = scenarios; 
    }
}
