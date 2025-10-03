package com.creditdefaultswap.riskengine.ore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * Generates dynamic market data files for ORE based on trade requirements
 */
@Component
public class OreMarketDataGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(OreMarketDataGenerator.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * Generates market data file content for the given trades
     */
    public String generateMarketData(Set<OrePortfolioGenerator.CDSTradeData> trades, LocalDate valuationDate) {
        logger.info("Generating dynamic market data for {} trades on valuation date {}", trades.size(), valuationDate);
        
        StringBuilder marketData = new StringBuilder();
        marketData.append("# Dynamically generated market data for CDS trades\n");
        marketData.append("# Generated at: ").append(LocalDate.now()).append("\n");
        marketData.append("# Valuation date: ").append(valuationDate).append("\n\n");
        
        String dateStr = valuationDate.format(DATE_FORMAT);
        
        // Collect unique currencies and reference entities from trades
        Set<String> currencies = new HashSet<>();
        Set<String> referenceEntities = new HashSet<>();
        
        for (OrePortfolioGenerator.CDSTradeData trade : trades) {
            currencies.add(trade.getCurrency());
            referenceEntities.add(trade.getReferenceEntity());
        }
        
        // Generate yield curves for all currencies
        marketData.append("# Yield Curves\n");
        for (String currency : currencies) {
            generateYieldCurve(marketData, dateStr, currency);
        }
        marketData.append("\n");
        
        // Generate FX rates for all non-USD currencies (needed for conversion to base currency)
        boolean needsFxRates = currencies.stream().anyMatch(c -> !c.equals("USD"));
        if (needsFxRates) {
            marketData.append("# FX Spot Rates\n");
            generateFxRates(marketData, dateStr, currencies);
            marketData.append("\n");
        }
        
        // Generate CDS curves for each reference entity
        marketData.append("# CDS Default Curves and Recovery Rates\n");
        for (String entity : referenceEntities) {
            OrePortfolioGenerator.CDSTradeData trade = trades.stream()
                .filter(t -> t.getReferenceEntity().equals(entity))
                .findFirst()
                .orElse(null);
            
            if (trade != null) {
                generateCDSCurve(marketData, dateStr, entity, trade.getCurrency(), trade.getSpread());
            }
        }
        
        String result = marketData.toString();
        logger.info("Generated market data with {} currencies and {} reference entities", 
            currencies.size(), referenceEntities.size());
        logger.debug("Market data content:\n{}", result);
        
        return result;
    }
    
    /**
     * Generates yield curve quotes for a currency
     */
    private void generateYieldCurve(StringBuilder sb, String date, String currency) {
        // Use flat yield curves with realistic rates by currency
        double baseRate = getBaseRateForCurrency(currency);
        
        // Use A365 day counter to match conventions.xml
        // Format: ZERO/RATE/CURRENCY/INDEX/DAYCOUNTER/TERM (6 tokens required)
        sb.append("# ").append(currency).append(" Yield Curve\n");
        sb.append(date).append(" ZERO/RATE/").append(currency).append("/").append(currency).append("6M/A365/1Y ").append(baseRate + 0.002).append("\n");
        sb.append(date).append(" ZERO/RATE/").append(currency).append("/").append(currency).append("6M/A365/3Y ").append(baseRate + 0.005).append("\n");
        sb.append(date).append(" ZERO/RATE/").append(currency).append("/").append(currency).append("6M/A365/5Y ").append(baseRate + 0.008).append("\n");
        sb.append(date).append(" ZERO/RATE/").append(currency).append("/").append(currency).append("6M/A365/10Y ").append(baseRate + 0.010).append("\n");
    }
    
    /**
     * Generates FX spot rates
     */
    private void generateFxRates(StringBuilder sb, String date, Set<String> currencies) {
        // Generate FX rates vs USD as base
        for (String currency : currencies) {
            if (!currency.equals("USD")) {
                double fxRate = getFxRate(currency);
                sb.append(date).append(" FX/RATE/").append(currency).append("/USD ").append(fxRate).append("\n");
            }
        }
        
        // Generate cross rates for EUR if needed
        if (currencies.contains("EUR") && currencies.size() > 2) {
            for (String currency : currencies) {
                if (!currency.equals("EUR") && !currency.equals("USD")) {
                    double fxRate = getFxRate(currency) / getFxRate("EUR");
                    sb.append(date).append(" FX/RATE/EUR/").append(currency).append(" ").append(fxRate).append("\n");
                }
            }
        }
    }
    
    /**
     * Generates CDS curve (credit spreads and recovery rate) for a reference entity
     */
    private void generateCDSCurve(StringBuilder sb, String date, String entity, String currency, BigDecimal spread) {
        // Convert spread from basis points to decimal if needed
        double spreadDecimal = convertSpreadToDecimal(spread);
        
        // Standard 40% recovery for corporates
        double recoveryRate = 0.4;
        
        sb.append("# ").append(entity).append(" ").append(currency).append("\n");
        sb.append(date).append(" RECOVERY_RATE/RATE/").append(entity).append("/SR/").append(currency).append(" ").append(recoveryRate).append("\n");
        // Use CDS/CREDIT_SPREAD instead of HAZARD_RATE for SpreadCDS curve type
        sb.append(date).append(" CDS/CREDIT_SPREAD/").append(entity).append("/SR/").append(currency).append("/1Y ").append(spreadDecimal).append("\n");
        sb.append(date).append(" CDS/CREDIT_SPREAD/").append(entity).append("/SR/").append(currency).append("/3Y ").append(spreadDecimal).append("\n");
        sb.append(date).append(" CDS/CREDIT_SPREAD/").append(entity).append("/SR/").append(currency).append("/5Y ").append(spreadDecimal).append("\n");
    }
    
    /**
     * Converts spread to decimal format
     * If spread > 1, assumes it's in basis points and divides by 10000
     * Otherwise assumes it's already in decimal format
     */
    private double convertSpreadToDecimal(BigDecimal spread) {
        double value = spread.doubleValue();
        if (value > 1.0) {
            // Spread is in basis points, convert to decimal
            return value / 10000.0;
        }
        return value;
    }
    
    /**
     * Returns base interest rate for a currency
     */
    private double getBaseRateForCurrency(String currency) {
        return switch (currency) {
            case "USD" -> 0.045;  // 4.5%
            case "EUR" -> 0.035;  // 3.5%
            case "GBP" -> 0.050;  // 5.0%
            case "CHF" -> 0.015;  // 1.5%
            case "JPY" -> 0.001;  // 0.1%
            case "AUD" -> 0.040;  // 4.0%
            default -> 0.040;     // Default 4%
        };
    }
    
    /**
     * Returns FX rate for currency vs USD
     */
    private double getFxRate(String currency) {
        return switch (currency) {
            case "EUR" -> 0.92;    // EUR/USD
            case "GBP" -> 0.79;    // GBP/USD
            case "CHF" -> 0.87;    // CHF/USD
            case "JPY" -> 149.5;   // JPY/USD
            case "AUD" -> 1.52;    // AUD/USD
            default -> 1.0;
        };
    }
}
