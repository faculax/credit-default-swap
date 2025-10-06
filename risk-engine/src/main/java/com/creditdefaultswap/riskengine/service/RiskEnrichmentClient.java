package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.model.RiskMeasures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Client service to call backend for enriching risk measures with platform-specific data
 * (e.g., accrued premium based on actual payment history)
 */
@Service
public class RiskEnrichmentClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskEnrichmentClient.class);
    
    @Value("${backend.base.url}")
    private String backendBaseUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Enriches a RiskMeasures object by calling the backend to get additional metrics
     * like accrued premium based on actual payment history
     */
    public void enrichRiskMeasures(RiskMeasures riskMeasures, Long tradeId) {
        try {
            String url = backendBaseUrl + "/api/lifecycle/trades/" + tradeId + "/enrichment";
            logger.debug("Calling backend enrichment for trade {}: {}", tradeId, url);
            
            Map<String, Object> enrichmentData = restTemplate.getForObject(url, Map.class);
            
            if (enrichmentData != null) {
                // Set accrued premium from backend calculation
                if (enrichmentData.containsKey("accruedPremium")) {
                    Object accruedPremiumObj = enrichmentData.get("accruedPremium");
                    if (accruedPremiumObj instanceof Number) {
                        BigDecimal accruedPremium = new BigDecimal(accruedPremiumObj.toString());
                        riskMeasures.setAccruedPremium(accruedPremium);
                        logger.debug("Enriched trade {} with accrued premium: {}", tradeId, accruedPremium);
                    }
                }
            }
        } catch (Exception e) {
            // Don't fail the entire request if enrichment fails
            logger.warn("Failed to enrich risk measures for trade {}: {}", tradeId, e.getMessage());
        }
    }
}
