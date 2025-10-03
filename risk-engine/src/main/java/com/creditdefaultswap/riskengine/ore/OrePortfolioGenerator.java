package com.creditdefaultswap.riskengine.ore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class OrePortfolioGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(OrePortfolioGenerator.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Generates a dynamic ORE portfolio XML based on CDS trade data
     */
    public String generatePortfolioXml(CDSTradeData cdsTradeData) {
        logger.debug("Generating ORE portfolio for CDS trade: {}", cdsTradeData.getTradeId());
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<Portfolio>\n");
        
        // Generate CDS trade definition
        xml.append("  <Trade id=\"CDS_").append(cdsTradeData.getTradeId()).append("\">\n");
        xml.append("    <TradeType>CreditDefaultSwap</TradeType>\n");
        xml.append("    <CreditDefaultSwapData>\n");
        xml.append("      <IssuerId>").append(cdsTradeData.getReferenceEntity()).append("</IssuerId>\n");
        xml.append("      <CreditCurveId>").append(cdsTradeData.getReferenceEntity()).append("</CreditCurveId>\n");
        xml.append("      <SettlesAccrual>true</SettlesAccrual>\n");
        xml.append("      <ProtectionPaymentTime>atDefault</ProtectionPaymentTime>\n");
        xml.append("      <LegData>\n");
        xml.append("        <LegType>Fixed</LegType>\n");
        xml.append("        <Payer>").append(cdsTradeData.getBuySellProtection().equals("BUY") ? "true" : "false").append("</Payer>\n");
        xml.append("        <Currency>").append(cdsTradeData.getCurrency()).append("</Currency>\n");
        xml.append("        <PaymentConvention>Following</PaymentConvention>\n");
        xml.append("        <DayCounter>").append(mapDayCountConvention(cdsTradeData.getDayCountConvention())).append("</DayCounter>\n");
        xml.append("        <Notionals>\n");
        xml.append("          <Notional>").append(cdsTradeData.getNotionalAmount()).append("</Notional>\n");
        xml.append("          <Exchanges>\n");
        xml.append("            <NotionalInitialExchange>false</NotionalInitialExchange>\n");
        xml.append("            <NotionalFinalExchange>false</NotionalFinalExchange>\n");
        xml.append("            <NotionalAmortizingExchange>false</NotionalAmortizingExchange>\n");
        xml.append("          </Exchanges>\n");
        xml.append("        </Notionals>\n");
        xml.append("        <ScheduleData>\n");
        xml.append("          <Rules>\n");
        xml.append("            <StartDate>").append(formatDate(cdsTradeData.getEffectiveDate())).append("</StartDate>\n");
        xml.append("            <EndDate>").append(formatDate(cdsTradeData.getMaturityDate())).append("</EndDate>\n");
        xml.append("            <Tenor>").append(mapFrequency(cdsTradeData.getPremiumFrequency())).append("</Tenor>\n");
        xml.append("            <Calendar>").append(mapCalendar(cdsTradeData.getPaymentCalendar(), cdsTradeData.getCurrency())).append("</Calendar>\n");
        xml.append("            <Convention>Following</Convention>\n");
        xml.append("            <TermConvention>Unadjusted</TermConvention>\n");
        xml.append("            <Rule>CDS2015</Rule>\n");
        xml.append("            <EndOfMonth/>\n");
        xml.append("            <FirstDate/>\n");
        xml.append("            <LastDate/>\n");
        xml.append("          </Rules>\n");
        xml.append("        </ScheduleData>\n");
        xml.append("        <FixedLegData>\n");
        xml.append("          <Rates>\n");
        xml.append("            <Rate>").append(convertSpreadToDecimal(cdsTradeData.getSpread())).append("</Rate>\n");
        xml.append("          </Rates>\n");
        xml.append("        </FixedLegData>\n");
        xml.append("      </LegData>\n");
        xml.append("    </CreditDefaultSwapData>\n");
        xml.append("  </Trade>\n");
        xml.append("</Portfolio>\n");
        
        String portfolioXml = xml.toString();
        logger.info("Generated ORE portfolio XML for CDS trade {} - Reference Entity: {}, Notional: {} {}", 
            cdsTradeData.getTradeId(), cdsTradeData.getReferenceEntity(), 
            cdsTradeData.getNotionalAmount(), cdsTradeData.getCurrency());
        logger.debug("Portfolio XML: {}", portfolioXml);
        
        return portfolioXml;
    }
    
    private String mapDayCountConvention(String dayCountConvention) {
        return switch (dayCountConvention.toUpperCase()) {
            case "ACT/360" -> "A360";
            case "ACT/365" -> "A365";
            case "30/360" -> "30/360";
            default -> "A360";
        };
    }
    
    private String mapCalendar(String calendar, String currency) {
        if (calendar == null || calendar.isEmpty()) {
            return currency;
        }
        return switch (calendar.toUpperCase()) {
            case "NYC", "NY", "US" -> "US";
            case "TARGET", "EUR" -> "TARGET";
            case "LONDON", "LON", "GBP" -> "UK";
            default -> currency;
        };
    }
    
    private String mapFrequency(String frequency) {
        return switch (frequency.toUpperCase()) {
            case "QUARTERLY" -> "3M";
            case "SEMIANNUAL" -> "6M";
            case "ANNUAL" -> "1Y";
            case "MONTHLY" -> "1M";
            default -> "3M";
        };
    }
    
    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
    
    private BigDecimal convertSpreadToDecimal(BigDecimal spread) {
        if (spread.compareTo(BigDecimal.ONE) > 0) {
            return spread.divide(new BigDecimal("10000"), 6, java.math.RoundingMode.HALF_UP);
        }
        return spread;
    }
    
    public static class CDSTradeData {
        private Long tradeId;
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
        
        public CDSTradeData() {}
        
        public CDSTradeData(Long tradeId, String referenceEntity, BigDecimal notionalAmount, 
                           BigDecimal spread, LocalDate maturityDate, LocalDate effectiveDate,
                           String currency, String premiumFrequency, String dayCountConvention,
                           String buySellProtection, String paymentCalendar) {
            this.tradeId = tradeId;
            this.referenceEntity = referenceEntity;
            this.notionalAmount = notionalAmount;
            this.spread = spread;
            this.maturityDate = maturityDate;
            this.effectiveDate = effectiveDate;
            this.currency = currency;
            this.premiumFrequency = premiumFrequency;
            this.dayCountConvention = dayCountConvention;
            this.buySellProtection = buySellProtection;
            this.paymentCalendar = paymentCalendar;
        }
        
        public Long getTradeId() { return tradeId; }
        public void setTradeId(Long tradeId) { this.tradeId = tradeId; }
        
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
