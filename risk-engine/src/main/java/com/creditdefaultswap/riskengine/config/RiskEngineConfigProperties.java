package com.creditdefaultswap.riskengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "risk")
public class RiskEngineConfigProperties {
    
    public enum Implementation {
        STUB, ORE
    }
    
    private Implementation implementation = Implementation.STUB;
    private final Ore ore = new Ore();
    
    public Implementation getImplementation() {
        return implementation;
    }
    
    public void setImplementation(Implementation implementation) {
        this.implementation = implementation;
    }
    
    public Ore getOre() {
        return ore;
    }
    
    public static class Ore {
        private String binaryPath = "/app/ore/bin/ore";
        private String configPath = "/app/ore/config/ore.xml";
        private String workDir = "/tmp/ore-work";
        private int timeoutSeconds = 10;
        private int warmupTimeoutSeconds = 30;
        private int restartDelaySeconds = 5;
        private int maxRestarts = 3;
        
        public String getBinaryPath() {
            return binaryPath;
        }
        
        public void setBinaryPath(String binaryPath) {
            this.binaryPath = binaryPath;
        }
        
        public String getConfigPath() {
            return configPath;
        }
        
        public void setConfigPath(String configPath) {
            this.configPath = configPath;
        }
        
        public String getWorkDir() {
            return workDir;
        }
        
        public void setWorkDir(String workDir) {
            this.workDir = workDir;
        }
        
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }
        
        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
        
        public int getWarmupTimeoutSeconds() {
            return warmupTimeoutSeconds;
        }
        
        public void setWarmupTimeoutSeconds(int warmupTimeoutSeconds) {
            this.warmupTimeoutSeconds = warmupTimeoutSeconds;
        }
        
        public int getRestartDelaySeconds() {
            return restartDelaySeconds;
        }
        
        public void setRestartDelaySeconds(int restartDelaySeconds) {
            this.restartDelaySeconds = restartDelaySeconds;
        }
        
        public int getMaxRestarts() {
            return maxRestarts;
        }
        
        public void setMaxRestarts(int maxRestarts) {
            this.maxRestarts = maxRestarts;
        }
    }
}