-- Fix enum type compatibility issues by converting to VARCHAR
-- This allows JPA @Enumerated(EnumType.STRING) to work properly

-- First, alter the margin_statements table
ALTER TABLE margin_statements 
ALTER COLUMN statement_format TYPE VARCHAR(50) USING statement_format::text;

ALTER TABLE margin_statements 
ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

-- Then, alter the margin_positions table  
ALTER TABLE margin_positions 
ALTER COLUMN position_type TYPE VARCHAR(50) USING position_type::text;

-- We can optionally drop the enum types if they're no longer needed
-- (but keeping them for now in case other parts of the system use them)

-- Add comments to document the change
COMMENT ON COLUMN margin_statements.statement_format IS 'Statement format as VARCHAR for JPA compatibility';
COMMENT ON COLUMN margin_statements.status IS 'Statement status as VARCHAR for JPA compatibility';
COMMENT ON COLUMN margin_positions.position_type IS 'Position type as VARCHAR for JPA compatibility';