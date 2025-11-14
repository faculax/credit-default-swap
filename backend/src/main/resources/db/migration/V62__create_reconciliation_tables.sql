-- Story 16.8: Valuation Reconciliation & Exceptions

-- Valuation tolerance rules
CREATE TABLE valuation_tolerance_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- NPV_CHANGE, SPREAD_CHANGE, PNL_THRESHOLD
    
    -- Tolerance thresholds
    absolute_threshold DECIMAL(20, 4), -- Absolute value threshold
    percentage_threshold DECIMAL(5, 2), -- Percentage change threshold
    
    -- Applicability
    applies_to VARCHAR(50), -- ALL, PORTFOLIO, TRADE_TYPE
    portfolio_id BIGINT REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    trade_type VARCHAR(50),
    
    severity VARCHAR(20) NOT NULL, -- INFO, WARNING, ERROR, CRITICAL
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_tolerance_severity CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL'))
);

-- Valuation exceptions detected
CREATE TABLE valuation_exceptions (
    id BIGSERIAL PRIMARY KEY,
    exception_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    
    exception_type VARCHAR(50) NOT NULL,
    -- Types: LARGE_NPV_CHANGE, LARGE_PNL, MISSING_VALUATION, 
    --        STALE_MARKET_DATA, NEGATIVE_ACCRUED, CALCULATION_ERROR
    
    -- Exception details
    current_value DECIMAL(20, 4),
    previous_value DECIMAL(20, 4),
    value_change DECIMAL(20, 4),
    percentage_change DECIMAL(5, 2),
    
    threshold_value DECIMAL(20, 4),
    rule_id BIGINT REFERENCES valuation_tolerance_rules(id) ON DELETE SET NULL,
    
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    -- Status: OPEN, UNDER_REVIEW, APPROVED, REJECTED, REVALUED
    
    -- Resolution tracking
    assigned_to VARCHAR(100),
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    
    -- Links
    valuation_result_id BIGINT,
    revaluation_result_id BIGINT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_exception_severity CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    CONSTRAINT chk_exception_status CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'REVALUED'))
);

-- External valuation comparison (if available)
CREATE TABLE external_valuations (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    
    external_source VARCHAR(50) NOT NULL, -- e.g., BLOOMBERG, MARKIT, COUNTERPARTY
    external_npv DECIMAL(20, 4),
    external_spread DECIMAL(10, 6),
    
    our_npv DECIMAL(20, 4),
    our_spread DECIMAL(10, 6),
    
    npv_difference DECIMAL(20, 4),
    spread_difference DECIMAL(10, 6),
    
    within_tolerance BOOLEAN,
    tolerance_threshold DECIMAL(20, 4),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_external_valuation UNIQUE(valuation_date, trade_id, external_source)
);

-- Revaluation requests and results
CREATE TABLE revaluation_requests (
    id BIGSERIAL PRIMARY KEY,
    request_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    
    request_reason VARCHAR(50) NOT NULL, -- EXCEPTION, USER_REQUEST, CORRECTION
    requested_by VARCHAR(100) NOT NULL,
    
    original_valuation_id BIGINT,
    new_valuation_id BIGINT,
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Status: PENDING, IN_PROGRESS, COMPLETED, FAILED
    
    original_npv DECIMAL(20, 4),
    new_npv DECIMAL(20, 4),
    npv_difference DECIMAL(20, 4),
    
    approved_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    notes TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_revaluation_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_revaluation_reason CHECK (request_reason IN ('EXCEPTION', 'USER_REQUEST', 'CORRECTION'))
);

-- Exception resolution workflow
CREATE TABLE exception_workflow_steps (
    id BIGSERIAL PRIMARY KEY,
    exception_id BIGINT NOT NULL REFERENCES valuation_exceptions(id) ON DELETE CASCADE,
    
    step_number INTEGER NOT NULL,
    step_action VARCHAR(50) NOT NULL, 
    -- Actions: ASSIGNED, INVESTIGATED, MARKET_DATA_CHECKED, 
    --          REVALUED, APPROVED, REJECTED
    
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    notes TEXT,
    attachments JSONB -- Store file references, screenshots, etc.
);

