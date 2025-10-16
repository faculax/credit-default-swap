-- Add PAYOUT event type to credit events table
-- This migration updates the event type constraint to include PAYOUT

-- Drop the old constraint
ALTER TABLE cds_credit_events DROP CONSTRAINT IF EXISTS ck_event_type;

-- Add the new constraint with PAYOUT included
ALTER TABLE cds_credit_events ADD CONSTRAINT ck_event_type 
    CHECK (event_type IN ('BANKRUPTCY', 'FAILURE_TO_PAY', 'RESTRUCTURING', 'OBLIGATION_DEFAULT', 'REPUDIATION_MORATORIUM', 'PAYOUT'));
