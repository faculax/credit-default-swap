package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cds_audit_log")
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;
    
    @NotBlank
    @Column(name = "entity_id", nullable = false)
    private String entityId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @NotBlank
    @Column(name = "actor", nullable = false, length = 100)
    private String actor;
    
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "correlation_id")
    private UUID correlationId;
    
    // Enums
    public enum EntityType {
        CREDIT_EVENT,
        CASH_SETTLEMENT,
        PHYSICAL_SETTLEMENT,
        TRADE
    }
    
    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE,
        TRANSITION,
        CALCULATE
    }
    
    // Constructors
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public AuditLog(EntityType entityType, String entityId, AuditAction action, String actor, String summary) {
        this();
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actor = actor;
        this.summary = summary;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public AuditAction getAction() {
        return action;
    }
    
    public void setAction(AuditAction action) {
        this.action = action;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getActor() {
        return actor;
    }
    
    public void setActor(String actor) {
        this.actor = actor;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public UUID getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }
}