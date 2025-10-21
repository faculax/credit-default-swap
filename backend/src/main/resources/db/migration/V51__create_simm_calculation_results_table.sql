-- V51: Create simm_calculation_results table for detailed SIMM results
-- This table stores breakdown of SIMM results by risk class and bucket

CREATE TABLE simm_calculation_results (
    id BIGSERIAL PRIMARY KEY,
    calculation_id BIGINT NOT NULL REFERENCES simm_calculations(id) ON DELETE CASCADE,
    risk_class VARCHAR(20) NOT NULL,
    bucket VARCHAR(10),
    weighted_sensitivity DECIMAL(20, 8),
    correlation_adjustment DECIMAL(20, 8),
    margin_component DECIMAL(20, 8),
    margin_component_usd DECIMAL(20, 8),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_simm_calculation_results_calculation ON simm_calculation_results(calculation_id);
CREATE INDEX idx_simm_calculation_results_risk_class ON simm_calculation_results(risk_class);
CREATE INDEX idx_simm_calculation_results_lookup ON simm_calculation_results(calculation_id, risk_class, bucket);

-- Comments
COMMENT ON TABLE simm_calculation_results IS 'Detailed SIMM margin components by risk class and bucket';
COMMENT ON COLUMN simm_calculation_results.risk_class IS 'Risk class: IR, FX, EQ, CO, CR_Q, CR_NQ';
COMMENT ON COLUMN simm_calculation_results.weighted_sensitivity IS 'Risk-weighted sensitivity amount';
COMMENT ON COLUMN simm_calculation_results.correlation_adjustment IS 'Correlation-adjusted amount';
COMMENT ON COLUMN simm_calculation_results.margin_component IS 'Margin contribution in reporting currency';
