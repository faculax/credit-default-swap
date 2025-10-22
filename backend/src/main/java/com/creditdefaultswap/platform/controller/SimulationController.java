package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.dto.SimulationRequest;
import com.creditdefaultswap.platform.dto.SimulationResponse;
import com.creditdefaultswap.platform.service.SimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/credit-simulation")
public class SimulationController {
    
    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);
    
    // Security: Sanitize log parameters to prevent CRLF injection attacks (CWE-117)
    private String sanitizeForLog(Object obj) {
        return obj == null ? "null" : obj.toString().replaceAll("[\r\n]", "_");
    }
    
    @Autowired
    private SimulationService simulationService;
    
    /**
     * POST /api/credit-simulation/portfolio/{portfolioId}
     * Submit a new correlated Monte Carlo simulation
     */
    @PostMapping("/portfolio/{portfolioId}")
    public ResponseEntity<SimulationResponse> runSimulation(
            @PathVariable Long portfolioId,
            @RequestBody SimulationRequest request) {
        
        log.info("Submitting simulation for portfolio {}: {} paths, {} horizons", 
                sanitizeForLog(portfolioId), request.getPaths(), request.getHorizons().size());
        
        try {
            SimulationResponse response = simulationService.submitSimulation(portfolioId, request);
            return ResponseEntity.accepted().body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", sanitizeForLog(e.getMessage()));
            throw e;
            
        } catch (Exception e) {
            log.error("Error submitting simulation: {}", sanitizeForLog(e.getMessage()), e);
            throw new RuntimeException("Failed to submit simulation: " + e.getMessage(), e);
        }
    }
    
    /**
     * GET /api/credit-simulation/runs/{runId}
     * Get simulation status and results
     */
    @GetMapping("/runs/{runId}")
    public ResponseEntity<SimulationResponse> getSimulationResults(@PathVariable String runId) {
        
        log.info("Getting simulation results for runId: {}", sanitizeForLog(runId));
        
        try {
            SimulationResponse response = simulationService.getSimulationResults(runId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Simulation not found: {}", sanitizeForLog(runId));
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving simulation results: {}", sanitizeForLog(e.getMessage()), e);
            throw new RuntimeException("Failed to retrieve simulation: " + e.getMessage(), e);
        }
    }
    
    /**
     * DELETE /api/credit-simulation/runs/{runId}
     * Cancel running simulation
     */
    @DeleteMapping("/runs/{runId}")
    public ResponseEntity<Void> cancelSimulation(@PathVariable String runId) {
        
        log.info("Canceling simulation: {}", sanitizeForLog(runId));
        
        try {
            simulationService.cancelSimulation(runId);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            log.error("Simulation not found: {}", sanitizeForLog(runId));
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error canceling simulation: {}", sanitizeForLog(e.getMessage()), e);
            throw new RuntimeException("Failed to cancel simulation: " + e.getMessage(), e);
        }
    }
}
