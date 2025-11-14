-- Story 16.7: Risk Reporting & Aggregation

-- Portfolio-level risk metrics
CREATE TABLE portfolio_risk_metrics (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    portfolio_id BIGINT NOT NULL REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    
    -- Credit spread sensitivity (CS01)
    cs01 DECIMAL(20, 4), -- P&L impact of 1bp parallel spread move
    cs01_long DECIMAL(20, 4), -- CS01 for protection bought
    cs01_short DECIMAL(20, 4), -- CS01 for protection sold
    
    -- Interest rate sensitivity (IR01)
    ir01 DECIMAL(20, 4), -- P&L impact of 1bp IR move
    ir01_usd DECIMAL(20, 4),
    ir01_eur DECIMAL(20, 4),
    ir01_gbp DECIMAL(20, 4),
    
    -- Jump-to-default risk (JTD)
    jtd DECIMAL(20, 4), -- Loss if all names default
    jtd_long DECIMAL(20, 4),
    jtd_short DECIMAL(20, 4),
    
    -- Recovery rate sensitivity (REC01)
    rec01 DECIMAL(20, 4), -- P&L impact of 1% recovery change
    
    -- Notional exposures
    gross_notional DECIMAL(20, 4),
    net_notional DECIMAL(20, 4),
    long_notional DECIMAL(20, 4),
    short_notional DECIMAL(20, 4),
    
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uq_portfolio_risk_date_portfolio UNIQUE(calculation_date, portfolio_id)
);

-- Counterparty-level risk aggregation
CREATE TABLE counterparty_risk_metrics (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    counterparty_id BIGINT NOT NULL, -- REFERENCES counterparties(id) ON DELETE CASCADE,
    
    cs01 DECIMAL(20, 4),
    cs01_long DECIMAL(20, 4),
    cs01_short DECIMAL(20, 4),
    
    ir01 DECIMAL(20, 4),
    
    jtd DECIMAL(20, 4),
    jtd_long DECIMAL(20, 4),
    jtd_short DECIMAL(20, 4),
    
    rec01 DECIMAL(20, 4),
    
    gross_notional DECIMAL(20, 4),
    net_notional DECIMAL(20, 4),
    long_notional DECIMAL(20, 4),
    short_notional DECIMAL(20, 4),
    
    trade_count INTEGER,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uq_counterparty_risk_date_counterparty UNIQUE(calculation_date, counterparty_id)
);

-- Sector-level risk aggregation
CREATE TABLE sector_risk_metrics (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    sector VARCHAR(100) NOT NULL, -- e.g., Financials, Technology, Energy
    
    cs01 DECIMAL(20, 4),
    cs01_long DECIMAL(20, 4),
    cs01_short DECIMAL(20, 4),
    
    ir01 DECIMAL(20, 4),
    
    jtd DECIMAL(20, 4),
    jtd_long DECIMAL(20, 4),
    jtd_short DECIMAL(20, 4),
    
    rec01 DECIMAL(20, 4),
    
    gross_notional DECIMAL(20, 4),
    net_notional DECIMAL(20, 4),
    long_notional DECIMAL(20, 4),
    short_notional DECIMAL(20, 4),
    
    reference_entity_count INTEGER,
    trade_count INTEGER,
    
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uq_sector_risk_date_sector UNIQUE(calculation_date, sector)
);

