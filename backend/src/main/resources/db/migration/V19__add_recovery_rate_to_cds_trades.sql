-- Add recovery_rate column to cds_trades table
ALTER TABLE public.cds_trades
ADD COLUMN recovery_rate DECIMAL(5,2) NOT NULL DEFAULT 40.00;

-- Add comment to explain the column
COMMENT ON COLUMN public.cds_trades.recovery_rate IS 'Recovery rate as a percentage (0-100). Default is 40% for senior unsecured corporate debt.';
