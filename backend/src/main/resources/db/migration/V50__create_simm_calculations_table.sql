-- V50: Create simm_calculations table for SIMM calculation tracking
-- This table stores SIMM IM calculation requests and results

CREATE TABLE simm_calculations (
    id BIGSERIAL PRIMARY KEY,
    calculation_id VARCHAR(100) UNIQUE NOT NULL,
    upload_id BIGINT NOT NULL REFERENCES crif_uploads(id) ON DELETE CASCADE,
    parameter_set_id BIGINT NOT NULL REFERENCES simm_parameter_sets(id),
    portfolio_id VARCHAR(100),
    calculation_date DATE NOT NULL,
    reporting_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    calculation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_im DECIMAL(20, 8),
    total_im_usd DECIMAL(20, 8),
    diversification_benefit DECIMAL(20, 8),
    calculation_time_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_simm_calculations_calculation_id ON simm_calculations(calculation_id);
CREATE INDEX idx_simm_calculations_upload_id ON simm_calculations(upload_id);
CREATE INDEX idx_simm_calculations_portfolio_id ON simm_calculations(portfolio_id) WHERE portfolio_id IS NOT NULL;
CREATE INDEX idx_simm_calculations_date ON simm_calculations(calculation_date DESC);
CREATE INDEX idx_simm_calculations_status ON simm_calculations(calculation_status);
CREATE INDEX idx_simm_calculations_created_at ON simm_calculations(created_at DESC);

-- Comments
COMMENT ON TABLE simm_calculations IS 'SIMM Initial Margin calculation requests and results';
COMMENT ON COLUMN simm_calculations.calculation_id IS 'Unique identifier for the calculation';
COMMENT ON COLUMN simm_calculations.calculation_status IS 'Status: PENDING, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN simm_calculations.total_im IS 'Total initial margin in reporting currency';
COMMENT ON COLUMN simm_calculations.diversification_benefit IS 'Cross-risk class diversification benefit';
