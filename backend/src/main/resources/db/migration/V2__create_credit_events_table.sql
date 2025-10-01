-- Create credit events table
CREATE TABLE cds_credit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_date DATE NOT NULL,
    notice_date DATE NOT NULL,
    settlement_method VARCHAR(20) NOT NULL,
    comments TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_credit_event_trade FOREIGN KEY (trade_id) REFERENCES cds_trades(id),
    CONSTRAINT ck_settlement_method CHECK (settlement_method IN ('CASH', 'PHYSICAL')),
    CONSTRAINT ck_event_type CHECK (event_type IN ('BANKRUPTCY', 'FAILURE_TO_PAY', 'RESTRUCTURING', 'OBLIGATION_DEFAULT', 'REPUDIATION_MORATORIUM')),
    CONSTRAINT ck_notice_date_after_event CHECK (notice_date >= event_date),
    CONSTRAINT uk_credit_event_unique UNIQUE (trade_id, event_type, event_date)
);

-- Create index for common queries
CREATE INDEX idx_credit_events_trade_id ON cds_credit_events(trade_id);
CREATE INDEX idx_credit_events_event_date ON cds_credit_events(event_date);