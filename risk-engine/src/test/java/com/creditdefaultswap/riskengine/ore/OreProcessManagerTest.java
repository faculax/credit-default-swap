package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OreProcessManagerTest {
    
    private RiskEngineConfigProperties config;
    private OreProcessManager oreProcessManager;
    
    @BeforeEach
    void setUp() {
        config = new RiskEngineConfigProperties();
        config.setImplementation(RiskEngineConfigProperties.Implementation.ORE);
        config.getOre().setBinaryPath("/bin/echo"); // Use echo for testing
        config.getOre().setWorkDir("/tmp/ore-test");
        config.getOre().setTimeoutSeconds(5);
        config.getOre().setWarmupTimeoutSeconds(2);
        config.getOre().setMaxRestarts(2);
        
        oreProcessManager = new OreProcessManager(config);
    }
    
    @Test
    void testGetStatus_WhenNoProcess() {
        OreProcessManager.ProcessStatus status = oreProcessManager.getStatus();
        
        assertFalse(status.isAlive());
        assertFalse(status.isWarmingUp());
        assertFalse(status.isReady());
        assertEquals(0, status.getRestartCount());
    }
    
    @Test
    void testIsProcessHealthy_WhenNoProcess() {
        assertFalse(oreProcessManager.isProcessHealthy());
    }
    
    @Test
    void testEnsureProcessReady_FailsWithInvalidBinary() {
        config.getOre().setBinaryPath("/invalid/path/to/ore");
        oreProcessManager = new OreProcessManager(config);
        
        Boolean result = oreProcessManager.ensureProcessReady().join();
        
        assertFalse(result);
    }
    
    @Test
    void testExecuteCalculation_WhenProcessNotHealthy() {
        assertThrows(RuntimeException.class, () -> {
            oreProcessManager.executeCalculation("<test>input</test>").join();
        });
    }
}