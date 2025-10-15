package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.ore.OrePortfolioGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

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
        return fetchCDSTradeData(tradeId, null);
    }
    
    /**
     * Fetches CDS trade data from the backend service with valuation date awareness
     * Throws exception if trade data cannot be retrieved
     * 
     * @param tradeId The trade ID to fetch
     * @param valuationDate The valuation date (used to ensure effective date doesn't go backwards in time)
     */
    public OrePortfolioGenerator.CDSTradeData fetchCDSTradeData(Long tradeId, LocalDate valuationDate) {
        logger.debug("Fetching CDS trade data for trade ID: {} with valuation date: {}", tradeId, valuationDate);
        
        String url = backendBaseUrl + "/api/cds-trades/" + tradeId;
        
        try {
            CDSTradeResponse response = restTemplate.getForObject(url, CDSTradeResponse.class);
            
            if (response == null) {
                logger.error("No trade data found for trade ID: {}", tradeId);
                throw new RuntimeException("Trade not found: " + tradeId);
            }
            
            // Fetch paid coupons to determine if we need to adjust the effective date
            // When coupons have been paid, we want to value only remaining cashflows
            LocalDate adjustedEffectiveDate = response.getEffectiveDate();
            try {
                String couponUrl = backendBaseUrl + "/api/lifecycle/trades/" + tradeId + "/coupon-schedule";
                CouponPeriod[] coupons = restTemplate.getForObject(couponUrl, CouponPeriod[].class);
                
                if (coupons != null && coupons.length > 0) {
                    // Check if any coupons have been paid
                    boolean hasPaidCoupons = false;
                    LocalDate lastPaidEndDate = null;
                    
                    for (CouponPeriod coupon : coupons) {
                        if (Boolean.TRUE.equals(coupon.getPaid())) {
                            hasPaidCoupons = true;
                            if (lastPaidEndDate == null || coupon.getPeriodEndDate().isAfter(lastPaidEndDate)) {
                                lastPaidEndDate = coupon.getPeriodEndDate();
                            }
                        }
                    }
                    
                    // If coupons have been paid, adjust effective date to the end of last paid period
                    // This ensures we only value remaining (unpaid) cashflows
                    // NOTE: We should NOT adjust the effective date based on the valuation date,
                    // as that would change the trade's contractual cashflow schedule.
                    // The valuation date (asofDate in ORE) controls the discount factors, not the schedule.
                    if (hasPaidCoupons && lastPaidEndDate != null) {
                        adjustedEffectiveDate = lastPaidEndDate;
                        logger.info("Adjusted effective date to {} (end of last paid coupon) for trade {}", 
                            adjustedEffectiveDate, tradeId);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not fetch coupon schedule for trade {}: {}", tradeId, e.getMessage());
                // Continue without adjustment
            }
            
            OrePortfolioGenerator.CDSTradeData tradeData = new OrePortfolioGenerator.CDSTradeData(
                response.getId(),
                response.getReferenceEntity(),
                response.getNotionalAmount(),
                response.getSpread(),
                response.getMaturityDate(),
                adjustedEffectiveDate,  // Use adjusted effective date
                response.getCurrency(),
                response.getPremiumFrequency(),
                response.getDayCountConvention(),
                response.getBuySellProtection(),
                response.getPaymentCalendar()
            );
            
            // Set recovery rate from trade data
            tradeData.setRecoveryRate(response.getRecoveryRate());
            
            // Don't set firstCouponDate - let ORE generate the schedule from the adjusted effective date
            
            logger.info("Successfully fetched CDS trade data: {} - {} (Recovery Rate: {}%)", 
                tradeId, response.getReferenceEntity(), response.getRecoveryRate());
            return tradeData;
            
        } catch (Exception e) {
            logger.error("Failed to fetch trade data for trade ID: {}", tradeId, e);
            throw new RuntimeException("Unable to fetch trade data for trade ID: " + tradeId, e);
        }
    }
    
    /**
     * Response DTO for coupon periods
     */
    public static class CouponPeriod {
        private Long id;
        private LocalDate periodStartDate;
        private LocalDate periodEndDate;
        private LocalDate paymentDate;
        private Boolean paid;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public LocalDate getPeriodStartDate() { return periodStartDate; }
        public void setPeriodStartDate(LocalDate periodStartDate) { this.periodStartDate = periodStartDate; }
        
        public LocalDate getPeriodEndDate() { return periodEndDate; }
        public void setPeriodEndDate(LocalDate periodEndDate) { this.periodEndDate = periodEndDate; }
        
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
        
        public Boolean getPaid() { return paid; }
        public void setPaid(Boolean paid) { this.paid = paid; }
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
        private BigDecimal recoveryRate;
        
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
        
        public BigDecimal getRecoveryRate() { return recoveryRate; }
        public void setRecoveryRate(BigDecimal recoveryRate) { this.recoveryRate = recoveryRate; }
    }
}