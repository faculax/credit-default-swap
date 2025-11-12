package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.LineageEvent;
import com.creditdefaultswap.platform.repository.LineageEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for tracking data lineage across the platform.
 * Captures inputs, transformations, and outputs for audit and compliance.
 */
@Service
public class LineageService {

    private static final Logger logger = LoggerFactory.getLogger(LineageService.class);

    private final LineageEventRepository lineageEventRepository;

    public LineageService(LineageEventRepository lineageEventRepository) {
        this.lineageEventRepository = lineageEventRepository;
    }

    /**
     * Track a data transformation with inputs and outputs.
     * Builds a structured lineage document capturing origin, path, transformations, consumers, and metadata.
     */
    public void trackTransformation(String dataset, String operation, 
                                   Map<String, Object> inputs, 
                                   Map<String, Object> outputs,
                                   String userName, String runId) {
        try {
            LineageEvent event = new LineageEvent();
            event.setDataset(dataset);
            event.setOperation(operation);
            event.setInputs(inputs);
            
            // Build structured lineage document
            Map<String, Object> enrichedOutputs = buildLineageDocument(dataset, operation, inputs, outputs, userName, runId);
            event.setOutputs(enrichedOutputs);
            event.setUserName(userName);
            event.setRunId(runId);
            
            lineageEventRepository.save(event);
            logger.debug("Lineage tracked: dataset={}, operation={}, runId={}", dataset, operation, runId);
        } catch (Exception e) {
            // Don't fail the main operation if lineage tracking fails
            logger.error("Failed to track lineage for dataset={}, operation={}", dataset, operation, e);
        }
    }

    /**
     * Build a structured lineage document following data lineage best practices.
     * Captures: Origin, Path, Transformations, Consumers, and Metadata.
     */
    private Map<String, Object> buildLineageDocument(String dataset,
                                                     String operation,
                                                     Map<String, Object> inputs,
                                                     Map<String, Object> outputs,
                                                     String userName,
                                                     String runId) {
        Map<String, Object> document = new HashMap<>();
        
        // 1. Origin: Where the data started
        document.put("origin", extractOrigin(dataset, inputs));
        
        // 2. Path: Every hop the data took (services, jobs, transformations)
        document.put("path", extractPath(outputs, dataset, operation));
        
        // 3. Transformations: How the value was changed
        document.put("transformations", extractTransformations(outputs, operation));
        
        // 4. Consumers: Which systems/reports are using this data
        document.put("consumers", extractConsumers(outputs, dataset));
        
        // 5. Metadata: Timestamps, owners, systems for compliance
        document.put("metadata", buildMetadata(outputs, userName, runId));
        
        // Preserve raw data for debugging
        document.put("raw_outputs", outputs != null ? outputs : new HashMap<>());
        
        return document;
    }

    /**
     * Extract origin information: source system/table/API where data started.
     */
    private Map<String, Object> extractOrigin(String dataset, Map<String, Object> inputs) {
        Map<String, Object> origin = new HashMap<>();
        origin.put("primary_dataset", dataset);
        origin.put("source_type", "database_table");
        
        // Extract all input sources
        List<Map<String, Object>> sources = new ArrayList<>();
        if (inputs != null) {
            inputs.forEach((key, value) -> {
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sourceMap = (Map<String, Object>) value;
                    Map<String, Object> source = new HashMap<>();
                    source.put("input_name", key);
                    source.put("details", sourceMap);
                    if (sourceMap.containsKey("dataset")) {
                        source.put("dataset", sourceMap.get("dataset"));
                    }
                    if (sourceMap.containsKey("source")) {
                        source.put("source_system", sourceMap.get("source"));
                    }
                    sources.add(source);
                }
            });
        }
        origin.put("input_sources", sources);
        
