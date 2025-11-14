-- Epic 16, Story 16.4: Accrued Interest Calculator
-- Creates table for storing accrued interest calculations

CREATE TABLE trade_accrued_interest (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    
    -- Accrued interest calculation
    accrued_interest DECIMAL(20, 4) NOT NULL,
    accrual_days INTEGER NOT NULL,
    
    -- Calculation inputs
    notional_amount DECIMAL(20, 4) NOT NULL,
    spread DECIMAL(10, 6) NOT NULL,
    day_count_convention VARCHAR(20) NOT NULL, -- ACT/360, ACT/365, 30/360, ACT/ACT
    
    -- Date range for accrual period
    accrual_start_date DATE NOT NULL,
    accrual_end_date DATE NOT NULL,
    
    -- Days calculation details
    numerator_days INTEGER NOT NULL,
    denominator_days INTEGER NOT NULL,
    day_count_fraction DECIMAL(10, 8) NOT NULL,
    
    currency VARCHAR(3) NOT NULL,
    
    -- Status
    calculation_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(calculation_date, trade_id),
    CONSTRAINT chk_accrued_status CHECK (
        calculation_status IN ('SUCCESS', 'FAILED')
    )
);

-- Indexes
CREATE INDEX idx_accrued_interest_date ON trade_accrued_interest(calculation_date);
CREATE INDEX idx_accrued_interest_trade ON trade_accrued_interest(trade_id);
CREATE INDEX idx_accrued_interest_date_trade ON trade_accrued_interest(calculation_date, trade_id);

-- Comments
COMMENT ON TABLE trade_accrued_interest IS 'Accrued premium interest calculations for CDS trades';
COMMENT ON COLUMN trade_accrued_interest.accrued_interest IS 'Total accrued interest amount';
COMMENT ON COLUMN trade_accrued_interest.accrual_days IS 'Number of days in current accrual period';
COMMENT ON COLUMN trade_accrued_interest.day_count_fraction IS 'Day count fraction (numerator/denominator)';
