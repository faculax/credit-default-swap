-- Epic 16, Story 16.1: Market Data Snapshot Service
-- Creates tables to capture and store daily market data snapshots for EOD valuation

-- Main snapshot header table
CREATE TABLE market_data_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL UNIQUE,
    snapshot_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Status: PENDING, IN_PROGRESS, COMPLETE, PARTIAL, FAILED
    
    data_sources TEXT[], -- Array of source systems (e.g., BLOOMBERG, MARKIT, INTERNAL)
    
    -- Counts
    cds_spread_count INTEGER DEFAULT 0,
    ir_curve_count INTEGER DEFAULT 0,
    fx_rate_count INTEGER DEFAULT 0,
    recovery_rate_count INTEGER DEFAULT 0,
    
    -- Validation
    validation_errors TEXT,
    missing_data_points TEXT,
    
    captured_by VARCHAR(100),
    completed_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_snapshot_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'COMPLETE', 'PARTIAL', 'FAILED')
    )
);

-- CDS spreads snapshot
CREATE TABLE snapshot_cds_spreads (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES market_data_snapshots(id) ON DELETE CASCADE,
    
    reference_entity_id BIGINT, -- Links to reference_entities if exists
    reference_entity_name VARCHAR(255) NOT NULL,
    
    tenor VARCHAR(10) NOT NULL, -- e.g., 1Y, 3Y, 5Y, 7Y, 10Y
    currency VARCHAR(3) NOT NULL,
    seniority VARCHAR(50) NOT NULL, -- SENIOR_UNSECURED, SUBORDINATED
    restructuring_clause VARCHAR(50), -- CR, MR, XR, MM
    
    spread DECIMAL(10, 6) NOT NULL, -- In basis points
    
    data_source VARCHAR(50) NOT NULL,
    quote_time TIMESTAMP WITH TIME ZONE,
    is_composite BOOLEAN DEFAULT FALSE, -- Average of multiple sources
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(snapshot_id, reference_entity_name, tenor, currency, seniority)
);

-- Interest rate curves snapshot
CREATE TABLE snapshot_ir_curves (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES market_data_snapshots(id) ON DELETE CASCADE,
    
    currency VARCHAR(3) NOT NULL,
    curve_type VARCHAR(50) NOT NULL, -- LIBOR, SOFR, OIS, GOVERNMENT
    
    tenor VARCHAR(10) NOT NULL, -- 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, 30Y
    rate DECIMAL(10, 8) NOT NULL, -- Interest rate (decimal, e.g., 0.0525 for 5.25%)
    
    data_source VARCHAR(50) NOT NULL,
    quote_time TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(snapshot_id, currency, curve_type, tenor)
);

-- FX rates snapshot
CREATE TABLE snapshot_fx_rates (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES market_data_snapshots(id) ON DELETE CASCADE,
    
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    
    rate DECIMAL(18, 8) NOT NULL, -- FX rate (e.g., 1.0850 for EUR/USD)
    
    data_source VARCHAR(50) NOT NULL,
    quote_time TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(snapshot_id, base_currency, quote_currency)
);

-- Recovery rates snapshot
CREATE TABLE snapshot_recovery_rates (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES market_data_snapshots(id) ON DELETE CASCADE,
    
    reference_entity_id BIGINT,
    reference_entity_name VARCHAR(255) NOT NULL,
    
    seniority VARCHAR(50) NOT NULL,
    recovery_rate DECIMAL(5, 4) NOT NULL, -- Decimal (e.g., 0.4000 for 40%)
    
    data_source VARCHAR(50) NOT NULL,
    quote_time TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(snapshot_id, reference_entity_name, seniority)
);

-- Indexes for performance
CREATE INDEX idx_snapshot_date ON market_data_snapshots(snapshot_date);
CREATE INDEX idx_snapshot_status ON market_data_snapshots(status);

CREATE INDEX idx_cds_spread_snapshot ON snapshot_cds_spreads(snapshot_id);
CREATE INDEX idx_cds_spread_entity ON snapshot_cds_spreads(reference_entity_name);
CREATE INDEX idx_cds_spread_tenor ON snapshot_cds_spreads(tenor);

CREATE INDEX idx_ir_curve_snapshot ON snapshot_ir_curves(snapshot_id);
CREATE INDEX idx_ir_curve_currency ON snapshot_ir_curves(currency);

CREATE INDEX idx_fx_rate_snapshot ON snapshot_fx_rates(snapshot_id);
CREATE INDEX idx_fx_rate_base ON snapshot_fx_rates(base_currency);

CREATE INDEX idx_recovery_rate_snapshot ON snapshot_recovery_rates(snapshot_id);
CREATE INDEX idx_recovery_rate_entity ON snapshot_recovery_rates(reference_entity_name);

-- Comments
COMMENT ON TABLE market_data_snapshots IS 'Daily market data snapshots for EOD valuation';
COMMENT ON TABLE snapshot_cds_spreads IS 'CDS spread quotes captured at snapshot time';
COMMENT ON TABLE snapshot_ir_curves IS 'Interest rate curve points captured at snapshot time';
COMMENT ON TABLE snapshot_fx_rates IS 'FX rates captured at snapshot time';
COMMENT ON TABLE snapshot_recovery_rates IS 'Recovery rate assumptions captured at snapshot time';
