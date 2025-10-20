-- Remove credit_curve_id and recovery_rate from bonds table
-- Bonds don't have recovery rate or credit curve attributes

-- Remove the recovery_rate constraint
ALTER TABLE public.bonds DROP CONSTRAINT IF EXISTS bonds_recovery_rate_check;

-- Drop the columns
ALTER TABLE public.bonds DROP COLUMN IF EXISTS credit_curve_id;
ALTER TABLE public.bonds DROP COLUMN IF EXISTS recovery_rate;
