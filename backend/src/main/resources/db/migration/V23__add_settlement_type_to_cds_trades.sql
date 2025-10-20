-- Add settlement_type column to cds_trades table
-- Settlement type determines how a CDS is settled upon credit event (CASH or PHYSICAL)

ALTER TABLE public.cds_trades 
ADD COLUMN settlement_type VARCHAR(20) NOT NULL DEFAULT 'CASH' 
CHECK (settlement_type IN ('CASH', 'PHYSICAL'));

-- Create index for settlement_type queries
CREATE INDEX IF NOT EXISTS idx_cds_settlement_type ON public.cds_trades(settlement_type);

-- Add comment to document the field
COMMENT ON COLUMN public.cds_trades.settlement_type IS 'Settlement method for credit events: CASH or PHYSICAL';
