-- Epic 16, Story 16.3: NPV Calculation via ORE
-- Creates tables for storing trade valuations and sensitivities

-- Trade valuations (NPV and components)
CREATE TABLE trade_valuations (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    
    -- NPV breakdown
    npv DECIMAL(20, 4) NOT NULL,
    premium_leg_pv DECIMAL(20, 4),
    protection_leg_pv DECIMAL(20, 4),
    
    -- Valuation inputs
    credit_spread DECIMAL(10, 6),
    recovery_rate DECIMAL(5, 4),
    discount_factor DECIMAL(10, 8),
    
    currency VARCHAR(3) NOT NULL,
    
    -- Calculation details
    calculation_method VARCHAR(50) DEFAULT 'ORE',
    calculation_time_ms INTEGER,
    ore_scenario_id VARCHAR(100),
    
    -- Status
    valuation_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    
    job_id VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(valuation_date, trade_id),
    CONSTRAINT chk_valuation_status CHECK (
        valuation_status IN ('SUCCESS', 'FAILED', 'PENDING')
    )
);

-- Trade valuation sensitivities (Greeks)
CREATE TABLE trade_valuation_sensitivities (
    id BIGSERIAL PRIMARY KEY,
    trade_valuation_id BIGINT NOT NULL REFERENCES trade_valuations(id) ON DELETE CASCADE,
    
    -- Credit spread sensitivity (CS01)
    cs01 DECIMAL(20, 4), -- P&L impact of 1bp parallel spread move
    
    -- Interest rate sensitivity (IR01)
    ir01 DECIMAL(20, 4), -- P&L impact of 1bp parallel IR move
    ir01_1y DECIMAL(20, 4),
    ir01_5y DECIMAL(20, 4),
    ir01_10y DECIMAL(20, 4),
    
    -- Jump-to-default (JTD)
    jtd DECIMAL(20, 4), -- Loss if reference entity defaults
    
    -- Recovery rate sensitivity (REC01)
    rec01 DECIMAL(20, 4), -- P&L impact of 1% recovery change
    
    -- Time decay (Theta)
    theta_1d DECIMAL(20, 4), -- P&L from 1 day time decay
    
    -- Duration and DV01
    duration_years DECIMAL(10, 4),
    dv01 DECIMAL(20, 4),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_trade_valuations_date ON trade_valuations(valuation_date);
CREATE INDEX idx_trade_valuations_trade ON trade_valuations(trade_id);
CREATE INDEX idx_trade_valuations_date_trade ON trade_valuations(valuation_date, trade_id);
CREATE INDEX idx_trade_valuations_status ON trade_valuations(valuation_status);
CREATE INDEX idx_trade_valuations_job ON trade_valuations(job_id);

CREATE INDEX idx_valuation_sensitivities_valuation ON trade_valuation_sensitivities(trade_valuation_id);

-- Comments
COMMENT ON TABLE trade_valuations IS 'NPV and valuation results for CDS trades';
COMMENT ON TABLE trade_valuation_sensitivities IS 'Risk sensitivities (Greeks) for CDS trades';
COMMENT ON COLUMN trade_valuations.npv IS 'Net present value of the trade';
COMMENT ON COLUMN trade_valuations.premium_leg_pv IS 'Present value of premium payments';
COMMENT ON COLUMN trade_valuations.protection_leg_pv IS 'Present value of protection leg';
COMMENT ON COLUMN trade_valuation_sensitivities.cs01 IS 'Credit spread sensitivity (1bp move)';
COMMENT ON COLUMN trade_valuation_sensitivities.ir01 IS 'Interest rate sensitivity (1bp move)';
COMMENT ON COLUMN trade_valuation_sensitivities.jtd IS 'Jump-to-default risk (immediate default loss)';
COMMENT ON COLUMN trade_valuation_sensitivities.rec01 IS 'Recovery rate sensitivity (1% move)';
