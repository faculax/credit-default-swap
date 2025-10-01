-- Create cash settlements table
CREATE TABLE cds_cash_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_event_id UUID NOT NULL,
    trade_id BIGINT NOT NULL,
    notional DECIMAL(15,2) NOT NULL,
    recovery_rate DECIMAL(5,4) NOT NULL,
    payout_amount DECIMAL(15,2) NOT NULL,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_cash_settlement_credit_event FOREIGN KEY (credit_event_id) REFERENCES cds_credit_events(id),
    CONSTRAINT fk_cash_settlement_trade FOREIGN KEY (trade_id) REFERENCES cds_trades(id),
    CONSTRAINT uk_cash_settlement_event UNIQUE (credit_event_id),
    CONSTRAINT ck_recovery_rate_range CHECK (recovery_rate >= 0 AND recovery_rate <= 1),
    CONSTRAINT ck_positive_notional CHECK (notional > 0),
    CONSTRAINT ck_non_negative_payout CHECK (payout_amount >= 0)
);

-- Create index for trade lookups
CREATE INDEX idx_cash_settlements_trade_id ON cds_cash_settlements(trade_id);