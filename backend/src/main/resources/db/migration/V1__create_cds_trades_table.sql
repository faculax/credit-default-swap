-- Create CDS Trades table in public schema
CREATE TABLE IF NOT EXISTS public.cds_trades (
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
CREATE INDEX IF NOT EXISTS idx_cds_reference_entity ON public.cds_trades(reference_entity);
CREATE INDEX IF NOT EXISTS idx_cds_counterparty ON public.cds_trades(counterparty);
CREATE INDEX IF NOT EXISTS idx_cds_trade_status ON public.cds_trades(trade_status);
CREATE INDEX IF NOT EXISTS idx_cds_trade_date ON public.cds_trades(trade_date);
CREATE INDEX IF NOT EXISTS idx_cds_created_at ON public.cds_trades(created_at);