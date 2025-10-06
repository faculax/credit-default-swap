-- Add payment tracking fields to coupon_periods table

ALTER TABLE coupon_periods 
    ADD COLUMN paid BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN paid_at TIMESTAMP;

-- Create index for querying unpaid coupons
CREATE INDEX idx_coupon_periods_paid ON coupon_periods(paid, payment_date);

-- Create index for trade ID and payment status queries
CREATE INDEX idx_coupon_periods_trade_paid ON coupon_periods(trade_id, paid);