-- Daily reconciliation summary
CREATE TABLE daily_reconciliation_summary (
    id BIGSERIAL PRIMARY KEY,
    reconciliation_date DATE NOT NULL UNIQUE,
    
    total_valuations INTEGER NOT NULL,
    total_exceptions INTEGER NOT NULL,
    
    -- Exception breakdown by severity
    info_count INTEGER DEFAULT 0,
    warning_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    critical_count INTEGER DEFAULT 0,
    
    -- Exception breakdown by type
    large_npv_change_count INTEGER DEFAULT 0,
    large_pnl_count INTEGER DEFAULT 0,
    missing_valuation_count INTEGER DEFAULT 0,
    calculation_error_count INTEGER DEFAULT 0,
    stale_market_data_count INTEGER DEFAULT 0,
    negative_accrued_count INTEGER DEFAULT 0,
    
    -- Status breakdown
    open_exceptions INTEGER DEFAULT 0,
    under_review_exceptions INTEGER DEFAULT 0,
    resolved_exceptions INTEGER DEFAULT 0,
    
    reconciliation_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    -- Status: IN_PROGRESS, PENDING_REVIEW, APPROVED, ISSUES
    
    approved_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    job_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_reconciliation_status CHECK (reconciliation_status IN ('IN_PROGRESS', 'PENDING_REVIEW', 'APPROVED', 'ISSUES'))
);

-- Indexes for performance
CREATE INDEX idx_tolerance_rules_active ON valuation_tolerance_rules(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_tolerance_rules_portfolio ON valuation_tolerance_rules(portfolio_id) WHERE portfolio_id IS NOT NULL;

CREATE INDEX idx_exceptions_date ON valuation_exceptions(exception_date);
CREATE INDEX idx_exceptions_trade ON valuation_exceptions(trade_id);
CREATE INDEX idx_exceptions_status ON valuation_exceptions(status);
CREATE INDEX idx_exceptions_severity ON valuation_exceptions(severity);
CREATE INDEX idx_exceptions_type ON valuation_exceptions(exception_type);
CREATE INDEX idx_exceptions_open ON valuation_exceptions(status, severity) WHERE status = 'OPEN';
CREATE INDEX idx_exceptions_date_status ON valuation_exceptions(exception_date, status);

CREATE INDEX idx_external_valuations_date ON external_valuations(valuation_date);
CREATE INDEX idx_external_valuations_trade ON external_valuations(trade_id);
CREATE INDEX idx_external_valuations_source ON external_valuations(external_source);

CREATE INDEX idx_revaluation_requests_status ON revaluation_requests(status);
CREATE INDEX idx_revaluation_requests_date ON revaluation_requests(valuation_date);
CREATE INDEX idx_revaluation_requests_trade ON revaluation_requests(trade_id);

CREATE INDEX idx_workflow_steps_exception ON exception_workflow_steps(exception_id);
CREATE INDEX idx_workflow_steps_date ON exception_workflow_steps(performed_at);

CREATE INDEX idx_reconciliation_summary_date ON daily_reconciliation_summary(reconciliation_date);
CREATE INDEX idx_reconciliation_summary_status ON daily_reconciliation_summary(reconciliation_status);

-- Comments for documentation
COMMENT ON TABLE valuation_tolerance_rules IS 'Configurable rules for detecting valuation exceptions';
COMMENT ON TABLE valuation_exceptions IS 'Detected valuation anomalies and exceptions requiring review';
COMMENT ON TABLE external_valuations IS 'Comparison with external valuation sources (Bloomberg, Markit, etc.)';
COMMENT ON TABLE revaluation_requests IS 'Requests for revaluing trades due to exceptions or corrections';
COMMENT ON TABLE exception_workflow_steps IS 'Audit trail of exception resolution workflow';
COMMENT ON TABLE daily_reconciliation_summary IS 'Daily summary of reconciliation process and exception counts';

COMMENT ON COLUMN valuation_exceptions.exception_type IS 'Type: LARGE_NPV_CHANGE, LARGE_PNL, MISSING_VALUATION, STALE_MARKET_DATA, NEGATIVE_ACCRUED, CALCULATION_ERROR';
COMMENT ON COLUMN valuation_exceptions.status IS 'Status: OPEN, UNDER_REVIEW, APPROVED, REJECTED, REVALUED';
COMMENT ON COLUMN valuation_exceptions.severity IS 'Severity: INFO, WARNING, ERROR, CRITICAL';
