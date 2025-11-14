-- Accounting Integration Schema
-- Stores accounting events generated from EOD valuations

-- Accounting events table
CREATE TABLE accounting_events (
    id BIGSERIAL PRIMARY KEY,
    event_date DATE NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    
    -- Trade reference
    trade_id BIGINT,
    reference_entity_name VARCHAR(255),
    
    -- Journal entry details
    account_code VARCHAR(20) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    debit_amount DECIMAL(20, 4),
    credit_amount DECIMAL(20, 4),
    currency VARCHAR(3) NOT NULL,
    
    -- Valuation context
    current_npv DECIMAL(20, 4),
    previous_npv DECIMAL(20, 4),
    npv_change DECIMAL(20, 4),
    accrued_change DECIMAL(20, 4),
    
    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    posted_to_gl BOOLEAN DEFAULT FALSE,
    posted_at TIMESTAMP,
    gl_batch_id VARCHAR(100),
    
    -- Source tracking
    valuation_job_id VARCHAR(100),
    description TEXT,
    error_message TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    posted_by VARCHAR(100),
    
    CONSTRAINT fk_accounting_trade FOREIGN KEY (trade_id) 
        REFERENCES cds_trades(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_accounting_events_date ON accounting_events(event_date);
CREATE INDEX idx_accounting_events_trade ON accounting_events(trade_id);
CREATE INDEX idx_accounting_events_status ON accounting_events(status);
CREATE INDEX idx_accounting_events_gl_batch ON accounting_events(gl_batch_id);
CREATE INDEX idx_accounting_events_job ON accounting_events(valuation_job_id);
CREATE INDEX idx_accounting_events_pending ON accounting_events(event_date, status) 
    WHERE status = 'PENDING';

-- Comments
COMMENT ON TABLE accounting_events IS 'Accounting events generated from EOD valuations for GL posting';
COMMENT ON COLUMN accounting_events.event_type IS 'MTM_VALUATION, MTM_PNL_UNREALIZED, ACCRUED_INTEREST, NEW_TRADE_BOOKING, etc.';
COMMENT ON COLUMN accounting_events.status IS 'PENDING, POSTED, FAILED, CANCELLED';
COMMENT ON COLUMN accounting_events.debit_amount IS 'Debit amount for journal entry';
COMMENT ON COLUMN accounting_events.credit_amount IS 'Credit amount for journal entry';
COMMENT ON COLUMN accounting_events.posted_to_gl IS 'Whether event has been posted to General Ledger';
COMMENT ON COLUMN accounting_events.gl_batch_id IS 'GL batch ID from accounting system';
