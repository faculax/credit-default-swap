package com.creditdefaultswap.riskengine.ore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Generates dynamic TodaysMarket configuration for ORE based on trade requirements
 */
@Component
public class OreTodaysMarketGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(OreTodaysMarketGenerator.class);
    
    /**
     * Generates TodaysMarket XML configuration for the given trades
     */
    public String generateTodaysMarket(Set<OrePortfolioGenerator.CDSTradeData> trades) {
        logger.info("Generating dynamic TodaysMarket configuration for {} trades", trades.size());
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<TodaysMarket>\n");
        
        // Collect unique currencies and reference entities
        Set<String> currencies = new java.util.HashSet<>();
        Set<String> referenceEntities = new java.util.HashSet<>();
        
        for (OrePortfolioGenerator.CDSTradeData trade : trades) {
            currencies.add(trade.getCurrency());
            referenceEntities.add(trade.getReferenceEntity());
        }
        
        // Configuration section - contains references only
        xml.append("  <Configuration id=\"default\">\n");
        xml.append("    <DiscountingCurvesId>default</DiscountingCurvesId>\n");
        xml.append("    <IndexForwardingCurvesId>default</IndexForwardingCurvesId>\n");
        xml.append("  </Configuration>\n");
        
        // Discount curves definitions
        xml.append("  <DiscountingCurves id=\"default\">\n");
        for (String currency : currencies) {
            xml.append("    <DiscountingCurve currency=\"").append(currency).append("\">Yield/").append(currency).append("/").append(currency).append("6M</DiscountingCurve>\n");
        }
        xml.append("  </DiscountingCurves>\n");
        
        // Index forwarding curves definitions
        xml.append("  <IndexForwardingCurves id=\"default\">\n");
        for (String currency : currencies) {
            xml.append("    <Index name=\"").append(currency).append("-LIBOR-6M\">Yield/").append(currency).append("/").append(currency).append("6M</Index>\n");
        }
        xml.append("  </IndexForwardingCurves>\n");
        
        // Yield curves definitions
        xml.append("  <YieldCurves id=\"default\">\n");
        for (String currency : currencies) {
            xml.append("    <YieldCurve name=\"").append(currency).append("6M\">Yield/").append(currency).append("/").append(currency).append("6M</YieldCurve>\n");
        }
        xml.append("  </YieldCurves>\n");
        
        // FX spots - always generate for non-USD currencies (needed for base currency conversion)
        boolean needsFxSpots = currencies.stream().anyMatch(c -> !c.equals("USD"));
        if (needsFxSpots) {
            xml.append("  <FxSpots id=\"default\">\n");
            for (String currency : currencies) {
                if (!currency.equals("USD")) {
                    xml.append("    <FxSpot pair=\"").append(currency).append("USD\">FX/").append(currency).append("/USD</FxSpot>\n");
                }
            }
            xml.append("  </FxSpots>\n");
        }
        
        // Default curves for CDS reference entities
        xml.append("  <DefaultCurves id=\"default\">\n");
        for (OrePortfolioGenerator.CDSTradeData trade : trades) {
            String entity = trade.getReferenceEntity();
            String currency = trade.getCurrency();
            xml.append("    <DefaultCurve name=\"").append(entity).append("\">Default/").append(currency).append("/").append(entity).append("_SR_").append(currency).append("</DefaultCurve>\n");
        }
        xml.append("  </DefaultCurves>\n");
        
        xml.append("</TodaysMarket>\n");
        
        String result = xml.toString();
        logger.info("Generated TodaysMarket configuration");
        logger.debug("TodaysMarket XML:\n{}", result);
        
        return result;
    }
}
