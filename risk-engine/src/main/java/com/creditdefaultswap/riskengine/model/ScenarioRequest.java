package com.creditdefaultswap.riskengine.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ScenarioRequest {
    private String scenarioId;
    private List<Long> tradeIds;
    private LocalDate valuationDate;
    private Map<String, Double> scenarios; // e.g. {"USD_1Y": 0.0001, "USD_5Y": 0.0002}
    
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
