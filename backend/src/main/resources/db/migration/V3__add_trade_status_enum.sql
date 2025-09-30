-- Update trade status to use enum and add new lifecycle states
CREATE TYPE trade_status_enum AS ENUM (
    'PENDING',
    'ACTIVE', 
    'CREDIT_EVENT_RECORDED',
    'TRIGGERED',
    'SETTLED_CASH',
    'SETTLED_PHYSICAL',
    'CANCELLED'
);

-- Add new status column
ALTER TABLE cds_trades ADD COLUMN trade_status_new trade_status_enum;

-- Migrate existing data (assuming current values are strings)
UPDATE cds_trades SET trade_status_new = 
    CASE 
        WHEN trade_status = 'PENDING' THEN 'PENDING'::trade_status_enum
        WHEN trade_status = 'ACTIVE' THEN 'ACTIVE'::trade_status_enum
        WHEN trade_status = 'CANCELLED' THEN 'CANCELLED'::trade_status_enum
        ELSE 'ACTIVE'::trade_status_enum -- Default for any other values
    END;

-- Make new column not null
ALTER TABLE cds_trades ALTER COLUMN trade_status_new SET NOT NULL;

-- Drop old column and rename new one
ALTER TABLE cds_trades DROP COLUMN trade_status;
ALTER TABLE cds_trades RENAME COLUMN trade_status_new TO trade_status;