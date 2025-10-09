package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.AuditLog;
import com.creditdefaultswap.platform.repository.AuditLogRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
}