-- Epic 8 Story 2: Daily VM/IM Statement Ingestion Schema
-- Creates tables for managing margin statements, collateral ledger, and processing status

-- Statement processing status enum type
CREATE TYPE statement_status AS ENUM ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DISPUTED', 'RETRYING');

-- Statement format enum type  
CREATE TYPE statement_format AS ENUM ('CSV', 'XML', 'JSON', 'PROPRIETARY');

-- Collateral position type enum
CREATE TYPE position_type AS ENUM ('VARIATION_MARGIN', 'INITIAL_MARGIN', 'EXCESS_COLLATERAL');

-- Main margin statements table
CREATE TABLE margin_statements (
    id BIGSERIAL PRIMARY KEY,
    statement_id VARCHAR(255) NOT NULL, -- CCP-provided statement identifier
    ccp_name VARCHAR(100) NOT NULL,
    member_firm VARCHAR(255) NOT NULL,
    account_number VARCHAR(100) NOT NULL,
    statement_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    statement_format statement_format NOT NULL,
    file_name VARCHAR(500),
    file_size BIGINT,
    raw_content TEXT, -- Store original file content for audit
    status statement_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    UNIQUE(statement_id, ccp_name, statement_date)
);

-- Margin positions extracted from statements
CREATE TABLE margin_positions (
    id BIGSERIAL PRIMARY KEY,
    statement_id BIGINT NOT NULL REFERENCES margin_statements(id) ON DELETE CASCADE,
    position_type position_type NOT NULL,
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    effective_date DATE NOT NULL,
    account_number VARCHAR(100) NOT NULL,
    portfolio_code VARCHAR(100),
    product_class VARCHAR(50), -- e.g., 'CDS', 'IRS', 'EQUITY'
    netting_set_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Collateral ledger for tracking positions over time
CREATE TABLE collateral_ledger (
    id BIGSERIAL PRIMARY KEY,
    ccp_name VARCHAR(100) NOT NULL,
    member_firm VARCHAR(255) NOT NULL,
    account_number VARCHAR(100) NOT NULL,
    position_date DATE NOT NULL,
    position_type position_type NOT NULL,
    currency VARCHAR(3) NOT NULL,
    opening_balance DECIMAL(20,4) DEFAULT 0,
    variation_margin DECIMAL(20,4) DEFAULT 0,
    initial_margin DECIMAL(20,4) DEFAULT 0,
    excess_collateral DECIMAL(20,4) DEFAULT 0,
    closing_balance DECIMAL(20,4) NOT NULL,
    source_statement_id BIGINT REFERENCES margin_statements(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ccp_name, member_firm, account_number, position_date, position_type, currency)
);

-- Discrepancy tracking for reconciliation
CREATE TABLE position_discrepancies (
    id BIGSERIAL PRIMARY KEY,
    statement_id BIGINT NOT NULL REFERENCES margin_statements(id) ON DELETE CASCADE,
    account_number VARCHAR(100) NOT NULL,
    position_type position_type NOT NULL,
    currency VARCHAR(3) NOT NULL,
    expected_amount DECIMAL(20,4) NOT NULL,
    actual_amount DECIMAL(20,4) NOT NULL,
    variance_amount DECIMAL(20,4) NOT NULL,
    variance_percentage DECIMAL(8,4),
    tolerance_threshold DECIMAL(8,4),
    is_material BOOLEAN NOT NULL DEFAULT false,
    resolution_status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, INVESTIGATING, RESOLVED, WAIVED
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- Statement processing log for audit trail
CREATE TABLE statement_processing_log (
    id BIGSERIAL PRIMARY KEY,
    statement_id BIGINT NOT NULL REFERENCES margin_statements(id) ON DELETE CASCADE,
    processing_step VARCHAR(100) NOT NULL, -- UPLOAD, PARSE, VALIDATE, RECONCILE, COMPLETE
    status VARCHAR(50) NOT NULL, -- SUCCESS, FAILURE, WARNING
    message TEXT,
    processing_time_ms INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tolerance configuration for discrepancy checking
CREATE TABLE margin_tolerance_config (
    id BIGSERIAL PRIMARY KEY,
    ccp_name VARCHAR(100) NOT NULL,
    position_type position_type NOT NULL,
    currency VARCHAR(3),
    absolute_threshold DECIMAL(20,4), -- Absolute amount threshold
    percentage_threshold DECIMAL(8,4), -- Percentage threshold
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ccp_name, position_type, currency)
);

-- Indexes for performance
CREATE INDEX idx_margin_statements_ccp_date ON margin_statements(ccp_name, statement_date);
CREATE INDEX idx_margin_statements_status ON margin_statements(status);
CREATE INDEX idx_margin_statements_created_at ON margin_statements(created_at);

CREATE INDEX idx_margin_positions_statement ON margin_positions(statement_id);
CREATE INDEX idx_margin_positions_effective_date ON margin_positions(effective_date);
CREATE INDEX idx_margin_positions_account ON margin_positions(account_number, position_type);

CREATE INDEX idx_collateral_ledger_account_date ON collateral_ledger(ccp_name, member_firm, account_number, position_date);
CREATE INDEX idx_collateral_ledger_position_date ON collateral_ledger(position_date);

CREATE INDEX idx_discrepancies_statement ON position_discrepancies(statement_id);
CREATE INDEX idx_discrepancies_material ON position_discrepancies(is_material, resolution_status);

CREATE INDEX idx_processing_log_statement ON statement_processing_log(statement_id);
CREATE INDEX idx_processing_log_step ON statement_processing_log(processing_step, status);

-- Insert default tolerance configurations
INSERT INTO margin_tolerance_config (ccp_name, position_type, currency, absolute_threshold, percentage_threshold) VALUES
('LCH', 'VARIATION_MARGIN', 'USD', 1000.00, 0.05), -- $1K or 5%
('LCH', 'INITIAL_MARGIN', 'USD', 5000.00, 0.02), -- $5K or 2%
('LCH', 'EXCESS_COLLATERAL', 'USD', 2000.00, 0.10), -- $2K or 10%
('CME', 'VARIATION_MARGIN', 'USD', 1500.00, 0.05),
('CME', 'INITIAL_MARGIN', 'USD', 7500.00, 0.02),
('CME', 'EXCESS_COLLATERAL', 'USD', 3000.00, 0.10),
('EUREX', 'VARIATION_MARGIN', 'EUR', 1000.00, 0.05),
('EUREX', 'INITIAL_MARGIN', 'EUR', 5000.00, 0.02),
('EUREX', 'EXCESS_COLLATERAL', 'EUR', 2000.00, 0.10);

-- Update function for updated_at columns
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at columns
CREATE TRIGGER update_margin_statements_updated_at 
    BEFORE UPDATE ON margin_statements 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_collateral_ledger_updated_at 
    BEFORE UPDATE ON collateral_ledger 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tolerance_config_updated_at 
    BEFORE UPDATE ON margin_tolerance_config 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();