-- Epic 16, Story 16.2: EOD Batch Job Framework
-- Creates tables for EOD valuation job orchestration and scheduling

-- EOD valuation job header
CREATE TABLE eod_valuation_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL UNIQUE,
    valuation_date DATE NOT NULL,
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Status: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    
    -- Execution timeline
    scheduled_time TIMESTAMP WITH TIME ZONE,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    
    -- Configuration
    dry_run BOOLEAN DEFAULT FALSE,
    manual_trigger BOOLEAN DEFAULT FALSE,
    triggered_by VARCHAR(100),
    
    -- Progress tracking
    current_step INTEGER DEFAULT 0,
    total_steps INTEGER DEFAULT 7,
    progress_percentage DECIMAL(5, 2) DEFAULT 0.00,
    
    -- Results summary
    total_trades_processed INTEGER DEFAULT 0,
    successful_valuations INTEGER DEFAULT 0,
    failed_valuations INTEGER DEFAULT 0,
    
    -- Error handling
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_job_status CHECK (
        status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')
    )
);

-- Individual job steps
CREATE TABLE eod_valuation_job_steps (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES eod_valuation_jobs(id) ON DELETE CASCADE,
    
    step_number INTEGER NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    -- Steps: CAPTURE_MARKET_DATA, LOAD_ACTIVE_TRADES, CALCULATE_NPV, 
    --        CALCULATE_ACCRUED, CALCULATE_PNL, AGGREGATE_RISK, RECONCILE
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    
    records_processed INTEGER DEFAULT 0,
    records_successful INTEGER DEFAULT 0,
    records_failed INTEGER DEFAULT 0,
    
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(job_id, step_number),
    CONSTRAINT chk_step_status CHECK (
        status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED')
    )
);

-- EOD valuation configuration
CREATE TABLE eod_valuation_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_valuation_jobs_date ON eod_valuation_jobs(valuation_date);
CREATE INDEX idx_valuation_jobs_status ON eod_valuation_jobs(status);
CREATE INDEX idx_valuation_jobs_job_id ON eod_valuation_jobs(job_id);

CREATE INDEX idx_job_steps_job ON eod_valuation_job_steps(job_id);
CREATE INDEX idx_job_steps_status ON eod_valuation_job_steps(status);

CREATE INDEX idx_valuation_config_key ON eod_valuation_config(config_key);

-- Insert default configuration
INSERT INTO eod_valuation_config (config_key, config_value, description) VALUES
    ('eod.schedule.cron', '0 0 18 * * MON-FRI', 'Cron expression for EOD job (6pm on business days)'),
    ('eod.schedule.timezone', 'America/New_York', 'Timezone for EOD schedule'),
    ('eod.batch.size', '100', 'Number of trades to process in each batch'),
    ('eod.retry.max', '3', 'Maximum number of retries for failed steps'),
    ('eod.retry.delay.seconds', '60', 'Delay in seconds between retries'),
    ('eod.notification.enabled', 'true', 'Enable email notifications'),
    ('eod.notification.emails', '', 'Comma-separated list of notification emails'),
    ('eod.parallel.enabled', 'true', 'Enable parallel processing'),
    ('eod.parallel.threads', '4', 'Number of parallel threads');

-- Comments
COMMENT ON TABLE eod_valuation_jobs IS 'EOD valuation batch job execution tracking';
COMMENT ON TABLE eod_valuation_job_steps IS 'Individual steps within EOD valuation jobs';
COMMENT ON TABLE eod_valuation_config IS 'Configuration parameters for EOD valuation jobs';
