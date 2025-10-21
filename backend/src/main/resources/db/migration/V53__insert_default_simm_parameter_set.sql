-- V53: Insert default SIMM parameter set (ISDA SIMM 2.6)
-- This provides a baseline parameter set for calculations

INSERT INTO simm_parameter_sets (
    version_name, 
    isda_version, 
    effective_date, 
    end_date, 
    description, 
    is_active,
    created_at
) VALUES (
    'ISDA SIMM 2.6',
    '2.6',
    '2023-04-01',
    NULL,
    'ISDA SIMM version 2.6 - Standard Initial Margin Model parameters effective from April 2023',
    true,
    CURRENT_TIMESTAMP
);

-- Add a comment to confirm insertion
COMMENT ON TABLE simm_parameter_sets IS 'ISDA SIMM parameter set versions for IM calculations - Default SIMM 2.6 inserted';
