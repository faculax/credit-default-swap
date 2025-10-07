-- Add enhanced portfolio metrics to risk cache
ALTER TABLE portfolio_risk_cache
ADD COLUMN total_notional DECIMAL(20, 4),
ADD COLUMN upfront_premium DECIMAL(18, 4),
ADD COLUMN total_paid_coupons DECIMAL(18, 4),
ADD COLUMN trade_count INTEGER,
ADD COLUMN net_protection_bought DECIMAL(20, 4),
ADD COLUMN average_maturity_years VARCHAR(20);
