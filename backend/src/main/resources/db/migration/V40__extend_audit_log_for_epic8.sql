-- Extend audit log to support Epic 8 margin clearing entity types
-- Drop the existing entity_type constraint and recreate it with SIMM_CALCULATION and SACCR_CALCULATION
ALTER TABLE public.cds_audit_log DROP CONSTRAINT IF EXISTS ck_entity_type;

-- Add the new constraint with Epic 8 entity types
ALTER TABLE public.cds_audit_log 
ADD CONSTRAINT ck_entity_type CHECK (entity_type IN (
    'CREDIT_EVENT', 
    'CASH_SETTLEMENT', 
    'PHYSICAL_SETTLEMENT', 
    'TRADE', 
    'SIMM_CALCULATION', 
    'SACCR_CALCULATION'
));

-- Add indexes for Epic 8 specific audit queries
CREATE INDEX IF NOT EXISTS idx_audit_log_simm ON public.cds_audit_log(entity_type, entity_id) 
WHERE entity_type = 'SIMM_CALCULATION';

CREATE INDEX IF NOT EXISTS idx_audit_log_saccr ON public.cds_audit_log(entity_type, entity_id) 
WHERE entity_type = 'SACCR_CALCULATION';