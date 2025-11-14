-- V59: Create EOD Valuation Storage and History Tables
-- Story 16.5: Valuation Storage & History
-- Purpose: Store consolidated daily valuations and maintain historical time series

-- EOD Valuation Results (Daily snapshot of all trade valuations)
CREATE TABLE eod_valuation_results (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL,
    
    -- NPV Components
    npv DECIMAL(20, 4) NOT NULL,
    premium_leg_pv DECIMAL(20, 4),
    protection_leg_pv DECIMAL(20, 4),
    
    -- Accrued Interest
    accrued_interest DECIMAL(20, 4) NOT NULL,
    accrual_days INTEGER,
    
    -- Total Value (NPV + Accrued)
    total_value DECIMAL(20, 4) NOT NULL,
    
    -- Market Data Used
    credit_spread DECIMAL(10, 6),
    recovery_rate DECIMAL(5, 4),
    discount_rate DECIMAL(10, 6),
    
    -- Risk Metrics
    cs01 DECIMAL(18, 4),  -- Credit spread 01
    ir01 DECIMAL(18, 4),  -- Interest rate 01
    jtd DECIMAL(18, 4),   -- Jump to Default
    rec01 DECIMAL(18, 4), -- Recovery rate 01
    
    -- Trade Details (snapshot)
    notional_amount DECIMAL(20, 2) NOT NULL,
    spread DECIMAL(10, 6) NOT NULL,
    maturity_date DATE NOT NULL,
    reference_entity VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    buy_sell_protection VARCHAR(10) NOT NULL,
    
    -- Calculation Metadata
    job_id VARCHAR(100),
    calculation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valuation_method VARCHAR(50),
    calculation_time_ms INTEGER,
    
    -- Status
    status VARCHAR(20) DEFAULT 'VALID',  -- VALID, INVALID, STALE, REVALUED
    validation_flags TEXT[],
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_valuation_date_trade UNIQUE (valuation_date, trade_id),
    CONSTRAINT fk_trade FOREIGN KEY (trade_id) REFERENCES cds_trades(id) ON DELETE CASCADE
);

-- Indexes for query performance
CREATE INDEX idx_eod_valuation_results_date ON eod_valuation_results(valuation_date);
CREATE INDEX idx_eod_valuation_results_trade ON eod_valuation_results(trade_id);
CREATE INDEX idx_eod_valuation_results_ref_entity ON eod_valuation_results(reference_entity);
CREATE INDEX idx_eod_valuation_results_status ON eod_valuation_results(status);
CREATE INDEX idx_eod_valuation_results_job ON eod_valuation_results(job_id);

-- EOD Portfolio Valuations (Aggregated by portfolio/book)
CREATE TABLE eod_portfolio_valuations (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    
    -- Portfolio Identification
    portfolio_id VARCHAR(50),
    portfolio_name VARCHAR(200),
    book VARCHAR(100),
    desk VARCHAR(100),
    business_unit VARCHAR(100),
    
    -- Aggregated Values
    total_npv DECIMAL(20, 4) NOT NULL,
    total_accrued DECIMAL(20, 4) NOT NULL,
    total_value DECIMAL(20, 4) NOT NULL,
    total_notional DECIMAL(20, 2) NOT NULL,
    
    -- Trade Counts
    num_trades INTEGER NOT NULL DEFAULT 0,
    num_buy_protection INTEGER DEFAULT 0,
    num_sell_protection INTEGER DEFAULT 0,
    
    -- Risk Metrics (Aggregated)
    total_cs01 DECIMAL(18, 4),
    total_ir01 DECIMAL(18, 4),
    total_jtd DECIMAL(18, 4),
    total_rec01 DECIMAL(18, 4),
    net_delta DECIMAL(18, 4),
    net_gamma DECIMAL(18, 4),
    
    -- P&L (vs Previous Day)
    daily_pnl DECIMAL(20, 4),
    pnl_mtd DECIMAL(20, 4),  -- Month to date
    pnl_ytd DECIMAL(20, 4),  -- Year to date
    
    -- Currency Breakdown (JSON for multiple currencies)
    currency_breakdown JSONB,
    
    -- Reference Entity Concentration (Top 10)
    top_exposures JSONB,
    
    -- Calculation Metadata
    job_id VARCHAR(100),
    calculation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_portfolio_valuation_date UNIQUE (valuation_date, portfolio_id, book)
);

-- Indexes for portfolio queries
CREATE INDEX idx_portfolio_valuations_date ON eod_portfolio_valuations(valuation_date);
CREATE INDEX idx_portfolio_valuations_portfolio ON eod_portfolio_valuations(portfolio_id);
CREATE INDEX idx_portfolio_valuations_book ON eod_portfolio_valuations(book);
CREATE INDEX idx_portfolio_valuations_desk ON eod_portfolio_valuations(desk);
CREATE INDEX idx_portfolio_valuations_bu ON eod_portfolio_valuations(business_unit);

-- Time Series Materialized View for Fast Historical Queries
-- (Useful for charting and trend analysis)
CREATE MATERIALIZED VIEW mv_valuation_time_series AS
SELECT 
    trade_id,
    valuation_date,
    npv,
    total_value,
    credit_spread,
    cs01,
    LAG(total_value) OVER (PARTITION BY trade_id ORDER BY valuation_date) as prev_value,
    total_value - LAG(total_value) OVER (PARTITION BY trade_id ORDER BY valuation_date) as daily_pnl
FROM eod_valuation_results
WHERE status = 'VALID'
ORDER BY trade_id, valuation_date;

-- Index on materialized view
CREATE INDEX idx_mv_valuation_ts_trade ON mv_valuation_time_series(trade_id);
CREATE INDEX idx_mv_valuation_ts_date ON mv_valuation_time_series(valuation_date);

-- Function to refresh the materialized view (can be called after EOD job)
CREATE OR REPLACE FUNCTION refresh_valuation_time_series()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_valuation_time_series;
END;
$$ LANGUAGE plpgsql;

-- Comments
COMMENT ON TABLE eod_valuation_results IS 'Trade-level EOD valuations with full NPV, accrued, and risk metrics';
COMMENT ON TABLE eod_portfolio_valuations IS 'Portfolio-level aggregated valuations and P&L';
COMMENT ON COLUMN eod_valuation_results.total_value IS 'NPV + Accrued Interest';
COMMENT ON COLUMN eod_portfolio_valuations.currency_breakdown IS 'JSON: {USD: {npv: 100000, notional: 1000000}, EUR: {...}}';
COMMENT ON COLUMN eod_portfolio_valuations.top_exposures IS 'JSON: [{entity: "ACME Corp", exposure: 50000, cs01: 500}, ...]';
