-- V10: Create portfolio aggregation tables

-- Create cds_portfolios table
CREATE TABLE cds_portfolios (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create unique index on lowercase name for case-insensitive uniqueness
CREATE UNIQUE INDEX uk_portfolio_name ON cds_portfolios(LOWER(name));

-- Create cds_portfolio_constituents table
CREATE TABLE cds_portfolio_constituents (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    trade_id BIGINT NOT NULL,
    weight_type VARCHAR(20) NOT NULL CHECK (weight_type IN ('NOTIONAL', 'PERCENT')),
    weight_value DECIMAL(15, 8) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_constituent_portfolio FOREIGN KEY (portfolio_id) REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    CONSTRAINT fk_constituent_trade FOREIGN KEY (trade_id) REFERENCES cds_trades(id) ON DELETE CASCADE,
    CONSTRAINT uk_portfolio_trade UNIQUE (portfolio_id, trade_id)
);

-- Create indexes for performance
CREATE INDEX idx_constituents_portfolio ON cds_portfolio_constituents(portfolio_id);
CREATE INDEX idx_constituents_trade ON cds_portfolio_constituents(trade_id);
CREATE INDEX idx_constituents_active ON cds_portfolio_constituents(active) WHERE active = TRUE;

-- Create portfolio_risk_cache table for caching aggregated risk metrics
CREATE TABLE portfolio_risk_cache (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    valuation_date DATE NOT NULL,
    aggregate_pv DECIMAL(18, 4),
    aggregate_accrued DECIMAL(18, 4),
    premium_leg_pv DECIMAL(18, 4),
    protection_leg_pv DECIMAL(18, 4),
    fair_spread_bps_weighted DECIMAL(10, 4),
    cs01 DECIMAL(18, 4),
    rec01 DECIMAL(18, 4),
    jtd DECIMAL(18, 4),
    top_5_pct_cs01 DECIMAL(10, 4),
    sector_breakdown JSONB,
    by_trade_breakdown JSONB,
    completeness_constituents INT,
    completeness_priced INT,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_cache_portfolio FOREIGN KEY (portfolio_id) REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    CONSTRAINT uk_portfolio_valuation_date UNIQUE (portfolio_id, valuation_date)
);

-- Create index for faster cache lookups
CREATE INDEX idx_risk_cache_portfolio_date ON portfolio_risk_cache(portfolio_id, valuation_date DESC);
