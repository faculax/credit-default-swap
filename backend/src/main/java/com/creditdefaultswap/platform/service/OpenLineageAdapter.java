package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.OpenLineageEvent;
import com.creditdefaultswap.platform.model.LineageEvent;
import com.creditdefaultswap.platform.repository.LineageEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter service to convert between OpenLineage format and internal lineage format.
 * Provides OpenLineage-compatible API while storing in internal format.
 */
@Service
public class OpenLineageAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OpenLineageAdapter.class);

    private final LineageEventRepository lineageEventRepository;
    private final LineageService lineageService;

    public OpenLineageAdapter(LineageEventRepository lineageEventRepository, 
                             LineageService lineageService) {
        this.lineageEventRepository = lineageEventRepository;
        this.lineageService = lineageService;
    }

    /**
     * Ingest an OpenLineage event and convert to internal format.
     */
    public LineageEvent ingestOpenLineageEvent(OpenLineageEvent olEvent) {
        // Extract inputs
        Map<String, Object> inputs = new HashMap<>();
        if (olEvent.getInputs() != null) {
            for (int i = 0; i < olEvent.getInputs().size(); i++) {
                OpenLineageEvent.Dataset ds = olEvent.getInputs().get(i);
                inputs.put("input_" + i, Map.of(
                    "name", ds.getName(),
                    "namespace", ds.getNamespace(),
                    "facets", ds.getFacets() != null ? ds.getFacets() : Collections.emptyMap()
                ));
            }
        }

        // Extract outputs
        Map<String, Object> outputs = new HashMap<>();
        if (olEvent.getOutputs() != null) {
            for (int i = 0; i < olEvent.getOutputs().size(); i++) {
                OpenLineageEvent.Dataset ds = olEvent.getOutputs().get(i);
                outputs.put("output_" + i, Map.of(
                    "name", ds.getName(),
                    "namespace", ds.getNamespace(),
                    "facets", ds.getFacets() != null ? ds.getFacets() : Collections.emptyMap()
                ));
            }
        }

        // Determine dataset name (use job name or first output)
        String dataset = olEvent.getJob() != null ? olEvent.getJob().getName() : 
                        (!olEvent.getOutputs().isEmpty() ? olEvent.getOutputs().get(0).getName() : "unknown");

        // Track using internal lineage service
        String operation = olEvent.getEventType() + "_" + 
                          (olEvent.getJob() != null ? olEvent.getJob().getName() : "UNKNOWN");
        String runId = olEvent.getRun() != null ? olEvent.getRun().getRunId() : UUID.randomUUID().toString();

        lineageService.trackTransformation(dataset, operation, inputs, outputs, "openlineage", runId);

        // Return the most recent event for this run
        List<LineageEvent> events = lineageEventRepository.findByRunIdOrderByCreatedAtDesc(runId);
        return events.isEmpty() ? null : events.get(0);
    }

    /**
     * Convert internal LineageEvent to OpenLineage format.
     */
    public OpenLineageEvent toOpenLineageEvent(LineageEvent event) {
        OpenLineageEvent olEvent = new OpenLineageEvent();
        
        // Set event time
        olEvent.setEventTime(ZonedDateTime.parse(event.getCreatedAt().toString()));
        
        // Set run
        OpenLineageEvent.Run run = new OpenLineageEvent.Run(event.getRunId());
        olEvent.setRun(run);
        
        // Set job (extract from operation or use dataset)
        OpenLineageEvent.Job job = new OpenLineageEvent.Job(event.getDataset());
        olEvent.setJob(job);
        
        // Convert inputs
        if (event.getInputs() != null) {
            List<OpenLineageEvent.Dataset> inputs = event.getInputs().entrySet().stream()
                .map(entry -> {
                    OpenLineageEvent.Dataset ds = new OpenLineageEvent.Dataset();
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> inputMap = (Map<String, Object>) entry.getValue();
                        ds.setName((String) inputMap.getOrDefault("name", entry.getKey()));
                        ds.setNamespace((String) inputMap.getOrDefault("namespace", "postgres://cds_platform"));
                    } else {
                        ds.setName(entry.getKey());
                    }
                    return ds;
                })
                .collect(Collectors.toList());
            olEvent.setInputs(inputs);
        }
        
        // Convert outputs
        if (event.getOutputs() != null) {
            List<OpenLineageEvent.Dataset> outputs = event.getOutputs().entrySet().stream()
                .map(entry -> {
                    OpenLineageEvent.Dataset ds = new OpenLineageEvent.Dataset();
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> outputMap = (Map<String, Object>) entry.getValue();
                        ds.setName((String) outputMap.getOrDefault("name", entry.getKey()));
                        ds.setNamespace((String) outputMap.getOrDefault("namespace", "postgres://cds_platform"));
                    } else {
                        ds.setName(entry.getKey());
                    }
                    return ds;
                })
                .collect(Collectors.toList());
            olEvent.setOutputs(outputs);
        }
        
        // Determine event type from operation
        if (event.getOperation().contains("COMPLETE") || event.getOperation().contains("CAPTURE")) {
            olEvent.setEventType("COMPLETE");
        } else if (event.getOperation().contains("START")) {
            olEvent.setEventType("START");
        } else if (event.getOperation().contains("FAIL")) {
            olEvent.setEventType("FAIL");
        } else {
            olEvent.setEventType("COMPLETE");
        }
        
        return olEvent;
    }

    /**
     * Convert list of internal events to OpenLineage format.
     */
    public List<OpenLineageEvent> toOpenLineageEvents(List<LineageEvent> events) {
        return events.stream()
            .map(this::toOpenLineageEvent)
            .collect(Collectors.toList());
    }
}
