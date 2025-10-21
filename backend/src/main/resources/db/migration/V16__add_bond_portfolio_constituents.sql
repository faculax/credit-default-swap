-- V16: Add support for bonds in portfolios
-- Allows portfolios to contain both CDS trades and corporate bonds

CREATE TABLE bond_portfolio_constituents (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    bond_id BIGINT NOT NULL,
    weight_type VARCHAR(20) NOT NULL CHECK (weight_type IN ('EQUAL', 'NOTIONAL', 'MARKET_VALUE')),
    weight_value DECIMAL(20, 4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_bond_portfolio FOREIGN KEY (portfolio_id) 
        REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    CONSTRAINT fk_bond_constituent FOREIGN KEY (bond_id) 
        REFERENCES bonds(id) ON DELETE CASCADE,
    CONSTRAINT unique_bond_in_portfolio UNIQUE (portfolio_id, bond_id)
);

-- Indexes for performance
CREATE INDEX idx_bond_constituents_portfolio ON bond_portfolio_constituents(portfolio_id);
CREATE INDEX idx_bond_constituents_bond ON bond_portfolio_constituents(bond_id);
CREATE INDEX idx_bond_constituents_active ON bond_portfolio_constituents(active) WHERE active = true;

-- Comments for documentation
COMMENT ON TABLE bond_portfolio_constituents IS 'Maps bonds to portfolios for multi-asset credit portfolios';
COMMENT ON COLUMN bond_portfolio_constituents.weight_type IS 'How the bond is weighted in the portfolio: EQUAL, NOTIONAL, or MARKET_VALUE';
COMMENT ON COLUMN bond_portfolio_constituents.weight_value IS 'Weight value corresponding to the weight_type';
COMMENT ON COLUMN bond_portfolio_constituents.active IS 'Whether this bond is currently active in the portfolio';
