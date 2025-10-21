package com.creditdefaultswap.platform.service.saccr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * SA-CCR Jurisdiction Service - Simplified
 * Manages jurisdiction-specific regulatory implementations and parameters
 */
@Service
@Slf4j
public class SaCcrJurisdictionService {
    
    /**
     * Check if a jurisdiction is supported
     */
    public boolean isJurisdictionSupported(String jurisdiction) {
        return Arrays.asList("US", "EU", "UK", "CA", "JP", "AU", "SG", "HK").contains(jurisdiction.toUpperCase());
    }
    
    /**
     * Get alpha factor for jurisdiction
     */
    public BigDecimal getAlphaFactor(String jurisdiction, LocalDate asOfDate) {
        // Default alpha factor per Basel III
        return new BigDecimal("1.4");
    }
    
    /**
     * Get supported jurisdictions
     */
    public List<JurisdictionConfig> getSupportedJurisdictions() {
        List<JurisdictionConfig> jurisdictions = new ArrayList<>();
        jurisdictions.add(new JurisdictionConfig("US", "United States", new BigDecimal("1.4"), true, true, "USD", Arrays.asList("CFTC", "OCC")));
        jurisdictions.add(new JurisdictionConfig("EU", "European Union", new BigDecimal("1.4"), true, true, "EUR", Arrays.asList("EBA", "ECB")));
        return jurisdictions;
    }
    
    /**
     * Get jurisdiction configuration
     */
    public JurisdictionConfig getJurisdictionConfig(String jurisdiction) {
        switch (jurisdiction.toUpperCase()) {
            case "US":
                return new JurisdictionConfig("US", "United States", new BigDecimal("1.4"), true, true, "USD", Arrays.asList("CFTC", "OCC"));
            case "EU":
                return new JurisdictionConfig("EU", "European Union", new BigDecimal("1.4"), true, true, "EUR", Arrays.asList("EBA", "ECB"));
            default:
                return new JurisdictionConfig("US", "United States (Default)", new BigDecimal("1.4"), true, true, "USD", Arrays.asList("CFTC", "OCC"));
        }
    }
    
    /**
     * Get jurisdiction comparison
     */
    public Map<String, Object> getJurisdictionComparison(List<String> jurisdictions, LocalDate asOfDate) {
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("jurisdictions", jurisdictions);
        comparison.put("asOfDate", asOfDate);
        comparison.put("alphaFactors", Map.of("US", "1.4", "EU", "1.4"));
        return comparison;
    }
    
    /**
     * Jurisdiction Configuration Class
     */
    public static class JurisdictionConfig {
        private final String code;
        private final String name;
        private final BigDecimal alphaFactor;
        private final boolean allowsNetting;
        private final boolean recognizesCollateral;
        private final String baseCurrency;
        private final List<String> regulatoryBodies;
        
        public JurisdictionConfig(String code, String name, BigDecimal alphaFactor, 
                                boolean allowsNetting, boolean recognizesCollateral, 
                                String baseCurrency, List<String> regulatoryBodies) {
            this.code = code;
            this.name = name;
            this.alphaFactor = alphaFactor;
            this.allowsNetting = allowsNetting;
            this.recognizesCollateral = recognizesCollateral;
            this.baseCurrency = baseCurrency;
            this.regulatoryBodies = regulatoryBodies;
        }
        
        // Getters
        public String getCode() { return code; }
        public String getName() { return name; }
        public BigDecimal getAlphaFactor() { return alphaFactor; }
        public boolean getAllowsNetting() { return allowsNetting; }
        public boolean getRecognizesCollateral() { return recognizesCollateral; }
        public String getBaseCurrency() { return baseCurrency; }
        public List<String> getRegulatoryBodies() { return regulatoryBodies; }
    }
}