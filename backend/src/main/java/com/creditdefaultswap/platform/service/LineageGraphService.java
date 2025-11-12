package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.lineage.LineageGraphDTO;
import com.creditdefaultswap.platform.model.LineageEvent;
import com.creditdefaultswap.platform.repository.LineageEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LineageGraphService {
    
    private static final Logger logger = LoggerFactory.getLogger(LineageGraphService.class);
    private static final String EVENT_ID = "event_id";
    private static final String TIMESTAMP = "timestamp";
    private static final String SERVICE_PREFIX = "service:";
    private static final String REPO_PREFIX = "repository:";
    private static final String ENDPOINT_PREFIX = "endpoint:";
    private static final String DATASET_TYPE = "dataset";
    private static final String TABLE_NAME = "table_name";
    
    private final LineageEventRepository lineageEventRepository;

    public LineageGraphService(LineageEventRepository lineageEventRepository) {
        this.lineageEventRepository = lineageEventRepository;
    }

    /**
     * Get lineage graph for a specific dataset (table)
     */
    public LineageGraphDTO getGraphForDataset(String datasetName, OffsetDateTime since) {
        logger.info("Building lineage graph for dataset: {} since {}", datasetName, since);
        
        List<LineageEvent> events;
        if (since != null) {
            // Use the enhanced query that finds events touching this table anywhere
            events = lineageEventRepository.findByAnyTableTouched(datasetName).stream()
                .filter(e -> e.getCreatedAt().isAfter(since))
                .toList();
        } else {
            // Use the enhanced query that finds events touching this table anywhere
            events = lineageEventRepository.findByAnyTableTouched(datasetName);
        }
        
        return buildGraph(events);
    }

    /**
     * Get lineage graph for a specific event ID
     */
    public LineageGraphDTO getGraphForEvent(UUID eventId) {
        logger.info("Building lineage graph for event ID: {}", eventId);
        
        Optional<LineageEvent> event = lineageEventRepository.findById(eventId);
        if (event.isEmpty()) {
            return new LineageGraphDTO(List.of(), List.of(), Map.of());
        }
        
        return buildGraph(List.of(event.get()));
    }

    /**
     * Get lineage graph for a correlation ID (full request trace)
     */
    public LineageGraphDTO getGraphForCorrelation(String correlationId) {
        logger.info("Building lineage graph for correlation ID: {}", correlationId);
        
        List<LineageEvent> events = lineageEventRepository.findByCorrelationId(correlationId);
        
        return buildGraph(events);
    }

    /**
     * Get lineage graph for a specific run ID
     */
    public LineageGraphDTO getGraphForRun(String runId) {
        logger.info("Building lineage graph for run ID: {}", runId);
        
        List<LineageEvent> events = lineageEventRepository.findByRunIdOrderByCreatedAtDesc(runId);
        
        return buildGraph(events);
    }

    /**
     * Get recent lineage activity (last N events)
     */
    public LineageGraphDTO getRecentActivity(int limit) {
        logger.info("Building lineage graph for recent activity (limit: {})", limit);
        
        List<LineageEvent> events = lineageEventRepository.findAll().stream()
            .sorted(Comparator.comparing(LineageEvent::getCreatedAt).reversed())
            .limit(limit)
            .toList();
        
        return buildGraph(events);
    }

    /**
     * Build graph structure from lineage events
     */
    @SuppressWarnings("unchecked")
    private LineageGraphDTO buildGraph(List<LineageEvent> events) {
        Map<String, LineageGraphDTO.GraphNode> nodeMap = new LinkedHashMap<>();
        List<LineageGraphDTO.GraphEdge> edges = new ArrayList<>();
        
        for (LineageEvent event : events) {
            try {
                Map<String, Object> outputs = event.getOutputs() != null ? event.getOutputs() : Map.of();
                
                // Check if we have the new structured path format
                List<Map<String, Object>> path = (List<Map<String, Object>>) outputs.get("path");
                
                if (path != null && !path.isEmpty()) {
                    // Build graph from structured path
                    buildGraphFromPath(path, event, nodeMap, edges);
                }
                
            } catch (Exception e) {
                logger.error("Error processing lineage event {}: {}", event.getId(), e.getMessage(), e);
            }
        }
        
        Map<String, Object> metadata = Map.of(
            "total_events", events.size(),
            "total_nodes", nodeMap.size(),
            "total_edges", edges.size(),
            "generated_at", OffsetDateTime.now().toString()
        );
        
        return new LineageGraphDTO(
            new ArrayList<>(nodeMap.values()),
            edges,
            metadata
        );
    }

    /**
     * Build graph from structured path format
     */
    @SuppressWarnings("unchecked")
    private void buildGraphFromPath(List<Map<String, Object>> path, LineageEvent event,
                                    Map<String, LineageGraphDTO.GraphNode> nodeMap,
                                    List<LineageGraphDTO.GraphEdge> edges) {
        String previousNodeId = null;
        int edgeCounter = edges.size();
        
        for (Map<String, Object> stage : path) {
            String layer = (String) stage.get("layer");
            String stageType = (String) stage.get("stage");
            String currentNodeId = null;
            String nodeType = null;
            String nodeLabel = null;
            Map<String, Object> nodeProps = new HashMap<>();
            
            // Build node ID and metadata based on stage type
            if ("http_endpoint".equals(stageType)) {
                String method = (String) stage.get("method");
                String endpoint = (String) stage.get("endpoint");
                currentNodeId = ENDPOINT_PREFIX + endpoint;
                nodeType = "endpoint";
                nodeLabel = method + " " + endpoint;
                nodeProps.put("http_method", method);
                nodeProps.put("endpoint", endpoint);
                nodeProps.put("layer", layer);
            } else if ("service".equals(stageType)) {
                String className = (String) stage.get("class");
                String method = (String) stage.get("method");
                currentNodeId = SERVICE_PREFIX + className + "." + method;
                nodeType = "service";
                nodeLabel = className + "." + method + "()";
                nodeProps.put("class", className);
                nodeProps.put("method", method);
                nodeProps.put("layer", layer);
            } else if ("repository".equals(stageType)) {
                String interfaceName = (String) stage.get("interface");
                String method = (String) stage.get("method");
                currentNodeId = REPO_PREFIX + interfaceName + "." + method;
                nodeType = "repository";
                nodeLabel = interfaceName + "." + method + "()";
                nodeProps.put("interface", interfaceName);
                nodeProps.put("method", method);
                nodeProps.put("layer", layer);
            } else if ("dataset".equals(stageType)) {
                String dataset = (String) stage.get("dataset");
                String operation = (String) stage.get("operation");
                currentNodeId = dataset;
                nodeType = DATASET_TYPE;
                nodeLabel = formatLabel(dataset);
                nodeProps.put(TABLE_NAME, dataset);
                nodeProps.put("operation", operation);
                nodeProps.put("layer", layer);
            }
            
            // Create or update node
            if (currentNodeId != null) {
                nodeMap.putIfAbsent(currentNodeId, 
                    new LineageGraphDTO.GraphNode(currentNodeId, nodeType, nodeLabel, nodeProps));
                
                // Create edge from previous stage to current stage
                if (previousNodeId != null) {
                    LineageGraphDTO.GraphNode prevNode = nodeMap.get(previousNodeId);
                    String edgeLabel = determineEdgeLabel(prevNode.type(), nodeType);
                    edges.add(createEdge(edgeCounter++, previousNodeId, currentNodeId, edgeLabel,
                        event.getId().toString(), event.getCreatedAt().toString(), null));
                }
                
                previousNodeId = currentNodeId;
            }
        }
    }

    /**
     * Determine edge label based on source and target node types
     */
    private String determineEdgeLabel(String sourceType, String targetType) {
        if ("endpoint".equals(sourceType) && "service".equals(targetType)) {
            return "CALLS";
        } else if ("service".equals(sourceType) && "service".equals(targetType)) {
            return "CALLS";
        } else if ("service".equals(sourceType) && "repository".equals(targetType)) {
            return "USES";
        } else if ("repository".equals(sourceType) && DATASET_TYPE.equals(targetType)) {
            return "PERSISTS";
        } else {
            return "FLOWS_TO";
        }
    }

    private LineageGraphDTO.GraphEdge createEdge(int counter, String source, String target, 
                                                 String operation, String eventId, 
                                                 String timestamp, String correlationId) {
        Map<String, Object> props = new HashMap<>();
        props.put(EVENT_ID, eventId);
        props.put(TIMESTAMP, timestamp);
        if (correlationId != null) {
            props.put("correlation_id", correlationId);
        }
        
        return new LineageGraphDTO.GraphEdge(
            operation + ":" + counter,
            source,
            target,
            operation,
            props
        );
    }

    private String formatLabel(String tableName) {
        // Convert snake_case to Title Case
        return Arrays.stream(tableName.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }
}