        return origin;
    }

    /**
     * Extract path: Every hop the data took through the system.
     * Shows: HTTP endpoint → Service → Repository → Dataset
     */
    private List<Map<String, Object>> extractPath(Map<String, Object> outputs,
                                                  String dataset,
                                                  String operation) {
        List<Map<String, Object>> path = new ArrayList<>();
        if (outputs == null) {
            return path;
        }

        // Stage 1: HTTP Endpoint (if available)
        if (outputs.containsKey("_http_method") || outputs.containsKey("_endpoint")) {
            Map<String, Object> httpStage = new HashMap<>();
            httpStage.put("stage", "http_endpoint");
            httpStage.put("layer", "presentation");
            httpStage.put("method", outputs.get("_http_method"));
            httpStage.put("endpoint", outputs.get("_endpoint"));
            httpStage.put("correlation_id", outputs.get("_correlation_id"));
            httpStage.put("timestamp", outputs.get("_start_time"));
            path.add(httpStage);
        }

        // Stage 2: Service Layer Calls
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> serviceCalls = safeMapList(outputs.get("_service_calls_detailed"));
        serviceCalls.forEach(call -> {
            Map<String, Object> stage = new HashMap<>();
            stage.put("stage", "service");
            stage.put("layer", "business_logic");
            stage.put("class", call.get("class"));
            stage.put("method", call.get("method"));
            stage.put("timestamp", call.get("timestamp"));
            path.add(stage);
        });

        // Stage 3: Repository/Data Access Layer
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> repositoryCalls = safeMapList(outputs.get("_repository_calls_detailed"));
        repositoryCalls.forEach(call -> {
            Map<String, Object> stage = new HashMap<>();
            stage.put("stage", "repository");
            stage.put("layer", "data_access");
            stage.put("interface", call.get("interface"));
            stage.put("method", call.get("method"));
            stage.put("timestamp", call.get("timestamp"));
            stage.put("type", call.getOrDefault("type", "SpringData"));
            path.add(stage);
        });

        // Stage 4: Dataset/Table Level
        Map<String, Object> datasetStage = new HashMap<>();
        datasetStage.put("stage", "dataset");
        datasetStage.put("layer", "persistence");
        datasetStage.put("dataset", dataset);
        datasetStage.put("operation", operation);
        datasetStage.put("tables_written", outputs.get("_tracked_tables_written"));
        datasetStage.put("tables_read", outputs.get("_tracked_tables_read"));
        path.add(datasetStage);

        return path;
    }

    /**
     * Extract transformations: How the data was changed (rules, mappings, enrichments).
     */
    private List<Map<String, Object>> extractTransformations(Map<String, Object> outputs, String operation) {
        List<Map<String, Object>> transformations = new ArrayList<>();
        if (outputs == null) {
            return transformations;
        }

        // Main operation transformation
        Map<String, Object> mainTransform = new HashMap<>();
        mainTransform.put("type", "operation");
        mainTransform.put("operation", operation);
        mainTransform.put("description", "Primary data transformation for " + operation);
        transformations.add(mainTransform);

        // Add table-level transformations from tracked operations (for multi-table writes)
        if (outputs.containsKey("table_transformations")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tableTransforms = (List<Map<String, Object>>) outputs.get("table_transformations");
            transformations.addAll(tableTransforms);
        }

        // Extract business-level transformations from outputs
        // (fields that don't start with "_" represent business data)
        outputs.entrySet().stream()
            .filter(entry -> !entry.getKey().startsWith("_"))
            .filter(entry -> !entry.getKey().equals("table_transformations")) // Already added above
            .filter(entry -> !entry.getKey().equals("affected_tables")) // Skip the list
            .filter(entry -> entry.getValue() instanceof Map)
            .forEach(entry -> {
                Map<String, Object> transformation = new HashMap<>();
                transformation.put("type", "business_logic");
                transformation.put("name", entry.getKey());
                transformation.put("details", entry.getValue());
                transformations.add(transformation);
            });

        return transformations;
    }

    /**
     * Extract consumers: Which reports/models/users are using this data.
     */
    private List<Map<String, Object>> extractConsumers(Map<String, Object> outputs, String dataset) {
        List<Map<String, Object>> consumers = new ArrayList<>();
        
        // Primary consumer: The dataset itself
        Map<String, Object> datasetConsumer = new HashMap<>();
        datasetConsumer.put("type", "dataset");
        datasetConsumer.put("name", dataset);
        datasetConsumer.put("description", "Primary consumer - data persisted to " + dataset);
        consumers.add(datasetConsumer);

        // API response consumer (if data flows back to client)
        if (outputs != null && outputs.containsKey("_response_dto_class")) {
            Map<String, Object> apiConsumer = new HashMap<>();
            apiConsumer.put("type", "api_response");
            apiConsumer.put("dto_class", outputs.get("_response_dto_class"));
            apiConsumer.put("dto_type", outputs.get("_response_dto_type"));
            apiConsumer.put("description", "Data returned to API client");
            consumers.add(apiConsumer);
        }

        // Downstream systems (can be extended based on actual integrations)
        if (outputs != null && outputs.containsKey("_downstream_systems")) {
            @SuppressWarnings("unchecked")
            List<String> downstreamSystems = (List<String>) outputs.get("_downstream_systems");
            downstreamSystems.forEach(system -> {
                Map<String, Object> downstreamConsumer = new HashMap<>();
                downstreamConsumer.put("type", "downstream_system");
                downstreamConsumer.put("system", system);
                consumers.add(downstreamConsumer);
            });
        }

        return consumers;
    }

    /**
     * Build metadata: Timestamps, owners, systems for compliance and audit.
     */
    private Map<String, Object> buildMetadata(Map<String, Object> outputs,
                                              String userName,
                                              String runId) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Compliance & Audit Information
        metadata.put("recorded_at", Instant.now().toString());
        metadata.put("user", userName);
        metadata.put("run_id", runId);
        metadata.put("source", outputs != null ? outputs.getOrDefault("_source", "runtime") : "runtime");
        
        // Performance Metrics
        metadata.put("duration_ms", outputs != null ? outputs.get("_duration_ms") : null);
        metadata.put("start_time", outputs != null ? outputs.get("_start_time") : null);
        
        // Request Tracking
        metadata.put("correlation_id", outputs != null ? outputs.get("_correlation_id") : null);
        metadata.put("http_method", outputs != null ? outputs.get("_http_method") : null);
        metadata.put("endpoint", outputs != null ? outputs.get("_endpoint") : null);
        
        // Data Contracts (DTOs)
        metadata.put("request_dto", outputs != null ? outputs.get("_request_dto_class") : null);
        metadata.put("response_dto", outputs != null ? outputs.get("_response_dto_class") : null);
        
        // Audit Trail
        if (outputs != null) {
            metadata.put("audit_ip_address", outputs.get("_audit_ip_address"));
            metadata.put("audit_user_agent", outputs.get("_audit_user_agent"));
            metadata.put("audit_session_id", outputs.get("_audit_session_id"));
            metadata.put("audit_request_id", outputs.get("_audit_request_id"));
        }
        
        // Data Quality & Confidence
        Map<String, Double> confidence = new HashMap<>();
        confidence.put("controller_to_service", 1.0);
        confidence.put("service_to_repository", 1.0);
        confidence.put("repository_to_table", 1.0);
        metadata.put("lineage_confidence", confidence);
        
        // Compliance Flags
        metadata.put("automated_capture", true);
        metadata.put("manual_review_required", false);
        
        return metadata;
    }

    /**
     * Safely extract a list of maps from an object.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeMapList(Object value) {
        if (value instanceof List<?>) {
            return ((List<?>) value).stream()
                .filter(item -> item instanceof Map)
                .map(item -> (Map<String, Object>) item)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Track CDS trade capture with comprehensive lineage.
     */
    public void trackTradeCapture(Long tradeId, String tradeType, String userName, Map<String, Object> tradeDetails) {
        // Inputs: what data sources were actually read
        Map<String, Object> inputs = new HashMap<>();
        
        // UI trade entry source
        Map<String, Object> uiTradeEntry = new HashMap<>();
        uiTradeEntry.put("source", "user_interface");
        uiTradeEntry.put("form", "new_single_name_cds");
        uiTradeEntry.put("user", userName);
        inputs.put("ui_trade_entry", uiTradeEntry);
        
        // Netting set lookup - actual database read that happens
        Map<String, Object> nettingSetLookup = new HashMap<>();
        nettingSetLookup.put("dataset", "netting_sets");
        nettingSetLookup.put("counterparty", tradeDetails.getOrDefault("counterparty", "UNKNOWN"));
        nettingSetLookup.put("purpose", "auto_assignment");
        if (tradeDetails.containsKey("ccpName")) {
            nettingSetLookup.put("ccp_name", tradeDetails.get("ccpName"));
        }
        if (tradeDetails.containsKey("currency")) {
            nettingSetLookup.put("currency", tradeDetails.get("currency"));
        }
        inputs.put("netting_set_lookup", nettingSetLookup);
        
        // Outputs: what was actually written
        Map<String, Object> outputs = new HashMap<>();
        
        // Trade record created - the only actual database write
        Map<String, Object> tradeRecordCreated = new HashMap<>();
        tradeRecordCreated.put("dataset", "cds_trades");
        tradeRecordCreated.put("trade_id", tradeId);
        tradeRecordCreated.put("notional", tradeDetails.getOrDefault("notionalAmount", tradeDetails.getOrDefault("notional", 0)));
        tradeRecordCreated.put("entity", tradeDetails.getOrDefault("entityName", tradeDetails.getOrDefault("referenceEntity", "UNKNOWN")));
        tradeRecordCreated.put("maturity", tradeDetails.getOrDefault("maturityDate", ""));
        tradeRecordCreated.put("spread_bps", tradeDetails.getOrDefault("spread", 0));
        
        // Include all available trade details from AOP
        if (tradeDetails.containsKey("upfrontAmount")) {
            tradeRecordCreated.put("upfront_amount", tradeDetails.get("upfrontAmount"));
        }
        if (tradeDetails.containsKey("buySellProtection")) {
            tradeRecordCreated.put("buy_sell_protection", tradeDetails.get("buySellProtection"));
        }
        if (tradeDetails.containsKey("counterparty")) {
            tradeRecordCreated.put("counterparty", tradeDetails.get("counterparty"));
        }
        if (tradeDetails.containsKey("tradeDate")) {
            tradeRecordCreated.put("trade_date", tradeDetails.get("tradeDate"));
        }
        if (tradeDetails.containsKey("currency")) {
            tradeRecordCreated.put("currency", tradeDetails.get("currency"));
        }
        if (tradeDetails.containsKey("recoveryRate")) {
            tradeRecordCreated.put("recovery_rate", tradeDetails.get("recoveryRate"));
        }
        if (tradeDetails.containsKey("nettingSetId")) {
            tradeRecordCreated.put("netting_set_id", tradeDetails.get("nettingSetId"));
        }
        outputs.put("trade_record_created", tradeRecordCreated);
        
        // Merge correlation metadata from tradeDetails into outputs
        if (tradeDetails != null) {
            tradeDetails.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("_"))
                .forEach(entry -> outputs.put(entry.getKey(), entry.getValue()));
        }
        
        trackTransformation("cds_trades", "TRADE_CAPTURE_" + tradeType, 
                          inputs, outputs, userName, "trade-capture-" + tradeId);
    }

    /**
     * Track credit event processing.
     */
    public void trackCreditEvent(Long tradeId, String eventType, UUID eventId, String userName) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("trade_id", tradeId);
        inputs.put("event_type", eventType);
        inputs.put("cds_trades", "source");
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("credit_event_id", eventId.toString());
        outputs.put("table", "credit_events");
        
        trackTransformation("credit_events", "CREDIT_EVENT_" + eventType,
                          inputs, outputs, userName, "event-" + eventId);
    }

    /**
     * Track credit event with full correlation and tracing details (enhanced version).
     */
    public void trackCreditEventWithDetails(Long tradeId, String eventType, UUID eventId, 
                                           String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("trade_id", tradeId);
        inputs.put("event_type", eventType);
        
        // Build inputs from tracked read operations
        if (details != null && details.containsKey("_tracked_tables_read")) {
            @SuppressWarnings("unchecked")
            List<String> trackedReads = (List<String>) details.get("_tracked_tables_read");
            for (String tableName : trackedReads) {
                inputs.put(tableName + "_read", tableName);
            }
        }
        
        // Merge the correlation/tracing details into outputs
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("credit_event_id", eventId.toString());
        outputs.put("table", "credit_events");
        
        // Add all correlation and tracing data from details
        if (details != null) {
            outputs.putAll(details);
            
            // Extract tracked write operations and create explicit transformation records
            if (details.containsKey("_tracked_tables_written")) {
                @SuppressWarnings("unchecked")
                List<String> trackedWrites = (List<String>) details.get("_tracked_tables_written");
                
                // Store the list of affected tables for intelligence display
                outputs.put("affected_tables", trackedWrites);
                
                // Create structured records for each table affected
                List<Map<String, Object>> tableTransformations = new ArrayList<>();
                for (String tableName : trackedWrites) {
                    Map<String, Object> tableTransform = new HashMap<>();
                    tableTransform.put("table", tableName);
                    tableTransform.put("operation", "WRITE");
                    tableTransform.put("event_context", "credit_event_" + eventType.toLowerCase());
                    
                    // Add context for known tables
                    switch (tableName) {
                        case "credit_events":
                            tableTransform.put("description", "Primary credit event record");
                            tableTransform.put("purpose", "Event recorded for trade " + tradeId);
                            break;
                        case "cds_trades":
                            tableTransform.put("description", "Trade status updated");
                            tableTransform.put("purpose", "Status changed to CREDIT_EVENT_RECORDED or SETTLED");
                            break;
                        case "cash_settlements":
                            tableTransform.put("description", "Cash settlement calculated");
                            tableTransform.put("purpose", "Recovery rate applied for cash settlement");
                            break;
                        case "physical_settlement_instructions":
                            tableTransform.put("description", "Physical settlement instruction created");
                            tableTransform.put("purpose", "Delivery obligations recorded");
                            break;
                        case "audit_logs":
                            tableTransform.put("description", "Audit trail recorded");
                            tableTransform.put("purpose", "Compliance and regulatory tracking");
                            break;
                        default:
                            tableTransform.put("description", "Data modified as part of credit event processing");
                            tableTransform.put("purpose", "Supporting data for event " + eventId);
                    }
                    
                    tableTransformations.add(tableTransform);
                }
                
                outputs.put("table_transformations", tableTransformations);
            }
        }
        
        trackTransformation("credit_events", "CREDIT_EVENT_" + eventType,
                          inputs, outputs, userName, "event-" + eventId);
    }

    /**
     * Track lifecycle event processing (coupon, maturity, etc).
     */
    public void trackLifecycleEvent(Long tradeId, String eventType, String userName) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("trade_id", tradeId);
        inputs.put("event_type", eventType);
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("updated_trade_id", tradeId);
        outputs.put("table", "cds_trades");
        
        trackTransformation("cds_trades", "LIFECYCLE_" + eventType,
                          inputs, outputs, userName, "lifecycle-" + tradeId + "-" + eventType);
    }

    /**
     * Track portfolio aggregation.
     */
    public void trackPortfolioAggregation(Long portfolioId, int tradeCount, String userName) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("cds_trades", "source");
        inputs.put("trade_count", tradeCount);
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("portfolio_id", portfolioId);
        outputs.put("table", "cds_portfolios");
        
        trackTransformation("cds_portfolios", "PORTFOLIO_AGGREGATION",
                          inputs, outputs, userName, "portfolio-" + portfolioId);
    }

    /**
     * Track pricing calculation.
     */
    public void trackPricingCalculation(String entityType, Long entityId, 
                                       String pricingMethod, String userName) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("entity_type", entityType);
        inputs.put("entity_id", entityId);
        inputs.put("market_data", "market_quotes");
        inputs.put("pricing_method", pricingMethod);
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("price_calculated", true);
        outputs.put("table", entityType.toLowerCase() + "s");
        
        trackTransformation(entityType.toLowerCase() + "_pricing", "PRICING_" + pricingMethod,
                          inputs, outputs, userName, "pricing-" + entityType + "-" + entityId);
    }

    /**
     * Track pricing calculation with full correlation and tracing details (enhanced version).
     */
    public void trackPricingCalculationWithDetails(String entityType, Long entityId, 
                                                  String pricingMethod, String userName,
                                                  Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("entity_type", entityType);
        inputs.put("entity_id", entityId);
        inputs.put("market_data", "market_quotes");
        inputs.put("pricing_method", pricingMethod);
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("price_calculated", true);
        outputs.put("table", entityType.toLowerCase() + "s");
        
        // Add all correlation and tracing data from details
        if (details != null) {
            outputs.putAll(details);
        }
        
        trackTransformation(entityType.toLowerCase() + "_pricing", "PRICING_" + pricingMethod,
                          inputs, outputs, userName, "pricing-" + entityType + "-" + entityId);
    }

    /**
     * Track margin calculation.
     */
    public void trackMarginCalculation(String marginType, Long accountId, 
                                      Object marginAmount, String userName) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("margin_type", marginType);
        inputs.put("account_id", accountId);
        inputs.put("positions", "cds_trades");
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("margin_amount", marginAmount);
        outputs.put("table", "margin_accounts");
        
        trackTransformation("margin_accounts", "MARGIN_" + marginType,
                          inputs, outputs, userName, "margin-" + accountId);
    }

    /**
     * Track schema migration (called from Flyway callback).
     */
    public void trackSchemaMigration(String migrationVersion, String description, String userName) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("migration_script", migrationVersion);
        inputs.put("description", description);
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("schema_updated", true);
        outputs.put("version", migrationVersion);
        
        trackTransformation("schema", "SCHEMA_MIGRATION",
                          inputs, outputs, userName != null ? userName : "flyway", "migration-" + migrationVersion);
    }

    /**
     * Track batch processing.
     */
    public void trackBatchProcess(String batchType, int recordsProcessed, 
                                 String inputSource, String outputDestination, String userName) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("batch_type", batchType);
        inputs.put("source", inputSource);
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("records_processed", recordsProcessed);
        outputs.put("destination", outputDestination);
        
        String runId = "batch-" + batchType + "-" + System.currentTimeMillis();
        trackTransformation(outputDestination, "BATCH_" + batchType,
                          inputs, outputs, userName, runId);
    }

    /**
     * Track bond creation/update - only actual database operations.
     */
    public void trackBondOperation(String operation, Long bondId, String userName, Map<String, Object> bondDetails) {
        Map<String, Object> inputs = new HashMap<>();
        
        // UI entry source
        Map<String, Object> uiEntry = new HashMap<>();
        uiEntry.put("source", "user_interface");
        uiEntry.put("form", "bond_management");
        uiEntry.put("user", userName);
        inputs.put("ui_bond_entry", uiEntry);
        
        // For UPDATE operation, read existing bond first
        if ("UPDATE".equalsIgnoreCase(operation)) {
            Map<String, Object> bondLookup = new HashMap<>();
            bondLookup.put("dataset", "bonds");
            bondLookup.put("bond_id", bondId);
            bondLookup.put("purpose", "load_existing_record");
            inputs.put("bond_lookup", bondLookup);
        }
        
        // Build outputs with actual bond data - only actual write
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> bondRecordCreated = new HashMap<>();
        bondRecordCreated.put("dataset", "bonds");
        bondRecordCreated.put("bond_id", bondId);
        bondRecordCreated.put("issuer", bondDetails.getOrDefault("issuer", "UNKNOWN"));
        bondRecordCreated.put("face_value", bondDetails.getOrDefault("faceValue", 0));
        bondRecordCreated.put("maturity", bondDetails.getOrDefault("maturityDate", ""));
        
        // Include all available bond details from AOP
        if (bondDetails.containsKey("isin")) {
            bondRecordCreated.put("isin", bondDetails.get("isin"));
        }
        if (bondDetails.containsKey("notional")) {
            bondRecordCreated.put("notional", bondDetails.get("notional"));
        }
        if (bondDetails.containsKey("currency")) {
            bondRecordCreated.put("currency", bondDetails.get("currency"));
        }
        if (bondDetails.containsKey("sector")) {
            bondRecordCreated.put("sector", bondDetails.get("sector"));
        }
        if (bondDetails.containsKey("seniority")) {
            bondRecordCreated.put("seniority", bondDetails.get("seniority"));
        }
        if (bondDetails.containsKey("couponRate")) {
            bondRecordCreated.put("coupon_rate", bondDetails.get("couponRate"));
        }
        outputs.put("bond_record_created", bondRecordCreated);
        
        // Merge correlation metadata from details into outputs
        if (bondDetails != null) {
            bondDetails.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("_"))
                .forEach(entry -> outputs.put(entry.getKey(), entry.getValue()));
        }
        
        trackTransformation("bonds", operation.toUpperCase() + "_BOND", 
                          inputs, outputs, userName, "bond-" + operation + "-" + bondId);
    }

    /**
     * Track portfolio aggregation operation.
     */
    public void trackPortfolioOperation(String operation, Long portfolioId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        Map<String, Object> outputs = new HashMap<>();
        
        // Operation-specific inputs and outputs
        switch (operation.toUpperCase()) {
            case "CREATE":
                // CREATE: Only checks portfolio name uniqueness
                Map<String, Object> uiEntry = new HashMap<>();
                uiEntry.put("source", "user_interface");
                uiEntry.put("form", "portfolio_management");
                uiEntry.put("user", userName);
                if (details.containsKey("name")) {
                    uiEntry.put("name", details.get("name"));
                }
                if (details.containsKey("description")) {
                    uiEntry.put("description", details.get("description"));
                }
                inputs.put("ui_portfolio_entry", uiEntry);
                
                Map<String, Object> nameCheck = new HashMap<>();
                nameCheck.put("dataset", "cds_portfolios");
                nameCheck.put("purpose", "uniqueness_validation");
                if (details.containsKey("name")) {
                    nameCheck.put("name", details.get("name"));
                }
                inputs.put("portfolio_name_check", nameCheck);
                
                // Output: Portfolio created
                Map<String, Object> portfolioCreated = new HashMap<>();
                portfolioCreated.put("dataset", "cds_portfolios");
                portfolioCreated.put("portfolio_id", portfolioId);
                if (details.containsKey("name")) {
                    portfolioCreated.put("name", details.get("name"));
                }
                if (details.containsKey("description")) {
                    portfolioCreated.put("description", details.get("description"));
                }
                portfolioCreated.put("total_positions", 0);
                portfolioCreated.put("status", "ACTIVE");
                outputs.put("portfolio_created", portfolioCreated);
                break;
                
            case "ATTACH_TRADES":
                // ATTACH_TRADES: Reads trades and updates portfolio constituents
                Map<String, Object> tradesLookup = new HashMap<>();
                tradesLookup.put("dataset", "cds_trades");
                tradesLookup.put("trade_count", details.getOrDefault("tradeCount", details.getOrDefault("tradesAttached", 0)));
                tradesLookup.put("purpose", "position_aggregation");
                inputs.put("cds_trades_lookup", tradesLookup);
                
                Map<String, Object> portfolioLookup = new HashMap<>();
                portfolioLookup.put("dataset", "cds_portfolios");
                portfolioLookup.put("portfolio_id", portfolioId);
                portfolioLookup.put("purpose", "attach_constituents");
                inputs.put("portfolio_lookup", portfolioLookup);
                
                // Output: Constituents created
                Map<String, Object> constituentsCreated = new HashMap<>();
                constituentsCreated.put("dataset", "cds_portfolio_constituents");
                constituentsCreated.put("portfolio_id", portfolioId);
                constituentsCreated.put("trades_attached", details.getOrDefault("tradeCount", details.getOrDefault("tradesAttached", 0)));
                if (details.containsKey("weightType")) {
                    constituentsCreated.put("weight_type", details.get("weightType"));
                }
                outputs.put("constituents_created", constituentsCreated);
                
                Map<String, Object> portfolioUpdated = new HashMap<>();
                portfolioUpdated.put("dataset", "cds_portfolios");
                portfolioUpdated.put("portfolio_id", portfolioId);
                portfolioUpdated.put("action", "positions_updated");
                portfolioUpdated.put("total_positions", details.getOrDefault("totalPositions", details.getOrDefault("tradesAttached", 0)));
                outputs.put("portfolio_updated", portfolioUpdated);
                break;
                
            case "ATTACH_BOND":
                // ATTACH_BOND: Reads bond and updates portfolio
                Map<String, Object> bondsLookup = new HashMap<>();
                bondsLookup.put("dataset", "bonds");
                bondsLookup.put("bond_id", details.getOrDefault("bondId", 0));
                bondsLookup.put("purpose", "fixed_income_attachment");
                inputs.put("bonds_lookup", bondsLookup);
                
                Map<String, Object> portfolioLookupBond = new HashMap<>();
                portfolioLookupBond.put("dataset", "cds_portfolios");
                portfolioLookupBond.put("portfolio_id", portfolioId);
                portfolioLookupBond.put("purpose", "attach_bond");
                inputs.put("portfolio_lookup", portfolioLookupBond);
                
                // Output: Bond attached
                Map<String, Object> bondAttached = new HashMap<>();
                bondAttached.put("dataset", "cds_portfolio_constituents");
                bondAttached.put("portfolio_id", portfolioId);
                if (details.containsKey("bondId")) {
                    bondAttached.put("bond_id", details.get("bondId"));
                }
                if (details.containsKey("weightType")) {
                    bondAttached.put("weight_type", details.get("weightType"));
                }
                if (details.containsKey("weightValue")) {
                    bondAttached.put("weight_value", details.get("weightValue"));
                }
                outputs.put("bond_attached", bondAttached);
                
                Map<String, Object> portfolioUpdatedBond = new HashMap<>();
                portfolioUpdatedBond.put("dataset", "cds_portfolios");
                portfolioUpdatedBond.put("portfolio_id", portfolioId);
                portfolioUpdatedBond.put("action", "bond_position_added");
                outputs.put("portfolio_updated", portfolioUpdatedBond);
                break;
                
            case "ATTACH_BASKET":
                // ATTACH_BASKET: Reads basket and updates portfolio
                Map<String, Object> basketLookup = new HashMap<>();
                basketLookup.put("dataset", "cds_baskets");
                basketLookup.put("basket_id", details.getOrDefault("basketId", 0));
                basketLookup.put("purpose", "basket_attachment");
                inputs.put("basket_lookup", basketLookup);
                
                Map<String, Object> portfolioLookupBasket = new HashMap<>();
                portfolioLookupBasket.put("dataset", "cds_portfolios");
                portfolioLookupBasket.put("portfolio_id", portfolioId);
                portfolioLookupBasket.put("purpose", "attach_basket");
                inputs.put("portfolio_lookup", portfolioLookupBasket);
                
                // Output: Basket attached
                Map<String, Object> basketAttached = new HashMap<>();
                basketAttached.put("dataset", "cds_portfolio_constituents");
                basketAttached.put("portfolio_id", portfolioId);
                if (details.containsKey("basketId")) {
                    basketAttached.put("basket_id", details.get("basketId"));
                }
                if (details.containsKey("weightType")) {
                    basketAttached.put("weight_type", details.get("weightType"));
                }
                if (details.containsKey("weightValue")) {
                    basketAttached.put("weight_value", details.get("weightValue"));
                }
                outputs.put("basket_attached", basketAttached);
                
                Map<String, Object> portfolioUpdatedBasket = new HashMap<>();
                portfolioUpdatedBasket.put("dataset", "cds_portfolios");
                portfolioUpdatedBasket.put("portfolio_id", portfolioId);
                portfolioUpdatedBasket.put("action", "basket_position_added");
                outputs.put("portfolio_updated", portfolioUpdatedBasket);
                break;
                
            case "PRICE":
                // PRICE: Reads all portfolio constituents and market data
                Map<String, Object> constituentsLookup = new HashMap<>();
                constituentsLookup.put("dataset", "cds_portfolio_constituents");
                constituentsLookup.put("portfolio_id", portfolioId);
                constituentsLookup.put("purpose", "position_valuation");
                inputs.put("constituents_lookup", constituentsLookup);
                
                Map<String, Object> marketDataLookup = new HashMap<>();
                marketDataLookup.put("dataset", "market_quotes");
                marketDataLookup.put("purpose", "pricing_reference");
                if (details.containsKey("valuationDate")) {
                    marketDataLookup.put("valuation_date", details.get("valuationDate"));
                }
                inputs.put("market_data_lookup", marketDataLookup);
                
                // Output: Portfolio priced
                Map<String, Object> portfolioPriced = new HashMap<>();
                portfolioPriced.put("dataset", "cds_portfolios");
                portfolioPriced.put("portfolio_id", portfolioId);
                portfolioPriced.put("action", "portfolio_priced");
                if (details.containsKey("totalPV")) {
                    portfolioPriced.put("total_pv", details.get("totalPV"));
                }
                if (details.containsKey("valuationDate")) {
                    portfolioPriced.put("valuation_date", details.get("valuationDate"));
                }
                outputs.put("portfolio_priced", portfolioPriced);
                
                // Risk metrics triggered for pricing
                Map<String, Object> riskMetrics = new HashMap<>();
                riskMetrics.put("dataset", "portfolio_risk_metrics");
                riskMetrics.put("action", "calculate_portfolio_var");
                riskMetrics.put("metrics", java.util.List.of("var", "cvar", "concentration"));
                outputs.put("risk_metrics_triggered", riskMetrics);
                break;
                
            default:
                logger.warn("Unknown portfolio operation: {}", operation);
                break;
        }
        
        // Merge correlation metadata from details into outputs (added by EnhancedLineageAspect)
        if (details != null) {
            details.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("_"))
                .forEach(entry -> outputs.put(entry.getKey(), entry.getValue()));
        }
        
        trackTransformation("cds_portfolios", operation.toUpperCase() + "_PORTFOLIO",
                          inputs, outputs, userName, "portfolio-" + operation + "-" + portfolioId);
    }

    /**
     * Track margin calculation operation.
     */
    public void trackMarginOperation(String marginType, String accountId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        
        // Positions lookup
        Map<String, Object> positionsLookup = new HashMap<>();
        positionsLookup.put("dataset", "cds_trades");
        positionsLookup.put("purpose", "margin_calculation_input");
        if (details.containsKey("portfolioId")) {
            positionsLookup.put("portfolio_id", details.get("portfolioId"));
        }
        inputs.put("positions_lookup", positionsLookup);
        
        // Market data lookup
        Map<String, Object> marketDataLookup = new HashMap<>();
        marketDataLookup.put("dataset", "market_quotes");
        marketDataLookup.put("purpose", "sensitivity_calculation");
        inputs.put("market_data_lookup", marketDataLookup);
        
        // Risk factors
        Map<String, Object> riskFactors = new HashMap<>();
        riskFactors.put("dataset", "risk_factors");
        riskFactors.put("purpose", marginType.toLowerCase() + "_calculation");
        inputs.put("risk_factors", riskFactors);
        
        // Build outputs with actual margin data
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> marginCalculated = new HashMap<>();
        marginCalculated.put("dataset", "margin_requirements");
        marginCalculated.put("margin_type", marginType);
        marginCalculated.put("account_id", accountId);
        marginCalculated.put("amount", details.getOrDefault("marginAmount", details.getOrDefault("totalIM", 0)));
        
        // Include all available margin details from AOP
        if (details.containsKey("calculationId")) {
            marginCalculated.put("calculation_id", details.get("calculationId"));
        }
        if (details.containsKey("totalIM")) {
            marginCalculated.put("total_im", details.get("totalIM"));
        }
        if (details.containsKey("calculationDate")) {
            marginCalculated.put("calculation_date", details.get("calculationDate"));
        }
        if (details.containsKey("parameterSetVersion")) {
            marginCalculated.put("parameter_set_version", details.get("parameterSetVersion"));
        }
        if (details.containsKey("isdaVersion")) {
            marginCalculated.put("isda_version", details.get("isdaVersion"));
        }
        if (details.containsKey("calculationTimeMs")) {
            marginCalculated.put("calculation_time_ms", details.get("calculationTimeMs"));
        }
        if (details.containsKey("status")) {
            marginCalculated.put("status", details.get("status"));
        }
        if (details.containsKey("breakdown")) {
            marginCalculated.put("breakdown", details.get("breakdown"));
        }
        outputs.put("margin_calculated", marginCalculated);
        
        // Margin call check
        Map<String, Object> marginCallCheck = new HashMap<>();
        marginCallCheck.put("dataset", "margin_calls");
        marginCallCheck.put("action", "evaluate_margin_breach");
        marginCallCheck.put("threshold_check", "performed");
        outputs.put("margin_call_check", marginCallCheck);
        
        // Merge correlation metadata from details into outputs
        if (details != null) {
            details.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("_"))
                .forEach(entry -> outputs.put(entry.getKey(), entry.getValue()));
        }
        
        trackTransformation("margin_requirements", "MARGIN_" + marginType.toUpperCase(),
                          inputs, outputs, userName, "margin-" + marginType + "-" + accountId);
    }

    /**
     * Track basket/index operation.
     */
    public void trackBasketOperation(String operation, Long basketId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        
        // Constituent lookup with actual count
        Map<String, Object> constituentLookup = new HashMap<>();
        constituentLookup.put("dataset", "reference_entities");
        constituentLookup.put("constituent_count", details.getOrDefault("constituentCount", details.getOrDefault("constituentsCount", 0)));
        constituentLookup.put("purpose", "basket_composition");
        inputs.put("constituent_lookup", constituentLookup);
        
        // Weights calculation
        Map<String, Object> weightsCalculation = new HashMap<>();
        weightsCalculation.put("source", "index_methodology");
        weightsCalculation.put("purpose", "weight_determination");
        inputs.put("weights_calculation", weightsCalculation);
        
        // Build outputs with actual basket data
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> basketCreated = new HashMap<>();
        basketCreated.put("dataset", "cds_baskets");
        basketCreated.put("basket_id", basketId);
        basketCreated.put("constituents", details.getOrDefault("constituentCount", details.getOrDefault("constituentsCount", 0)));
        
        // Include all available basket details from AOP
        if (details.containsKey("name")) {
            basketCreated.put("name", details.get("name"));
        }
        if (details.containsKey("notional")) {
            basketCreated.put("notional", details.get("notional"));
        }
        if (details.containsKey("type")) {
            basketCreated.put("type", details.get("type"));
        }
        if (details.containsKey("constituents")) {
            basketCreated.put("constituent_details", details.get("constituents"));
        }
        outputs.put("basket_created", basketCreated);
        
        // Index pricing trigger
        Map<String, Object> indexPricing = new HashMap<>();
        indexPricing.put("dataset", "index_prices");
        indexPricing.put("action", "calculate_index_spread");
        indexPricing.put("basket_id", basketId);
        outputs.put("index_pricing_triggered", indexPricing);
        
        // Merge correlation metadata from details into outputs
        if (details != null) {
            details.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("_"))
                .forEach(entry -> outputs.put(entry.getKey(), entry.getValue()));
        }
        
        trackTransformation("cds_baskets", operation.toUpperCase() + "_BASKET",
                          inputs, outputs, userName, "basket-" + operation + "-" + basketId);
    }

    /**
     * Track novation operation.
     */
    public void trackNovationOperation(Long originalTradeId, Long newTradeId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        
        // Original trade lookup
        Map<String, Object> originalTrade = new HashMap<>();
        originalTrade.put("dataset", "cds_trades");
        originalTrade.put("trade_id", originalTradeId);
        originalTrade.put("purpose", "novation_source");
        inputs.put("original_trade", originalTrade);
        
        // Counterparty validation with actual counterparty
        Map<String, Object> counterpartyValidation = new HashMap<>();
        counterpartyValidation.put("dataset", "counterparties");
        counterpartyValidation.put("new_counterparty", details.getOrDefault("newCounterparty", details.getOrDefault("counterparty", "UNKNOWN")));
        counterpartyValidation.put("purpose", "novation_approval");
        inputs.put("counterparty_validation", counterpartyValidation);
        
        // Build outputs with actual novation data
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> originalTradeUpdated = new HashMap<>();
        originalTradeUpdated.put("dataset", "cds_trades");
        originalTradeUpdated.put("trade_id", originalTradeId);
        originalTradeUpdated.put("action", "mark_novated");
        
        // Include novation details from AOP
        if (details.containsKey("effectiveDate")) {
            originalTradeUpdated.put("effective_date", details.get("effectiveDate"));
        }
        outputs.put("original_trade_updated", originalTradeUpdated);
        
        Map<String, Object> newTradeCreated = new HashMap<>();
        newTradeCreated.put("dataset", "cds_trades");
        newTradeCreated.put("trade_id", newTradeId);
        newTradeCreated.put("action", "novation_substitute");
        
        if (details.containsKey("newCounterparty") || details.containsKey("counterparty")) {
            newTradeCreated.put("counterparty", details.getOrDefault("newCounterparty", details.get("counterparty")));
        }
        outputs.put("new_trade_created", newTradeCreated);
        
        Map<String, Object> positionRebalanced = new HashMap<>();
        positionRebalanced.put("dataset", "portfolio_positions");
        positionRebalanced.put("action", "transfer_exposure");
        outputs.put("position_rebalanced", positionRebalanced);
        
        // Merge correlation metadata from details into outputs
        if (details != null) {
            details.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("_"))
                .forEach(entry -> outputs.put(entry.getKey(), entry.getValue()));
        }
        
        trackTransformation("cds_trades", "NOVATION",
                          inputs, outputs, userName, "novation-" + originalTradeId + "-to-" + newTradeId);
    }

    /**
     * Track lifecycle event (coupon, maturity, etc).
     */
    public void trackLifecycleOperation(String eventType, Long tradeId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        
        // Trade lookup
        Map<String, Object> tradeLookup = new HashMap<>();
        tradeLookup.put("dataset", "cds_trades");
        tradeLookup.put("trade_id", tradeId);
        tradeLookup.put("purpose", "lifecycle_processing");
        inputs.put("trade_lookup", tradeLookup);
        
        // Build inputs from tracked read operations
        if (details != null && details.containsKey("_tracked_tables_read")) {
            @SuppressWarnings("unchecked")
            List<String> trackedReads = (List<String>) details.get("_tracked_tables_read");
            for (String tableName : trackedReads) {
                inputs.put(tableName + "_read", tableName);
            }
        }
        
        // Schedule data
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("dataset", "payment_schedules");
        scheduleData.put("event_type", eventType);
        scheduleData.put("purpose", "payment_calculation");
        inputs.put("schedule_data", scheduleData);
        
        // Build outputs with actual lifecycle data
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> tradeUpdated = new HashMap<>();
        tradeUpdated.put("dataset", "cds_trades");
        tradeUpdated.put("trade_id", tradeId);
        tradeUpdated.put("action", "lifecycle_event_processed");
        
        // Include lifecycle details from AOP
        if (details != null) {
            if (details.containsKey("paymentDate")) {
                tradeUpdated.put("payment_date", details.get("paymentDate"));
            }
            if (details.containsKey("amount")) {
                tradeUpdated.put("payment_amount", details.get("amount"));
            }
        }
        outputs.put("trade_updated", tradeUpdated);
        
        Map<String, Object> cashflowGenerated = new HashMap<>();
        cashflowGenerated.put("dataset", "cashflows");
        cashflowGenerated.put("event_type", eventType);
        cashflowGenerated.put("action", "payment_scheduled");
        
        if (details != null && details.containsKey("amount")) {
            cashflowGenerated.put("amount", details.get("amount"));
        }
        outputs.put("cashflow_generated", cashflowGenerated);
        
        // Position closure on maturity
        if (eventType.equals("MATURITY")) {
            Map<String, Object> positionClosed = new HashMap<>();
            positionClosed.put("dataset", "portfolio_positions");
            positionClosed.put("action", "remove_matured_position");
            outputs.put("position_closed", positionClosed);
        }
        
        // Merge correlation metadata from details into outputs
        if (details != null) {
            outputs.putAll(details);
            
            // Extract tracked write operations and create explicit transformation records
            if (details.containsKey("_tracked_tables_written")) {
                @SuppressWarnings("unchecked")
                List<String> trackedWrites = (List<String>) details.get("_tracked_tables_written");
                
                // Store the list of affected tables for intelligence display
                outputs.put("affected_tables", trackedWrites);
                
                // Create structured records for each table affected
                List<Map<String, Object>> tableTransformations = new ArrayList<>();
                for (String tableName : trackedWrites) {
                    Map<String, Object> tableTransform = new HashMap<>();
                    tableTransform.put("table", tableName);
                    tableTransform.put("operation", "WRITE");
                    tableTransform.put("event_context", "lifecycle_" + eventType.toLowerCase());
                    
                    // Add context for known tables
                    switch (tableName) {
                        case "notional_adjustments":
                            tableTransform.put("description", "Notional adjustment record");
                            tableTransform.put("purpose", getTerminationPurpose(eventType, details));
                            break;
                        case "cds_trades":
                            tableTransform.put("description", "Trade status/notional updated");
                            tableTransform.put("purpose", getTradeUpdatePurpose(eventType, details));
                            break;
                        case "coupon_periods":
                            tableTransform.put("description", "Coupon payment recorded");
                            tableTransform.put("purpose", "Coupon marked as paid for trade " + tradeId);
                            break;
                        case "accrual_events":
                            tableTransform.put("description", "Daily accrual calculated");
                            tableTransform.put("purpose", "Premium accrual posted for accounting");
                            break;
                        case "trade_amendments":
                            tableTransform.put("description", "Trade amendment applied");
                            tableTransform.put("purpose", "Trade terms modified per amendment request");
                            break;
                        case "audit_logs":
                            tableTransform.put("description", "Audit trail recorded");
                            tableTransform.put("purpose", "Compliance and regulatory tracking");
                            break;
                        default:
                            tableTransform.put("description", "Data modified as part of lifecycle processing");
                            tableTransform.put("purpose", "Supporting data for " + eventType + " event");
                    }
                    
                    tableTransformations.add(tableTransform);
                }
                
                outputs.put("table_transformations", tableTransformations);
            }
        }
        
        trackTransformation("cds_lifecycles", "LIFECYCLE_" + eventType.toUpperCase(),
                          inputs, outputs, userName, "lifecycle-" + eventType + "-" + tradeId);
    }
    
    /**
     * Helper to generate termination-specific purpose descriptions
     */
    private String getTerminationPurpose(String eventType, Map<String, Object> details) {
        if (eventType.contains("FULL_TERMINATION")) {
            return "Trade fully terminated - entire notional closed";
        } else if (eventType.contains("PARTIAL_TERMINATION")) {
            if (details != null && details.containsKey("terminationAmount")) {
                return "Partial termination of " + details.get("terminationAmount") + " notional";
            }
            return "Partial termination - notional reduced";
        }
        return "Notional adjustment recorded";
    }
    
    /**
     * Helper to generate trade update purpose descriptions
     */
    private String getTradeUpdatePurpose(String eventType, Map<String, Object> details) {
        if (eventType.contains("TERMINATION")) {
            return "Trade status updated to TERMINATED";
        } else if (eventType.contains("COUPON")) {
            return "Last paid coupon date updated";
        } else if (eventType.contains("AMENDMENT")) {
            return "Trade terms updated per amendment";
        } else if (eventType.contains("ACCRUAL")) {
            return "Accrual balance updated";
        }
        return "Trade data modified for lifecycle event";
    }
}
