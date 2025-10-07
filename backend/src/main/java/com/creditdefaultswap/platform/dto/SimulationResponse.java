package com.creditdefaultswap.platform.dto;

import com.creditdefaultswap.platform.model.SimulationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class SimulationResponse {
    
    private String runId;
    private Long portfolioId;
    private String valuationDate;
    private Integer paths;
    private SimulationStatus status;
    private Long seedUsed;
    private List<HorizonMetricsDto> horizons;
    private List<ContributorDto> contributors;
    private SimulationSettingsDto settings;
    private String errorMessage;
    private Long runtimeMs;
    
    // Getters and Setters
    public String getRunId() {
        return runId;
    }
    
    public void setRunId(String runId) {
        this.runId = runId;
    }
    
    public Long getPortfolioId() {
        return portfolioId;
    }
    
    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }
    
    public String getValuationDate() {
        return valuationDate;
    }
    
    public void setValuationDate(String valuationDate) {
        this.valuationDate = valuationDate;
    }
    
    public Integer getPaths() {
        return paths;
    }
    
    public void setPaths(Integer paths) {
        this.paths = paths;
    }
    
    public SimulationStatus getStatus() {
        return status;
    }
    
    public void setStatus(SimulationStatus status) {
        this.status = status;
    }
    
    public Long getSeedUsed() {
        return seedUsed;
    }
    
    public void setSeedUsed(Long seedUsed) {
        this.seedUsed = seedUsed;
    }
    
    public List<HorizonMetricsDto> getHorizons() {
        return horizons;
    }
    
    public void setHorizons(List<HorizonMetricsDto> horizons) {
        this.horizons = horizons;
    }
    
    public List<ContributorDto> getContributors() {
        return contributors;
    }
    
    public void setContributors(List<ContributorDto> contributors) {
        this.contributors = contributors;
    }
    
    public SimulationSettingsDto getSettings() {
        return settings;
    }
    
    public void setSettings(SimulationSettingsDto settings) {
        this.settings = settings;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getRuntimeMs() {
        return runtimeMs;
    }
    
    public void setRuntimeMs(Long runtimeMs) {
        this.runtimeMs = runtimeMs;
    }
    
    public static class HorizonMetricsDto {
        private String tenor;
        
        @JsonProperty("pAnyDefault")
        private Double pAnyDefault;
        
        private Double expectedDefaults;
        private LossMetricsDto loss;
        private DiversificationDto diversification;
        
        // Getters and Setters
        public String getTenor() {
            return tenor;
        }
        
        public void setTenor(String tenor) {
            this.tenor = tenor;
        }
        
        public Double getPAnyDefault() {
            return pAnyDefault;
        }
        
        public void setPAnyDefault(Double pAnyDefault) {
            this.pAnyDefault = pAnyDefault;
        }
        
        public Double getExpectedDefaults() {
            return expectedDefaults;
        }
        
        public void setExpectedDefaults(Double expectedDefaults) {
            this.expectedDefaults = expectedDefaults;
        }
        
        public LossMetricsDto getLoss() {
            return loss;
        }
        
        public void setLoss(LossMetricsDto loss) {
            this.loss = loss;
        }
        
        public DiversificationDto getDiversification() {
            return diversification;
        }
        
        public void setDiversification(DiversificationDto diversification) {
            this.diversification = diversification;
        }
    }
    
    public static class LossMetricsDto {
        private Double mean;
        private Double var95;
        private Double var99;
        private Double es97_5;
        
        // Getters and Setters
        public Double getMean() {
            return mean;
        }
        
        public void setMean(Double mean) {
            this.mean = mean;
        }
        
        public Double getVar95() {
            return var95;
        }
        
        public void setVar95(Double var95) {
            this.var95 = var95;
        }
        
        public Double getVar99() {
            return var99;
        }
        
        public void setVar99(Double var99) {
            this.var99 = var99;
        }
        
        public Double getEs97_5() {
            return es97_5;
        }
        
        public void setEs97_5(Double es97_5) {
            this.es97_5 = es97_5;
        }
    }
    
    public static class DiversificationDto {
        private Double sumStandaloneEl;
        private Double portfolioEl;
        private Double benefitPct;
        
        // Getters and Setters
        public Double getSumStandaloneEl() {
            return sumStandaloneEl;
        }
        
        public void setSumStandaloneEl(Double sumStandaloneEl) {
            this.sumStandaloneEl = sumStandaloneEl;
        }
        
        public Double getPortfolioEl() {
            return portfolioEl;
        }
        
        public void setPortfolioEl(Double portfolioEl) {
            this.portfolioEl = portfolioEl;
        }
        
        public Double getBenefitPct() {
            return benefitPct;
        }
        
        public void setBenefitPct(Double benefitPct) {
            this.benefitPct = benefitPct;
        }
    }
    
    public static class ContributorDto {
        private String entity;
        private Double marginalElPct;
        private Double beta;
        private Double standaloneEl;
        
        // Getters and Setters
        public String getEntity() {
            return entity;
        }
        
        public void setEntity(String entity) {
            this.entity = entity;
        }
        
        public Double getMarginalElPct() {
            return marginalElPct;
        }
        
        public void setMarginalElPct(Double marginalElPct) {
            this.marginalElPct = marginalElPct;
        }
        
        public Double getBeta() {
            return beta;
        }
        
        public void setBeta(Double beta) {
            this.beta = beta;
        }
        
        public Double getStandaloneEl() {
            return standaloneEl;
        }
        
        public void setStandaloneEl(Double standaloneEl) {
            this.standaloneEl = standaloneEl;
        }
    }
    
    public static class SimulationSettingsDto {
        private Boolean stochasticRecovery;
        
        // Getters and Setters
        public Boolean getStochasticRecovery() {
            return stochasticRecovery;
        }
        
        public void setStochasticRecovery(Boolean stochasticRecovery) {
            this.stochasticRecovery = stochasticRecovery;
        }
    }
}
