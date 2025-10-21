-- V48: Create simm_correlations table for SIMM correlation coefficients
-- This table stores correlation parameters for aggregation

CREATE TABLE simm_correlations (
    id BIGSERIAL PRIMARY KEY,
    parameter_set_id BIGINT NOT NULL REFERENCES simm_parameter_sets(id) ON DELETE CASCADE,
    risk_class VARCHAR(20) NOT NULL,
    correlation_type VARCHAR(20) NOT NULL,
    bucket_from VARCHAR(10),
    bucket_to VARCHAR(10),
    risk_factor_from VARCHAR(100),
    risk_factor_to VARCHAR(100),
    correlation DECIMAL(8, 6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_simm_correlations_parameter_set ON simm_correlations(parameter_set_id);
CREATE INDEX idx_simm_correlations_risk_class ON simm_correlations(risk_class);
CREATE INDEX idx_simm_correlations_type ON simm_correlations(correlation_type);
CREATE INDEX idx_simm_correlations_buckets ON simm_correlations(parameter_set_id, risk_class, bucket_from, bucket_to);

-- Comments
COMMENT ON TABLE simm_correlations IS 'SIMM correlation coefficients for risk aggregation';
COMMENT ON COLUMN simm_correlations.risk_class IS 'Risk class: IR, FX, EQ, CO, CR_Q, CR_NQ';
COMMENT ON COLUMN simm_correlations.correlation_type IS 'Type: WITHIN_BUCKET or CROSS_BUCKET';
COMMENT ON COLUMN simm_correlations.bucket_from IS 'Source bucket for cross-bucket correlations';
COMMENT ON COLUMN simm_correlations.bucket_to IS 'Target bucket for cross-bucket correlations';
COMMENT ON COLUMN simm_correlations.correlation IS 'Correlation coefficient (-1 to 1)';