-- Firm-wide risk summary
CREATE TABLE firm_risk_summary (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL UNIQUE,
    
    -- Aggregated sensitivities
    total_cs01 DECIMAL(20, 4),
    total_cs01_long DECIMAL(20, 4),
    total_cs01_short DECIMAL(20, 4),
    
    total_ir01 DECIMAL(20, 4),
    total_ir01_usd DECIMAL(20, 4),
    total_ir01_eur DECIMAL(20, 4),
    total_ir01_gbp DECIMAL(20, 4),
    
    total_jtd DECIMAL(20, 4),
    total_jtd_long DECIMAL(20, 4),
    total_jtd_short DECIMAL(20, 4),
    
    total_rec01 DECIMAL(20, 4),
    
    -- Notional exposures
    total_gross_notional DECIMAL(20, 4),
    total_net_notional DECIMAL(20, 4),
    total_long_notional DECIMAL(20, 4),
    total_short_notional DECIMAL(20, 4),
    
    -- Risk metrics
    var_95 DECIMAL(20, 4), -- 1-day 95% VaR
    var_99 DECIMAL(20, 4), -- 1-day 99% VaR
    expected_shortfall DECIMAL(20, 4), -- CVaR/Expected Shortfall
    
    -- Counts
    total_trade_count INTEGER,
    total_portfolio_count INTEGER,
    total_counterparty_count INTEGER,
    total_reference_entity_count INTEGER,
    
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Risk concentration analysis
CREATE TABLE risk_concentration (
    id BIGSERIAL PRIMARY KEY,
    calculation_date DATE NOT NULL,
    concentration_type VARCHAR(50) NOT NULL, -- TOP_10_NAMES, TOP_5_SECTORS, TOP_10_COUNTERPARTIES
    
    reference_entity_id BIGINT, -- REFERENCES reference_entities(id),
    reference_entity_name VARCHAR(255),
    sector VARCHAR(100),
    counterparty_id BIGINT, -- REFERENCES counterparties(id),
    
    cs01 DECIMAL(20, 4),
    jtd DECIMAL(20, 4),
    gross_notional DECIMAL(20, 4),
    net_notional DECIMAL(20, 4),
    
    percentage_of_total DECIMAL(5, 2), -- e.g., 15.25% of total risk
    percentage_of_total_cs01 DECIMAL(5, 2),
    percentage_of_total_jtd DECIMAL(5, 2),
    
    ranking INTEGER,
    trade_count INTEGER,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Risk limits and thresholds
CREATE TABLE risk_limits (
    id BIGSERIAL PRIMARY KEY,
    
    -- Scope (one of these will be non-null)
    portfolio_id BIGINT REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    counterparty_id BIGINT, -- REFERENCES counterparties(id) ON DELETE CASCADE,
    sector VARCHAR(100),
    firm_wide BOOLEAN DEFAULT FALSE,
    
    limit_type VARCHAR(50) NOT NULL, -- CS01, IR01, JTD, NOTIONAL, VAR_95, VAR_99
    limit_value DECIMAL(20, 4) NOT NULL,
    warning_threshold DECIMAL(20, 4), -- e.g., 80% of limit
    
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    is_active BOOLEAN DEFAULT TRUE,
    
    description TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(100),
    
    CONSTRAINT chk_risk_limit_scope CHECK (
        (portfolio_id IS NOT NULL AND counterparty_id IS NULL AND sector IS NULL AND firm_wide = FALSE) OR
        (portfolio_id IS NULL AND counterparty_id IS NOT NULL AND sector IS NULL AND firm_wide = FALSE) OR
        (portfolio_id IS NULL AND counterparty_id IS NULL AND sector IS NOT NULL AND firm_wide = FALSE) OR
        (portfolio_id IS NULL AND counterparty_id IS NULL AND sector IS NULL AND firm_wide = TRUE)
    )
);

-- Risk limit breaches/alerts
CREATE TABLE risk_limit_breaches (
    id BIGSERIAL PRIMARY KEY,
    breach_date DATE NOT NULL,
    risk_limit_id BIGINT NOT NULL REFERENCES risk_limits(id) ON DELETE CASCADE,
    
    current_value DECIMAL(20, 4) NOT NULL,
    limit_value DECIMAL(20, 4) NOT NULL,
    breach_percentage DECIMAL(5, 2), -- e.g., 105.50% (5.50% over limit)
    breach_amount DECIMAL(20, 4), -- Absolute amount over limit
    
    breach_severity VARCHAR(20) NOT NULL, -- WARNING, BREACH, CRITICAL
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    
    notified_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_breach_severity CHECK (breach_severity IN ('WARNING', 'BREACH', 'CRITICAL'))
);

-- Materialized view for risk time series
CREATE MATERIALIZED VIEW mv_risk_time_series AS
SELECT 
    calculation_date,
    total_cs01,
    total_ir01,
    total_jtd,
    total_gross_notional,
    total_net_notional,
    var_95,
    var_99,
    
    -- Cumulative metrics (running totals)
    SUM(total_cs01) OVER (ORDER BY calculation_date) AS cumulative_cs01,
    
    -- Rolling averages (20-day)
    AVG(total_cs01) OVER (ORDER BY calculation_date ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS avg_cs01_20d,
    AVG(total_jtd) OVER (ORDER BY calculation_date ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS avg_jtd_20d,
    AVG(var_95) OVER (ORDER BY calculation_date ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS avg_var_95_20d,
    
    -- Standard deviations (20-day)
    STDDEV(total_cs01) OVER (ORDER BY calculation_date ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS stddev_cs01_20d,
    STDDEV(var_95) OVER (ORDER BY calculation_date ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS stddev_var_95_20d
    
FROM firm_risk_summary
ORDER BY calculation_date DESC;

-- Create indexes
CREATE INDEX idx_portfolio_risk_date ON portfolio_risk_metrics(calculation_date);
CREATE INDEX idx_portfolio_risk_portfolio ON portfolio_risk_metrics(portfolio_id);
CREATE INDEX idx_portfolio_risk_date_portfolio ON portfolio_risk_metrics(calculation_date, portfolio_id);

-- CREATE INDEX idx_counterparty_risk_date ON counterparty_risk_metrics(calculation_date);
-- CREATE INDEX idx_counterparty_risk_counterparty ON counterparty_risk_metrics(counterparty_id);

CREATE INDEX idx_sector_risk_date ON sector_risk_metrics(calculation_date);
CREATE INDEX idx_sector_risk_sector ON sector_risk_metrics(sector);

CREATE INDEX idx_firm_risk_date ON firm_risk_summary(calculation_date);

CREATE INDEX idx_risk_concentration_date ON risk_concentration(calculation_date);
CREATE INDEX idx_risk_concentration_type ON risk_concentration(concentration_type);
CREATE INDEX idx_risk_concentration_date_type ON risk_concentration(calculation_date, concentration_type);
CREATE INDEX idx_risk_concentration_ranking ON risk_concentration(calculation_date, concentration_type, ranking);

CREATE INDEX idx_risk_limits_active ON risk_limits(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_risk_limits_portfolio ON risk_limits(portfolio_id) WHERE portfolio_id IS NOT NULL;
CREATE INDEX idx_risk_limits_counterparty ON risk_limits(counterparty_id) WHERE counterparty_id IS NOT NULL;
CREATE INDEX idx_risk_limits_sector ON risk_limits(sector) WHERE sector IS NOT NULL;

CREATE INDEX idx_risk_breaches_date ON risk_limit_breaches(breach_date);
CREATE INDEX idx_risk_breaches_limit ON risk_limit_breaches(risk_limit_id);
CREATE INDEX idx_risk_breaches_unresolved ON risk_limit_breaches(is_resolved, breach_date) 
    WHERE is_resolved = FALSE;
CREATE INDEX idx_risk_breaches_severity ON risk_limit_breaches(breach_severity, is_resolved);

CREATE UNIQUE INDEX idx_mv_risk_time_series_date ON mv_risk_time_series(calculation_date);

-- Function to refresh risk time series materialized view
CREATE OR REPLACE FUNCTION refresh_risk_time_series()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_risk_time_series;
END;
$$ LANGUAGE plpgsql;

-- Comments for documentation
COMMENT ON TABLE portfolio_risk_metrics IS 'Portfolio-level risk aggregation with sensitivities and notional exposures';
COMMENT ON TABLE counterparty_risk_metrics IS 'Counterparty-level risk aggregation';
COMMENT ON TABLE sector_risk_metrics IS 'Sector/industry-level risk aggregation';
COMMENT ON TABLE firm_risk_summary IS 'Firm-wide risk summary with VaR metrics';
COMMENT ON TABLE risk_concentration IS 'Risk concentration analysis (top entities, sectors, counterparties)';
COMMENT ON TABLE risk_limits IS 'Configurable risk limits and thresholds';
COMMENT ON TABLE risk_limit_breaches IS 'Tracking and alerting for risk limit breaches';

COMMENT ON COLUMN portfolio_risk_metrics.cs01 IS 'Credit spread sensitivity: P&L impact of 1bp parallel spread move';
COMMENT ON COLUMN portfolio_risk_metrics.ir01 IS 'Interest rate sensitivity: P&L impact of 1bp IR move';
COMMENT ON COLUMN portfolio_risk_metrics.jtd IS 'Jump-to-default risk: Loss if all reference entities default';
COMMENT ON COLUMN portfolio_risk_metrics.rec01 IS 'Recovery rate sensitivity: P&L impact of 1% recovery change';

COMMENT ON COLUMN firm_risk_summary.var_95 IS '1-day Value at Risk at 95% confidence level';
COMMENT ON COLUMN firm_risk_summary.var_99 IS '1-day Value at Risk at 99% confidence level';
COMMENT ON COLUMN firm_risk_summary.expected_shortfall IS 'Conditional Value at Risk (CVaR): Expected loss beyond VaR';
