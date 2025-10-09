-- V20__create_simm_schema.sql
-- SIMM (Standard Initial Margin Model) Database Schema
-- Implements ISDA SIMM 2.6+ data structures for initial margin calculation

-- SIMM parameter sets with versioning
CREATE TABLE simm_parameter_sets (
    id BIGSERIAL PRIMARY KEY,
    version_name VARCHAR(50) NOT NULL,
    isda_version VARCHAR(20) NOT NULL, -- e.g., "2.6", "2.7"
    effective_date DATE NOT NULL,
    end_date DATE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(version_name, effective_date)
);

-- SIMM risk weights by bucket and risk class
CREATE TABLE simm_risk_weights (
    id BIGSERIAL PRIMARY KEY,
    parameter_set_id BIGINT NOT NULL,
    risk_class VARCHAR(20) NOT NULL, -- IR, FX, EQ, CO, CR_Q, CR_NQ
    bucket VARCHAR(10) NOT NULL,
    risk_weight DECIMAL(10,6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parameter_set_id) REFERENCES simm_parameter_sets(id)
);

-- SIMM correlation matrices (within bucket and cross-bucket)
CREATE TABLE simm_correlations (
    id BIGSERIAL PRIMARY KEY,
    parameter_set_id BIGINT NOT NULL,
    risk_class VARCHAR(20) NOT NULL,
    correlation_type VARCHAR(20) NOT NULL, -- WITHIN_BUCKET, CROSS_BUCKET
    bucket_from VARCHAR(10),
    bucket_to VARCHAR(10),
    risk_factor_from VARCHAR(100),
    risk_factor_to VARCHAR(100),
    correlation DECIMAL(8,6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parameter_set_id) REFERENCES simm_parameter_sets(id)
);

-- CRIF (Common Risk Interchange Format) data uploads
CREATE TABLE crif_uploads (
    id BIGSERIAL PRIMARY KEY,
    upload_id VARCHAR(100) NOT NULL UNIQUE,
    filename VARCHAR(255) NOT NULL,
    portfolio_id VARCHAR(100),
    valuation_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    upload_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_records INTEGER NOT NULL DEFAULT 0,
    valid_records INTEGER NOT NULL DEFAULT 0,
    error_records INTEGER NOT NULL DEFAULT 0,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- CRIF sensitivity records
CREATE TABLE crif_sensitivities (
    id BIGSERIAL PRIMARY KEY,
    upload_id BIGINT NOT NULL,
    trade_id VARCHAR(100),
    portfolio_id VARCHAR(100),
    product_class VARCHAR(50) NOT NULL, -- RatesFX, Credit, Equity, Commodity
    risk_type VARCHAR(50) NOT NULL, -- Risk_IRCurve, Risk_CreditQ, Risk_Equity, etc.
    risk_class VARCHAR(20) NOT NULL, -- IR, FX, EQ, CO, CR_Q, CR_NQ
    bucket VARCHAR(10),
    label1 VARCHAR(100), -- Currency, Index Name, etc.
    label2 VARCHAR(100), -- Tenor, Sub-curve, etc.
    amount_base_currency DECIMAL(20,8) NOT NULL,
    amount_usd DECIMAL(20,8), -- Converted to USD for calculations
    collect_regulations VARCHAR(50),
    post_regulations VARCHAR(50),
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (upload_id) REFERENCES crif_uploads(id) ON DELETE CASCADE
);

-- SIMM calculation requests
CREATE TABLE simm_calculations (
    id BIGSERIAL PRIMARY KEY,
    calculation_id VARCHAR(100) NOT NULL UNIQUE,
    upload_id BIGINT NOT NULL,
    parameter_set_id BIGINT NOT NULL,
    portfolio_id VARCHAR(100),
    calculation_date DATE NOT NULL,
    reporting_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    calculation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    total_im DECIMAL(20,8),
    total_im_usd DECIMAL(20,8),
    diversification_benefit DECIMAL(20,8),
    calculation_time_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (upload_id) REFERENCES crif_uploads(id),
    FOREIGN KEY (parameter_set_id) REFERENCES simm_parameter_sets(id)
);

-- SIMM calculation results by risk class
CREATE TABLE simm_calculation_results (
    id BIGSERIAL PRIMARY KEY,
    calculation_id BIGINT NOT NULL,
    risk_class VARCHAR(20) NOT NULL,
    bucket VARCHAR(10),
    weighted_sensitivity DECIMAL(20,8),
    correlation_adjustment DECIMAL(20,8),
    margin_component DECIMAL(20,8),
    margin_component_usd DECIMAL(20,8),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (calculation_id) REFERENCES simm_calculations(id) ON DELETE CASCADE
);

-- SIMM calculation audit trail
CREATE TABLE simm_calculation_audit (
    id BIGSERIAL PRIMARY KEY,
    calculation_id BIGINT NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    step_order INTEGER NOT NULL,
    input_data JSONB,
    output_data JSONB,
    calculation_details JSONB,
    processing_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (calculation_id) REFERENCES simm_calculations(id) ON DELETE CASCADE
);

-- FX rates for currency conversion
CREATE TABLE fx_rates (
    id BIGSERIAL PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    rate_date DATE NOT NULL,
    rate DECIMAL(12,8) NOT NULL,
    source VARCHAR(50) NOT NULL, -- ECB, FED, MANUAL, etc.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(base_currency, quote_currency, rate_date)
);

-- SIMM bucket mappings for risk factor classification
CREATE TABLE simm_bucket_mappings (
    id BIGSERIAL PRIMARY KEY,
    parameter_set_id BIGINT NOT NULL,
    risk_class VARCHAR(20) NOT NULL,
    risk_factor VARCHAR(100) NOT NULL,
    bucket VARCHAR(10) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parameter_set_id) REFERENCES simm_parameter_sets(id),
    UNIQUE(parameter_set_id, risk_class, risk_factor)
);

