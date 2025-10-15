-- Create Basket tables for multi-name credit derivatives
-- Epic 15: Basket & Multi-Name Credit Derivatives

-- Basket Definitions table
CREATE TABLE IF NOT EXISTS public.basket_definitions (
    id BIGSERIAL PRIMARY KEY,
    
    -- Identification
    name VARCHAR(80) NOT NULL UNIQUE,
    
    -- Basket Type & Parameters
    type VARCHAR(20) NOT NULL,  -- FIRST_TO_DEFAULT, NTH_TO_DEFAULT, TRANCHETTE
    nth INTEGER,                -- Required for NTH_TO_DEFAULT (1 to numberOfNames)
    attachment_point DECIMAL(9,6),  -- For TRANCHETTE (e.g., 0.03 = 3%)
    detachment_point DECIMAL(9,6),  -- For TRANCHETTE (must be > attachment_point)
    
    -- Premium & Conventions
    premium_frequency VARCHAR(20) NOT NULL,  -- QUARTERLY, SEMI_ANNUAL
    day_count VARCHAR(20) NOT NULL,          -- ACT_360, ACT_365F
    
    -- Economics
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    notional DECIMAL(18,2) NOT NULL,
    maturity_date DATE NOT NULL,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Constraints
    CHECK (notional > 0),
    CHECK (type IN ('FIRST_TO_DEFAULT', 'NTH_TO_DEFAULT', 'TRANCHETTE')),
    CHECK (premium_frequency IN ('QUARTERLY', 'SEMI_ANNUAL')),
    CHECK (day_count IN ('ACT_360', 'ACT_365F')),
    CHECK (
        (type = 'NTH_TO_DEFAULT' AND nth IS NOT NULL AND nth >= 1) OR
        (type != 'NTH_TO_DEFAULT')
    ),
    CHECK (
        (type = 'TRANCHETTE' AND attachment_point IS NOT NULL AND detachment_point IS NOT NULL AND 
         attachment_point >= 0 AND attachment_point < detachment_point AND detachment_point <= 1) OR
        (type != 'TRANCHETTE')
    )
);

-- Basket Constituents table
CREATE TABLE IF NOT EXISTS public.basket_constituents (
    id BIGSERIAL PRIMARY KEY,
    
    -- Foreign Key
    basket_id BIGINT NOT NULL REFERENCES public.basket_definitions(id) ON DELETE CASCADE,
    
    -- Constituent Details
    issuer VARCHAR(40) NOT NULL,           -- Reference entity code (e.g., 'AAPL')
    weight DECIMAL(18,10),                 -- Fraction of notional; NULL means equal weight
    recovery_override DECIMAL(5,4),        -- Optional override; NULL uses default
    seniority VARCHAR(20),                 -- SR_UNSEC, SR_SEC, SUBORD
    sector VARCHAR(30),                    -- For simulation factor mapping
    
    -- Ordering
    sequence_order INTEGER NOT NULL DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CHECK (weight IS NULL OR (weight >= 0 AND weight <= 1)),
    CHECK (recovery_override IS NULL OR (recovery_override >= 0 AND recovery_override <= 1)),
    CHECK (seniority IS NULL OR seniority IN ('SR_UNSEC', 'SR_SEC', 'SUBORD')),
    
    -- Unique constraint: no duplicate issuers in same basket
    UNIQUE (basket_id, issuer)
);

-- Indexes for performance
CREATE INDEX idx_basket_definitions_type ON public.basket_definitions(type);
CREATE INDEX idx_basket_definitions_currency ON public.basket_definitions(currency);
CREATE INDEX idx_basket_definitions_maturity_date ON public.basket_definitions(maturity_date);
CREATE INDEX idx_basket_constituents_basket_id ON public.basket_constituents(basket_id);
CREATE INDEX idx_basket_constituents_issuer ON public.basket_constituents(issuer);
CREATE INDEX idx_basket_constituents_sector ON public.basket_constituents(sector);

-- Comments for documentation
COMMENT ON TABLE public.basket_definitions IS 'Basket credit derivative definitions (First-to-Default, N-th-to-Default, Tranche)';
COMMENT ON TABLE public.basket_constituents IS 'Constituent reference entities for basket instruments';
COMMENT ON COLUMN public.basket_definitions.type IS 'Basket type: FIRST_TO_DEFAULT, NTH_TO_DEFAULT, TRANCHETTE';
COMMENT ON COLUMN public.basket_definitions.nth IS 'For N-th-to-Default: which default triggers (1=first, 2=second, etc.)';
COMMENT ON COLUMN public.basket_definitions.attachment_point IS 'For tranches: lower bound of loss slice (0-1)';
COMMENT ON COLUMN public.basket_definitions.detachment_point IS 'For tranches: upper bound of loss slice (0-1)';
COMMENT ON COLUMN public.basket_constituents.weight IS 'Notional weight; NULL means equal weighting';
COMMENT ON COLUMN public.basket_constituents.recovery_override IS 'Override default recovery rate; NULL uses issuer default';
