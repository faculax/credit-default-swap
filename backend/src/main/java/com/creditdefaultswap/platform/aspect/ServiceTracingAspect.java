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

/**
 * Intercepts service layer methods to track service calls in correlation context.
 * 
 * Order = 2 (runs after controller aspect, before repository aspect)
 */
@Aspect
@Component
@Order(2)
public class ServiceTracingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceTracingAspect.class);
    
    /**
     * Intercept all @Service methods
     */
    @Around("@within(org.springframework.stereotype.Service) && execution(public * *(..))")
    public Object traceServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // Only trace if correlation context is active
        if (!RequestCorrelationContext.isInitialized()) {
            return joinPoint.proceed();
        }
        
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String serviceClass = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = signature.getMethod().getName();
            Object[] args = joinPoint.getArgs();
            
            // Record service call
            RequestCorrelationContext.recordServiceCall(serviceClass, methodName, args);
            
            logger.trace("Service call: {}.{}", serviceClass, methodName);
            
            // Execute service method
            return joinPoint.proceed();
            
        } catch (Throwable e) {
            logger.error("Service call failed: {}", e.getMessage());
            throw e;
        }
    }
}
