package com.creditdefaultswap.platform.lineage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Per-request correlation context for tracking the entire request lifecycle.
 * Automatically correlates controller → service → repository → entity operations.
 * 
 * Thread-safe and request-scoped.
 */
public class RequestCorrelationContext {
    
    private RequestCorrelationContext() {
        // Private constructor to prevent instantiation
    }
    
    private static final Logger logger = LoggerFactory.getLogger(RequestCorrelationContext.class);
    
    // Thread-local storage for per-request context
    private static final ThreadLocal<CorrelationContext> CONTEXT = new ThreadLocal<>();
    
    /**
     * Initialize context at the start of a request
     */
    public static void initialize(String correlationId, String httpMethod, String endpoint, String userName) {
        CorrelationContext context = new CorrelationContext(correlationId, httpMethod, endpoint, userName);
        CONTEXT.set(context);
        logger.debug("Initialized correlation context: {}", correlationId);
    }
    
    /**
     * Get current context (null if not initialized)
     */
    public static CorrelationContext get() {
        return CONTEXT.get();
    }
    
    /**
     * Check if context is initialized
     */
    public static boolean isInitialized() {
        return CONTEXT.get() != null;
    }
    
    /**
     * Clear context (call at end of request)
     */
    public static void clear() {
        CorrelationContext context = CONTEXT.get();
        if (context != null) {
            logger.debug("Clearing correlation context: {} (duration: {}ms)", 
                context.getCorrelationId(), context.getDuration());
        }
        CONTEXT.remove();
    }
    
    /**
     * Record controller layer information
     */
    public static void recordController(String controllerClass, String methodName, 
                                        Object requestDto, Map<String, String> pathVariables) {
        CorrelationContext context = get();
        if (context != null) {
            context.setControllerClass(controllerClass);
            context.setControllerMethod(methodName);
            context.setRequestDto(requestDto);
            context.setPathVariables(pathVariables);
            logger.trace("Recorded controller: {}.{}", controllerClass, methodName);
        }
    }
    
    /**
     * Record service layer call
     */
    public static void recordServiceCall(String serviceClass, String methodName, Object[] args) {
        CorrelationContext context = get();
        if (context != null) {
            context.addServiceCall(serviceClass, methodName, args);
            logger.trace("Recorded service call: {}.{}", serviceClass, methodName);
        }
    }
    
    /**
     * Record repository layer call
     */
    public static void recordRepositoryCall(String repositoryInterface, String methodName, 
                                           Object[] args, Object result) {
        CorrelationContext context = get();
        if (context != null) {
            context.addRepositoryCall(repositoryInterface, methodName, args, result);
            logger.trace("Recorded repository call: {}.{}", repositoryInterface, methodName);
        }
    }
    
    /**
     * Record entity operation (from JPA listener)
     */
    public static void recordEntityOperation(String tableName, String operationType, Object entityId) {
        CorrelationContext context = get();
        if (context != null) {
            context.addEntityOperation(tableName, operationType, entityId);
            logger.trace("Recorded entity operation: {} on {} (id: {})", operationType, tableName, entityId);
        }
    }
    
    /**
     * Record response DTO
     */
    public static void recordResponse(Object responseDto) {
        CorrelationContext context = get();
        if (context != null) {
            context.setResponseDto(responseDto);
            logger.trace("Recorded response DTO: {}", responseDto != null ? responseDto.getClass().getSimpleName() : "null");
        }
    }
    
    /**
     * Record audit information (IP, user agent, session, etc.)
     */
    public static void recordAuditInfo(Map<String, String> auditInfo) {
        CorrelationContext context = get();
        if (context != null) {
            context.setAuditInfo(auditInfo);
            logger.trace("Recorded audit info: {}", auditInfo);
        }
    }
    
    /**
     * Correlation context data class
     */
    public static class CorrelationContext {
        private final String correlationId;
        private final String httpMethod;
        private final String endpoint;
        private final String userName;
        private final long startTime;
        
        // Controller layer
        private String controllerClass;
        private String controllerMethod;
        private Object requestDto;
        private Map<String, String> pathVariables;
        
        // Service layer
        private final List<ServiceCall> serviceCalls = new ArrayList<>();
        
        // Repository layer
        private final List<RepositoryCall> repositoryCalls = new ArrayList<>();
        
        // Entity layer
        private final Map<String, List<EntityOperation>> entityOperations = new LinkedHashMap<>();
        
        // Response
        private Object responseDto;
        
        // Audit information
        private Map<String, String> auditInfo;
        
        public CorrelationContext(String correlationId, String httpMethod, String endpoint, String userName) {
            this.correlationId = correlationId;
            this.httpMethod = httpMethod;
            this.endpoint = endpoint;
            this.userName = userName;
            this.startTime = System.currentTimeMillis();
            this.auditInfo = new LinkedHashMap<>();
        }
        
        public void addServiceCall(String serviceClass, String methodName, Object[] args) {
            serviceCalls.add(new ServiceCall(serviceClass, methodName, args));
        }
        
