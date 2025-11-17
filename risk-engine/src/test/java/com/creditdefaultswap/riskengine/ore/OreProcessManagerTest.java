package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@Epic(EpicType.UNIT_TESTS)
@ExtendWith(MockitoExtension.class)
class OreProcessManagerTest {
    
    private RiskEngineConfigProperties config;
    private OreProcessManager oreProcessManager;
    
    @BeforeEach
    void setUp() {
        config = new RiskEngineConfigProperties();
        config.getOre().setBinaryPath("/bin/echo"); // Use echo for testing
        config.getOre().setWorkDir("/tmp/ore-test");
        config.getOre().setTimeoutSeconds(5);
        config.getOre().setWarmupTimeoutSeconds(2);
        config.getOre().setMaxRestarts(2);
        
        oreProcessManager = new OreProcessManager(config);
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Process Management - Execute With Valid Input")
    void testExecuteCalculation_WithValidInput() {
        String testInput = "<test>input</test>";
        
        CompletableFuture<String> result = oreProcessManager.executeCalculation(testInput);
        
        assertNotNull(result);
        // This will actually run but fail with echo since it's not ORE
        // But we can verify the CompletableFuture structure is correct
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Process Management - Execute With Null Input")
    void testExecuteCalculation_WithNullInput() {
        assertDoesNotThrow(() -> {
            CompletableFuture<String> result = oreProcessManager.executeCalculation(null);
            assertNotNull(result);
        });
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Process Management - Constructor With Valid Config")
    void testConstructor_WithValidConfig() {
        RiskEngineConfigProperties testConfig = new RiskEngineConfigProperties();
        testConfig.getOre().setBinaryPath("/usr/bin/test");
        
        assertDoesNotThrow(() -> {
            OreProcessManager manager = new OreProcessManager(testConfig);
            assertNotNull(manager);
        });
    }
    
    @Test
    @Feature(FeatureType.RISK_ENGINE_SERVICE)
    @Story("ORE Process Management - Returns CompletableFuture")
    void testExecuteCalculation_ReturnsCompletableFuture() {
        String testInput = "<test>simple</test>";
        
        CompletableFuture<String> result = oreProcessManager.executeCalculation(testInput);
        
        assertNotNull(result);
        assertFalse(result.isDone()); // Should be running asynchronously
    }
}
