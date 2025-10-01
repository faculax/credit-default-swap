-- Create audit log table for compliance tracking
CREATE TABLE cds_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(100) NOT NULL,
    summary TEXT,
    correlation_id UUID,
    
    CONSTRAINT ck_entity_type CHECK (entity_type IN ('CREDIT_EVENT', 'CASH_SETTLEMENT', 'PHYSICAL_SETTLEMENT', 'TRADE')),
    CONSTRAINT ck_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'TRANSITION', 'CALCULATE'))
);

-- Create indexes for common audit queries
CREATE INDEX idx_audit_log_entity ON cds_audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_timestamp ON cds_audit_log(timestamp);
CREATE INDEX idx_audit_log_correlation ON cds_audit_log(correlation_id);
CREATE INDEX idx_audit_log_actor ON cds_audit_log(actor);