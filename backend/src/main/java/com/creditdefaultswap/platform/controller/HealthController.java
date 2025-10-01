package com.creditdefaultswap.platform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

/**
 * Custom health controller to provide health endpoints under /api path
 * Provides basic health checks without requiring actuator dependency
 */
@RestController
public class HealthController {

    private final DataSource dataSource;

    @Autowired
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Root health check endpoint (for direct /health requests)
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> rootHealth() {
        System.out.println("HealthController: /health endpoint called");
        return health();
    }

    /**
     * API health check endpoint  
     * GET /api/health
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        System.out.println("HealthController: /api/health endpoint called");
        return health();
    }

    /**
     * Actuator-style health check (for compatibility)
     * GET /actuator/health
     */
    @GetMapping("/actuator/health") 
    public ResponseEntity<Map<String, Object>> actuatorHealth() {
        System.out.println("HealthController: /actuator/health endpoint called");
        return health();
    }

    private ResponseEntity<Map<String, Object>> health() {
        boolean isHealthy = performHealthCheck();
        
        if (isHealthy) {
            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "service", "credit-default-swap-platform"
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "timestamp", System.currentTimeMillis(),
                "service", "credit-default-swap-platform"
            ));
        }
    }

    /**
     * Detailed health status endpoint
     * GET /api/health/status
     */
    @GetMapping("/api/health/status")
    public ResponseEntity<Map<String, Object>> healthStatus() {
        boolean dbHealthy = checkDatabaseHealth();
        boolean overallHealthy = dbHealthy; // Add more checks as needed
        
        return ResponseEntity.ok(Map.of(
            "status", overallHealthy ? "UP" : "DOWN",
            "components", Map.of(
                "database", Map.of(
                    "status", dbHealthy ? "UP" : "DOWN"
                )
            ),
            "timestamp", System.currentTimeMillis(),
            "service", "credit-default-swap-platform"
        ));
    }

    private boolean performHealthCheck() {
        return checkDatabaseHealth();
    }

    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (Exception e) {
            return false;
        }
    }
}