        public void addRepositoryCall(String repositoryInterface, String methodName, Object[] args, Object result) {
            repositoryCalls.add(new RepositoryCall(repositoryInterface, methodName, args, result));
        }
        
        public void addEntityOperation(String tableName, String operationType, Object entityId) {
            entityOperations.computeIfAbsent(tableName, k -> new ArrayList<>())
                .add(new EntityOperation(operationType, entityId));
        }
        
        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
        
        // Getters and setters
        public String getCorrelationId() { return correlationId; }
        public String getHttpMethod() { return httpMethod; }
        public String getEndpoint() { return endpoint; }
        public String getUserName() { return userName; }
        public long getStartTime() { return startTime; }
        
        public String getControllerClass() { return controllerClass; }
        public void setControllerClass(String controllerClass) { this.controllerClass = controllerClass; }
        
        public String getControllerMethod() { return controllerMethod; }
        public void setControllerMethod(String controllerMethod) { this.controllerMethod = controllerMethod; }
        
        public Object getRequestDto() { return requestDto; }
        public void setRequestDto(Object requestDto) { this.requestDto = requestDto; }
        
        public Map<String, String> getPathVariables() { return pathVariables; }
        public void setPathVariables(Map<String, String> pathVariables) { this.pathVariables = pathVariables; }
        
        public List<ServiceCall> getServiceCalls() { return new ArrayList<>(serviceCalls); }
        public List<RepositoryCall> getRepositoryCalls() { return new ArrayList<>(repositoryCalls); }
        public Map<String, List<EntityOperation>> getEntityOperations() { return new LinkedHashMap<>(entityOperations); }
        
        public Object getResponseDto() { return responseDto; }
        public void setResponseDto(Object responseDto) { this.responseDto = responseDto; }
        
        public Map<String, String> getAuditInfo() { return new LinkedHashMap<>(auditInfo); }
        public void setAuditInfo(Map<String, String> auditInfo) { 
            if (auditInfo != null) {
                this.auditInfo = new LinkedHashMap<>(auditInfo); 
            }
        }
        
        /**
         * Get unique tables that were read
         */
        public Set<String> getTablesRead() {
            Set<String> tables = new LinkedHashSet<>();
            for (Map.Entry<String, List<EntityOperation>> entry : entityOperations.entrySet()) {
                for (EntityOperation op : entry.getValue()) {
                    if ("READ".equals(op.getOperationType())) {
                        tables.add(entry.getKey());
                        break;
                    }
                }
            }
            return tables;
        }
        
        /**
         * Get unique tables that were written
         */
        public Set<String> getTablesWritten() {
            Set<String> tables = new LinkedHashSet<>();
            for (Map.Entry<String, List<EntityOperation>> entry : entityOperations.entrySet()) {
                for (EntityOperation op : entry.getValue()) {
                    if ("INSERT".equals(op.getOperationType()) || "UPDATE".equals(op.getOperationType())) {
                        tables.add(entry.getKey());
                        break;
                    }
                }
            }
            return tables;
        }
        
        /**
         * Generate summary for logging
         */
        public String getSummary() {
            return String.format(
                "Request[%s %s] -> %d services -> %d repositories -> %d entities (%d reads, %d writes) in %dms",
                httpMethod, endpoint, 
                serviceCalls.size(), 
                repositoryCalls.size(),
                entityOperations.values().stream().mapToInt(List::size).sum(),
                getTablesRead().size(),
                getTablesWritten().size(),
                getDuration()
            );
        }
    }
    
    /**
     * Service call record
     */
    public static class ServiceCall {
        private final String serviceClass;
        private final String methodName;
        private final Object[] args;
        private final long timestamp;
        
        public ServiceCall(String serviceClass, String methodName, Object[] args) {
            this.serviceClass = serviceClass;
            this.methodName = methodName;
            this.args = args;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getServiceClass() { return serviceClass; }
        public String getMethodName() { return methodName; }
        public Object[] getArgs() { return args; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Repository call record
     */
    public static class RepositoryCall {
        private final String repositoryInterface;
        private final String methodName;
        private final Object[] args;
        private final Object result;
        private final long timestamp;
        
        public RepositoryCall(String repositoryInterface, String methodName, Object[] args, Object result) {
            this.repositoryInterface = repositoryInterface;
            this.methodName = methodName;
            this.args = args;
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getRepositoryInterface() { return repositoryInterface; }
        public String getMethodName() { return methodName; }
        public Object[] getArgs() { return args; }
        public Object getResult() { return result; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Entity operation record
     */
    public static class EntityOperation {
        private final String operationType;
        private final Object entityId;
        private final long timestamp;
        
        public EntityOperation(String operationType, Object entityId) {
            this.operationType = operationType;
            this.entityId = entityId;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getOperationType() { return operationType; }
        public Object getEntityId() { return entityId; }
        public long getTimestamp() { return timestamp; }
    }
}
