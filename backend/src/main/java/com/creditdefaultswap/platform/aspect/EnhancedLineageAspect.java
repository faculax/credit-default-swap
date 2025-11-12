package com.creditdefaultswap.platform.aspect;

import com.creditdefaultswap.platform.annotation.TrackLineage;
import com.creditdefaultswap.platform.lineage.DatabaseOperationTracker;
import com.creditdefaultswap.platform.lineage.RequestCorrelationContext;
import com.creditdefaultswap.platform.lineage.RequestCorrelationContext.CorrelationContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced lineage tracking aspect that enriches lineage events with correlation data.
 * Works with existing LineageAspect to add multi-layer tracing metadata.
 * 
 * Order = 2 (runs after ControllerTracingAspect which is Order=1, but before LineageAspect which is Order=5)
 */
@Aspect
@Component
@Order(2)
public class EnhancedLineageAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedLineageAspect.class);
    
    public EnhancedLineageAspect() {
        logger.error("==== EnhancedLineageAspect BEAN CREATED ====");
    }
    
    @Value("${lineage.correlation.enabled:true}")
    private boolean correlationEnabled;
    
    /**
     * Enrich lineage tracking with correlation context data
     */
    @Around("@annotation(com.creditdefaultswap.platform.annotation.TrackLineage)")
    public Object enrichWithCorrelationData(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.error("==== EnhancedLineageAspect @Around TRIGGERED for: {} ====", joinPoint.getSignature().getName());
        
        // Extract the annotation from the method
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        TrackLineage trackLineage = method.getAnnotation(TrackLineage.class);
        
        if (trackLineage == null) {
            logger.error("@TrackLineage annotation not found on method!");
            return joinPoint.proceed();
        }
        
        if (!correlationEnabled) {
            return joinPoint.proceed();
        }
        
        try {
            // Get correlation context BEFORE executing method
            CorrelationContext context = RequestCorrelationContext.get();
            if (context == null) {
                logger.warn("No correlation context available for lineage enrichment");
                return joinPoint.proceed();
            }
            
            // Inject initial correlation data BEFORE method execution
            logger.debug("Injecting correlation data BEFORE method execution");
            injectCorrelationData(context, trackLineage);
            
            // Execute method - service/repo calls will populate the context
            Object result = joinPoint.proceed();
            
            // Update correlation data AFTER execution to capture complete call chains
            logger.debug("Updating correlation data AFTER method execution with complete call chains");
            updateCorrelationDataAfterExecution(context);
            
            // Log summary
            logCorrelationSummary(context, trackLineage);
            
            return result;
            
        } catch (Throwable e) {
            logger.error("Error in correlation tracking: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Update correlation data AFTER method execution to capture complete service/repo chains
     */
    private void updateCorrelationDataAfterExecution(CorrelationContext context) {
        // Get existing metadata and update it with complete call chains
        Map<String, Object> correlationMetadata = DatabaseOperationTracker.getCorrelationMetadata();
        
        // Update duration (if not already set)
        if (!correlationMetadata.containsKey("_duration_ms")) {
            correlationMetadata.put("_duration_ms", context.getDuration());
        }
        
        // Update service call chain (now complete after execution)
        if (!context.getServiceCalls().isEmpty()) {
            List<String> serviceCallChain = context.getServiceCalls().stream()
                .map(call -> call.getServiceClass() + "." + call.getMethodName())
                .collect(Collectors.toList());
            correlationMetadata.put("_service_call_chain", serviceCallChain);
            correlationMetadata.put("_service_call_count", context.getServiceCalls().size());
            
            // Add detailed service calls with timestamps
            List<Map<String, Object>> serviceCallDetails = context.getServiceCalls().stream()
                .map(call -> {
                    Map<String, Object> callDetail = new LinkedHashMap<>();
                    callDetail.put("class", call.getServiceClass());
                    callDetail.put("method", call.getMethodName());
                    callDetail.put("timestamp", call.getTimestamp());
                    return callDetail;
                })
                .collect(Collectors.toList());
            correlationMetadata.put("_service_calls_detailed", serviceCallDetails);
        }
        
        // Update repository call chain (now complete after execution)
        if (!context.getRepositoryCalls().isEmpty()) {
            List<String> repoCallChain = context.getRepositoryCalls().stream()
                .map(call -> call.getRepositoryInterface() + "." + call.getMethodName())
                .collect(Collectors.toList());
            correlationMetadata.put("_repository_call_chain", repoCallChain);
            correlationMetadata.put("_repository_call_count", context.getRepositoryCalls().size());
            
            // Add detailed repository calls with timestamps
            List<Map<String, Object>> repoCallDetails = context.getRepositoryCalls().stream()
                .map(call -> {
                    Map<String, Object> callDetail = new LinkedHashMap<>();
                    callDetail.put("interface", call.getRepositoryInterface());
                    callDetail.put("method", call.getMethodName());
                    callDetail.put("timestamp", call.getTimestamp());
                    callDetail.put("type", "SpringData");
                    return callDetail;
                })
                .collect(Collectors.toList());
            correlationMetadata.put("_repository_calls_detailed", repoCallDetails);
        }
        
        // Update tables read/written
        Set<String> tablesRead = context.getTablesRead();
        Set<String> tablesWritten = context.getTablesWritten();
        if (!tablesRead.isEmpty()) {
            correlationMetadata.put("_tables_read_from_context", new ArrayList<>(tablesRead));
        }
        if (!tablesWritten.isEmpty()) {
            correlationMetadata.put("_tables_written_from_context", new ArrayList<>(tablesWritten));
        }
        
        // Update the metadata in tracker
        DatabaseOperationTracker.setCorrelationMetadata(correlationMetadata);
        
        logger.debug("Updated correlation data after execution: {} fields", correlationMetadata.size());
    }
    
    /**
     * Inject correlation data into tracking context for LineageAspect to include
     */
    private void injectCorrelationData(CorrelationContext context, TrackLineage annotation) {
        // Store correlation metadata in thread-local for LineageAspect to access
        Map<String, Object> correlationMetadata = new LinkedHashMap<>();
        
        // Request metadata
        correlationMetadata.put("_correlation_id", context.getCorrelationId());
        correlationMetadata.put("_http_method", context.getHttpMethod());
        correlationMetadata.put("_endpoint", context.getEndpoint());
        correlationMetadata.put("_user_name", context.getUserName());
        correlationMetadata.put("_duration_ms", context.getDuration());
        
        // Controller info
        if (context.getControllerClass() != null) {
            correlationMetadata.put("_controller_class", context.getControllerClass());
            correlationMetadata.put("_controller_method", context.getControllerMethod());
        }
        
        // Request/Response DTOs
        if (context.getRequestDto() != null) {
            correlationMetadata.put("_request_dto_type", context.getRequestDto().getClass().getSimpleName());
        }
        if (context.getResponseDto() != null) {
            correlationMetadata.put("_response_dto_type", context.getResponseDto().getClass().getSimpleName());
        }
        
        // Path variables
        if (context.getPathVariables() != null && !context.getPathVariables().isEmpty()) {
            correlationMetadata.put("_path_variables", new LinkedHashMap<>(context.getPathVariables()));
        }
        
        // Service call chain
        if (!context.getServiceCalls().isEmpty()) {
            List<String> serviceCallChain = context.getServiceCalls().stream()
                .map(call -> call.getServiceClass() + "." + call.getMethodName())
                .collect(Collectors.toList());
            correlationMetadata.put("_service_call_chain", serviceCallChain);
            correlationMetadata.put("_service_call_count", context.getServiceCalls().size());
        }
        
        // Repository query chain
        if (!context.getRepositoryCalls().isEmpty()) {
            List<String> repoCallChain = context.getRepositoryCalls().stream()
                .map(call -> call.getRepositoryInterface() + "." + call.getMethodName())
                .collect(Collectors.toList());
            correlationMetadata.put("_repository_call_chain", repoCallChain);
            correlationMetadata.put("_repository_call_count", context.getRepositoryCalls().size());
        }
        
        // Entity operations summary
        Map<String, List<String>> entityOps = new LinkedHashMap<>();
        context.getEntityOperations().forEach((table, operations) -> {
            List<String> opSummary = operations.stream()
                .map(op -> op.getOperationType())
                .collect(Collectors.toList());
            entityOps.put(table, opSummary);
        });
        if (!entityOps.isEmpty()) {
            correlationMetadata.put("_entity_operations", entityOps);
        }
        
        // Store in DatabaseOperationTracker's context
        DatabaseOperationTracker.setCorrelationMetadata(correlationMetadata);
        
        logger.debug("Injected correlation data: {} fields", correlationMetadata.size());
    }
    
    /**
     * Log correlation summary
     */
    private void logCorrelationSummary(CorrelationContext context, TrackLineage annotation) {
        logger.info("=== Correlation Context Summary ===");
        logger.info("Correlation ID: {}", context.getCorrelationId());
        logger.info("Request: {} {}", context.getHttpMethod(), context.getEndpoint());
        logger.info("User: {}", context.getUserName());
        logger.info("Operation: {} - {}", annotation.operationType(), annotation.operation());
        logger.info("Duration: {}ms", context.getDuration());
        
        if (!context.getServiceCalls().isEmpty()) {
            logger.info("Service calls: {}", context.getServiceCalls().size());
            context.getServiceCalls().forEach(call -> 
                logger.info("  - {}.{}", call.getServiceClass(), call.getMethodName())
            );
        }
        
        if (!context.getRepositoryCalls().isEmpty()) {
            logger.info("Repository calls: {}", context.getRepositoryCalls().size());
            context.getRepositoryCalls().forEach(call -> 
                logger.info("  - {}.{}", call.getRepositoryInterface(), call.getMethodName())
            );
        }
        
        Set<String> tablesRead = context.getTablesRead();
        Set<String> tablesWritten = context.getTablesWritten();
        
        if (!tablesRead.isEmpty()) {
            logger.info("Tables READ: {}", tablesRead);
        }
        
        if (!tablesWritten.isEmpty()) {
            logger.info("Tables WRITTEN: {}", tablesWritten);
        }
        
        logger.info("=====================================");
    }
}

