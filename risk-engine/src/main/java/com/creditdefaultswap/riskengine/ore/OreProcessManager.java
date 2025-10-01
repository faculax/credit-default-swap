package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class OreProcessManager {
    
    private static final Logger logger = LoggerFactory.getLogger(OreProcessManager.class);
    
    private final RiskEngineConfigProperties config;
    private final AtomicReference<Process> currentProcess = new AtomicReference<>();
    private final AtomicInteger restartCount = new AtomicInteger(0);
    private volatile boolean isWarmingUp = false;
    
    @Autowired
    public OreProcessManager(RiskEngineConfigProperties config) {
        this.config = config;
    }
    
    /**
     * Ensures ORE process is running and warmed up
     */
    public CompletableFuture<Boolean> ensureProcessReady() {
        return CompletableFuture.supplyAsync(() -> {
            if (isProcessHealthy()) {
                return true;
            }
            
            logger.info("ORE process not healthy, starting new process");
            return startProcess();
        });
    }
    
    /**
     * Starts a new ORE process
     */
    private boolean startProcess() {
        if (restartCount.get() >= config.getOre().getMaxRestarts()) {
            logger.error("Maximum restart attempts ({}) exceeded for ORE process", 
                config.getOre().getMaxRestarts());
            return false;
        }
        
        try {
            // Stop existing process if any
            stopProcess();
            
            // Create work directory if it doesn't exist
            Path workDir = Paths.get(config.getOre().getWorkDir());
            Files.createDirectories(workDir);
            
            // Start new process
            ProcessBuilder processBuilder = new ProcessBuilder(
                config.getOre().getBinaryPath(),
                "--config", config.getOre().getConfigPath(),
                "--workdir", config.getOre().getWorkDir()
            );
            
            processBuilder.directory(workDir.toFile());
            processBuilder.redirectErrorStream(true);
            
            logger.info("Starting ORE process: {}", String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            currentProcess.set(process);
            
            // Start monitoring thread
            startProcessMonitoring(process);
            
            // Wait for warmup
            if (waitForWarmup()) {
                restartCount.set(0); // Reset restart count on successful start
                logger.info("ORE process started successfully");
                return true;
            } else {
                logger.error("ORE process failed to warm up");
                stopProcess();
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to start ORE process", e);
            restartCount.incrementAndGet();
            return false;
        }
    }
    
    /**
     * Waits for ORE process to warm up
     */
    private boolean waitForWarmup() {
        isWarmingUp = true;
        try {
            // Simple warmup strategy: wait for process to be responsive
            // In a real implementation, this might involve sending a test calculation
            Thread.sleep(config.getOre().getWarmupTimeoutSeconds() * 1000L);
            
            Process process = currentProcess.get();
            if (process != null && process.isAlive()) {
                logger.info("ORE process warmed up successfully");
                return true;
            } else {
                logger.error("ORE process died during warmup");
                return false;
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Warmup interrupted", e);
            return false;
        } finally {
            isWarmingUp = false;
        }
    }
    
    /**
     * Starts monitoring thread for the process
     */
    private void startProcessMonitoring(Process process) {
        CompletableFuture.runAsync(() -> {
            try {
                int exitCode = process.waitFor();
                logger.warn("ORE process exited with code: {}", exitCode);
                
                // Auto-restart if the process dies unexpectedly
                if (restartCount.get() < config.getOre().getMaxRestarts()) {
                    logger.info("Attempting to restart ORE process in {} seconds", 
                        config.getOre().getRestartDelaySeconds());
                    
                    Thread.sleep(config.getOre().getRestartDelaySeconds() * 1000L);
                    restartCount.incrementAndGet();
                    startProcess();
                } else {
                    logger.error("ORE process died and maximum restarts exceeded");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Process monitoring interrupted");
            } catch (Exception e) {
                logger.error("Error in process monitoring", e);
            }
        });
    }
    
    /**
     * Checks if the current process is healthy
     */
    public boolean isProcessHealthy() {
        Process process = currentProcess.get();
        return process != null && process.isAlive() && !isWarmingUp;
    }
    
    /**
     * Executes a calculation with the ORE process
     */
    public CompletableFuture<String> executeCalculation(String inputXml) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isProcessHealthy()) {
                throw new RuntimeException("ORE process is not healthy");
            }
            
            try {
                Process process = currentProcess.get();
                
                // Write input XML to process stdin
                process.getOutputStream().write(inputXml.getBytes());
                process.getOutputStream().flush();
                
                // Read output with timeout
                boolean finished = process.waitFor(config.getOre().getTimeoutSeconds(), TimeUnit.SECONDS);
                if (!finished) {
                    logger.error("ORE calculation timed out after {} seconds", 
                        config.getOre().getTimeoutSeconds());
                    throw new RuntimeException("ORE calculation timeout");
                }
                
                // Read the output
                String output = new String(process.getInputStream().readAllBytes());
                logger.debug("ORE calculation completed, output length: {}", output.length());
                
                return output;
                
            } catch (Exception e) {
                logger.error("Error executing ORE calculation", e);
                throw new RuntimeException("ORE calculation failed", e);
            }
        });
    }
    
    /**
     * Stops the current ORE process
     */
    public void stopProcess() {
        Process process = currentProcess.getAndSet(null);
        if (process != null) {
            logger.info("Stopping ORE process");
            process.destroyForcibly();
            try {
                boolean terminated = process.waitFor(5, TimeUnit.SECONDS);
                if (!terminated) {
                    logger.warn("ORE process did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Gets current process status
     */
    public ProcessStatus getStatus() {
        Process process = currentProcess.get();
        if (process == null) {
            return new ProcessStatus(false, false, restartCount.get());
        }
        
        return new ProcessStatus(
            process.isAlive(),
            isWarmingUp,
            restartCount.get()
        );
    }
    
    public static class ProcessStatus {
        private final boolean alive;
        private final boolean warmingUp;
        private final int restartCount;
        
        public ProcessStatus(boolean alive, boolean warmingUp, int restartCount) {
            this.alive = alive;
            this.warmingUp = warmingUp;
            this.restartCount = restartCount;
        }
        
        public boolean isAlive() {
            return alive;
        }
        
        public boolean isWarmingUp() {
            return warmingUp;
        }
        
        public int getRestartCount() {
            return restartCount;
        }
        
        public boolean isReady() {
            return alive && !warmingUp;
        }
    }
}