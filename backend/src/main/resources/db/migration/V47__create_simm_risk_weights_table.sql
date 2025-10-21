-- V47: Create simm_risk_weights table for SIMM risk weights
-- This table stores risk weights by risk class and bucket

CREATE TABLE simm_risk_weights (
    id BIGSERIAL PRIMARY KEY,
    parameter_set_id BIGINT NOT NULL REFERENCES simm_parameter_sets(id) ON DELETE CASCADE,
    risk_class VARCHAR(20) NOT NULL,
    bucket VARCHAR(10) NOT NULL,
    risk_weight DECIMAL(10, 6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_simm_risk_weights_parameter_set ON simm_risk_weights(parameter_set_id);
CREATE INDEX idx_simm_risk_weights_risk_class ON simm_risk_weights(risk_class);
CREATE INDEX idx_simm_risk_weights_lookup ON simm_risk_weights(parameter_set_id, risk_class, bucket);

-- Comments
COMMENT ON TABLE simm_risk_weights IS 'SIMM risk weights for sensitivity weighting';
COMMENT ON COLUMN simm_risk_weights.risk_class IS 'Risk class: IR, FX, EQ, CO, CR_Q, CR_NQ';
COMMENT ON COLUMN simm_risk_weights.bucket IS 'SIMM bucket identifier';
COMMENT ON COLUMN simm_risk_weights.risk_weight IS 'Risk weight factor for the bucket';
