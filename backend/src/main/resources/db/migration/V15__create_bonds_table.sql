-- Create Bonds table for cash corporate / sovereign credit bonds
-- Epic 14: Credit Bonds Enablement

CREATE TABLE IF NOT EXISTS public.bonds (
    id BIGSERIAL PRIMARY KEY,
    
    -- Identification
    isin VARCHAR(12),                                -- Unique ISIN identifier (nullable until assigned)
    
    -- Issuer & Credit Info
    issuer VARCHAR(40) NOT NULL,                     -- Maps to reference entity (e.g., 'AAPL')
    seniority VARCHAR(20) NOT NULL,                  -- SR_UNSEC, SR_SEC, SUBORD
    credit_curve_id VARCHAR(40),                     -- Hazard curve linkage (e.g., 'AAPL_SR_USD')
    recovery_rate DECIMAL(5,4) DEFAULT 0.4000,       -- For JTD & risky PV (0-1)
    sector VARCHAR(30),                              -- Aggregation & correlation (e.g., 'TECH')
    
    -- Economics
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',     -- Pricing & aggregation
    notional DECIMAL(18,2) NOT NULL,                -- Face amount
    coupon_rate DECIMAL(9,6) NOT NULL,              -- Annualized fixed coupon rate
    coupon_frequency VARCHAR(20) NOT NULL,           -- ANNUAL, SEMI_ANNUAL, QUARTERLY
    day_count VARCHAR(20) NOT NULL,                  -- ACT_ACT, THIRTY_360
    
    -- Dates
    issue_date DATE NOT NULL,                        -- Start of accrual
    maturity_date DATE NOT NULL,                     -- Redemption date
    settlement_days INT DEFAULT 2,                   -- Settlement lag
    
    -- Pricing Convention
    face_value DECIMAL(18,2) DEFAULT 100.00,        -- Per-unit face (if quoting per 100)
    price_convention VARCHAR(10) DEFAULT 'CLEAN',   -- CLEAN or DIRTY
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Constraints
    CHECK (notional > 0),
    CHECK (coupon_rate >= 0),
    CHECK (issue_date < maturity_date),
    CHECK (recovery_rate >= 0 AND recovery_rate <= 1),
    CHECK (settlement_days >= 0)
);

-- Indexes for common queries
CREATE INDEX idx_bonds_issuer ON public.bonds(issuer);
CREATE INDEX idx_bonds_maturity_date ON public.bonds(maturity_date);
CREATE INDEX idx_bonds_sector ON public.bonds(sector);
CREATE INDEX idx_bonds_currency ON public.bonds(currency);

-- Optional unique constraint on ISIN when present
CREATE UNIQUE INDEX idx_bonds_isin_unique ON public.bonds(isin) WHERE isin IS NOT NULL;
