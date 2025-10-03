package com.creditdefaultswap.riskengine.ore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Generates dynamic CurveConfig for ORE based on trade requirements
 */
@Component
public class OreCurveConfigGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(OreCurveConfigGenerator.class);
    
    /**
     * Generates CurveConfig XML for the given trades
     */
    public String generateCurveConfig(Set<OrePortfolioGenerator.CDSTradeData> trades) {
        logger.info("Generating dynamic CurveConfig for {} trades", trades.size());
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<CurveConfiguration>\n");
        
        // Collect unique currencies and reference entities
        Set<String> currencies = new java.util.HashSet<>();
        Set<String> referenceEntities = new java.util.HashSet<>();
        
        for (OrePortfolioGenerator.CDSTradeData trade : trades) {
            currencies.add(trade.getCurrency());
            referenceEntities.add(trade.getReferenceEntity());
        }
        
        // Yield curves
        xml.append("  <YieldCurves>\n");
        for (String currency : currencies) {
            generateYieldCurve(xml, currency);
        }
        xml.append("  </YieldCurves>\n");
        
        // Default curves
        xml.append("  <DefaultCurves>\n");
        for (OrePortfolioGenerator.CDSTradeData trade : trades) {
            generateDefaultCurve(xml, trade.getReferenceEntity(), trade.getCurrency());
        }
        xml.append("  </DefaultCurves>\n");
        
        xml.append("</CurveConfiguration>\n");
        
        String result = xml.toString();
        logger.info("Generated CurveConfig");
        logger.debug("CurveConfig XML:\n{}", result);
        
        return result;
    }
    
    private void generateYieldCurve(StringBuilder xml, String currency) {
        // Use A365 day counter to match conventions.xml
        String dayCounter = "A365";
        String curveId = currency + "6M";
        
        xml.append("    <YieldCurve>\n");
        xml.append("      <CurveId>").append(curveId).append("</CurveId>\n");
        xml.append("      <CurveDescription>").append(currency).append(" 6M curve</CurveDescription>\n");
        xml.append("      <Currency>").append(currency).append("</Currency>\n");
        // Self-reference for bootstrapping zero curve
        xml.append("      <DiscountCurve>").append(curveId).append("</DiscountCurve>\n");
        xml.append("      <Segments>\n");
        xml.append("        <Direct>\n");
        xml.append("          <Type>Zero</Type>\n");
        xml.append("          <Quotes>\n");
        // Format: ZERO/RATE/CURRENCY/INDEX/DAYCOUNTER/TERM (6 tokens required even when TenorBased=false)
        xml.append("            <Quote>ZERO/RATE/").append(currency).append("/").append(currency).append("6M/").append(dayCounter).append("/1Y</Quote>\n");
        xml.append("            <Quote>ZERO/RATE/").append(currency).append("/").append(currency).append("6M/").append(dayCounter).append("/3Y</Quote>\n");
        xml.append("            <Quote>ZERO/RATE/").append(currency).append("/").append(currency).append("6M/").append(dayCounter).append("/5Y</Quote>\n");
        xml.append("            <Quote>ZERO/RATE/").append(currency).append("/").append(currency).append("6M/").append(dayCounter).append("/10Y</Quote>\n");
        xml.append("          </Quotes>\n");
        xml.append("          <Conventions>").append(currency).append("-ZERO-CONVENTIONS-TENOR-BASED</Conventions>\n");
        xml.append("        </Direct>\n");
        xml.append("      </Segments>\n");
        xml.append("      <InterpolationVariable>Discount</InterpolationVariable>\n");
        xml.append("      <InterpolationMethod>LogLinear</InterpolationMethod>\n");
        xml.append("      <YieldCurveDayCounter>").append(dayCounter).append("</YieldCurveDayCounter>\n");
        xml.append("    </YieldCurve>\n");
    }
    
    private void generateDefaultCurve(StringBuilder xml, String entity, String currency) {
        String curveId = currency + "6M";
        
        xml.append("    <DefaultCurve>\n");
        xml.append("      <CurveId>").append(entity).append("_SR_").append(currency).append("</CurveId>\n");
        xml.append("      <CurveDescription>").append(entity).append(" SR CDS ").append(currency).append("</CurveDescription>\n");
        xml.append("      <Currency>").append(currency).append("</Currency>\n");
        xml.append("      <Type>SpreadCDS</Type>\n");
        // DiscountCurve must use market spec format like the example
        xml.append("      <DiscountCurve>Yield/").append(currency).append("/").append(curveId).append("</DiscountCurve>\n");
        xml.append("      <DayCounter>A365</DayCounter>\n");
        xml.append("      <RecoveryRate>RECOVERY_RATE/RATE/").append(entity).append("/SR/").append(currency).append("</RecoveryRate>\n");
        xml.append("      <Quotes>\n");
        xml.append("        <Quote>CDS/CREDIT_SPREAD/").append(entity).append("/SR/").append(currency).append("/1Y</Quote>\n");
        xml.append("        <Quote>CDS/CREDIT_SPREAD/").append(entity).append("/SR/").append(currency).append("/3Y</Quote>\n");
        xml.append("        <Quote>CDS/CREDIT_SPREAD/").append(entity).append("/SR/").append(currency).append("/5Y</Quote>\n");
        xml.append("      </Quotes>\n");
        xml.append("      <Conventions>CDS-STANDARD-CONVENTIONS</Conventions>\n");
        xml.append("    </DefaultCurve>\n");
    }
}
