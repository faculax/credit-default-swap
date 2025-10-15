-- V18: Add support for baskets in portfolios
-- Allows portfolios to contain CDS trades, corporate bonds, and basket derivatives

CREATE TABLE basket_portfolio_constituents (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    basket_id BIGINT NOT NULL,
    weight_type VARCHAR(20) NOT NULL CHECK (weight_type IN ('EQUAL', 'NOTIONAL', 'MARKET_VALUE')),
    weight_value DECIMAL(20, 4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_basket_portfolio FOREIGN KEY (portfolio_id) 
        REFERENCES cds_portfolios(id) ON DELETE CASCADE,
    CONSTRAINT fk_basket_constituent FOREIGN KEY (basket_id) 
        REFERENCES basket_definitions(id) ON DELETE CASCADE,
    CONSTRAINT unique_basket_in_portfolio UNIQUE (portfolio_id, basket_id)
);

-- Indexes for performance
CREATE INDEX idx_basket_constituents_portfolio ON basket_portfolio_constituents(portfolio_id);
CREATE INDEX idx_basket_constituents_basket ON basket_portfolio_constituents(basket_id);
CREATE INDEX idx_basket_constituents_active ON basket_portfolio_constituents(active) WHERE active = true;

-- Comments for documentation
COMMENT ON TABLE basket_portfolio_constituents IS 'Maps basket derivatives to portfolios for multi-asset credit portfolios';
COMMENT ON COLUMN basket_portfolio_constituents.weight_type IS 'How the basket is weighted in the portfolio: EQUAL, NOTIONAL, or MARKET_VALUE';
COMMENT ON COLUMN basket_portfolio_constituents.weight_value IS 'Weight value corresponding to the weight_type';
COMMENT ON COLUMN basket_portfolio_constituents.active IS 'Whether this basket is currently active in the portfolio';
