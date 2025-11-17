package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.model.RiskMeasures;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Epic(EpicType.UNIT_TESTS)
class OreOutputParserTest {
    
    private OreOutputParser oreOutputParser;
    
    @Mock
    private MarketDataSnapshotBuilder marketDataSnapshotBuilder;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        oreOutputParser = new OreOutputParser(marketDataSnapshotBuilder);
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Output Parsing - Parse Valid Output")
    void testParseRiskMeasures_ValidOutput() {
        String validOreOutput = """
            <?xml version="1.0"?>
            <ORE>
              <NPV>
                <Value>1000000.50</Value>
                <Currency>USD</Currency>
              </NPV>
              <Sensitivity>
                <RiskFactor>YieldCurve/USD/1Y</RiskFactor>
                <Delta>100.25</Delta>
              </Sensitivity>
              <Gamma>
                <Value>0.001234</Value>
              </Gamma>
              <VaR>
                <Value>50000.75</Value>
              </VaR>
              <ExpectedShortfall>
                <Value>75000.80</Value>
              </ExpectedShortfall>
              <Delta>0.500000</Delta>
              <Vega>250.50</Vega>
              <Theta>-5.25</Theta>
              <Rho>125.75</Rho>
            </ORE>
            """;
        
        RiskMeasures result = oreOutputParser.parseRiskMeasures(validOreOutput, 123L, "USD");
        
        assertNotNull(result);
        assertEquals(123L, result.getTradeId());
        assertEquals(new BigDecimal("1000000.50"), result.getNpv());
        assertEquals("USD", result.getCurrency());
        assertEquals(new BigDecimal("100.25"), result.getDv01());
        assertEquals(new BigDecimal("0.001234"), result.getGamma());
        assertEquals(new BigDecimal("50000.75"), result.getVar95());
        assertEquals(new BigDecimal("75000.80"), result.getExpectedShortfall());
        
        assertNotNull(result.getGreeks());
        assertTrue(result.getGreeks().containsKey("delta"));
        assertTrue(result.getGreeks().containsKey("vega"));
        assertTrue(result.getGreeks().containsKey("theta"));
        assertTrue(result.getGreeks().containsKey("rho"));
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Output Parsing - Parse Invalid XML")
    void testParseRiskMeasures_InvalidXml() {
        String invalidXml = "not valid xml";
        
        RiskMeasures result = oreOutputParser.parseRiskMeasures(invalidXml, 123L, "USD");
        
        assertNotNull(result);
        assertEquals(123L, result.getTradeId());
        assertEquals(BigDecimal.ZERO, result.getNpv());
        assertEquals("USD", result.getCurrency());
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Output Parsing - Validate Valid XML")
    void testIsValidOutput_ValidXml() {
        String validOreOutput = """
            <?xml version="1.0"?>
            <ORE>
              <NPV>
                <Value>1000000.50</Value>
              </NPV>
            </ORE>
            """;
        
        assertTrue(oreOutputParser.isValidOutput(validOreOutput));
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Output Parsing - Validate Invalid XML")
    void testIsValidOutput_InvalidXml() {
        assertFalse(oreOutputParser.isValidOutput("invalid xml"));
        assertFalse(oreOutputParser.isValidOutput(null));
        assertFalse(oreOutputParser.isValidOutput(""));
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Output Parsing - Extract Error Message")
    void testExtractErrorMessage() {
        String errorOutput = """
            <?xml version="1.0"?>
            <ORE>
              <Error>Calculation failed: Invalid market data</Error>
            </ORE>
            """;
        
        String errorMessage = oreOutputParser.extractErrorMessage(errorOutput);
        
        assertEquals("Calculation failed: Invalid market data", errorMessage);
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Output Parsing - Extract Warning Message")
    void testExtractErrorMessage_WithWarning() {
        String warningOutput = """
            <?xml version="1.0"?>
            <ORE>
              <Warning>Market data incomplete</Warning>
            </ORE>
            """;
        
        String errorMessage = oreOutputParser.extractErrorMessage(warningOutput);
        
        assertEquals("Warning: Market data incomplete", errorMessage);
    }
}
