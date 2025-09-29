-- Create CDS Trades table
CREATE TABLE cds_trades (
    id BIGSERIAL PRIMARY KEY,
    reference_entity VARCHAR(50) NOT NULL,
    notional_amount DECIMAL(15,2) NOT NULL,
    spread DECIMAL(10,4) NOT NULL,
    maturity_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    counterparty VARCHAR(50) NOT NULL,
    trade_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    premium_frequency VARCHAR(20) NOT NULL DEFAULT 'QUARTERLY',
    day_count_convention VARCHAR(20) NOT NULL DEFAULT 'ACT_360',
    buy_sell_protection VARCHAR(10) CHECK (buy_sell_protection IN ('BUY', 'SELL')) NOT NULL,
    restructuring_clause VARCHAR(50),
    payment_calendar VARCHAR(10) NOT NULL DEFAULT 'NYC',
    accrual_start_date DATE NOT NULL,
    trade_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX idx_cds_reference_entity ON cds_trades(reference_entity);
CREATE INDEX idx_cds_counterparty ON cds_trades(counterparty);
CREATE INDEX idx_cds_trade_status ON cds_trades(trade_status);
CREATE INDEX idx_cds_trade_date ON cds_trades(trade_date);
CREATE INDEX idx_cds_created_at ON cds_trades(created_at);