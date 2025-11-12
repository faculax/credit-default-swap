package com.creditdefaultswap.platform.aspect;

import com.creditdefaultswap.platform.lineage.RequestCorrelationContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Intercepts REST controller methods to initialize request correlation context.
 * Extracts request DTOs, path variables, and response DTOs automatically.
 * 
 * Order = 1 (runs first, before service/repository aspects)
 */
@Aspect
@Component
@Order(1)
public class ControllerTracingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(ControllerTracingAspect.class);
    
    /**
     * Intercept all @RestController methods
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object traceControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Check if method has request mapping
        if (!hasRequestMapping(method)) {
            return joinPoint.proceed();
        }
        
        try {
            // Generate correlation ID
            String correlationId = UUID.randomUUID().toString();
            
            // Extract HTTP method and endpoint
            String httpMethod = extractHttpMethod(method);
            String endpoint = extractEndpoint(method, joinPoint.getTarget().getClass());
            
            // Extract user from security context
            String userName = extractUserName();
            
            // Initialize correlation context
            RequestCorrelationContext.initialize(correlationId, httpMethod, endpoint, userName);
            
            // Record audit information
            Map<String, String> auditInfo = extractAuditInfo();
            RequestCorrelationContext.recordAuditInfo(auditInfo);
            
            // Record controller information
            String controllerClass = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = method.getName();
            
            // Extract request DTO and path variables
            Object requestDto = extractRequestDto(method, joinPoint.getArgs());
            Map<String, String> pathVariables = extractPathVariables(method, joinPoint.getArgs());
            
            RequestCorrelationContext.recordController(controllerClass, methodName, requestDto, pathVariables);
            
            logger.debug("Starting request trace: {} {} [{}]", httpMethod, endpoint, correlationId);
            
            // Execute controller method
            Object result = joinPoint.proceed();
            
            // Record response DTO
            RequestCorrelationContext.recordResponse(result);
            
            // Log summary
            RequestCorrelationContext.CorrelationContext context = RequestCorrelationContext.get();
            if (context != null) {
                logger.info("Request completed: {}", context.getSummary());
            }
            
            return result;
            
        } finally {
            // Clear context
            RequestCorrelationContext.clear();
        }
    }
    
    /**
     * Check if method has any request mapping annotation
     */
    private boolean hasRequestMapping(Method method) {
        return method.isAnnotationPresent(RequestMapping.class) ||
               method.isAnnotationPresent(GetMapping.class) ||
               method.isAnnotationPresent(PostMapping.class) ||
               method.isAnnotationPresent(PutMapping.class) ||
               method.isAnnotationPresent(PatchMapping.class) ||
               method.isAnnotationPresent(DeleteMapping.class);
    }
    
    /**
     * Extract HTTP method from mapping annotation
     */
    private String extractHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.method().length > 0) {
            return requestMapping.method()[0].name();
        }
        
        return "GET"; // default
    }
    
    /**
     * Extract endpoint path from mapping annotations
     */
    private String extractEndpoint(Method method, Class<?> controllerClass) {
        // Get base path from controller
        String basePath = "";
        if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping classMapping = controllerClass.getAnnotation(RequestMapping.class);
            if (classMapping.value().length > 0) {
                basePath = classMapping.value()[0];
            } else if (classMapping.path().length > 0) {
                basePath = classMapping.path()[0];
            }
        }
        
        // Get method path
        String methodPath = "";
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            methodPath = mapping.value().length > 0 ? mapping.value()[0] : "";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            methodPath = mapping.value().length > 0 ? mapping.value()[0] : "";
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            methodPath = mapping.value().length > 0 ? mapping.value()[0] : "";
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = method.getAnnotation(PatchMapping.class);
            methodPath = mapping.value().length > 0 ? mapping.value()[0] : "";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
            methodPath = mapping.value().length > 0 ? mapping.value()[0] : "";
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.value().length > 0) {
                methodPath = mapping.value()[0];
            } else if (mapping.path().length > 0) {
                methodPath = mapping.path()[0];
            }
        }
        
        // Combine paths
        String fullPath = basePath + methodPath;
        return fullPath.isEmpty() ? "/" : fullPath;
    }
    
    /**
     * Extract user name from security context
     */
    private String extractUserName() {
        // Try to extract from request header (set by authentication layer)
        try {
            org.springframework.web.context.request.RequestAttributes attrs = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) attrs).getRequest();
                
                // Check for authenticated user in header
                String user = request.getHeader("X-User-Name");
                if (user != null && !user.isEmpty()) {
                    return user;
                }
                
                // Check for remote user
                String remoteUser = request.getRemoteUser();
                if (remoteUser != null) {
                    return remoteUser;
                }
            }
        } catch (Exception e) {
            // No request context
        }
        
        // Fallback to system
        return "system";
    }
    
    /**
     * Extract additional audit information from HTTP request
     */
    private Map<String, String> extractAuditInfo() {
        Map<String, String> auditInfo = new LinkedHashMap<>();
        
        try {
            org.springframework.web.context.request.RequestAttributes attrs = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) attrs).getRequest();
                
                // Client IP address
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty()) {
                    ipAddress = request.getRemoteAddr();
                }
                auditInfo.put("ip_address", ipAddress);
                
                // User agent
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null) {
                    auditInfo.put("user_agent", userAgent);
                }
                
                // Session ID
                jakarta.servlet.http.HttpSession session = request.getSession(false);
                if (session != null) {
                    auditInfo.put("session_id", session.getId());
                }
                
                // Request ID (if provided)
                String requestId = request.getHeader("X-Request-ID");
                if (requestId != null) {
                    auditInfo.put("request_id", requestId);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract audit info: {}", e.getMessage());
        }
        
        return auditInfo;
    }
    
    /**
     * Extract request DTO from method arguments
     */
    private Object extractRequestDto(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            // Look for @RequestBody annotation
            if (param.isAnnotationPresent(RequestBody.class)) {
                return args[i];
            }
        }
        return null;
    }
    
    /**
     * Extract path variables from method arguments
     */
    private Map<String, String> extractPathVariables(Method method, Object[] args) {
        Map<String, String> pathVars = new LinkedHashMap<>();
        Parameter[] parameters = method.getParameters();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVar = param.getAnnotation(PathVariable.class);
                String name = pathVar.value().isEmpty() ? pathVar.name() : pathVar.value();
                if (name.isEmpty()) {
                    name = param.getName();
                }
                pathVars.put(name, String.valueOf(args[i]));
            }
        }
        
        return pathVars;
    }
}
