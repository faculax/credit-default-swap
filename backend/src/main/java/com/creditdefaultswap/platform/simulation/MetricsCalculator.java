package com.creditdefaultswap.platform.simulation;

import java.util.Arrays;

/**
 * Utility class for calculating risk metrics from loss distributions
 */
public class MetricsCalculator {
    
    /**
     * Calculate Value at Risk at given quantile
     * @param losses Array of loss values from Monte Carlo paths
     * @param quantile VaR quantile (e.g., 0.95 for VaR95)
     * @return VaR value
     */
    public static double calculateVaR(double[] losses, double quantile) {
        if (losses == null || losses.length == 0) {
            return 0.0;
        }
        
        double[] sortedLosses = Arrays.copyOf(losses, losses.length);
        Arrays.sort(sortedLosses);
        
        int index = (int) Math.ceil(quantile * sortedLosses.length) - 1;
        index = Math.max(0, Math.min(index, sortedLosses.length - 1));
        
        return sortedLosses[index];
    }
    
    /**
     * Calculate Expected Shortfall (Conditional VaR) at given quantile
     * @param losses Array of loss values from Monte Carlo paths
     * @param quantile ES quantile (e.g., 0.975 for ES97.5)
     * @return ES value
     */
    public static double calculateES(double[] losses, double quantile) {
        if (losses == null || losses.length == 0) {
            return 0.0;
        }
        
        double[] sortedLosses = Arrays.copyOf(losses, losses.length);
        Arrays.sort(sortedLosses);
        
        int thresholdIndex = (int) Math.ceil(quantile * sortedLosses.length) - 1;
        thresholdIndex = Math.max(0, Math.min(thresholdIndex, sortedLosses.length - 1));
        
        // ES is the mean of losses exceeding VaR
        double sum = 0.0;
        int count = 0;
        for (int i = thresholdIndex; i < sortedLosses.length; i++) {
            sum += sortedLosses[i];
            count++;
        }
        
        return count > 0 ? sum / count : 0.0;
    }
    
    /**
     * Calculate mean of array
     */
    public static double calculateMean(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        
        return sum / values.length;
    }
    
    /**
     * Calculate diversification benefit percentage
     * benefitPct = (sumStandaloneEL - portfolioEL) / sumStandaloneEL * 100
     */
    public static double calculateDiversificationBenefit(double sumStandaloneEl, double portfolioEl) {
        if (sumStandaloneEl == 0.0) {
            return 0.0;
        }
        
        double benefit = (sumStandaloneEl - portfolioEl) / sumStandaloneEl * 100.0;
        
        // Round to 1 decimal place
        return Math.round(benefit * 10.0) / 10.0;
    }
    
    /**
     * Count defaults in a boolean array
     */
    public static int countDefaults(boolean[] defaults) {
        if (defaults == null || defaults.length == 0) {
            return 0;
        }
        
        int count = 0;
        for (boolean defaulted : defaults) {
            if (defaulted) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Check if any entity defaulted
     */
    public static boolean anyDefault(boolean[] defaults) {
        if (defaults == null || defaults.length == 0) {
            return false;
        }
        
        for (boolean defaulted : defaults) {
            if (defaulted) {
                return true;
            }
        }
        
        return false;
    }
}
