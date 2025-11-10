package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.OpenLineageEvent;
import com.creditdefaultswap.platform.model.LineageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service to forward lineage events to Marquez (OpenLineage backend).
 * Enables visualization and centralized lineage management.
 */
@Service
public class MarquezForwarder {

    private static final Logger logger = LoggerFactory.getLogger(MarquezForwarder.class);

    private final RestTemplate restTemplate;
    private final OpenLineageAdapter openLineageAdapter;

    @Value("${marquez.url:http://marquez:5000}")
    private String marquezUrl;

    @Value("${marquez.enabled:false}")
    private boolean marquezEnabled;

    public MarquezForwarder(RestTemplate restTemplate,
                           OpenLineageAdapter openLineageAdapter) {
        this.restTemplate = restTemplate;
        this.openLineageAdapter = openLineageAdapter;
    }

    /**
     * Forward a lineage event to Marquez asynchronously.
     * Failures are logged but don't block the main operation.
     */
    public void forwardToMarquez(LineageEvent event) {
        if (!marquezEnabled) {
            logger.debug("Marquez forwarding disabled");
            return;
        }

        try {
            // Convert to OpenLineage format
            OpenLineageEvent olEvent = openLineageAdapter.toOpenLineageEvent(event);

            // Forward to Marquez
            String endpoint = marquezUrl + "/api/v1/lineage";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<OpenLineageEvent> request = new HttpEntity<>(olEvent, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                endpoint,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Lineage event forwarded to Marquez: dataset={}, runId={}", 
                    event.getDataset(), event.getRunId());
            } else {
                logger.warn("Failed to forward lineage to Marquez: status={}", 
                    response.getStatusCode());
            }

        } catch (Exception e) {
            // Don't fail the main operation if Marquez forwarding fails
            logger.error("Error forwarding lineage to Marquez for dataset={}: {}", 
                event.getDataset(), e.getMessage());
        }
    }

    /**
     * Forward an OpenLineage event directly to Marquez.
     */
    public void forwardOpenLineageToMarquez(OpenLineageEvent olEvent) {
        if (!marquezEnabled) {
            logger.debug("Marquez forwarding disabled");
            return;
        }

        try {
            String endpoint = marquezUrl + "/api/v1/lineage";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<OpenLineageEvent> request = new HttpEntity<>(olEvent, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                endpoint,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("OpenLineage event forwarded to Marquez: job={}, runId={}", 
                    olEvent.getJob().getName(), olEvent.getRun().getRunId());
            } else {
                logger.warn("Failed to forward OpenLineage to Marquez: status={}", 
                    response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error forwarding OpenLineage to Marquez: {}", e.getMessage());
        }
    }

    /**
     * Health check for Marquez connectivity.
     */
    public boolean isMarquezHealthy() {
        if (!marquezEnabled) {
            return false;
        }

        try {
            String healthEndpoint = marquezUrl.replace(":5000", ":5001") + "/healthcheck";
            ResponseEntity<String> response = restTemplate.getForEntity(healthEndpoint, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.debug("Marquez health check failed: {}", e.getMessage());
            return false;
        }
    }
}
