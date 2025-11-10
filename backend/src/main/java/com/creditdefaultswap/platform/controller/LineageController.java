package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.dto.OpenLineageEvent;
import com.creditdefaultswap.platform.model.LineageEvent;
import com.creditdefaultswap.platform.repository.LineageEventRepository;
import com.creditdefaultswap.platform.service.OpenLineageAdapter;
import com.creditdefaultswap.platform.service.MarquezForwarder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lineage")
public class LineageController {

    private final LineageEventRepository lineageEventRepository;
    private final OpenLineageAdapter openLineageAdapter;
    private final MarquezForwarder marquezForwarder;

    public LineageController(LineageEventRepository lineageEventRepository,
                           OpenLineageAdapter openLineageAdapter,
                           MarquezForwarder marquezForwarder) {
        this.lineageEventRepository = lineageEventRepository;
        this.openLineageAdapter = openLineageAdapter;
        this.marquezForwarder = marquezForwarder;
    }

    /**
     * POST /api/lineage - Ingest a lineage event
     */
    @PostMapping
    public ResponseEntity<LineageEvent> ingestLineageEvent(@RequestBody LineageEvent event) {
        LineageEvent saved = lineageEventRepository.save(event);
        
        // Forward to Marquez asynchronously
        marquezForwarder.forwardToMarquez(saved);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/lineage?dataset=... - Query lineage by dataset
     */
    @GetMapping
    public ResponseEntity<List<LineageEvent>> queryLineageByDataset(@RequestParam(required = false) String dataset) {
        if (dataset == null || dataset.isEmpty()) {
            // Return all events if no dataset specified
            List<LineageEvent> events = lineageEventRepository.findAll();
            return ResponseEntity.ok(events);
        }
        List<LineageEvent> events = lineageEventRepository.findByDatasetOrderByCreatedAtDesc(dataset);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/lineage/datasets - Get all unique dataset names
     */
    @GetMapping("/datasets")
    public ResponseEntity<List<String>> getAllDatasets() {
        List<String> datasets = lineageEventRepository.findAll()
            .stream()
            .map(LineageEvent::getDataset)
            .distinct()
            .sorted()
            .toList();
        return ResponseEntity.ok(datasets);
    }

    /**
     * GET /api/lineage/run/{runId} - Query lineage by run ID
     */
    @GetMapping("/run/{runId}")
    public ResponseEntity<List<LineageEvent>> queryLineageByRunId(@PathVariable String runId) {
        List<LineageEvent> events = lineageEventRepository.findByRunIdOrderByCreatedAtDesc(runId);
        return ResponseEntity.ok(events);
    }

    /**
     * POST /api/lineage/openlineage - Ingest OpenLineage-formatted event
     * OpenLineage spec: https://openlineage.io/spec/
     */
    @PostMapping("/openlineage")
    public ResponseEntity<LineageEvent> ingestOpenLineageEvent(@RequestBody OpenLineageEvent olEvent) {
        LineageEvent saved = openLineageAdapter.ingestOpenLineageEvent(olEvent);
        
        // Forward to Marquez
        marquezForwarder.forwardOpenLineageToMarquez(olEvent);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/lineage/openlineage?dataset=... - Query lineage in OpenLineage format by dataset
     */
    @GetMapping("/openlineage")
    public ResponseEntity<List<OpenLineageEvent>> queryOpenLineageByDataset(@RequestParam String dataset) {
        List<LineageEvent> events = lineageEventRepository.findByDatasetOrderByCreatedAtDesc(dataset);
        List<OpenLineageEvent> olEvents = openLineageAdapter.toOpenLineageEvents(events);
        return ResponseEntity.ok(olEvents);
    }

    /**
     * GET /api/lineage/openlineage/run/{runId} - Query lineage in OpenLineage format by run ID
     */
    @GetMapping("/openlineage/run/{runId}")
    public ResponseEntity<List<OpenLineageEvent>> queryOpenLineageByRunId(@PathVariable String runId) {
        List<LineageEvent> events = lineageEventRepository.findByRunIdOrderByCreatedAtDesc(runId);
        List<OpenLineageEvent> olEvents = openLineageAdapter.toOpenLineageEvents(events);
        return ResponseEntity.ok(olEvents);
    }

    /**
     * GET /api/lineage/marquez/health - Check Marquez connectivity
     */
    @GetMapping("/marquez/health")
    public ResponseEntity<String> checkMarquezHealth() {
        boolean healthy = marquezForwarder.isMarquezHealthy();
        if (healthy) {
            return ResponseEntity.ok("{\"status\":\"healthy\",\"marquez\":\"connected\"}");
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"status\":\"unhealthy\",\"marquez\":\"disconnected\"}");
        }
    }
}
