-- V54: Make margin statement enum creation idempotent
-- This migration wraps the enum type creation from V36 in idempotent blocks
-- Since the enums already exist (created by V36), these blocks will safely do nothing

-- Statement processing status enum type (idempotent)
DO $$ BEGIN
    CREATE TYPE statement_status AS ENUM ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DISPUTED', 'RETRYING');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Statement format enum type (idempotent)
DO $$ BEGIN
    CREATE TYPE statement_format AS ENUM ('CSV', 'XML', 'JSON', 'PROPRIETARY');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Collateral position type enum (idempotent)
DO $$ BEGIN
    CREATE TYPE position_type AS ENUM ('VARIATION_MARGIN', 'INITIAL_MARGIN', 'EXCESS_COLLATERAL');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;
