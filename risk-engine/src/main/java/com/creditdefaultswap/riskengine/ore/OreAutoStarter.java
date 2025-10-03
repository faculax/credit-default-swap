package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Validates ORE setup on application startup.
 * This ensures ORE binary and configuration are available for batch calculations.
 */
@Component
public class OreAutoStarter {

    private static final Logger logger = LoggerFactory.getLogger(OreAutoStarter.class);

    private final RiskEngineConfigProperties config;

    public OreAutoStarter(RiskEngineConfigProperties config) {
        this.config = config;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        logger.info("Risk implementation=ORE -> validating ORE setup for batch calculations");
        
        boolean binaryExists = Files.exists(Paths.get(config.getOre().getBinaryPath()));
        boolean configExists = Files.exists(Paths.get(config.getOre().getConfigPath()));
        
        if (binaryExists && configExists) {
            logger.info("ORE validation complete - binary and config found, ready for batch calculations");
            logger.info("ORE binary: {}", config.getOre().getBinaryPath());
            logger.info("ORE config: {}", config.getOre().getConfigPath());
        } else {
            logger.error("ORE validation failed - binary exists: {}, config exists: {}; risk calculations will fail", 
                binaryExists, configExists);
            logger.error("Expected binary: {}", config.getOre().getBinaryPath());
            logger.error("Expected config: {}", config.getOre().getConfigPath());
            throw new IllegalStateException("ORE setup validation failed - missing binary or config files");
        }
    }
}
