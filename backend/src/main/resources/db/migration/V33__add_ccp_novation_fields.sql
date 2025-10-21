-- Add CCP and novation related fields to cds_trades table
ALTER TABLE public.cds_trades 
ADD COLUMN IF NOT EXISTS ccp_name VARCHAR(50),
ADD COLUMN IF NOT EXISTS ccp_member_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS clearing_account VARCHAR(50),
ADD COLUMN IF NOT EXISTS netting_set_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS original_trade_id BIGINT,
ADD COLUMN IF NOT EXISTS novation_timestamp TIMESTAMP,
ADD COLUMN IF NOT EXISTS novation_reference VARCHAR(100),
ADD COLUMN IF NOT EXISTS uti VARCHAR(100),
ADD COLUMN IF NOT EXISTS usi VARCHAR(100),
ADD COLUMN IF NOT EXISTS is_cleared BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

-- Add foreign key constraint for original trade reference
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_original_trade'
        AND table_name = 'cds_trades'
    ) THEN
        ALTER TABLE public.cds_trades 
        ADD CONSTRAINT fk_original_trade 
        FOREIGN KEY (original_trade_id) REFERENCES public.cds_trades(id);
    END IF;
END $$;

-- Create indexes for novation-related queries
CREATE INDEX IF NOT EXISTS idx_cds_ccp_name ON public.cds_trades(ccp_name);
CREATE INDEX IF NOT EXISTS idx_cds_original_trade_id ON public.cds_trades(original_trade_id);
CREATE INDEX IF NOT EXISTS idx_cds_is_cleared ON public.cds_trades(is_cleared);
CREATE INDEX IF NOT EXISTS idx_cds_novation_timestamp ON public.cds_trades(novation_timestamp);
CREATE INDEX IF NOT EXISTS idx_cds_novation_reference ON public.cds_trades(novation_reference);

-- Add unique constraint on UTI and USI if they exist
CREATE UNIQUE INDEX IF NOT EXISTS idx_cds_uti_unique ON public.cds_trades(uti) WHERE uti IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_cds_usi_unique ON public.cds_trades(usi) WHERE usi IS NOT NULL;