package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import com.creditdefaultswap.riskengine.model.RiskMeasures;
import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.ore.OreInputBuilder;
import com.creditdefaultswap.riskengine.ore.OreOutputParser;
import com.creditdefaultswap.riskengine.ore.OreProcessManager;
import com.creditdefaultswap.riskengine.ore.OrePortfolioGenerator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Epic(EpicType.UNIT_TESTS)
@ExtendWith(MockitoExtension.class)
public class RiskCalculationServiceTest {

    @Mock
    private RiskEngineConfigProperties config;
    
    @Mock
    private OreProcessManager oreProcessManager;
    
    @Mock
    private OreInputBuilder oreInputBuilder;
    
    @Mock
    private OreOutputParser oreOutputParser;
    
    @Mock
    private TradeDataService tradeDataService;

    private RiskCalculationService service;

    @BeforeEach
    void setUp() {
        service = new RiskCalculationService(config, oreProcessManager, oreInputBuilder, 
                                           oreOutputParser, tradeDataService);
    }

    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("Risk Calculation - ORE Success")
    void testCalculateRiskMeasures_OreSuccess() {
        // Arrange
        ScenarioRequest request = new ScenarioRequest();
        request.setScenarioId("TEST_SCENARIO");
        request.setTradeIds(List.of(1L, 2L));
        
        String oreInput = "<ORE>test input</ORE>";
        String oreOutput = "<ORE>test output</ORE>";
        
        when(oreInputBuilder.buildRiskCalculationInput(request)).thenReturn(oreInput);
        when(oreProcessManager.executeCalculation(oreInput)).thenReturn(CompletableFuture.completedFuture(oreOutput));
        when(oreOutputParser.isValidOutput(oreOutput)).thenReturn(true);
        
        // Mock trade data
        OrePortfolioGenerator.CDSTradeData tradeData1 = createMockTradeData(1L, "USD");
        OrePortfolioGenerator.CDSTradeData tradeData2 = createMockTradeData(2L, "EUR");
        when(tradeDataService.fetchCDSTradeData(1L)).thenReturn(tradeData1);
        when(tradeDataService.fetchCDSTradeData(2L)).thenReturn(tradeData2);
        
        // Mock parsed risk measures
        RiskMeasures measures1 = createMockRiskMeasures(1L, "USD");
        RiskMeasures measures2 = createMockRiskMeasures(2L, "EUR");
        when(oreOutputParser.parseRiskMeasures(oreOutput, 1L, "USD")).thenReturn(measures1);
        when(oreOutputParser.parseRiskMeasures(oreOutput, 2L, "EUR")).thenReturn(measures2);
        
        // Act
        CompletableFuture<List<RiskMeasures>> future = service.calculateRiskMeasures(request);
        List<RiskMeasures> result = future.join();
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getTradeId());
        assertEquals(2L, result.get(1).getTradeId());
        assertEquals("USD", result.get(0).getCurrency());
        assertEquals("EUR", result.get(1).getCurrency());
        
        verify(oreInputBuilder).buildRiskCalculationInput(request);
        verify(oreProcessManager).executeCalculation(oreInput);
        verify(oreOutputParser).isValidOutput(oreOutput);
        verify(tradeDataService).fetchCDSTradeData(1L);
        verify(tradeDataService).fetchCDSTradeData(2L);
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("Risk Calculation - ORE Invalid Output")
    void testCalculateRiskMeasures_OreInvalidOutput() {
        // Arrange
        ScenarioRequest request = new ScenarioRequest();
        request.setScenarioId("TEST_SCENARIO");
        request.setTradeIds(List.of(1L));
        
        String oreInput = "<ORE>test input</ORE>";
        String oreOutput = "<ORE>error output</ORE>";
        String errorMessage = "ORE calculation failed: invalid input";
        
        when(oreInputBuilder.buildRiskCalculationInput(request)).thenReturn(oreInput);
        when(oreProcessManager.executeCalculation(oreInput)).thenReturn(CompletableFuture.completedFuture(oreOutput));
        when(oreOutputParser.isValidOutput(oreOutput)).thenReturn(false);
        when(oreOutputParser.extractErrorMessage(oreOutput)).thenReturn(errorMessage);
        
        // Act & Assert
        CompletableFuture<List<RiskMeasures>> future = service.calculateRiskMeasures(request);
        
        CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertTrue(exception.getCause().getMessage().contains("ORE calculation failed"));
        
        verify(oreOutputParser).isValidOutput(oreOutput);
        verify(oreOutputParser).extractErrorMessage(oreOutput);
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("Risk Calculation - ORE Execution Exception")
    void testCalculateRiskMeasures_OreExecutionException() {
        // Arrange
        ScenarioRequest request = new ScenarioRequest();
        request.setScenarioId("TEST_SCENARIO");
        request.setTradeIds(List.of(1L));
        
        String oreInput = "<ORE>test input</ORE>";
        RuntimeException oreException = new RuntimeException("ORE process failed");
        
        when(oreInputBuilder.buildRiskCalculationInput(request)).thenReturn(oreInput);
        when(oreProcessManager.executeCalculation(oreInput)).thenReturn(CompletableFuture.failedFuture(oreException));
        
        // Act & Assert
        CompletableFuture<List<RiskMeasures>> future = service.calculateRiskMeasures(request);
        
        CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("ORE calculation failed", exception.getCause().getMessage());
        
        verify(oreInputBuilder).buildRiskCalculationInput(request);
        verify(oreProcessManager).executeCalculation(oreInput);
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("Risk Calculation - Get Engine Status")
    void testGetEngineStatus() {
        // Arrange
        RiskEngineConfigProperties.Ore oreConfig = new RiskEngineConfigProperties.Ore();
        oreConfig.setBinaryPath("/test/ore/bin/ore");
        oreConfig.setConfigPath("/test/ore/config/ore.xml");
        oreConfig.setTimeoutSeconds(30);
        
        when(config.getOre()).thenReturn(oreConfig);
        
        // Act
        Map<String, Object> status = service.getEngineStatus();
        
        // Assert
        assertNotNull(status);
        assertEquals("ORE", status.get("implementation"));
        assertEquals("batch", status.get("mode"));
        assertTrue(status.containsKey("ore"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> oreStatus = (Map<String, Object>) status.get("ore");
        assertEquals("/test/ore/bin/ore", oreStatus.get("binaryPath"));
        assertEquals("/test/ore/config/ore.xml", oreStatus.get("configPath"));
        assertEquals(30, oreStatus.get("timeoutSeconds"));
        assertEquals("batch-execution", oreStatus.get("mode"));
    }
    
    private OrePortfolioGenerator.CDSTradeData createMockTradeData(Long tradeId, String currency) {
        return new OrePortfolioGenerator.CDSTradeData(
            tradeId,
            "TEST_ENTITY_" + tradeId,
            new BigDecimal("1000000"),
            new BigDecimal("0.0125"),
            LocalDate.now().plusYears(5),
            LocalDate.now(),
            currency,
            "QUARTERLY",
            "ACT/360",
            "BUY",
            "TARGET"
        );
    }
    
    private RiskMeasures createMockRiskMeasures(Long tradeId, String currency) {
        RiskMeasures measures = new RiskMeasures();
        measures.setTradeId(tradeId);
        measures.setCurrency(currency);
        measures.setNpv(new BigDecimal("50000.00"));
        measures.setDv01(new BigDecimal("100.50"));
        measures.setGamma(new BigDecimal("0.001234"));
        measures.setVar95(new BigDecimal("75000.00"));
        measures.setExpectedShortfall(new BigDecimal("95000.00"));
        return measures;
    }
}

