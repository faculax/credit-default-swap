package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.LineageEvent;
import com.creditdefaultswap.platform.repository.LineageEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
     * Runs asynchronously to avoid blocking main operations.
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
            event.setOutputs(outputs);
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
     * Track CDS trade capture with comprehensive lineage.
     */
    public void trackTradeCapture(Long tradeId, String tradeType, String userName, Map<String, Object> tradeDetails) {
        // Comprehensive inputs: what data sources were read/consulted
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("ui_trade_entry", Map.of(
            "source", "user_interface",
            "form", "new_single_name_cds",
            "user", userName
        ));
        
        // Reference data lookup
        inputs.put("reference_data_lookup", Map.of(
            "dataset", "issuer_master",
            "entity", tradeDetails.getOrDefault("entityName", "UNKNOWN"),
            "purpose", "entity_validation_and_enrichment"
        ));
        
        // Market data lookup
        if (tradeDetails.containsKey("spread") || tradeDetails.containsKey("upfrontAmount")) {
            inputs.put("market_data_lookup", Map.of(
                "dataset", "market_quotes",
                "spread_bps", tradeDetails.getOrDefault("spread", 0),
                "purpose", "pricing_reference"
            ));
        }
        
        // Position lookup (check existing exposures)
        inputs.put("position_check", Map.of(
            "dataset", "portfolio_positions",
            "entity", tradeDetails.getOrDefault("entityName", "UNKNOWN"),
            "purpose", "concentration_risk_check"
        ));
        
        // Comprehensive outputs: all downstream impacts
        Map<String, Object> outputs = new HashMap<>();
        
        // Primary: trade record created
        outputs.put("trade_record_created", Map.of(
            "dataset", "cds_trades",
            "trade_id", tradeId,
            "notional", tradeDetails.getOrDefault("notional", 0),
            "entity", tradeDetails.getOrDefault("entityName", "UNKNOWN"),
            "maturity", tradeDetails.getOrDefault("maturityDate", ""),
            "spread_bps", tradeDetails.getOrDefault("spread", 0)
        ));
        
        // Portfolio position updated
        outputs.put("position_updated", Map.of(
            "dataset", "portfolio_positions",
            "entity", tradeDetails.getOrDefault("entityName", "UNKNOWN"),
            "action", "increment_notional",
            "notional_delta", tradeDetails.getOrDefault("notional", 0)
        ));
        
        // Valuation scheduled
        outputs.put("valuation_scheduled", Map.of(
            "dataset", "cds_valuations",
            "trade_id", tradeId,
            "action", "mark_to_market_required",
            "trigger", "new_trade_booking"
        ));
        
        // Risk calculation triggered
        outputs.put("risk_metrics_triggered", Map.of(
            "dataset", "risk_metrics",
            "entity", tradeDetails.getOrDefault("entityName", "UNKNOWN"),
            "action", "recalculate_credit_exposure",
            "metrics", java.util.List.of("credit_exposure", "counterparty_risk", "concentration_risk", "jump_to_default")
        ));
        
        // Margin calculation triggered
        outputs.put("margin_calculation_triggered", Map.of(
            "dataset", "margin_requirements",
            "action", "recalculate_im_vm",
            "methods", java.util.List.of("SIMM", "SA-CCR"),
            "trade_id", tradeId
        ));
        
        // Regulatory reporting impact
        outputs.put("regulatory_reporting_flagged", Map.of(
            "dataset", "regulatory_reports",
            "reports", java.util.List.of("EMIR", "Dodd-Frank", "MiFID_II"),
            "action", "include_in_next_submission"
        ));
        
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
     * Track bond creation/update with comprehensive lineage.
     */
    public void trackBondOperation(String operation, Long bondId, String userName, Map<String, Object> bondDetails) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("ui_bond_entry", Map.of(
            "source", "user_interface",
            "form", "bond_management",
            "user", userName
        ));
        
        inputs.put("reference_data_lookup", Map.of(
            "dataset", "issuer_master",
            "issuer", bondDetails.getOrDefault("issuer", "UNKNOWN"),
            "purpose", "issuer_validation"
        ));
        
        inputs.put("market_data_lookup", Map.of(
            "dataset", "market_quotes",
            "coupon_rate", bondDetails.getOrDefault("couponRate", 0),
            "purpose", "yield_curve_reference"
        ));
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("bond_record_created", Map.of(
            "dataset", "bonds",
            "bond_id", bondId,
            "issuer", bondDetails.getOrDefault("issuer", "UNKNOWN"),
            "face_value", bondDetails.getOrDefault("faceValue", 0),
            "maturity", bondDetails.getOrDefault("maturityDate", "")
        ));
        
        outputs.put("portfolio_updated", Map.of(
            "dataset", "portfolio_positions",
            "issuer", bondDetails.getOrDefault("issuer", "UNKNOWN"),
            "action", "add_bond_position"
        ));
        
        outputs.put("valuation_scheduled", Map.of(
            "dataset", "bond_valuations",
            "bond_id", bondId,
            "action", "price_calculation_required"
        ));
        
        trackTransformation("bonds", operation.toUpperCase() + "_BOND", 
                          inputs, outputs, userName, "bond-" + operation + "-" + bondId);
    }

    /**
     * Track portfolio aggregation operation.
     */
    public void trackPortfolioOperation(String operation, Long portfolioId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("cds_trades_lookup", Map.of(
            "dataset", "cds_trades",
            "trade_count", details.getOrDefault("tradeCount", 0),
            "purpose", "position_aggregation"
        ));
        
        inputs.put("bonds_lookup", Map.of(
            "dataset", "bonds",
            "bond_count", details.getOrDefault("bondCount", 0),
            "purpose", "fixed_income_aggregation"
        ));
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("portfolio_created", Map.of(
            "dataset", "cds_portfolios",
            "portfolio_id", portfolioId,
            "total_positions", details.getOrDefault("totalPositions", 0)
        ));
        
        outputs.put("risk_metrics_triggered", Map.of(
            "dataset", "portfolio_risk_metrics",
            "action", "calculate_portfolio_var",
            "metrics", java.util.List.of("var", "cvar", "concentration")
        ));
        
        trackTransformation("cds_portfolios", operation.toUpperCase() + "_PORTFOLIO",
                          inputs, outputs, userName, "portfolio-" + operation + "-" + portfolioId);
    }

    /**
     * Track margin calculation operation.
     */
    public void trackMarginOperation(String marginType, String accountId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("positions_lookup", Map.of(
            "dataset", "cds_trades",
            "purpose", "margin_calculation_input"
        ));
        
        inputs.put("market_data_lookup", Map.of(
            "dataset", "market_quotes",
            "purpose", "sensitivity_calculation"
        ));
        
        inputs.put("risk_factors", Map.of(
            "dataset", "risk_factors",
            "purpose", marginType.toLowerCase() + "_calculation"
        ));
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("margin_calculated", Map.of(
            "dataset", "margin_requirements",
            "margin_type", marginType,
            "account_id", accountId,
            "amount", details.getOrDefault("marginAmount", 0)
        ));
        
        outputs.put("margin_call_check", Map.of(
            "dataset", "margin_calls",
            "action", "evaluate_margin_breach",
            "threshold_check", "performed"
        ));
        
        trackTransformation("margin_requirements", "MARGIN_" + marginType.toUpperCase(),
                          inputs, outputs, userName, "margin-" + marginType + "-" + accountId);
    }

    /**
     * Track basket/index operation.
     */
    public void trackBasketOperation(String operation, Long basketId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("constituent_lookup", Map.of(
            "dataset", "reference_entities",
            "constituent_count", details.getOrDefault("constituentCount", 0),
            "purpose", "basket_composition"
        ));
        
        inputs.put("weights_calculation", Map.of(
            "source", "index_methodology",
            "purpose", "weight_determination"
        ));
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("basket_created", Map.of(
            "dataset", "cds_baskets",
            "basket_id", basketId,
            "constituents", details.getOrDefault("constituentCount", 0)
        ));
        
        outputs.put("index_pricing_triggered", Map.of(
            "dataset", "index_prices",
            "action", "calculate_index_spread",
            "basket_id", basketId
        ));
        
        trackTransformation("cds_baskets", operation.toUpperCase() + "_BASKET",
                          inputs, outputs, userName, "basket-" + operation + "-" + basketId);
    }

    /**
     * Track novation operation.
     */
    public void trackNovationOperation(Long originalTradeId, Long newTradeId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("original_trade", Map.of(
            "dataset", "cds_trades",
            "trade_id", originalTradeId,
            "purpose", "novation_source"
        ));
        
        inputs.put("counterparty_validation", Map.of(
            "dataset", "counterparties",
            "new_counterparty", details.getOrDefault("newCounterparty", "UNKNOWN"),
            "purpose", "novation_approval"
        ));
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("original_trade_updated", Map.of(
            "dataset", "cds_trades",
            "trade_id", originalTradeId,
            "action", "mark_novated"
        ));
        
        outputs.put("new_trade_created", Map.of(
            "dataset", "cds_trades",
            "trade_id", newTradeId,
            "action", "novation_substitute"
        ));
        
        outputs.put("position_rebalanced", Map.of(
            "dataset", "portfolio_positions",
            "action", "transfer_exposure"
        ));
        
        trackTransformation("cds_trades", "NOVATION",
                          inputs, outputs, userName, "novation-" + originalTradeId + "-to-" + newTradeId);
    }

    /**
     * Track lifecycle event (coupon, maturity, etc).
     */
    public void trackLifecycleOperation(String eventType, Long tradeId, String userName, Map<String, Object> details) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("trade_lookup", Map.of(
            "dataset", "cds_trades",
            "trade_id", tradeId,
            "purpose", "lifecycle_processing"
        ));
        
        inputs.put("schedule_data", Map.of(
            "dataset", "payment_schedules",
            "event_type", eventType,
            "purpose", "payment_calculation"
        ));
        
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("trade_updated", Map.of(
            "dataset", "cds_trades",
            "trade_id", tradeId,
            "action", "lifecycle_event_processed"
        ));
        
        outputs.put("cashflow_generated", Map.of(
            "dataset", "cashflows",
            "event_type", eventType,
            "action", "payment_scheduled"
        ));
        
        if (eventType.equals("MATURITY")) {
            outputs.put("position_closed", Map.of(
                "dataset", "portfolio_positions",
                "action", "remove_matured_position"
            ));
        }
        
        trackTransformation("cds_trades", "LIFECYCLE_" + eventType.toUpperCase(),
                          inputs, outputs, userName, "lifecycle-" + eventType + "-" + tradeId);
    }
}
