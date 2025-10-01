package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import com.creditdefaultswap.riskengine.model.RiskMeasures;
import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.ore.OreInputBuilder;
import com.creditdefaultswap.riskengine.ore.OreOutputParser;
import com.creditdefaultswap.riskengine.ore.OreProcessManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    private RiskCalculationService service;

    @BeforeEach
    void setUp() {
        service = new RiskCalculationService(config, oreProcessManager, oreInputBuilder, oreOutputParser);
    }

    // Legacy tests
    @Test
    void testBaseCalculationNotNull() {
        RiskMeasures measures = service.calculateBase(123L);
        assertNotNull(measures.getPvClean());
        assertNotNull(measures.getParSpread());
        assertNotNull(measures.getTradeId());
        assertEquals(123L, measures.getTradeId());
    }

    @Test
    void testParallelShiftAdjustsSpread() {
        RiskMeasures base = service.calculateBase(5L);
        RiskMeasures shifted = service.shiftParallel(base, 10);
        assertEquals(base.getParSpread().add(java.math.BigDecimal.valueOf(10)), shifted.getParSpread());
        assertEquals(5L, shifted.getTradeId());
    }
    
    // New ORE integration tests
    @Test
    void testCalculateRiskMeasures_WithStubImplementation() {
        when(config.getImplementation()).thenReturn(RiskEngineConfigProperties.Implementation.STUB);
        
        ScenarioRequest request = new ScenarioRequest();
        request.setScenarioId("TEST_SCENARIO");
        request.setTradeIds(List.of(1L, 2L));
        
        CompletableFuture<List<RiskMeasures>> future = service.calculateRiskMeasures(request);
        List<RiskMeasures> result = future.join();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getTradeId());
        assertEquals(2L, result.get(1).getTradeId());
        
        verify(oreProcessManager, never()).ensureProcessReady();
    }
    
    @Test
    void testCalculateRiskMeasures_WithOreImplementation_Success() {
        when(config.getImplementation()).thenReturn(RiskEngineConfigProperties.Implementation.ORE);
        when(oreProcessManager.ensureProcessReady()).thenReturn(CompletableFuture.completedFuture(true));
        when(oreInputBuilder.buildRiskCalculationInput(any())).thenReturn("<ORE>test input</ORE>");
        when(oreProcessManager.executeCalculation(anyString())).thenReturn(CompletableFuture.completedFuture("<ORE>test output</ORE>"));
        when(oreOutputParser.isValidOutput(anyString())).thenReturn(true);
        
        RiskMeasures mockMeasures1 = new RiskMeasures();
        mockMeasures1.setTradeId(1L);
        RiskMeasures mockMeasures2 = new RiskMeasures();
        mockMeasures2.setTradeId(2L);
        
        when(oreOutputParser.parseRiskMeasures(anyString(), eq(1L))).thenReturn(mockMeasures1);
        when(oreOutputParser.parseRiskMeasures(anyString(), eq(2L))).thenReturn(mockMeasures2);
        
        ScenarioRequest request = new ScenarioRequest();
        request.setScenarioId("TEST_SCENARIO");
        request.setTradeIds(List.of(1L, 2L));
        
        CompletableFuture<List<RiskMeasures>> future = service.calculateRiskMeasures(request);
        List<RiskMeasures> result = future.join();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getTradeId());
        assertEquals(2L, result.get(1).getTradeId());
        
        verify(oreProcessManager).ensureProcessReady();
        verify(oreInputBuilder).buildRiskCalculationInput(request);
        verify(oreProcessManager).executeCalculation("<ORE>test input</ORE>");
        verify(oreOutputParser).isValidOutput("<ORE>test output</ORE>");
    }
    
    @Test
    void testCalculateRiskMeasures_WithOreImplementation_ProcessNotReady() {
        when(config.getImplementation()).thenReturn(RiskEngineConfigProperties.Implementation.ORE);
        when(oreProcessManager.ensureProcessReady()).thenReturn(CompletableFuture.completedFuture(false));
        
        ScenarioRequest request = new ScenarioRequest();
        request.setScenarioId("TEST_SCENARIO");
        request.setTradeIds(List.of(1L));
        
        CompletableFuture<List<RiskMeasures>> future = service.calculateRiskMeasures(request);
        List<RiskMeasures> result = future.join();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getTradeId());
        
        verify(oreProcessManager).ensureProcessReady();
        verify(oreInputBuilder, never()).buildRiskCalculationInput(any());
    }
    
    @Test
    void testGetEngineStatus_StubImplementation() {
        when(config.getImplementation()).thenReturn(RiskEngineConfigProperties.Implementation.STUB);
        
        Map<String, Object> status = service.getEngineStatus();
        
        assertNotNull(status);
        assertEquals("STUB", status.get("implementation"));
        assertFalse(status.containsKey("oreProcess"));
    }
    
    @Test
    void testGetEngineStatus_OreImplementation() {
        when(config.getImplementation()).thenReturn(RiskEngineConfigProperties.Implementation.ORE);
        
        OreProcessManager.ProcessStatus processStatus = new OreProcessManager.ProcessStatus(true, false, 0);
        when(oreProcessManager.getStatus()).thenReturn(processStatus);
        
        Map<String, Object> status = service.getEngineStatus();
        
        assertNotNull(status);
        assertEquals("ORE", status.get("implementation"));
        assertTrue(status.containsKey("oreProcess"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> oreStatus = (Map<String, Object>) status.get("oreProcess");
        assertTrue((Boolean) oreStatus.get("alive"));
        assertTrue((Boolean) oreStatus.get("ready"));
        assertFalse((Boolean) oreStatus.get("warmingUp"));
        assertEquals(0, oreStatus.get("restartCount"));
    }
}
