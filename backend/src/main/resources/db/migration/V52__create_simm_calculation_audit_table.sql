-- V52: Create simm_calculation_audit table for calculation audit trail
-- This table stores step-by-step audit trail of SIMM calculations

CREATE TABLE simm_calculation_audit (
    id BIGSERIAL PRIMARY KEY,
    calculation_id BIGINT NOT NULL REFERENCES simm_calculations(id) ON DELETE CASCADE,
    step_name VARCHAR(100) NOT NULL,
    step_order INTEGER NOT NULL,
    input_data TEXT,
    output_data TEXT,
    calculation_details TEXT,
    processing_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_simm_calculation_audit_calculation ON simm_calculation_audit(calculation_id);
CREATE INDEX idx_simm_calculation_audit_step_order ON simm_calculation_audit(calculation_id, step_order);

-- Comments
COMMENT ON TABLE simm_calculation_audit IS 'Audit trail for SIMM calculation steps';
COMMENT ON COLUMN simm_calculation_audit.step_name IS 'Name of the calculation step';
COMMENT ON COLUMN simm_calculation_audit.step_order IS 'Sequential order of the step';
COMMENT ON COLUMN simm_calculation_audit.input_data IS 'JSON input data for the step';
COMMENT ON COLUMN simm_calculation_audit.output_data IS 'JSON output data from the step';
COMMENT ON COLUMN simm_calculation_audit.calculation_details IS 'Detailed calculation methodology';