-- Create indexes for performance
CREATE INDEX idx_crif_sensitivities_upload_id ON crif_sensitivities(upload_id);
CREATE INDEX idx_crif_sensitivities_risk_class ON crif_sensitivities(risk_class);
CREATE INDEX idx_crif_sensitivities_bucket ON crif_sensitivities(bucket);
CREATE INDEX idx_crif_sensitivities_portfolio_id ON crif_sensitivities(portfolio_id);

CREATE INDEX idx_simm_calculations_upload_id ON simm_calculations(upload_id);
CREATE INDEX idx_simm_calculations_portfolio_id ON simm_calculations(portfolio_id);
CREATE INDEX idx_simm_calculations_calculation_date ON simm_calculations(calculation_date);

CREATE INDEX idx_simm_calculation_results_calculation_id ON simm_calculation_results(calculation_id);
CREATE INDEX idx_simm_calculation_results_risk_class ON simm_calculation_results(risk_class);

CREATE INDEX idx_simm_risk_weights_parameter_set_id ON simm_risk_weights(parameter_set_id);
CREATE INDEX idx_simm_risk_weights_risk_class ON simm_risk_weights(risk_class, bucket);

CREATE INDEX idx_simm_correlations_parameter_set_id ON simm_correlations(parameter_set_id);
CREATE INDEX idx_simm_correlations_risk_class ON simm_correlations(risk_class, correlation_type);

CREATE INDEX idx_fx_rates_currencies_date ON fx_rates(base_currency, quote_currency, rate_date);

CREATE INDEX idx_simm_bucket_mappings_parameter_set_id ON simm_bucket_mappings(parameter_set_id);
CREATE INDEX idx_simm_bucket_mappings_risk_class ON simm_bucket_mappings(risk_class);

-- Insert default SIMM 2.6 parameter set
INSERT INTO simm_parameter_sets (version_name, isda_version, effective_date, description, is_active)
VALUES ('SIMM_2_6_DEFAULT', '2.6', '2024-01-01', 'ISDA SIMM 2.6 Default Parameters', true);

-- Insert sample FX rates (USD base)
INSERT INTO fx_rates (base_currency, quote_currency, rate_date, rate, source) VALUES
('USD', 'EUR', CURRENT_DATE, 0.92, 'ECB'),
('USD', 'GBP', CURRENT_DATE, 0.82, 'ECB'),
('USD', 'JPY', CURRENT_DATE, 150.0, 'ECB'),
('USD', 'CHF', CURRENT_DATE, 0.91, 'ECB'),
('USD', 'CAD', CURRENT_DATE, 1.35, 'ECB'),
('USD', 'AUD', CURRENT_DATE, 1.53, 'ECB'),
('EUR', 'USD', CURRENT_DATE, 1.087, 'ECB'),
('GBP', 'USD', CURRENT_DATE, 1.22, 'ECB'),
('JPY', 'USD', CURRENT_DATE, 0.00667, 'ECB'),
('CHF', 'USD', CURRENT_DATE, 1.099, 'ECB'),
('CAD', 'USD', CURRENT_DATE, 0.741, 'ECB'),
('AUD', 'USD', CURRENT_DATE, 0.653, 'ECB');