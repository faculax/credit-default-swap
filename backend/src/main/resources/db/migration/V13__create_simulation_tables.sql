-- V13: Create Monte Carlo simulation tables for correlated credit default analysis

-- Main simulation run tracking table
CREATE TABLE simulation_runs (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL UNIQUE,
    portfolio_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETE', 'FAILED', 'CANCELED')),
    valuation_date DATE NOT NULL,
    paths INTEGER NOT NULL,
    request_payload JSONB NOT NULL,
    seed_used BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    runtime_ms BIGINT,
    CONSTRAINT fk_simulation_portfolio FOREIGN KEY (portfolio_id) REFERENCES cds_portfolios(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_simulation_runs_portfolio ON simulation_runs(portfolio_id);
CREATE INDEX idx_simulation_runs_status ON simulation_runs(status);
CREATE INDEX idx_simulation_runs_created ON simulation_runs(created_at DESC);
CREATE INDEX idx_simulation_runs_run_id ON simulation_runs(run_id);

-- Horizon-level metrics (per tenor results)
CREATE TABLE simulation_horizon_metrics (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL,
    tenor VARCHAR(10) NOT NULL,
    p_any_default DECIMAL(10, 6),
    expected_defaults DECIMAL(10, 4),
    loss_mean DECIMAL(18, 4),
    loss_var95 DECIMAL(18, 4),
    loss_var99 DECIMAL(18, 4),
    loss_es97_5 DECIMAL(18, 4),
    sum_standalone_el DECIMAL(18, 4),
    portfolio_el DECIMAL(18, 4),
    diversification_benefit_pct DECIMAL(10, 4),
    CONSTRAINT fk_horizon_metrics_run FOREIGN KEY (run_id) REFERENCES simulation_runs(run_id) ON DELETE CASCADE,
    CONSTRAINT uk_run_tenor UNIQUE (run_id, tenor)
);

-- Create index for lookups
CREATE INDEX idx_horizon_metrics_run ON simulation_horizon_metrics(run_id);

-- Per-entity contribution metrics
CREATE TABLE simulation_contributors (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    marginal_el_pct DECIMAL(10, 4),
    beta DECIMAL(6, 4),
    standalone_el DECIMAL(18, 4),
    recovery_mean DECIMAL(6, 4),
    recovery_stdev DECIMAL(6, 4),
    CONSTRAINT fk_contributor_run FOREIGN KEY (run_id) REFERENCES simulation_runs(run_id) ON DELETE CASCADE
);

-- Create indexes for queries
CREATE INDEX idx_contributors_run ON simulation_contributors(run_id);
CREATE INDEX idx_contributors_entity ON simulation_contributors(entity_name);

-- Audit trail for simulation runs (optional, for compliance)
CREATE TABLE simulation_audit (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(50),
    event_data JSONB,
    CONSTRAINT fk_audit_run FOREIGN KEY (run_id) REFERENCES simulation_runs(run_id) ON DELETE CASCADE
);

-- Create index for audit queries
CREATE INDEX idx_simulation_audit_run ON simulation_audit(run_id);
CREATE INDEX idx_simulation_audit_timestamp ON simulation_audit(event_timestamp DESC);
