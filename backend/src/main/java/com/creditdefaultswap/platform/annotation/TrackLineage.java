package com.creditdefaultswap.platform.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically track data lineage for controller methods.
 * 
 * When applied to a controller method, the LineageAspect will automatically:
 * - Extract operation details from method parameters and return value
 * - Build comprehensive input/output maps
 * - Track lineage events in the database
 * 
 * Usage:
 * <pre>
 * {@code
 * @PostMapping
 * @TrackLineage(
 *     operationType = LineageOperationType.TRADE,
 *     operation = "CREATE",
 *     entityIdFromResult = "id"
 * )
 * public ResponseEntity<?> createTrade(@RequestBody TradeRequest request) {
 *     // Business logic
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackLineage {
    
    /**
     * The type of operation being performed
     */
    LineageOperationType operationType();
    
    /**
     * Specific operation name (e.g., "CREATE", "UPDATE", "PRICE", "CALCULATE")
     */
    String operation() default "";
    
    /**
     * Path to extract entity ID from the result object (e.g., "id", "getId()")
     * Leave empty to extract from @PathVariable
     */
    String entityIdFromResult() default "";
    
    /**
     * Name of the @PathVariable parameter containing the entity ID
     * Leave empty if ID should be extracted from result
     */
    String entityIdParam() default "id";
    
    /**
     * Custom actor/user performing the operation
     * Default: "system"
     */
    String actor() default "system";
    
    /**
     * Whether to auto-extract all request body fields as lineage details
     * Default: true
     */
    boolean autoExtractDetails() default true;
    
    /**
     * Specific fields to include from request body (if not auto-extracting all)
     * Empty array means include all fields when autoExtractDetails=true
     */
    String[] includeFields() default {};
    
    /**
     * Fields to exclude from lineage details (e.g., sensitive data)
     */
    String[] excludeFields() default {};
}
