package com.creditdefaultswap.platform.dto;

/**
 * DTO for convergence diagnostics in basket pricing
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public class ConvergenceDiagnostics {
    
    private Integer pathsUsed;
    private Double standardErrorFairSpreadBps;
    private Integer iterations;
    private Boolean converged;
    private String convergenceMessage;
    
    // Constructors
    public ConvergenceDiagnostics() {}
    
    public ConvergenceDiagnostics(Integer pathsUsed, Integer iterations, Boolean converged) {
        this.pathsUsed = pathsUsed;
        this.iterations = iterations;
        this.converged = converged;
    }
    
    // Getters and Setters
    public Integer getPathsUsed() {
        return pathsUsed;
    }
    
    public void setPathsUsed(Integer pathsUsed) {
        this.pathsUsed = pathsUsed;
    }
    
    public Double getStandardErrorFairSpreadBps() {
        return standardErrorFairSpreadBps;
    }
    
    public void setStandardErrorFairSpreadBps(Double standardErrorFairSpreadBps) {
        this.standardErrorFairSpreadBps = standardErrorFairSpreadBps;
    }
    
    public Integer getIterations() {
        return iterations;
    }
    
    public void setIterations(Integer iterations) {
        this.iterations = iterations;
    }
    
    public Boolean getConverged() {
        return converged;
    }
    
    public void setConverged(Boolean converged) {
        this.converged = converged;
    }
    
    public String getConvergenceMessage() {
        return convergenceMessage;
    }
    
    public void setConvergenceMessage(String convergenceMessage) {
        this.convergenceMessage = convergenceMessage;
    }
}
