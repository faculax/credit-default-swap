-- V20: Add obligation reference to CDS trades
-- Links a CDS trade to a specific bond (obligation) issued by the reference entity

ALTER TABLE public.cds_trades 
ADD COLUMN obligation_id BIGINT;

-- Add foreign key to bonds table
ALTER TABLE public.cds_trades 
ADD CONSTRAINT fk_cds_trade_obligation 
FOREIGN KEY (obligation_id) 
REFERENCES public.bonds(id) 
ON DELETE SET NULL;

-- Create index for performance when querying by obligation
CREATE INDEX idx_cds_trades_obligation ON public.cds_trades(obligation_id);

-- Add comment for documentation
COMMENT ON COLUMN public.cds_trades.obligation_id IS 'Optional reference to a specific bond (obligation) from the reference entity issuer';
