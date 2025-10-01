package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Auto-starts the ORE process on application startup when implementation is ORE.
 * This ensures the /status endpoint reports an alive process without waiting for
 * the first calculation call to lazily trigger startup.
 */
@Component
public class OreAutoStarter {

    private static final Logger logger = LoggerFactory.getLogger(OreAutoStarter.class);

    private final RiskEngineConfigProperties config;
    private final OreProcessManager oreProcessManager;

    public OreAutoStarter(RiskEngineConfigProperties config, OreProcessManager oreProcessManager) {
        this.config = config;
        this.oreProcessManager = oreProcessManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (config.getImplementation() == RiskEngineConfigProperties.Implementation.ORE) {
            logger.info("Risk implementation=ORE -> triggering ORE process auto-start");
            oreProcessManager.ensureProcessReady().thenAccept(started -> {
                if (started) {
                    logger.info("ORE process auto-start complete (status: alive={} ready={})", 
                        oreProcessManager.getStatus().isAlive(), oreProcessManager.getStatus().isReady());
                } else {
                    logger.warn("ORE process auto-start failed; system will fallback to STUB on calculations until manual recovery");
                }
            });
        } else {
            logger.info("Risk implementation is {} -> skipping ORE auto-start", config.getImplementation());
        }
    }
}
