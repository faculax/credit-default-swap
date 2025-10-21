-- Extend audit log to support novation actions
-- Drop the existing constraint and recreate it with NOVATION action
ALTER TABLE public.cds_audit_log DROP CONSTRAINT IF EXISTS ck_action;

-- Add the new constraint with NOVATION action
ALTER TABLE public.cds_audit_log 
ADD CONSTRAINT ck_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'TRANSITION', 'CALCULATE', 'NOVATION'));

-- Add index for novation-specific queries
CREATE INDEX IF NOT EXISTS idx_audit_log_novation ON public.cds_audit_log(action, correlation_id) 
WHERE action = 'NOVATION';