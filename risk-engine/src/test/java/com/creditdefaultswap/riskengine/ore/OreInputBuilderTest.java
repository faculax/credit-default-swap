package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.service.TradeDataService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Epic("Unit Tests")
class OreInputBuilderTest {
    
    private OreInputBuilder oreInputBuilder;
    
    @Mock
    private OrePortfolioGenerator portfolioGenerator;
    
    @Mock
    private OreMarketDataGenerator marketDataGenerator;
    
    @Mock
    private OreTodaysMarketGenerator todaysMarketGenerator;
    
    @Mock
    private OreCurveConfigGenerator curveConfigGenerator;
    
    @Mock
    private TradeDataService tradeDataService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        oreInputBuilder = new OreInputBuilder(portfolioGenerator, marketDataGenerator, 
            todaysMarketGenerator, curveConfigGenerator, tradeDataService);
        
        // Set up default mock behaviors
        when(marketDataGenerator.generateMarketData(any(), any())).thenReturn("mock market data");
        when(todaysMarketGenerator.generateTodaysMarket(any())).thenReturn("mock todays market");
        when(curveConfigGenerator.generateCurveConfig(any())).thenReturn("mock curve config");
        when(portfolioGenerator.generatePortfolioXml(any())).thenReturn("mock portfolio");
        when(tradeDataService.fetchCDSTradeData(any())).thenReturn(createMockTradeData());
    }
    
    private OrePortfolioGenerator.CDSTradeData createMockTradeData() {
        return new OrePortfolioGenerator.CDSTradeData(
            1L,
            "TEST_ENTITY",
            new BigDecimal("1000000"),
            new BigDecimal("0.01"),
            LocalDate.of(2025, 12, 31),
            LocalDate.now(),
            "USD",
            "QUARTERLY",
            "ACT/360",
            "BUY",
            "US"
        );
    }
    
    @Test
    @Feature("Risk Engine Service")
    @Story("ORE Input Building - Build Risk Calculation Input")
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
    @Feature("Risk Engine Service")
    @Story("ORE Input Building - Build Health Check Input")
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
    @Feature("Risk Engine Service")
    @Story("ORE Input Building - Build With Null Date")
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