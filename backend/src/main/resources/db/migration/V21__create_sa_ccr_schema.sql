-- V21: SA-CCR (Standardized Approach for Counterparty Credit Risk) Engine Schema
-- Implements Basel III regulatory capital calculation framework

-- Netting sets for trade aggregation
CREATE TABLE netting_sets (
    id BIGSERIAL PRIMARY KEY,
    netting_set_id VARCHAR(100) NOT NULL UNIQUE,
    counterparty_id VARCHAR(100) NOT NULL,
    legal_agreement_type VARCHAR(50) NOT NULL, -- ISDA, CSA, etc.
    agreement_date DATE,
    governing_law VARCHAR(10), -- US, UK, EU, etc.
    netting_eligible BOOLEAN NOT NULL DEFAULT true,
    collateral_agreement BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Supervisory parameters for SA-CCR calculations
CREATE TABLE sa_ccr_supervisory_parameters (
    id BIGSERIAL PRIMARY KEY,
    jurisdiction VARCHAR(10) NOT NULL, -- US, EU, UK, etc.
    asset_class VARCHAR(50) NOT NULL, -- CREDIT, IR, FX, EQ, CO
    parameter_type VARCHAR(50) NOT NULL, -- SUPERVISORY_FACTOR, CORRELATION, etc.
    tenor_bucket VARCHAR(20), -- 1Y, 2Y, 5Y, etc. (for IR)
    credit_quality VARCHAR(20), -- IG, HY (for Credit)
    parameter_value DECIMAL(10,6) NOT NULL,
    effective_date DATE NOT NULL,
    expiry_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(jurisdiction, asset_class, parameter_type, tenor_bucket, credit_quality, effective_date)
);

-- SA-CCR calculations
CREATE TABLE sa_ccr_calculations (
    id BIGSERIAL PRIMARY KEY,
    calculation_id VARCHAR(100) NOT NULL UNIQUE,
    netting_set_id VARCHAR(100) NOT NULL,
    calculation_date DATE NOT NULL,
    jurisdiction VARCHAR(10) NOT NULL,
    alpha_factor DECIMAL(5,3) NOT NULL DEFAULT 1.4,
    
    -- Replacement Cost components
    gross_mtm DECIMAL(20,8) NOT NULL, -- Gross mark-to-market
    vm_received DECIMAL(20,8) NOT NULL DEFAULT 0, -- Variation margin received
    vm_posted DECIMAL(20,8) NOT NULL DEFAULT 0, -- Variation margin posted
    im_received DECIMAL(20,8) NOT NULL DEFAULT 0, -- Initial margin received
    im_posted DECIMAL(20,8) NOT NULL DEFAULT 0, -- Initial margin posted
    replacement_cost DECIMAL(20,8) NOT NULL, -- RC = max(V - C, 0)
    
    -- Potential Future Exposure components
    effective_notional DECIMAL(20,8) NOT NULL, -- Aggregated notional
    supervisory_addon DECIMAL(20,8) NOT NULL, -- Sum of supervisory add-ons
    multiplier DECIMAL(10,6) NOT NULL, -- M = min(1, (Floor + (1-Floor) × exp(trades)))
    potential_future_exposure DECIMAL(20,8) NOT NULL, -- PFE = M × AddOn
    
    -- Final results
    exposure_at_default DECIMAL(20,8) NOT NULL, -- EAD = α × (RC + PFE)
    
    calculation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    FOREIGN KEY (netting_set_id) REFERENCES netting_sets(netting_set_id)
);

-- SA-CCR calculation details by asset class
CREATE TABLE sa_ccr_calculation_details (
    id BIGSERIAL PRIMARY KEY,
    calculation_id VARCHAR(100) NOT NULL,
    asset_class VARCHAR(50) NOT NULL,
    effective_notional DECIMAL(20,8) NOT NULL,
    supervisory_factor DECIMAL(10,6) NOT NULL,
    supervisory_addon DECIMAL(20,8) NOT NULL,
    hedge_ratio DECIMAL(10,6) DEFAULT 0, -- For offsetting positions
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (calculation_id) REFERENCES sa_ccr_calculations(calculation_id) ON DELETE CASCADE
);

-- Trade mapping to netting sets and asset classes
CREATE TABLE sa_ccr_trade_mappings (
    id BIGSERIAL PRIMARY KEY,
    calculation_id VARCHAR(100) NOT NULL,
    trade_id BIGINT NOT NULL,
    netting_set_id VARCHAR(100) NOT NULL,
    asset_class VARCHAR(50) NOT NULL,
    notional_amount DECIMAL(20,8) NOT NULL,
    maturity_date DATE,
    effective_maturity DECIMAL(10,3), -- Years
    current_mtm DECIMAL(20,8) NOT NULL,
    supervisory_delta DECIMAL(10,6) DEFAULT 1.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (calculation_id) REFERENCES sa_ccr_calculations(calculation_id) ON DELETE CASCADE,
    FOREIGN KEY (netting_set_id) REFERENCES netting_sets(netting_set_id),
    FOREIGN KEY (trade_id) REFERENCES cds_trades(id)
);

-- SA-CCR audit trail
CREATE TABLE sa_ccr_audit_trail (
    id BIGSERIAL PRIMARY KEY,
    calculation_id VARCHAR(100) NOT NULL,
    step_number INTEGER NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    description TEXT,
    input_values JSONB,
    output_values JSONB,
    parameters_used JSONB,
    calculation_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (calculation_id) REFERENCES sa_ccr_calculations(calculation_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_netting_sets_counterparty ON netting_sets(counterparty_id);
CREATE INDEX idx_sa_ccr_calculations_date ON sa_ccr_calculations(calculation_date);
CREATE INDEX idx_sa_ccr_calculations_netting_set ON sa_ccr_calculations(netting_set_id);
CREATE INDEX idx_sa_ccr_supervisory_params_lookup ON sa_ccr_supervisory_parameters(jurisdiction, asset_class, effective_date);
CREATE INDEX idx_sa_ccr_trade_mappings_calc ON sa_ccr_trade_mappings(calculation_id);
CREATE INDEX idx_sa_ccr_audit_trail_calc ON sa_ccr_audit_trail(calculation_id);

-- Insert default supervisory parameters for major jurisdictions
INSERT INTO sa_ccr_supervisory_parameters (jurisdiction, asset_class, parameter_type, parameter_value, effective_date) VALUES
-- Alpha factor
('US', 'ALL', 'ALPHA_FACTOR', 1.4, '2017-01-01'),
('EU', 'ALL', 'ALPHA_FACTOR', 1.4, '2021-06-28'),
('UK', 'ALL', 'ALPHA_FACTOR', 1.4, '2022-01-01'),

-- Credit supervisory factors
('US', 'CREDIT', 'SUPERVISORY_FACTOR', 0.0050, '2017-01-01'), -- 0.5% for IG
('US', 'CREDIT', 'SUPERVISORY_FACTOR', 0.0130, '2017-01-01'), -- 1.3% for HY
('EU', 'CREDIT', 'SUPERVISORY_FACTOR', 0.0050, '2021-06-28'),
('EU', 'CREDIT', 'SUPERVISORY_FACTOR', 0.0130, '2021-06-28'),

-- Interest Rate supervisory factors (by tenor)
('US', 'IR', 'SUPERVISORY_FACTOR', 0.0050, '2017-01-01'), -- 0.5% for <2Y
('US', 'IR', 'SUPERVISORY_FACTOR', 0.0075, '2017-01-01'), -- 0.75% for 2-5Y
('US', 'IR', 'SUPERVISORY_FACTOR', 0.0150, '2017-01-01'), -- 1.5% for >5Y
('EU', 'IR', 'SUPERVISORY_FACTOR', 0.0050, '2021-06-28'),
('EU', 'IR', 'SUPERVISORY_FACTOR', 0.0075, '2021-06-28'),
('EU', 'IR', 'SUPERVISORY_FACTOR', 0.0150, '2021-06-28'),

-- FX supervisory factors
('US', 'FX', 'SUPERVISORY_FACTOR', 0.0400, '2017-01-01'), -- 4%
('EU', 'FX', 'SUPERVISORY_FACTOR', 0.0400, '2021-06-28'),
('UK', 'FX', 'SUPERVISORY_FACTOR', 0.0400, '2022-01-01'),

-- Equity supervisory factors
('US', 'EQUITY', 'SUPERVISORY_FACTOR', 0.3200, '2017-01-01'), -- 32%
('EU', 'EQUITY', 'SUPERVISORY_FACTOR', 0.3200, '2021-06-28'),
('UK', 'EQUITY', 'SUPERVISORY_FACTOR', 0.3200, '2022-01-01'),

-- Commodity supervisory factors
('US', 'COMMODITY', 'SUPERVISORY_FACTOR', 0.4000, '2017-01-01'), -- 40%
('EU', 'COMMODITY', 'SUPERVISORY_FACTOR', 0.4000, '2021-06-28'),
('UK', 'COMMODITY', 'SUPERVISORY_FACTOR', 0.4000, '2022-01-01'),

-- Correlation parameters
('US', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0000, '2017-01-01'), -- No cross-asset correlation
('EU', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0000, '2021-06-28'),
('UK', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0000, '2022-01-01');

-- Insert sample netting sets
INSERT INTO netting_sets (netting_set_id, counterparty_id, legal_agreement_type, agreement_date, governing_law, netting_eligible, collateral_agreement) VALUES
('NS_JPM_001', 'JPM', 'ISDA_MASTER', '2020-01-15', 'NY', true, true),
('NS_GS_001', 'GOLDMAN_SACHS', 'ISDA_MASTER', '2019-06-20', 'NY', true, true),
('NS_DB_001', 'DEUTSCHE_BANK', 'ISDA_MASTER', '2020-03-10', 'UK', true, true),
('NS_CS_001', 'CREDIT_SUISSE', 'ISDA_MASTER', '2019-11-05', 'UK', true, false),
('NS_MS_001', 'MORGAN_STANLEY', 'ISDA_MASTER', '2021-01-20', 'NY', true, true);