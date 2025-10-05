package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class OreProcessManager {
    
    private static final Logger logger = LoggerFactory.getLogger(OreProcessManager.class);
    
    private final RiskEngineConfigProperties config;
    
    @Autowired
    public OreProcessManager(RiskEngineConfigProperties config) {
        this.config = config;
    }
    
    /**
     * Executes a calculation with ORE in batch mode
     */
    public CompletableFuture<String> executeCalculation(String workingDirPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Executing ORE batch calculation");
                
                // Use the dynamic config file written to working directory
                Path workingDir = Paths.get(workingDirPath);
                Path configPath = workingDir.resolve("ore.xml");
                String configFile = configPath.getFileName().toString();
                
                ProcessBuilder processBuilder = new ProcessBuilder(
                    config.getOre().getBinaryPath(),
                    configFile
                );
                
                processBuilder.directory(workingDir.toFile());
                processBuilder.redirectErrorStream(true);
                
                logger.info("ORE Command: {} {} (working dir: {})", 
                    config.getOre().getBinaryPath(), configFile, workingDir);
                logger.info("Using dynamic ORE config: {}, Timeout: {}s", 
                    configPath, config.getOre().getTimeoutSeconds());
                
                Process process = processBuilder.start();
                
                // Wait for completion with timeout
                boolean finished = process.waitFor(config.getOre().getTimeoutSeconds(), TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new RuntimeException("ORE calculation timed out after " + 
                        config.getOre().getTimeoutSeconds() + " seconds");
                }
                
                int exitCode = process.exitValue();
                String output = new String(process.getInputStream().readAllBytes());
                
                if (exitCode == 0) {
                    // Extract key metrics from ORE output
                    String runtime = extractRuntime(output);
                    String analytics = extractAnalytics(output);
                    logger.info("ORE completed successfully - Runtime: {}, Analytics: {}", runtime, analytics);
                    logger.debug("ORE detailed output: {}", output);
                    return output;
                } else {
                    logger.error("ORE calculation failed with exit code: {}", exitCode);
                    logger.error("ORE output: {}", output);
                    throw new RuntimeException("ORE calculation failed with exit code: " + exitCode);
                }
                
            } catch (Exception e) {
                logger.error("Failed to execute ORE calculation", e);
                throw new RuntimeException("ORE calculation execution failed", e);
            }
        });
    }
    
    /**
     * Extracts runtime from ORE output
     */
    private String extractRuntime(String output) {
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("run time:")) {
                return line.trim();
            }
        }
        return "unknown";
    }
    
    /**
     * Extracts analytics information from ORE output
     */
    private String extractAnalytics(String output) {
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("Requested analytics")) {
                return line.split("Requested analytics")[1].trim();
            }
        }
        return "unknown";
    }
}