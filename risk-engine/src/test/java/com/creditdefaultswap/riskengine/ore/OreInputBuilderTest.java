package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OreInputBuilderTest {
    
    private OreInputBuilder oreInputBuilder;
    
    @BeforeEach
    void setUp() {
        oreInputBuilder = new OreInputBuilder();
    }
    
    @Test
    void testBuildRiskCalculationInput() {
        ScenarioRequest request = new ScenarioRequest();
        request.setScenarioId("TEST_SCENARIO");
        request.setTradeIds(List.of(1L, 2L));
        request.setValuationDate(LocalDate.of(2024, 1, 15));
        request.setScenarios(Map.of("USD_1Y", 0.0001, "USD_5Y", 0.0002));
        
        String xml = oreInputBuilder.buildRiskCalculationInput(request);
        
        assertNotNull(xml);
        assertTrue(xml.contains("<?xml version=\"1.0\"?>"));
        assertTrue(xml.contains("<ORE>"));
        assertTrue(xml.contains("</ORE>"));
        assertTrue(xml.contains("2024-01-15"));
        assertTrue(xml.contains("Trade id=\"1\""));
        assertTrue(xml.contains("Trade id=\"2\""));
        assertTrue(xml.contains("YieldCurve"));
        assertTrue(xml.contains("USD"));
    }
    
    @Test
    void testBuildHealthCheckInput() {
        String xml = oreInputBuilder.buildHealthCheckInput();
        
        assertNotNull(xml);
        assertTrue(xml.contains("<?xml version=\"1.0\"?>"));
        assertTrue(xml.contains("<ORE>"));
        assertTrue(xml.contains("</ORE>"));
        assertTrue(xml.contains("Setup"));
        assertTrue(xml.contains("Analytics"));
    }
    
    @Test
    void testBuildRiskCalculationInput_WithNullDate() {
        ScenarioRequest request = new ScenarioRequest();
        request.setScenarioId("TEST_SCENARIO");
        request.setTradeIds(List.of(1L));
        request.setValuationDate(null); // Test null date handling
        
        String xml = oreInputBuilder.buildRiskCalculationInput(request);
        
        assertNotNull(xml);
        assertTrue(xml.contains(LocalDate.now().toString()));
    }
}