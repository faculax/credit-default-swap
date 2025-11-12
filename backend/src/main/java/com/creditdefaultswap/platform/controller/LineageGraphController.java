package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.dto.lineage.LineageGraphDTO;
import com.creditdefaultswap.platform.service.LineageGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/lineage/graph")
@CrossOrigin(origins = "*")
public class LineageGraphController {
    
    private static final Logger logger = LoggerFactory.getLogger(LineageGraphController.class);
    private final LineageGraphService lineageGraphService;

    public LineageGraphController(LineageGraphService lineageGraphService) {
        this.lineageGraphService = lineageGraphService;
    }

    /**
     * Get lineage graph for a specific dataset (table)
     * GET /api/lineage/graph/dataset/{datasetName}?since=2024-01-01T00:00:00Z
     */
    @GetMapping("/dataset/{datasetName}")
    public ResponseEntity<LineageGraphDTO> getGraphForDataset(
            @PathVariable String datasetName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
        logger.info("GET /api/lineage/graph/dataset/{} with since={}", datasetName, since);
        
        LineageGraphDTO graph = lineageGraphService.getGraphForDataset(datasetName, since);
        return ResponseEntity.ok(graph);
    }

    /**
     * Get lineage graph for a specific event
     * GET /api/lineage/graph/event/{eventId}
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<LineageGraphDTO> getGraphForEvent(@PathVariable UUID eventId) {
        logger.info("GET /api/lineage/graph/event/{}", eventId);
        
        LineageGraphDTO graph = lineageGraphService.getGraphForEvent(eventId);
        return ResponseEntity.ok(graph);
    }

    /**
     * Get lineage graph for a correlation ID (full request trace)
     * GET /api/lineage/graph/correlation/{correlationId}
     */
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<LineageGraphDTO> getGraphForCorrelation(@PathVariable String correlationId) {
        logger.info("GET /api/lineage/graph/correlation/{}", correlationId);
        
        LineageGraphDTO graph = lineageGraphService.getGraphForCorrelation(correlationId);
        return ResponseEntity.ok(graph);
    }

    /**
     * Get lineage graph for a specific run ID
     * GET /api/lineage/graph/run/{runId}
     */
    @GetMapping("/run/{runId}")
    public ResponseEntity<LineageGraphDTO> getGraphForRun(@PathVariable String runId) {
        logger.info("GET /api/lineage/graph/run/{}", runId);
        
        LineageGraphDTO graph = lineageGraphService.getGraphForRun(runId);
        return ResponseEntity.ok(graph);
    }

    /**
     * Get recent lineage activity
     * GET /api/lineage/graph/recent?limit=100
     */
    @GetMapping("/recent")
    public ResponseEntity<LineageGraphDTO> getRecentActivity(
            @RequestParam(defaultValue = "100") int limit) {
        logger.info("GET /api/lineage/graph/recent with limit={}", limit);
        
        LineageGraphDTO graph = lineageGraphService.getRecentActivity(limit);
        return ResponseEntity.ok(graph);
    }
}
