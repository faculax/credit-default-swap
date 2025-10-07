package com.creditdefaultswap.platform.simulation;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggregates simulation results across Monte Carlo paths
 */
public class SimulationResult {
    
    private final int numPaths;
    private final int numEntities;
    private final int numHorizons;
    
    // Storage for per-path results
    private final double[][] lossesPerHorizon;  // [horizon][path]
    private final int[][] defaultCountsPerHorizon;  // [horizon][path]
    private final boolean[][] anyDefaultPerHorizon;  // [horizon][path]
    
    // Per-entity loss tracking for marginal calculations
    private final double[][][] entityLossesPerHorizon;  // [horizon][entity][path]
    
    // Metadata
    private final double[] horizonYears;
    private final String[] entityNames;
    private final double[] betas;
    private final double[] notionals;
    private final double[] recoveries;
    
    public SimulationResult(int numPaths, int numEntities, int numHorizons, 
                           double[] horizonYears, String[] entityNames, 
                           double[] betas, double[] notionals, double[] recoveries) {
        this.numPaths = numPaths;
        this.numEntities = numEntities;
        this.numHorizons = numHorizons;
        this.horizonYears = horizonYears;
        this.entityNames = entityNames;
        this.betas = betas;
        this.notionals = notionals;
        this.recoveries = recoveries;
        
        // Initialize arrays
        this.lossesPerHorizon = new double[numHorizons][numPaths];
        this.defaultCountsPerHorizon = new int[numHorizons][numPaths];
        this.anyDefaultPerHorizon = new boolean[numHorizons][numPaths];
        this.entityLossesPerHorizon = new double[numHorizons][numEntities][numPaths];
    }
    
    /**
     * Record results for a single path
     */
    public void recordPath(int pathIndex, double[] defaultTimes) {
        for (int h = 0; h < numHorizons; h++) {
            double horizon = horizonYears[h];
            double totalLoss = 0.0;
            int defaultCount = 0;
            boolean anyDefault = false;
            
            for (int i = 0; i < numEntities; i++) {
                if (defaultTimes[i] <= horizon) {
                    double lgd = 1.0 - recoveries[i];
                    double loss = notionals[i] * lgd;
                    totalLoss += loss;
                    defaultCount++;
                    anyDefault = true;
                    
                    entityLossesPerHorizon[h][i][pathIndex] = loss;
                } else {
                    entityLossesPerHorizon[h][i][pathIndex] = 0.0;
                }
            }
            
            lossesPerHorizon[h][pathIndex] = totalLoss;
            defaultCountsPerHorizon[h][pathIndex] = defaultCount;
            anyDefaultPerHorizon[h][pathIndex] = anyDefault;
        }
    }
    
    /**
     * Calculate all metrics for a specific horizon
     */
    public HorizonMetrics calculateHorizonMetrics(int horizonIndex) {
        HorizonMetrics metrics = new HorizonMetrics();
        
        double[] losses = lossesPerHorizon[horizonIndex];
        
        // Calculate loss metrics
        metrics.lossMean = MetricsCalculator.calculateMean(losses);
        metrics.lossVar95 = MetricsCalculator.calculateVaR(losses, 0.95);
        metrics.lossVar99 = MetricsCalculator.calculateVaR(losses, 0.99);
        metrics.lossEs975 = MetricsCalculator.calculateES(losses, 0.975);
        
        // Calculate default metrics
        int anyDefaultCount = 0;
        int totalDefaultCount = 0;
        for (int p = 0; p < numPaths; p++) {
            if (anyDefaultPerHorizon[horizonIndex][p]) {
                anyDefaultCount++;
            }
            totalDefaultCount += defaultCountsPerHorizon[horizonIndex][p];
        }
        
        metrics.pAnyDefault = (double) anyDefaultCount / numPaths;
        metrics.expectedDefaults = (double) totalDefaultCount / numPaths;
        
        // Calculate standalone ELs and marginal contributions
        double sumStandaloneEl = 0.0;
        Map<String, Double> marginalContributions = new HashMap<>();
        
        for (int i = 0; i < numEntities; i++) {
            double[] entityLosses = entityLossesPerHorizon[horizonIndex][i];
            double standaloneEl = MetricsCalculator.calculateMean(entityLosses);
            sumStandaloneEl += standaloneEl;
            
            // Marginal contribution is just the entity's EL as percentage of total
            marginalContributions.put(entityNames[i], standaloneEl);
        }
        
        metrics.sumStandaloneEl = sumStandaloneEl;
        metrics.portfolioEl = metrics.lossMean;  // Expected loss
        
        // Calculate diversification benefit
        metrics.diversificationBenefitPct = MetricsCalculator.calculateDiversificationBenefit(
            sumStandaloneEl, metrics.portfolioEl);
        
        // Calculate marginal EL percentages
        metrics.marginalElPcts = new HashMap<>();
        for (Map.Entry<String, Double> entry : marginalContributions.entrySet()) {
            double pct = sumStandaloneEl > 0 
                ? (entry.getValue() / sumStandaloneEl * 100.0) 
                : 0.0;
            metrics.marginalElPcts.put(entry.getKey(), pct);
        }
        
        return metrics;
    }
    
    public double[] getBetas() {
        return betas;
    }
    
    public String[] getEntityNames() {
        return entityNames;
    }
    
    public int getNumHorizons() {
        return numHorizons;
    }
    
    public double[] getHorizonYears() {
        return horizonYears;
    }
    
    /**
     * Container for horizon-level metrics
     */
    public static class HorizonMetrics {
        public double pAnyDefault;
        public double expectedDefaults;
        public double lossMean;
        public double lossVar95;
        public double lossVar99;
        public double lossEs975;
        public double sumStandaloneEl;
        public double portfolioEl;
        public double diversificationBenefitPct;
        public Map<String, Double> marginalElPcts;
    }
}
