-- V11: Fix weight_value precision to support large notional amounts

-- The original DECIMAL(15, 8) only allows 7 digits before decimal (max ~9,999,999)
-- This is too small for notional amounts which can be in millions/billions
-- Change to DECIMAL(20, 4) which allows 16 digits before decimal (enough for billions)
-- and 4 decimal places for precision

ALTER TABLE cds_portfolio_constituents 
ALTER COLUMN weight_value TYPE DECIMAL(20, 4);
