package com.creditdefaultswap.platform.dto.lineage;

import java.util.List;
import java.util.Map;

public record LineageGraphDTO(
    List<GraphNode> nodes,
    List<GraphEdge> edges,
    Map<String, Object> metadata
) {
    public record GraphNode(
        String id,
        String type,
        String label,
        Map<String, Object> properties
    ) {}

    public record GraphEdge(
        String id,
        String source,
        String target,
        String operation,
        Map<String, Object> properties
    ) {}
}
