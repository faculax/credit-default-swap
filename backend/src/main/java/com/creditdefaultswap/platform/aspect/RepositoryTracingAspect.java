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
 * Intercepts repository layer methods to track database calls and collect entities.
 * 
 * Order = 3 (runs after controller and service aspects)
 */
@Aspect
@Component
@Order(3)
public class RepositoryTracingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryTracingAspect.class);
    
    /**
     * Intercept all Spring Data JPA repository methods
     */
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))")
    public Object traceRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // Only trace if correlation context is active
        if (!RequestCorrelationContext.isInitialized()) {
            return joinPoint.proceed();
        }
        
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String repositoryInterface = joinPoint.getTarget().getClass().getInterfaces()[0].getSimpleName();
            String methodName = signature.getMethod().getName();
            Object[] args = joinPoint.getArgs();
            
            logger.trace("Repository call: {}.{}", repositoryInterface, methodName);
            
            // Execute repository method
            Object result = joinPoint.proceed();
            
            // Record repository call with result (entities will be extracted by JPA listener)
            RequestCorrelationContext.recordRepositoryCall(repositoryInterface, methodName, args, result);
            
            return result;
            
        } catch (Throwable e) {
            logger.error("Repository call failed: {}", e.getMessage());
            throw e;
        }
    }
}
