-- Revert trade_status from PostgreSQL ENUM to VARCHAR
-- Reason: Hibernate is binding the Java Enum as a varchar which causes
--  "column is of type trade_status_enum but expression is of type character varying"
-- Using a plain VARCHAR avoids needing a custom PostgreSQL enum type handler.

ALTER TABLE cds_trades
    ADD COLUMN trade_status_text VARCHAR(40);

-- Copy existing enum values (cast enum to text)
UPDATE cds_trades
    SET trade_status_text = trade_status::text;

-- Make sure no nulls remain; default to 'PENDING' if somehow null
UPDATE cds_trades
    SET trade_status_text = 'PENDING'
    WHERE trade_status_text IS NULL;

-- Set NOT NULL constraint
ALTER TABLE cds_trades
    ALTER COLUMN trade_status_text SET NOT NULL;

-- Drop old enum column and rename new one
ALTER TABLE cds_trades DROP COLUMN trade_status;
ALTER TABLE cds_trades RENAME COLUMN trade_status_text TO trade_status;

-- Drop the PostgreSQL enum type now that it's unused
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'trade_status_enum') THEN
        DROP TYPE trade_status_enum;
    END IF;
END$$;

-- Optional: add a check constraint to keep values aligned with Java enum
ALTER TABLE cds_trades
    ADD CONSTRAINT chk_trade_status_values CHECK (trade_status IN (
        'PENDING',
        'ACTIVE',
        'CREDIT_EVENT_RECORDED',
        'TRIGGERED',
        'SETTLED_CASH',
        'SETTLED_PHYSICAL',
        'CANCELLED'
    ));

-- Note: If later we want native enum again, we can introduce a custom Hibernate type
-- (e.g. via hibernate-types library) and a forward migration.