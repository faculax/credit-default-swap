-- Create physical settlement instructions table
CREATE TABLE cds_physical_settlement_instructions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_event_id UUID NOT NULL,
    trade_id BIGINT NOT NULL,
    reference_obligation_isin VARCHAR(12),
    proposed_delivery_date DATE,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_physical_settlement_credit_event FOREIGN KEY (credit_event_id) REFERENCES cds_credit_events(id),
    CONSTRAINT fk_physical_settlement_trade FOREIGN KEY (trade_id) REFERENCES cds_trades(id),
    CONSTRAINT uk_physical_settlement_event UNIQUE (credit_event_id),
    CONSTRAINT ck_instruction_status CHECK (status IN ('DRAFT', 'PENDING', 'CONFIRMED', 'COMPLETED'))
);

-- Create index for trade lookups
CREATE INDEX idx_physical_settlements_trade_id ON cds_physical_settlement_instructions(trade_id);