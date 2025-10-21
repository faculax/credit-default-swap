-- V44: Create crif_sensitivities table for SIMM sensitivity data
-- This table stores CRIF (Common Risk Interchange Format) sensitivity records
-- Compliant with ISDA SIMM 2.6+ specification

CREATE TABLE crif_sensitivities (
    id BIGSERIAL PRIMARY KEY,
    upload_id BIGINT NOT NULL REFERENCES crif_uploads(id) ON DELETE CASCADE,
    trade_id VARCHAR(100),
    portfolio_id VARCHAR(100),
    product_class VARCHAR(50) NOT NULL,
    risk_type VARCHAR(50) NOT NULL,
    risk_class VARCHAR(20) NOT NULL,
    bucket VARCHAR(10),
    label1 VARCHAR(100),
    label2 VARCHAR(100),
    amount_base_currency DECIMAL(20, 8) NOT NULL,
    amount_usd DECIMAL(20, 8),
    collect_regulations VARCHAR(50),
    post_regulations VARCHAR(50),
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_crif_sensitivities_upload_id ON crif_sensitivities(upload_id);
CREATE INDEX idx_crif_sensitivities_risk_class ON crif_sensitivities(risk_class);
CREATE INDEX idx_crif_sensitivities_product_class ON crif_sensitivities(product_class);
CREATE INDEX idx_crif_sensitivities_trade_id ON crif_sensitivities(trade_id) WHERE trade_id IS NOT NULL;
CREATE INDEX idx_crif_sensitivities_portfolio_id ON crif_sensitivities(portfolio_id) WHERE portfolio_id IS NOT NULL;

-- Comments
COMMENT ON TABLE crif_sensitivities IS 'CRIF (Common Risk Interchange Format) sensitivity records for SIMM calculations';
COMMENT ON COLUMN crif_sensitivities.upload_id IS 'Reference to the CRIF upload batch';
COMMENT ON COLUMN crif_sensitivities.product_class IS 'SIMM product class: RatesFX, Credit, Equity, Commodity';
COMMENT ON COLUMN crif_sensitivities.risk_type IS 'Specific risk type (e.g., Risk_IRCurve, Risk_CreditQ)';
COMMENT ON COLUMN crif_sensitivities.risk_class IS 'High-level risk class: IR, FX, EQ, CO, CR_Q, CR_NQ';
COMMENT ON COLUMN crif_sensitivities.bucket IS 'SIMM bucket for correlation purposes';
COMMENT ON COLUMN crif_sensitivities.label1 IS 'First risk factor label (e.g., Currency, Index Name)';
COMMENT ON COLUMN crif_sensitivities.label2 IS 'Second risk factor label (e.g., Tenor, Sub-curve)';
COMMENT ON COLUMN crif_sensitivities.amount_base_currency IS 'Sensitivity amount in base currency';
COMMENT ON COLUMN crif_sensitivities.amount_usd IS 'Sensitivity amount converted to USD';
