package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Component
public class OreInputBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(OreInputBuilder.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Builds ORE XML input for risk calculation
     */
    public String buildRiskCalculationInput(ScenarioRequest request) {
        logger.debug("Building ORE input for scenario: {}", request.getScenarioId());
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<ORE>\n");
        
        // Add setup section
        appendSetup(xml, request);
        
        // Add market data
        appendMarketData(xml, request);
        
        // Add portfolio (trades)
        appendPortfolio(xml, request);
        
        // Add scenario definitions
        appendScenarios(xml, request);
        
        // Add analytics configuration
        appendAnalytics(xml, request);
        
        xml.append("</ORE>\n");
        
        String result = xml.toString();
        logger.debug("Generated ORE XML input, length: {}", result.length());
        
        return result;
    }
    
    private void appendSetup(StringBuilder xml, ScenarioRequest request) {
        LocalDate valuationDate = request.getValuationDate() != null ? 
            request.getValuationDate() : LocalDate.now();
        
        xml.append("  <Setup>\n");
        xml.append("    <Parameter name=\"asofDate\">").append(DATE_FORMAT.format(valuationDate)).append("</Parameter>\n");
        xml.append("    <Parameter name=\"inputPath\">Input</Parameter>\n");
        xml.append("    <Parameter name=\"outputPath\">Output</Parameter>\n");
        xml.append("    <Parameter name=\"logMask\">255</Parameter>\n");
        xml.append("    <Parameter name=\"logLevel\">2</Parameter>\n");
        xml.append("  </Setup>\n");
    }
    
    private void appendMarketData(StringBuilder xml, ScenarioRequest request) {
        xml.append("  <Markets>\n");
        xml.append("    <Configuration>\n");
        xml.append("      <Market>\n");
        xml.append("        <YieldCurves>\n");
        
        // Add yield curves based on trade currencies
        for (Long tradeId : request.getTradeIds()) {
            // For now, add USD curve - in real implementation, get currency from trade
            xml.append("          <YieldCurve>\n");
            xml.append("            <CurveId>USD</CurveId>\n");
            xml.append("            <CurveDescription>USD yield curve</CurveDescription>\n");
            xml.append("            <Currency>USD</Currency>\n");
            xml.append("            <YieldCurveSegments>\n");
            xml.append("              <Simple>\n");
            xml.append("                <Type>Deposit</Type>\n");
            xml.append("                <Quotes>\n");
            xml.append("                  <Quote>MM/USD/0D/1D</Quote>\n");
            xml.append("                  <Quote>MM/USD/1D/1W</Quote>\n");
            xml.append("                  <Quote>MM/USD/1W/1M</Quote>\n");
            xml.append("                </Quotes>\n");
            xml.append("              </Simple>\n");
            xml.append("            </YieldCurveSegments>\n");
            xml.append("          </YieldCurve>\n");
        }
        
        xml.append("        </YieldCurves>\n");
        xml.append("        <DefaultCurves/>\n");
        xml.append("        <SwaptionVolatilities/>\n");
        xml.append("        <CapFloorVolatilities/>\n");
        xml.append("        <FxVolatilities/>\n");
        xml.append("        <EquityVolatilities/>\n");
        xml.append("      </Market>\n");
        xml.append("    </Configuration>\n");
        xml.append("  </Markets>\n");
    }
    
    private void appendPortfolio(StringBuilder xml, ScenarioRequest request) {
        xml.append("  <Portfolio>\n");
        
        for (Long tradeId : request.getTradeIds()) {
            xml.append("    <Trade id=\"").append(tradeId).append("\">\n");
            xml.append("      <TradeType>Swap</TradeType>\n");
            xml.append("      <Envelope>\n");
            xml.append("        <CounterParty>CPTY_").append(tradeId).append("</CounterParty>\n");
            xml.append("        <NettingSetId>NETTING_").append(tradeId).append("</NettingSetId>\n");
            xml.append("        <Portfolio>Portfolio</Portfolio>\n");
            xml.append("      </Envelope>\n");
            xml.append("      <SwapData>\n");
            xml.append("        <LegData>\n");
            xml.append("          <LegType>Fixed</LegType>\n");
            xml.append("          <Payer>false</Payer>\n");
            xml.append("          <Currency>USD</Currency>\n");
            xml.append("          <Notionals>\n");
            xml.append("            <Notional>1000000</Notional>\n");
            xml.append("          </Notionals>\n");
            xml.append("          <DayCounter>30/360</DayCounter>\n");
            xml.append("          <PaymentConvention>F</PaymentConvention>\n");
            xml.append("          <FixedLegData>\n");
            xml.append("            <Rates>\n");
            xml.append("              <Rate>0.05</Rate>\n");
            xml.append("            </Rates>\n");
            xml.append("          </FixedLegData>\n");
            xml.append("          <ScheduleData>\n");
            xml.append("            <Rules>\n");
            xml.append("              <StartDate>2024-01-01</StartDate>\n");
            xml.append("              <EndDate>2029-01-01</EndDate>\n");
            xml.append("              <Tenor>6M</Tenor>\n");
            xml.append("              <Calendar>USD</Calendar>\n");
            xml.append("              <Convention>MF</Convention>\n");
            xml.append("              <TermConvention>MF</TermConvention>\n");
            xml.append("              <Rule>Forward</Rule>\n");
            xml.append("            </Rules>\n");
            xml.append("          </ScheduleData>\n");
            xml.append("        </LegData>\n");
            xml.append("        <LegData>\n");
            xml.append("          <LegType>Floating</LegType>\n");
            xml.append("          <Payer>true</Payer>\n");
            xml.append("          <Currency>USD</Currency>\n");
            xml.append("          <Notionals>\n");
            xml.append("            <Notional>1000000</Notional>\n");
            xml.append("          </Notionals>\n");
            xml.append("          <DayCounter>ACT/360</DayCounter>\n");
            xml.append("          <PaymentConvention>F</PaymentConvention>\n");
            xml.append("          <FloatingLegData>\n");
            xml.append("            <Index>USD-LIBOR-3M</Index>\n");
            xml.append("          </FloatingLegData>\n");
            xml.append("          <ScheduleData>\n");
            xml.append("            <Rules>\n");
            xml.append("              <StartDate>2024-01-01</StartDate>\n");
            xml.append("              <EndDate>2029-01-01</EndDate>\n");
            xml.append("              <Tenor>3M</Tenor>\n");
            xml.append("              <Calendar>USD</Calendar>\n");
            xml.append("              <Convention>MF</Convention>\n");
            xml.append("              <TermConvention>MF</TermConvention>\n");
            xml.append("              <Rule>Forward</Rule>\n");
            xml.append("            </Rules>\n");
            xml.append("          </ScheduleData>\n");
            xml.append("        </LegData>\n");
            xml.append("      </SwapData>\n");
            xml.append("    </Trade>\n");
        }
        
        xml.append("  </Portfolio>\n");
    }
    
    private void appendScenarios(StringBuilder xml, ScenarioRequest request) {
        xml.append("  <SensitivityAnalysis>\n");
        xml.append("    <PricingEngines>\n");
        xml.append("      <Product type=\"Swap\">\n");
        xml.append("        <Model>DiscountedCashflows</Model>\n");
        xml.append("        <ModelParameters/>\n");
        xml.append("        <Engine>DiscountingSwapEngine</Engine>\n");
        xml.append("        <EngineParameters/>\n");
        xml.append("      </Product>\n");
        xml.append("    </PricingEngines>\n");
        
        xml.append("    <SensitivityConfig>\n");
        xml.append("      <SensitivityScenarioData>\n");
        
        // Add interest rate scenarios
        xml.append("        <YieldCurves>\n");
        xml.append("          <YieldCurve>\n");
        xml.append("            <SensitivityParameter>YieldCurve/USD</SensitivityParameter>\n");
        xml.append("            <ShiftType>Absolute</ShiftType>\n");
        xml.append("            <ShiftSize>0.0001</ShiftSize>\n");
        xml.append("            <KeyType>Tenor</KeyType>\n");
        xml.append("            <KeyValues>1Y,2Y,3Y,5Y,7Y,10Y</KeyValues>\n");
        xml.append("          </YieldCurve>\n");
        xml.append("        </YieldCurves>\n");
        
        // Add scenarios based on request
        if (request.getScenarios() != null) {
            for (Map.Entry<String, Double> scenario : request.getScenarios().entrySet()) {
                xml.append("        <YieldCurveScenario>\n");
                xml.append("          <Key>").append(scenario.getKey()).append("</Key>\n");
                xml.append("          <ShiftSize>").append(scenario.getValue()).append("</ShiftSize>\n");
                xml.append("        </YieldCurveScenario>\n");
            }
        }
        
        xml.append("      </SensitivityScenarioData>\n");
        xml.append("    </SensitivityConfig>\n");
        xml.append("  </SensitivityAnalysis>\n");
    }
    
    private void appendAnalytics(StringBuilder xml, ScenarioRequest request) {
        xml.append("  <Analytics>\n");
        xml.append("    <Analytic type=\"npv\">true</Analytic>\n");
        xml.append("    <Analytic type=\"sensitivity\">true</Analytic>\n");
        xml.append("    <Analytic type=\"stress\">false</Analytic>\n");
        xml.append("    <Analytic type=\"parametricvar\">false</Analytic>\n");
        xml.append("    <Analytic type=\"simulation\">false</Analytic>\n");
        xml.append("  </Analytics>\n");
    }
    
    /**
     * Builds ORE XML input for a simple health check
     */
    public String buildHealthCheckInput() {
        return "<?xml version=\"1.0\"?>\n" +
               "<ORE>\n" +
               "  <Setup>\n" +
               "    <Parameter name=\"asofDate\">" + DATE_FORMAT.format(LocalDate.now()) + "</Parameter>\n" +
               "    <Parameter name=\"inputPath\">Input</Parameter>\n" +
               "    <Parameter name=\"outputPath\">Output</Parameter>\n" +
               "  </Setup>\n" +
               "  <Analytics>\n" +
               "    <Analytic type=\"npv\">false</Analytic>\n" +
               "  </Analytics>\n" +
               "</ORE>\n";
    }
}