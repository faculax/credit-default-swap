-- V60: Create Daily P&L Results Tables
-- Story 16.6: Daily P&L Calculation Engine
-- Purpose: Calculate and store daily P&L with attribution (market move, theta, credit events)

-- Daily P&L Results (Trade-level P&L with attribution)
CREATE TABLE daily_pnl_results (
    id BIGSERIAL PRIMARY KEY,
    pnl_date DATE NOT NULL,
    trade_id BIGINT NOT NULL,
    
    -- Current day valuation (T)
    current_total_value DECIMAL(20, 4) NOT NULL,
    current_npv DECIMAL(20, 4) NOT NULL,
    current_accrued DECIMAL(20, 4) NOT NULL,
    
    -- Previous day valuation (T-1)
    previous_total_value DECIMAL(20, 4),
    previous_npv DECIMAL(20, 4),
    previous_accrued DECIMAL(20, 4),
    
    -- Total P&L
    total_pnl DECIMAL(20, 4) NOT NULL,
    pnl_percentage DECIMAL(10, 6),
    
    -- P&L Attribution Components
    market_pnl DECIMAL(20, 4),        -- P&L from market moves (spread, rates, FX)
    theta_pnl DECIMAL(20, 4),         -- Time decay / carry
    accrued_pnl DECIMAL(20, 4),       -- Change in accrued interest
    credit_event_pnl DECIMAL(20, 4),  -- P&L from credit events (defaults, downgrades)
    trade_pnl DECIMAL(20, 4),         -- P&L from new trades / terminations
    unexplained_pnl DECIMAL(20, 4),   -- Residual / unexplained
    
    -- Market moves that drove P&L
    spread_move_bps DECIMAL(10, 4),   -- Change in credit spread (bps)
    rate_move_bps DECIMAL(10, 4),     -- Change in interest rates (bps)
    fx_move_pct DECIMAL(10, 6),       -- FX rate change (%)
    recovery_move_pct DECIMAL(10, 6), -- Recovery rate change (%)
    
    -- Risk attribution
    cs01_pnl DECIMAL(20, 4),  -- P&L explained by CS01 × spread move
    ir01_pnl DECIMAL(20, 4),  -- P&L explained by IR01 × rate move
    
    -- Trade details (snapshot)
    notional_amount DECIMAL(20, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference_entity VARCHAR(100) NOT NULL,
    buy_sell_protection VARCHAR(10) NOT NULL,
    
    -- P&L Flags
    large_pnl_flag BOOLEAN DEFAULT FALSE,     -- |P&L| > threshold
    unexplained_pnl_flag BOOLEAN DEFAULT FALSE, -- Unexplained > tolerance
    credit_event_flag BOOLEAN DEFAULT FALSE,   -- Credit event occurred
    new_trade_flag BOOLEAN DEFAULT FALSE,      -- Trade started today
    terminated_trade_flag BOOLEAN DEFAULT FALSE, -- Trade ended today
    
    -- Calculation Metadata
    job_id VARCHAR(100),
    calculation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    calculation_method VARCHAR(50) DEFAULT 'STANDARD',
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_pnl_date_trade UNIQUE (pnl_date, trade_id),
    CONSTRAINT fk_trade FOREIGN KEY (trade_id) REFERENCES cds_trades(id) ON DELETE CASCADE
);

-- Indexes for P&L queries
CREATE INDEX idx_daily_pnl_date ON daily_pnl_results(pnl_date);
CREATE INDEX idx_daily_pnl_trade ON daily_pnl_results(trade_id);
CREATE INDEX idx_daily_pnl_ref_entity ON daily_pnl_results(reference_entity);
CREATE INDEX idx_daily_pnl_large_flag ON daily_pnl_results(large_pnl_flag) WHERE large_pnl_flag = TRUE;
CREATE INDEX idx_daily_pnl_unexplained_flag ON daily_pnl_results(unexplained_pnl_flag) WHERE unexplained_pnl_flag = TRUE;
CREATE INDEX idx_daily_pnl_credit_event_flag ON daily_pnl_results(credit_event_flag) WHERE credit_event_flag = TRUE;

-- Daily Portfolio P&L (Aggregated P&L by portfolio/book)
CREATE TABLE daily_portfolio_pnl (
    id BIGSERIAL PRIMARY KEY,
    pnl_date DATE NOT NULL,
    
    -- Portfolio Identification
    portfolio_id VARCHAR(50),
    portfolio_name VARCHAR(200),
    book VARCHAR(100),
    desk VARCHAR(100),
    business_unit VARCHAR(100),
    
    -- Aggregated P&L
    total_pnl DECIMAL(20, 4) NOT NULL,
    market_pnl DECIMAL(20, 4),
    theta_pnl DECIMAL(20, 4),
    accrued_pnl DECIMAL(20, 4),
    credit_event_pnl DECIMAL(20, 4),
    trade_pnl DECIMAL(20, 4),
    unexplained_pnl DECIMAL(20, 4),
    
    -- Risk-based P&L
    cs01_pnl DECIMAL(20, 4),
    ir01_pnl DECIMAL(20, 4),
    
    -- P&L Metrics
    num_trades INTEGER NOT NULL DEFAULT 0,
    num_large_movers INTEGER DEFAULT 0,      -- Trades with large P&L
    num_unexplained INTEGER DEFAULT 0,        -- Trades with unexplained P&L
    num_credit_events INTEGER DEFAULT 0,      -- Trades with credit events
    
    -- Top Winners & Losers (JSON)
    top_winners JSONB,   -- [{tradeId, refEntity, pnl, attribution}, ...]
    top_losers JSONB,    -- [{tradeId, refEntity, pnl, attribution}, ...]
    
    -- P&L by attribution (JSON breakdown)
    pnl_attribution_breakdown JSONB,
    
    -- Cumulative P&L
    pnl_wtd DECIMAL(20, 4),  -- Week to date
    pnl_mtd DECIMAL(20, 4),  -- Month to date
    pnl_qtd DECIMAL(20, 4),  -- Quarter to date
    pnl_ytd DECIMAL(20, 4),  -- Year to date
    
    -- Calculation Metadata
    job_id VARCHAR(100),
    calculation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_portfolio_pnl_date UNIQUE (pnl_date, portfolio_id, book)
);

-- Indexes for portfolio P&L queries
CREATE INDEX idx_portfolio_pnl_date ON daily_portfolio_pnl(pnl_date);
CREATE INDEX idx_portfolio_pnl_portfolio ON daily_portfolio_pnl(portfolio_id);
CREATE INDEX idx_portfolio_pnl_book ON daily_portfolio_pnl(book);
CREATE INDEX idx_portfolio_pnl_desk ON daily_portfolio_pnl(desk);
CREATE INDEX idx_portfolio_pnl_bu ON daily_portfolio_pnl(business_unit);

-- P&L Attribution Summary (Aggregate view for reporting)
CREATE TABLE pnl_attribution_summary (
    id BIGSERIAL PRIMARY KEY,
    pnl_date DATE NOT NULL,
    aggregation_level VARCHAR(50) NOT NULL, -- FIRM, DESK, BOOK, PORTFOLIO
    aggregation_key VARCHAR(200),            -- Desk name, portfolio ID, etc.
    
    -- Attribution totals
    total_pnl DECIMAL(20, 4) NOT NULL,
    market_pnl DECIMAL(20, 4),
    market_pnl_pct DECIMAL(10, 4),
    theta_pnl DECIMAL(20, 4),
    theta_pnl_pct DECIMAL(10, 4),
    accrued_pnl DECIMAL(20, 4),
    accrued_pnl_pct DECIMAL(10, 4),
    credit_event_pnl DECIMAL(20, 4),
    credit_event_pnl_pct DECIMAL(10, 4),
    trade_pnl DECIMAL(20, 4),
    trade_pnl_pct DECIMAL(10, 4),
    unexplained_pnl DECIMAL(20, 4),
    unexplained_pnl_pct DECIMAL(10, 4),
    
    -- Quality metrics
    num_trades INTEGER,
    explained_percentage DECIMAL(10, 4),  -- % of P&L that is explained
    
    -- Calculation Metadata
    job_id VARCHAR(100),
    calculation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_pnl_attribution UNIQUE (pnl_date, aggregation_level, aggregation_key)
);

-- Indexes for attribution summary
CREATE INDEX idx_attribution_date ON pnl_attribution_summary(pnl_date);
CREATE INDEX idx_attribution_level ON pnl_attribution_summary(aggregation_level);
CREATE INDEX idx_attribution_key ON pnl_attribution_summary(aggregation_key);

-- Materialized view for P&L time series
CREATE MATERIALIZED VIEW mv_pnl_time_series AS
SELECT 
    trade_id,
    pnl_date,
    total_pnl,
    market_pnl,
    theta_pnl,
    current_total_value,
    SUM(total_pnl) OVER (PARTITION BY trade_id ORDER BY pnl_date) as cumulative_pnl,
    AVG(total_pnl) OVER (PARTITION BY trade_id ORDER BY pnl_date ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) as rolling_20d_avg_pnl
FROM daily_pnl_results
WHERE pnl_date >= CURRENT_DATE - INTERVAL '1 year'
ORDER BY trade_id, pnl_date;

-- Index on materialized view
CREATE INDEX idx_mv_pnl_ts_trade ON mv_pnl_time_series(trade_id);
CREATE INDEX idx_mv_pnl_ts_date ON mv_pnl_time_series(pnl_date);

-- Function to refresh P&L time series
CREATE OR REPLACE FUNCTION refresh_pnl_time_series()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pnl_time_series;
END;
$$ LANGUAGE plpgsql;

-- Comments
COMMENT ON TABLE daily_pnl_results IS 'Trade-level daily P&L with full attribution breakdown';
COMMENT ON TABLE daily_portfolio_pnl IS 'Portfolio-level aggregated daily P&L';
COMMENT ON TABLE pnl_attribution_summary IS 'Summary of P&L attribution at various levels (firm, desk, book)';
COMMENT ON COLUMN daily_pnl_results.market_pnl IS 'P&L from market moves (spreads, rates, FX, recovery)';
COMMENT ON COLUMN daily_pnl_results.theta_pnl IS 'Time decay / carry P&L';
COMMENT ON COLUMN daily_pnl_results.unexplained_pnl IS 'Residual P&L not explained by attribution model';
COMMENT ON COLUMN daily_portfolio_pnl.top_winners IS 'JSON: Top 10 trades with highest positive P&L';
COMMENT ON COLUMN daily_portfolio_pnl.top_losers IS 'JSON: Top 10 trades with highest negative P&L';
