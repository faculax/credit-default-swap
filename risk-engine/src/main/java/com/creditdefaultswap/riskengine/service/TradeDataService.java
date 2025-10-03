package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.ore.OrePortfolioGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TradeDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(TradeDataService.class);
    
    private final RestTemplate restTemplate;
    private final String backendBaseUrl;
    
    public TradeDataService(RestTemplate restTemplate, 
                           @Value("${backend.base.url:http://localhost:8080}") String backendBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendBaseUrl = backendBaseUrl;
    }
    
    /**
     * Fetches CDS trade data from the backend service
     * Throws exception if trade data cannot be retrieved
     */
    public OrePortfolioGenerator.CDSTradeData fetchCDSTradeData(Long tradeId) {
        logger.debug("Fetching CDS trade data for trade ID: {}", tradeId);
        
        String url = backendBaseUrl + "/api/cds-trades/" + tradeId;
        
        try {
            CDSTradeResponse response = restTemplate.getForObject(url, CDSTradeResponse.class);
            
            if (response == null) {
                logger.error("No trade data found for trade ID: {}", tradeId);
                throw new RuntimeException("Trade not found: " + tradeId);
            }
            
            OrePortfolioGenerator.CDSTradeData tradeData = new OrePortfolioGenerator.CDSTradeData(
                response.getId(),
                response.getReferenceEntity(),
                response.getNotionalAmount(),
                response.getSpread(),
                response.getMaturityDate(),
                response.getEffectiveDate(),
                response.getCurrency(),
                response.getPremiumFrequency(),
                response.getDayCountConvention(),
                response.getBuySellProtection(),
                response.getPaymentCalendar()
            );
            
            logger.info("Successfully fetched CDS trade data: {} - {}", tradeId, response.getReferenceEntity());
            return tradeData;
            
        } catch (Exception e) {
            logger.error("Failed to fetch trade data for trade ID: {}", tradeId, e);
            throw new RuntimeException("Unable to fetch trade data for trade ID: " + tradeId, e);
        }
    }
    
    /**
     * Response DTO matching the backend CDSTrade model
     */
    public static class CDSTradeResponse {
        private Long id;
        private String referenceEntity;
        private BigDecimal notionalAmount;
        private BigDecimal spread;
        private LocalDate maturityDate;
        private LocalDate effectiveDate;
        private String currency;
        private String premiumFrequency;
        private String dayCountConvention;
        private String buySellProtection;
        private String paymentCalendar;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getReferenceEntity() { return referenceEntity; }
        public void setReferenceEntity(String referenceEntity) { this.referenceEntity = referenceEntity; }
        
        public BigDecimal getNotionalAmount() { return notionalAmount; }
        public void setNotionalAmount(BigDecimal notionalAmount) { this.notionalAmount = notionalAmount; }
        
        public BigDecimal getSpread() { return spread; }
        public void setSpread(BigDecimal spread) { this.spread = spread; }
        
        public LocalDate getMaturityDate() { return maturityDate; }
        public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
        
        public LocalDate getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getPremiumFrequency() { return premiumFrequency; }
        public void setPremiumFrequency(String premiumFrequency) { this.premiumFrequency = premiumFrequency; }
        
        public String getDayCountConvention() { return dayCountConvention; }
        public void setDayCountConvention(String dayCountConvention) { this.dayCountConvention = dayCountConvention; }
        
        public String getBuySellProtection() { return buySellProtection; }
        public void setBuySellProtection(String buySellProtection) { this.buySellProtection = buySellProtection; }
        
        public String getPaymentCalendar() { return paymentCalendar; }
        public void setPaymentCalendar(String paymentCalendar) { this.paymentCalendar = paymentCalendar; }
    }
}