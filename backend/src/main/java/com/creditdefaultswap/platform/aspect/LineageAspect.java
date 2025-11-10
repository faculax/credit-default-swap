package com.creditdefaultswap.platform.aspect;

import com.creditdefaultswap.platform.annotation.LineageOperationType;
import com.creditdefaultswap.platform.annotation.TrackLineage;
import com.creditdefaultswap.platform.service.LineageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AOP Aspect for automatic data lineage tracking.
 * 
 * Intercepts controller methods annotated with @TrackLineage and automatically:
 * - Extracts entity IDs from results or path variables
 * - Builds detailed input/output maps from request/response
 * - Routes to appropriate LineageService tracking method
 * - Handles errors gracefully without breaking business logic
 * 
 * Epic 10: Reporting, Audit & Replay
 */
@Aspect
@Component
public class LineageAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LineageAspect.class);
    
    @Autowired
    private LineageService lineageService;
    
    /**
     * Intercept methods annotated with @TrackLineage after successful execution
     */
    @AfterReturning(
        pointcut = "@annotation(trackLineage)",
        returning = "result"
    )
    public void trackLineageAfterSuccess(JoinPoint joinPoint, TrackLineage trackLineage, Object result) {
        try {
            // Extract method details
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            
            // Extract entity ID
            String entityId = extractEntityId(trackLineage, args, result, signature);
            if (entityId == null) {
                logger.warn("Could not extract entity ID for lineage tracking: {}", signature.getName());
                return;
            }
            
            // Build lineage details map
            Map<String, Object> details = buildDetailsMap(trackLineage, args, result, signature);
            
            // Get operation name
            String operation = trackLineage.operation().isEmpty() 
                ? signature.getName().toUpperCase() 
                : trackLineage.operation();
            
            // Route to appropriate tracking method
            routeToTracker(trackLineage.operationType(), operation, entityId, trackLineage.actor(), details);
            
            logger.debug("Lineage tracked: {} {} for entity {}", 
                trackLineage.operationType(), operation, entityId);
            
        } catch (Exception e) {
            // Never break business logic due to lineage tracking failures
            logger.error("Lineage tracking failed for {}: {}", 
                joinPoint.getSignature().getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Extract entity ID from result or path variables
     */
    private String extractEntityId(TrackLineage annotation, Object[] args, Object result, MethodSignature signature) {
        // Try extracting from result first
        if (!annotation.entityIdFromResult().isEmpty()) {
            String id = extractFromResult(result, annotation.entityIdFromResult());
            if (id != null) return id;
        }
        
        // Try extracting from @PathVariable
        Parameter[] parameters = signature.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PathVariable pathVar = parameters[i].getAnnotation(PathVariable.class);
            if (pathVar != null) {
                String paramName = pathVar.value().isEmpty() ? parameters[i].getName() : pathVar.value();
                if (paramName.equals(annotation.entityIdParam()) || 
                    paramName.equals("id") || 
                    paramName.endsWith("Id")) {
                    return String.valueOf(args[i]);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract ID from result object (handles ResponseEntity unwrapping)
     */
    private String extractFromResult(Object result, String path) {
        try {
            Object target = result;
            
            // Unwrap ResponseEntity
            if (target instanceof ResponseEntity) {
                target = ((ResponseEntity<?>) target).getBody();
            }
            
            if (target == null) return null;
            
            // Try direct field access
            try {
                Field field = target.getClass().getDeclaredField(path);
                field.setAccessible(true);
                Object value = field.get(target);
                return value != null ? String.valueOf(value) : null;
            } catch (NoSuchFieldException e) {
                // Try getter method
                String getterName = "get" + path.substring(0, 1).toUpperCase() + path.substring(1);
                Method getter = target.getClass().getMethod(getterName);
                Object value = getter.invoke(target);
                return value != null ? String.valueOf(value) : null;
            }
        } catch (Exception e) {
            logger.debug("Could not extract ID from result: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Build details map from method arguments and result
     */
    private Map<String, Object> buildDetailsMap(TrackLineage annotation, Object[] args, 
                                                  Object result, MethodSignature signature) {
        Map<String, Object> details = new HashMap<>();
        
        if (!annotation.autoExtractDetails()) {
            return details;
        }
        
        // Extract from @RequestBody parameter
        Parameter[] parameters = signature.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (requestBody != null && args[i] != null) {
                extractFieldsFromObject(args[i], details, annotation.includeFields(), annotation.excludeFields());
                break;
            }
        }
        
        // Extract from result if needed
        extractFieldsFromResult(result, details, annotation);
        
        return details;
    }
    
    /**
     * Extract fields from an object (request DTO)
     */
    private void extractFieldsFromObject(Object obj, Map<String, Object> details, 
                                          String[] includeFields, String[] excludeFields) {
        try {
            Set<String> includeSet = includeFields.length > 0 ? new HashSet<>(Arrays.asList(includeFields)) : null;
            Set<String> excludeSet = new HashSet<>(Arrays.asList(excludeFields));
            
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                
                // Skip excluded fields
                if (excludeSet.contains(fieldName)) continue;
                
                // Check include list
                if (includeSet != null && !includeSet.contains(fieldName)) continue;
                
                Object value = field.get(obj);
                if (value != null) {
                    details.put(fieldName, convertToSerializable(value));
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract fields from object: {}", e.getMessage());
        }
    }
    
    /**
     * Extract useful fields from result
     */
    private void extractFieldsFromResult(Object result, Map<String, Object> details, TrackLineage annotation) {
        try {
            Object target = result;
            
            // Unwrap ResponseEntity
            if (target instanceof ResponseEntity) {
                target = ((ResponseEntity<?>) target).getBody();
            }
            
            if (target == null) return;
            
            // Add common response fields if not already present
            addIfNotPresent(details, target, "id");
            addIfNotPresent(details, target, "status");
            addIfNotPresent(details, target, "calculationId");
            addIfNotPresent(details, target, "basketId");
            addIfNotPresent(details, target, "bondId");
            addIfNotPresent(details, target, "portfolioId");
            
        } catch (Exception e) {
            logger.debug("Could not extract fields from result: {}", e.getMessage());
        }
    }
    
    /**
     * Add field to details if exists and not already present
     */
    private void addIfNotPresent(Map<String, Object> details, Object obj, String fieldName) {
        if (details.containsKey(fieldName)) return;
        
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            if (value != null) {
                details.put(fieldName, convertToSerializable(value));
            }
        } catch (Exception e) {
            // Field doesn't exist, ignore
        }
    }
    
    /**
     * Convert value to serializable format
     */
    private Object convertToSerializable(Object value) {
        if (value == null) return null;
        
        // Primitive types and strings
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        
        // Dates
        if (value instanceof LocalDate || value instanceof LocalDateTime) {
            return value.toString();
        }
        
        // BigDecimal
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        
        // Collections - return size
        if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        }
        
        // Maps
        if (value instanceof Map) {
            return value;
        }
        
        // Default: convert to string
        return value.toString();
    }
    
    /**
     * Route to appropriate LineageService tracking method
     */
    private void routeToTracker(LineageOperationType type, String operation, 
                                 String entityId, String actor, Map<String, Object> details) {
        try {
            switch (type) {
                case TRADE:
                    lineageService.trackTradeCapture(Long.parseLong(entityId), operation, actor, details);
                    break;
                case BOND:
                    lineageService.trackBondOperation(operation, Long.parseLong(entityId), actor, details);
                    break;
                case PORTFOLIO:
                    lineageService.trackPortfolioOperation(operation, Long.parseLong(entityId), actor, details);
                    break;
                case BASKET:
                    lineageService.trackBasketOperation(operation, Long.parseLong(entityId), actor, details);
                    break;
                case MARGIN:
                    lineageService.trackMarginOperation(operation, entityId, actor, details);
                    break;
                case LIFECYCLE:
                    lineageService.trackLifecycleOperation(operation, Long.parseLong(entityId), actor, details);
                    break;
                case NOVATION:
                    // Extract original and new trade IDs from details
                    if (details.containsKey("originalTradeId")) {
                        String originalTradeId = details.get("originalTradeId").toString();
                        lineageService.trackNovationOperation(Long.parseLong(originalTradeId), 
                            Long.parseLong(entityId), actor, details);
                    }
                    break;
                case PRICING:
                    lineageService.trackPricingCalculation(operation, Long.parseLong(entityId), "MARKET", actor);
                    break;
                case CREDIT_EVENT:
                    // trackCreditEvent has different signature, log for now
                    logger.info("Credit event lineage: {} for entity {}", operation, entityId);
                    break;
                case GENERIC:
                default:
                    logger.debug("Generic lineage tracking for {} operation on entity {}", operation, entityId);
                    break;
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse entity ID for lineage tracking: {}", entityId);
        }
    }
}
