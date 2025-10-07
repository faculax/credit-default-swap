package com.creditdefaultswap.platform.dto;

import java.util.List;
import java.util.Map;

public class SimulationRequest {
    
    private String valuationDate;
    private List<String> horizons;
    private Integer paths;
    private FactorModelConfig factorModel;
    private StochasticRecoveryConfig stochasticRecovery;
    private Long seed;
    private Boolean includePerPath;
    
    // Getters and Setters
    public String getValuationDate() {
        return valuationDate;
    }
    
    public void setValuationDate(String valuationDate) {
        this.valuationDate = valuationDate;
    }
    
    public List<String> getHorizons() {
        return horizons;
    }
    
    public void setHorizons(List<String> horizons) {
        this.horizons = horizons;
    }
    
    public Integer getPaths() {
        return paths;
    }
    
    public void setPaths(Integer paths) {
        this.paths = paths;
    }
    
    public FactorModelConfig getFactorModel() {
        return factorModel;
    }
    
    public void setFactorModel(FactorModelConfig factorModel) {
        this.factorModel = factorModel;
    }
    
    public StochasticRecoveryConfig getStochasticRecovery() {
        return stochasticRecovery;
    }
    
    public void setStochasticRecovery(StochasticRecoveryConfig stochasticRecovery) {
        this.stochasticRecovery = stochasticRecovery;
    }
    
    public Long getSeed() {
        return seed;
    }
    
    public void setSeed(Long seed) {
        this.seed = seed;
    }
    
    public Boolean getIncludePerPath() {
        return includePerPath;
    }
    
    public void setIncludePerPath(Boolean includePerPath) {
        this.includePerPath = includePerPath;
    }
    
    public static class FactorModelConfig {
        private String type;
        private Double systemicLoadingDefault;
        private Map<String, Double> sectorOverrides;
        private Map<String, Double> idOverrides;
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Double getSystemicLoadingDefault() {
            return systemicLoadingDefault;
        }
        
        public void setSystemicLoadingDefault(Double systemicLoadingDefault) {
            this.systemicLoadingDefault = systemicLoadingDefault;
        }
        
        public Map<String, Double> getSectorOverrides() {
            return sectorOverrides;
        }
        
        public void setSectorOverrides(Map<String, Double> sectorOverrides) {
            this.sectorOverrides = sectorOverrides;
        }
        
        public Map<String, Double> getIdOverrides() {
            return idOverrides;
        }
        
        public void setIdOverrides(Map<String, Double> idOverrides) {
            this.idOverrides = idOverrides;
        }
    }
    
    public static class StochasticRecoveryConfig {
        private Boolean enabled;
        
        // Getters and Setters
        public Boolean getEnabled() {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
