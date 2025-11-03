package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.ore.OrePortfolioGenerator.CDSTradeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Generates ORE stresstest.xml configuration for recovery rate and spread stress scenarios
 */
@Component
public class OreStressTestGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(OreStressTestGenerator.class);
    
    /**
     * Generates a stresstest.xml file with recovery rate and spread scenarios
     * 
     * @param tradeData The CDS trade data
     * @param recoveryRateShifts List of absolute recovery rate values (e.g., 30 for 30%)
     * @param spreadShifts List of spread shifts in basis points (e.g., 50 for +50bp)
     * @param combined Whether to generate combined scenarios
     * @return XML content for stresstest.xml
     */
    public String generateStressTestConfig(
            CDSTradeData tradeData, 
            List<BigDecimal> recoveryRateShifts,
            List<BigDecimal> spreadShifts,
            boolean combined) {
        
        logger.info("Generating stress test configuration for {} with {} recovery rates, {} spread shifts, combined={}",
            tradeData.getReferenceEntity(), 
            recoveryRateShifts != null ? recoveryRateShifts.size() : 0,
            spreadShifts != null ? spreadShifts.size() : 0,
            combined);
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<StressTesting>\n");
        
        // Generate spread stress scenarios
        if (spreadShifts != null && !spreadShifts.isEmpty()) {
            for (BigDecimal spreadShift : spreadShifts) {
                generateSpreadStressScenario(xml, tradeData, spreadShift);
            }
        }
        
        // Generate recovery rate stress scenarios
        if (recoveryRateShifts != null && !recoveryRateShifts.isEmpty()) {
            for (BigDecimal recoveryRate : recoveryRateShifts) {
                generateRecoveryRateStressScenario(xml, tradeData, recoveryRate);
            }
        }
        
        // Generate combined scenarios if requested
        if (combined && recoveryRateShifts != null && spreadShifts != null) {
            for (BigDecimal recoveryRate : recoveryRateShifts) {
                for (BigDecimal spreadShift : spreadShifts) {
                    generateCombinedStressScenario(xml, tradeData, recoveryRate, spreadShift);
                }
            }
        }
        
        xml.append("</StressTesting>\n");
        
        String result = xml.toString();
        logger.debug("Generated stress test configuration:\n{}", result);
        return result;
    }
    
    /**
     * Generates a spread stress scenario (parallel shift to credit curve)
     */
    private void generateSpreadStressScenario(StringBuilder xml, CDSTradeData tradeData, BigDecimal spreadShiftBps) {
        String scenarioId = "spread_" + spreadShiftBps.intValue() + "bp";
        double shiftDecimal = spreadShiftBps.doubleValue() / 10000.0; // Convert bps to decimal
        
        xml.append("  <StressTest id=\"").append(scenarioId).append("\">\n");
        xml.append("    <SurvivalProbabilities>\n");
        xml.append("      <SurvivalProbability name=\"").append(tradeData.getReferenceEntity()).append("\">\n");
        xml.append("        <ShiftType>Absolute</ShiftType>\n");
        xml.append("        <Shifts>").append(shiftDecimal).append(",").append(shiftDecimal).append(",")
           .append(shiftDecimal).append(",").append(shiftDecimal).append(",").append(shiftDecimal).append("</Shifts>\n");
        xml.append("        <ShiftTenors>1Y,2Y,3Y,5Y,10Y</ShiftTenors>\n");
        xml.append("      </SurvivalProbability>\n");
        xml.append("    </SurvivalProbabilities>\n");
        
        // Add empty sections for other risk factors
        appendEmptyRiskFactors(xml);
        
        xml.append("  </StressTest>\n\n");
    }
    
    /**
     * Generates a recovery rate stress scenario
     */
    private void generateRecoveryRateStressScenario(StringBuilder xml, CDSTradeData tradeData, BigDecimal recoveryRatePercent) {
        String scenarioId = "recovery_" + recoveryRatePercent.intValue() + "pct";
        
        // Calculate the shift from base recovery rate
        BigDecimal baseRecoveryRate = tradeData.getRecoveryRate() != null ? 
            tradeData.getRecoveryRate() : new BigDecimal("40");
        double shiftDecimal = (recoveryRatePercent.doubleValue() - baseRecoveryRate.doubleValue()) / 100.0;
        
        xml.append("  <StressTest id=\"").append(scenarioId).append("\">\n");
        xml.append("    <RecoveryRates>\n");
        xml.append("      <RecoveryRate name=\"").append(tradeData.getReferenceEntity()).append("\">\n");
        xml.append("        <ShiftType>Absolute</ShiftType>\n");
        xml.append("        <ShiftSize>").append(shiftDecimal).append("</ShiftSize>\n");
        xml.append("      </RecoveryRate>\n");
        xml.append("    </RecoveryRates>\n");
        
        // Add empty sections for other risk factors
        appendEmptyRiskFactors(xml);
        
        xml.append("  </StressTest>\n\n");
    }
    
    /**
     * Generates a combined recovery rate + spread stress scenario
     */
    private void generateCombinedStressScenario(StringBuilder xml, CDSTradeData tradeData, 
                                                BigDecimal recoveryRatePercent, BigDecimal spreadShiftBps) {
        String scenarioId = "combined_recovery" + recoveryRatePercent.intValue() + "_spread" + spreadShiftBps.intValue() + "bp";
        
        // Calculate shifts
        BigDecimal baseRecoveryRate = tradeData.getRecoveryRate() != null ? 
            tradeData.getRecoveryRate() : new BigDecimal("40");
        double recoveryShiftDecimal = (recoveryRatePercent.doubleValue() - baseRecoveryRate.doubleValue()) / 100.0;
        double spreadShiftDecimal = spreadShiftBps.doubleValue() / 10000.0;
        
        xml.append("  <StressTest id=\"").append(scenarioId).append("\">\n");
        
        // Recovery rate shift
        xml.append("    <RecoveryRates>\n");
        xml.append("      <RecoveryRate name=\"").append(tradeData.getReferenceEntity()).append("\">\n");
        xml.append("        <ShiftType>Absolute</ShiftType>\n");
        xml.append("        <ShiftSize>").append(recoveryShiftDecimal).append("</ShiftSize>\n");
        xml.append("      </RecoveryRate>\n");
        xml.append("    </RecoveryRates>\n");
        
        // Spread shift
        xml.append("    <SurvivalProbabilities>\n");
        xml.append("      <SurvivalProbability name=\"").append(tradeData.getReferenceEntity()).append("\">\n");
        xml.append("        <ShiftType>Absolute</ShiftType>\n");
        xml.append("        <Shifts>").append(spreadShiftDecimal).append(",").append(spreadShiftDecimal).append(",")
           .append(spreadShiftDecimal).append(",").append(spreadShiftDecimal).append(",")
           .append(spreadShiftDecimal).append("</Shifts>\n");
        xml.append("        <ShiftTenors>1Y,2Y,3Y,5Y,10Y</ShiftTenors>\n");
        xml.append("      </SurvivalProbability>\n");
        xml.append("    </SurvivalProbabilities>\n");
        
        // Add empty sections for other risk factors (excluding RecoveryRates and SurvivalProbabilities)
        xml.append("    <DiscountCurves />\n");
        xml.append("    <IndexCurves />\n");
        xml.append("    <YieldCurves />\n");
        xml.append("    <FxSpots />\n");
        xml.append("    <FxVolatilities />\n");
        xml.append("    <SwaptionVolatilities />\n");
        xml.append("    <CapFloorVolatilities />\n");
        xml.append("    <EquitySpots />\n");
        xml.append("    <EquityVolatilities />\n");
        xml.append("    <SecuritySpreads />\n");
        
        xml.append("  </StressTest>\n\n");
    }
    
    /**
     * Appends empty risk factor sections to stress test
     */
    private void appendEmptyRiskFactors(StringBuilder xml) {
        xml.append("    <DiscountCurves />\n");
        xml.append("    <IndexCurves />\n");
        xml.append("    <YieldCurves />\n");
        xml.append("    <FxSpots />\n");
        xml.append("    <FxVolatilities />\n");
        xml.append("    <SwaptionVolatilities />\n");
        xml.append("    <CapFloorVolatilities />\n");
        xml.append("    <EquitySpots />\n");
        xml.append("    <EquityVolatilities />\n");
        xml.append("    <SecuritySpreads />\n");
        xml.append("    <RecoveryRates />\n");
        xml.append("    <SurvivalProbabilities />\n");
    }
}
