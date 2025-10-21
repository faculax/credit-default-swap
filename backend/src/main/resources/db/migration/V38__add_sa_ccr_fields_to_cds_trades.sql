-- V23: Add SA-CCR specific fields to CDS trades table
-- These fields are required for SA-CCR exposure calculations

-- Add mark-to-market value field for SA-CCR calculations
ALTER TABLE cds_trades 
ADD COLUMN mark_to_market_value DECIMAL(15,2);

-- Add upfront amount field for SA-CCR calculations  
ALTER TABLE cds_trades 
ADD COLUMN upfront_amount DECIMAL(15,2);

-- Add index on mark_to_market_value for performance during SA-CCR calculations
CREATE INDEX idx_cds_mark_to_market_value ON cds_trades(mark_to_market_value);

-- Update existing sample trades with mock SA-CCR values for testing
UPDATE cds_trades 
SET mark_to_market_value = 
    CASE 
        WHEN buy_sell_protection = 'BUY' THEN (notional_amount * spread * 0.001)::DECIMAL(15,2)
        ELSE (notional_amount * spread * -0.001)::DECIMAL(15,2)
    END,
    upfront_amount = 
    CASE 
        WHEN notional_amount > 50000000 THEN (notional_amount * 0.005)::DECIMAL(15,2)
        ELSE 0.00
    END
WHERE mark_to_market_value IS NULL;