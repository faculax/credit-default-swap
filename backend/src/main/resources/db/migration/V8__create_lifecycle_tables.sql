-- Epic 5: Routine Lifecycle & Position Changes
-- Create tables for coupon schedules, accruals, amendments, notional adjustments, novations, and compression

-- Add version column to cds_trades table for amendment tracking
ALTER TABLE cds_trades ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- Update TradeStatus enum to include new lifecycle states
ALTER TABLE cds_trades DROP CONSTRAINT IF EXISTS chk_trade_status_values;
ALTER TABLE cds_trades 
    ADD CONSTRAINT chk_trade_status_values CHECK (trade_status IN (
        'PENDING',
        'ACTIVE',
        'CREDIT_EVENT_RECORDED',
        'TRIGGERED',
        'SETTLED_CASH',
        'SETTLED_PHYSICAL',
        'CANCELLED',
        'PARTIALLY_TERMINATED',
        'NOVATED',
        'TERMINATED',
        'COMPRESSED'
    ));

-- Coupon Periods table for IMM schedule generation
CREATE TABLE coupon_periods (
    id BIGSERIAL PRIMARY KEY,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    payment_date DATE NOT NULL,
    accrual_days INTEGER NOT NULL,
    notional_amount DECIMAL(15,2) NOT NULL,
    day_count_convention VARCHAR(20) NOT NULL DEFAULT 'ACT_360',
    business_day_convention VARCHAR(20) NOT NULL DEFAULT 'MODIFIED_FOLLOWING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(trade_id, period_start_date)
);

-- Accrual Events table for daily accrual tracking
CREATE TABLE accrual_events (
    id BIGSERIAL PRIMARY KEY,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    coupon_period_id BIGINT NOT NULL REFERENCES coupon_periods(id) ON DELETE CASCADE,
    accrual_date DATE NOT NULL,
    accrual_amount DECIMAL(15,2) NOT NULL,
    cumulative_accrual DECIMAL(15,2) NOT NULL,
    day_count_fraction DECIMAL(10,8) NOT NULL,
    notional_amount DECIMAL(15,2) NOT NULL,
    trade_version INTEGER NOT NULL DEFAULT 1,
    posted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(trade_id, accrual_date, trade_version)
);

-- Trade Amendments table for versioning and audit trail
CREATE TABLE trade_amendments (
    id BIGSERIAL PRIMARY KEY,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    amendment_date DATE NOT NULL,
    previous_version INTEGER NOT NULL,
    new_version INTEGER NOT NULL,
    field_name VARCHAR(50) NOT NULL,
    previous_value TEXT,
    new_value TEXT,
    amendment_reason TEXT,
    pnl_impact_estimate DECIMAL(15,2),
    amended_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Notional Adjustments table for partial terminations and reductions
CREATE TABLE notional_adjustments (
    id BIGSERIAL PRIMARY KEY,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    adjustment_date DATE NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL CHECK (adjustment_type IN ('PARTIAL_TERMINATION', 'FULL_TERMINATION', 'REDUCTION')),
    original_notional DECIMAL(15,2) NOT NULL,
    adjustment_amount DECIMAL(15,2) NOT NULL,
    remaining_notional DECIMAL(15,2) NOT NULL,
    unwind_cash_amount DECIMAL(15,2),
    adjustment_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trade Novations table for counterparty transfers
CREATE TABLE trade_novations (
    id BIGSERIAL PRIMARY KEY,
    original_trade_id BIGINT NOT NULL REFERENCES cds_trades(id),
    new_trade_id BIGINT REFERENCES cds_trades(id),
    novation_date DATE NOT NULL,
    original_counterparty VARCHAR(50) NOT NULL,
    new_counterparty VARCHAR(50) NOT NULL,
    novation_reason TEXT,
    fee_amount DECIMAL(15,2),
    fee_currency VARCHAR(3),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Compression Proposals table for multilateral netting
CREATE TABLE compression_proposals (
    id BIGSERIAL PRIMARY KEY,
    proposal_id VARCHAR(50) NOT NULL UNIQUE,
    proposal_date DATE NOT NULL,
    execution_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'EXECUTED', 'REJECTED', 'CANCELLED')),
    cs01_tolerance_check BOOLEAN NOT NULL DEFAULT false,
    total_gross_notional_before DECIMAL(18,2),
    total_gross_notional_after DECIMAL(18,2),
    net_notional_reduction DECIMAL(18,2),
    number_of_trades_affected INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Compression Proposal Items table for individual trade adjustments
CREATE TABLE compression_proposal_items (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL REFERENCES compression_proposals(id) ON DELETE CASCADE,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    original_notional DECIMAL(15,2) NOT NULL,
    adjusted_notional DECIMAL(15,2) NOT NULL,
    notional_change DECIMAL(15,2) NOT NULL,
    termination_flag BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(proposal_id, trade_id)
);

-- Indexes for performance
CREATE INDEX idx_coupon_periods_trade_id ON coupon_periods(trade_id);
CREATE INDEX idx_coupon_periods_payment_date ON coupon_periods(payment_date);

CREATE INDEX idx_accrual_events_trade_id ON accrual_events(trade_id);
CREATE INDEX idx_accrual_events_accrual_date ON accrual_events(accrual_date);
CREATE INDEX idx_accrual_events_trade_version ON accrual_events(trade_id, trade_version);

CREATE INDEX idx_trade_amendments_trade_id ON trade_amendments(trade_id);
CREATE INDEX idx_trade_amendments_version ON trade_amendments(trade_id, new_version);

CREATE INDEX idx_notional_adjustments_trade_id ON notional_adjustments(trade_id);
CREATE INDEX idx_notional_adjustments_date ON notional_adjustments(adjustment_date);

CREATE INDEX idx_trade_novations_original_trade ON trade_novations(original_trade_id);
CREATE INDEX idx_trade_novations_new_trade ON trade_novations(new_trade_id);
CREATE INDEX idx_trade_novations_date ON trade_novations(novation_date);

CREATE INDEX idx_compression_proposals_date ON compression_proposals(proposal_date);
CREATE INDEX idx_compression_proposals_status ON compression_proposals(status);

CREATE INDEX idx_compression_items_proposal ON compression_proposal_items(proposal_id);
CREATE INDEX idx_compression_items_trade ON compression_proposal_items(trade_id);