-- Create CCP accounts table for novation support
CREATE TABLE IF NOT EXISTS public.ccp_accounts (
    id BIGSERIAL PRIMARY KEY,
    ccp_name VARCHAR(50) NOT NULL,
    member_firm VARCHAR(100) NOT NULL,
    member_id VARCHAR(50) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(100),
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('HOUSE', 'CLIENT', 'SEGREGATED')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_APPROVAL')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Ensure unique combination of CCP, member firm, and account number
    CONSTRAINT uk_ccp_member_account UNIQUE (ccp_name, member_firm, account_number)
);

-- Create CCP account product eligibility table
CREATE TABLE IF NOT EXISTS public.ccp_account_product_eligibility (
    ccp_account_id BIGINT NOT NULL,
    product_type VARCHAR(50) NOT NULL,
    
    PRIMARY KEY (ccp_account_id, product_type),
    CONSTRAINT fk_ccp_account_eligibility 
        FOREIGN KEY (ccp_account_id) REFERENCES public.ccp_accounts(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_ccp_accounts_ccp_name ON public.ccp_accounts(ccp_name);
CREATE INDEX IF NOT EXISTS idx_ccp_accounts_member_firm ON public.ccp_accounts(member_firm);
CREATE INDEX IF NOT EXISTS idx_ccp_accounts_status ON public.ccp_accounts(status);
CREATE INDEX IF NOT EXISTS idx_ccp_accounts_created_at ON public.ccp_accounts(created_at);

-- Index for product eligibility lookups
CREATE INDEX IF NOT EXISTS idx_ccp_eligibility_product ON public.ccp_account_product_eligibility(product_type);

-- Insert some sample CCP accounts for testing
INSERT INTO public.ccp_accounts (ccp_name, member_firm, member_id, account_number, account_name, account_type, status) VALUES
('LCH', 'Goldman Sachs', 'GS001', 'HOUSE-001', 'GS House Account', 'HOUSE', 'ACTIVE'),
('LCH', 'Goldman Sachs', 'GS002', 'CLIENT-001', 'GS Client Account', 'CLIENT', 'ACTIVE'),
('CME', 'Morgan Stanley', 'MS001', 'HOUSE-001', 'MS House Account', 'HOUSE', 'ACTIVE'),
('CME', 'JPMorgan', 'JPM001', 'HOUSE-001', 'JPM House Account', 'HOUSE', 'ACTIVE'),
('DTCC', 'Citi', 'CITI001', 'HOUSE-001', 'Citi House Account', 'HOUSE', 'ACTIVE')
ON CONFLICT (ccp_name, member_firm, account_number) DO NOTHING;

-- Insert product eligibilities for the sample accounts
INSERT INTO public.ccp_account_product_eligibility (ccp_account_id, product_type) 
SELECT id, 'CDS' FROM public.ccp_accounts WHERE ccp_name IN ('LCH', 'CME', 'DTCC')
ON CONFLICT (ccp_account_id, product_type) DO NOTHING;

INSERT INTO public.ccp_account_product_eligibility (ccp_account_id, product_type) 
SELECT id, 'IRS' FROM public.ccp_accounts WHERE ccp_name IN ('LCH', 'CME')
ON CONFLICT (ccp_account_id, product_type) DO NOTHING;