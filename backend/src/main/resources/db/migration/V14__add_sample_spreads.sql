-- Add realistic CDS spreads to existing trades for simulation testing
-- Using typical investment-grade and high-yield spreads

UPDATE cds_trades 
SET spread = 150.00  -- 150 bps (moderate risk)
WHERE reference_entity = 'TSLA';

UPDATE cds_trades 
SET spread = 160.00  -- 160 bps (similar to TSLA)
WHERE reference_entity = 'TESLA';

UPDATE cds_trades 
SET spread = 45.00   -- 45 bps (investment grade, low risk)
WHERE reference_entity = 'APPLE';

UPDATE cds_trades 
SET spread = 65.00   -- 65 bps (investment grade, moderate risk)
WHERE reference_entity = 'AMAZON';
