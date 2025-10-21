package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.AuditLog;
import com.creditdefaultswap.platform.repository.AuditLogRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Autowired
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Log an audit entry. Uses REQUIRES_NEW to ensure audit is persisted even if main transaction rolls back
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAudit(AuditLog.EntityType entityType, String entityId, AuditLog.AuditAction action, 
                        String actor, String summary) {
        AuditLog auditLog = new AuditLog(entityType, entityId, action, actor, summary);
        
        // Get correlation ID from MDC if available
        String correlationIdStr = MDC.get("correlationId");
        if (correlationIdStr != null) {
            try {
                auditLog.setCorrelationId(UUID.fromString(correlationIdStr));
            } catch (IllegalArgumentException e) {
                // Log warning but continue
            }
        }
        
        auditLogRepository.save(auditLog);
    }
    
    /**
     * Log an audit entry with explicit correlation ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAudit(AuditLog.EntityType entityType, String entityId, AuditLog.AuditAction action, 
                        String actor, String summary, UUID correlationId) {
        AuditLog auditLog = new AuditLog(entityType, entityId, action, actor, summary);
        auditLog.setCorrelationId(correlationId);
        auditLogRepository.save(auditLog);
    }
    
    /**
     * Log credit event creation
     */
    public void logCreditEventCreation(UUID eventId, String actor, String tradeReferenceEntity) {
        logAudit(AuditLog.EntityType.CREDIT_EVENT, eventId.toString(), AuditLog.AuditAction.CREATE, 
                actor, "Credit event recorded for trade: " + tradeReferenceEntity);
    }
    
    /**
     * Log cash settlement calculation
     */
    public void logCashSettlementCalculation(UUID settlementId, String actor, String amount) {
        logAudit(AuditLog.EntityType.CASH_SETTLEMENT, settlementId.toString(), AuditLog.AuditAction.CALCULATE,
                actor, "Cash settlement calculated: " + amount);
    }
    
    /**
     * Log physical settlement scaffold creation
     */
    public void logPhysicalSettlementCreation(UUID instructionId, String actor) {
        logAudit(AuditLog.EntityType.PHYSICAL_SETTLEMENT, instructionId.toString(), AuditLog.AuditAction.CREATE,
                actor, "Physical settlement instruction scaffold created");
    }
    
    /**
     * Log trade status transition
     */
    public void logTradeStatusTransition(Long tradeId, String actor, String fromStatus, String toStatus) {
        logAudit(AuditLog.EntityType.TRADE, tradeId.toString(), AuditLog.AuditAction.TRANSITION,
                actor, String.format("Trade status changed from %s to %s", fromStatus, toStatus));
    }
    
    /**
     * Log SIMM calculation start
     */
    public void logSimmCalculationStart(String calculationId, String actor, String portfolioId, String parameterSetVersion) {
        logAudit(AuditLog.EntityType.SIMM_CALCULATION, calculationId, AuditLog.AuditAction.CREATE,
                actor, String.format("SIMM calculation started for portfolio %s using parameter set %s", portfolioId, parameterSetVersion));
    }
    
    /**
     * Log SIMM calculation completion
     */
    public void logSimmCalculationCompletion(String calculationId, String actor, String totalIm, long calculationTimeMs) {
        logAudit(AuditLog.EntityType.SIMM_CALCULATION, calculationId, AuditLog.AuditAction.CALCULATE,
                actor, String.format("SIMM calculation completed: total IM=%s USD, calculation time=%d ms", totalIm, calculationTimeMs));
    }
    
    /**
     * Log SIMM calculation failure
     */
    public void logSimmCalculationFailure(String calculationId, String actor, String errorMessage) {
        logAudit(AuditLog.EntityType.SIMM_CALCULATION, calculationId, AuditLog.AuditAction.UPDATE,
                actor, String.format("SIMM calculation failed: %s", errorMessage));
    }
    
    /**
     * Log SA-CCR calculation
     */
    public void logSaCcrCalculation(String nettingSetId, String actor, String exposureAtDefault, String methodology) {
        logAudit(AuditLog.EntityType.SACCR_CALCULATION, nettingSetId, AuditLog.AuditAction.CALCULATE,
                actor, String.format("SA-CCR calculation completed: EAD=%s using %s methodology", exposureAtDefault, methodology));
    }
    
    /**
     * Log margin statement processing
     */
    public void logMarginStatementProcessing(String statementId, String actor, String ccpName, String status, String vmAmount, String imAmount) {
        logAudit(AuditLog.EntityType.MARGIN_STATEMENT, statementId, AuditLog.AuditAction.UPDATE,
                actor, String.format("Margin statement from %s processed with status %s: VM=%s, IM=%s", ccpName, status, vmAmount, imAmount));
    }
    
    /**
     * Get audit trail for a specific entity
     */
    public List<AuditLog> getAuditTrail(AuditLog.EntityType entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }
    
    /**
     * Get recent audit trail for an entity (limited to avoid large results)
     */
    public List<AuditLog> getRecentAuditTrail(AuditLog.EntityType entityType, String entityId) {
        List<AuditLog> fullTrail = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
        // Return only the most recent 50 entries to avoid performance issues
        return fullTrail.size() > 50 ? fullTrail.subList(0, 50) : fullTrail;
    }
}