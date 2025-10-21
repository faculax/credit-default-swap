package com.creditdefaultswap.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for logging statement processing steps for audit trail
 */
@Service
public class StatementProcessingLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(StatementProcessingLogService.class);
    
    /**
     * Log a processing step for audit trail
     * 
     * @param statementId The statement ID
     * @param step The processing step (UPLOAD, PARSE, VALIDATE, RECONCILE, COMPLETE)
     * @param status The status (SUCCESS, FAILURE, WARNING)
     * @param message Optional message with details
     */
    public void logProcessingStep(Long statementId, String step, String status, String message) {
        String logMessage = String.format("Statement[%d] %s - %s: %s", 
                statementId, step, status, message != null ? message : "");
        
        if ("FAILURE".equals(status)) {
            logger.error(logMessage);
        } else if ("WARNING".equals(status)) {
            logger.warn(logMessage);
        } else {
            logger.info(logMessage);
        }
        
        // In a full implementation, this would also save to statement_processing_log table
        // For now, we just log to application logs
    }
}