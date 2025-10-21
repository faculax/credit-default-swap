-- V49: Create simm_bucket_mappings table for risk factor bucketing
-- This table maps risk factors to SIMM buckets

CREATE TABLE simm_bucket_mappings (
    id BIGSERIAL PRIMARY KEY,
    parameter_set_id BIGINT NOT NULL REFERENCES simm_parameter_sets(id) ON DELETE CASCADE,
    risk_class VARCHAR(20) NOT NULL,
    risk_factor VARCHAR(100) NOT NULL,
    bucket VARCHAR(10) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_simm_bucket_mappings_parameter_set ON simm_bucket_mappings(parameter_set_id);
CREATE INDEX idx_simm_bucket_mappings_risk_class ON simm_bucket_mappings(risk_class);
CREATE INDEX idx_simm_bucket_mappings_lookup ON simm_bucket_mappings(parameter_set_id, risk_class, risk_factor);

-- Comments
COMMENT ON TABLE simm_bucket_mappings IS 'SIMM bucket assignments for risk factors';
COMMENT ON COLUMN simm_bucket_mappings.risk_class IS 'Risk class: IR, FX, EQ, CO, CR_Q, CR_NQ';
COMMENT ON COLUMN simm_bucket_mappings.risk_factor IS 'Specific risk factor (e.g., currency, index name)';
COMMENT ON COLUMN simm_bucket_mappings.bucket IS 'Assigned SIMM bucket';
COMMENT ON COLUMN simm_bucket_mappings.description IS 'Description of the risk factor';
