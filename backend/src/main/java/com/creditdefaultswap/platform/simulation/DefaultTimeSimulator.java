package com.creditdefaultswap.platform.simulation;

import java.util.Random;

/**
 * Default time simulator using Gaussian one-factor copula model
 */
public class DefaultTimeSimulator {
    
    private final Random random;
    private final double[] betas;
    private final double[][] survivalCurves;  // [entity][time points]
    private final double[] timePoints;        // horizons in years
    
    public DefaultTimeSimulator(long seed, double[] betas, double[][] survivalCurves, double[] timePoints) {
        this.random = new Random(seed);
        this.betas = betas;
        this.survivalCurves = survivalCurves;
        this.timePoints = timePoints;
    }
    
    /**
     * Generate default times for all entities in one path
     * @return default times in years for each entity
     */
    public double[] generateDefaultTimes() {
        int numEntities = betas.length;
        double[] defaultTimes = new double[numEntities];
        
        // Draw systemic factor Z ~ N(0,1)
        double Z = random.nextGaussian();
        
        for (int i = 0; i < numEntities; i++) {
            // Draw idiosyncratic factor ε_i ~ N(0,1)
            double epsilon = random.nextGaussian();
            
            // Compute latent variable: X_i = β_i * Z + sqrt(1 - β_i^2) * ε_i
            double beta = betas[i];
            double Xi = beta * Z + Math.sqrt(1 - beta * beta) * epsilon;
            
            // Transform to uniform: U_i = Φ(X_i)
            double Ui = cumulativeNormalDistribution(Xi);
            
            // Invert survival curve to get default time
            defaultTimes[i] = invertSurvivalCurve(i, Ui);
        }
        
        return defaultTimes;
    }
    
    /**
     * Check if entity defaulted by given horizon
     */
    public boolean[] checkDefaults(double[] defaultTimes, double horizon) {
        boolean[] defaults = new boolean[defaultTimes.length];
        
        for (int i = 0; i < defaultTimes.length; i++) {
            defaults[i] = defaultTimes[i] <= horizon;
        }
        
        return defaults;
    }
    
    /**
     * Cumulative standard normal distribution using approximation
     */
    private double cumulativeNormalDistribution(double x) {
        // Using error function approximation
        // Φ(x) = 0.5 * (1 + erf(x / sqrt(2)))
        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
    }
    
    /**
     * Error function approximation (Abramowitz and Stegun)
     */
    private double erf(double x) {
        // Constants
        double a1 =  0.254829592;
        double a2 = -0.284496736;
        double a3 =  1.421413741;
        double a4 = -1.453152027;
        double a5 =  1.061405429;
        double p  =  0.3275911;
        
        // Save the sign of x
        int sign = (x < 0) ? -1 : 1;
        x = Math.abs(x);
        
        // A&S formula 7.1.26
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        
        return sign * y;
    }
    
    /**
     * Invert survival curve to find default time
     * Given uniform U, find smallest t such that S(t) <= U
     * In the copula model: if U > S(t), then default by time t
     */
    private double invertSurvivalCurve(int entityIndex, double uniformValue) {
        double[] survivalValues = survivalCurves[entityIndex];
        
        // If uniform value is very high (U > all survival probs), return large time (no default)
        if (uniformValue > survivalValues[0]) {
            return Double.MAX_VALUE;  // No default within horizon (high survival)
        }
        
        // Find first time point where survival probability drops below uniform value
        // Default occurs when U > S(t), i.e., when S(t) < U
        for (int i = 0; i < timePoints.length; i++) {
            double survivalProb = survivalValues[i];
            
            if (survivalProb <= uniformValue) {
                // Linear interpolation between time points
                if (i == 0) {
                    // Default happens between 0 and first time point
                    // S(0) = 1.0, S(t1) = survivalProb
                    double fraction = (1.0 - uniformValue) / (1.0 - survivalProb);
                    return timePoints[0] * fraction;
                } else {
                    double prevSurvivalProb = survivalValues[i - 1];
                    double fraction = (prevSurvivalProb - uniformValue) / (prevSurvivalProb - survivalProb);
                    return timePoints[i - 1] + fraction * (timePoints[i] - timePoints[i - 1]);
                }
            }
        }
        
        return Double.MAX_VALUE;  // No default
    }
}
