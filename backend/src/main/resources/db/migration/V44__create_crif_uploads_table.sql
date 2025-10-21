-- V44: Create crif_uploads table for CRIF file upload tracking
-- This table stores metadata about CRIF file uploads for SIMM calculation

CREATE TABLE crif_uploads (
    id BIGSERIAL PRIMARY KEY,
    upload_id VARCHAR(100) UNIQUE NOT NULL,
    filename VARCHAR(255) NOT NULL,
    portfolio_id VARCHAR(100),
    valuation_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    upload_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_records INTEGER NOT NULL DEFAULT 0,
    valid_records INTEGER NOT NULL DEFAULT 0,
    error_records INTEGER NOT NULL DEFAULT 0,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_crif_uploads_upload_id ON crif_uploads(upload_id);
CREATE INDEX idx_crif_uploads_portfolio_id ON crif_uploads(portfolio_id) WHERE portfolio_id IS NOT NULL;
CREATE INDEX idx_crif_uploads_valuation_date ON crif_uploads(valuation_date);
CREATE INDEX idx_crif_uploads_status ON crif_uploads(processing_status);
CREATE INDEX idx_crif_uploads_created_at ON crif_uploads(created_at DESC);

-- Comments
COMMENT ON TABLE crif_uploads IS 'Tracks CRIF (Common Risk Interchange Format) file uploads for SIMM calculations';
COMMENT ON COLUMN crif_uploads.upload_id IS 'Unique identifier for the upload';
COMMENT ON COLUMN crif_uploads.filename IS 'Name of the uploaded CRIF file';
COMMENT ON COLUMN crif_uploads.portfolio_id IS 'Portfolio identifier from the CRIF file';
COMMENT ON COLUMN crif_uploads.valuation_date IS 'Valuation date for the sensitivity data';
COMMENT ON COLUMN crif_uploads.currency IS 'Base currency for the sensitivity amounts';
COMMENT ON COLUMN crif_uploads.processing_status IS 'Processing status: PENDING, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN crif_uploads.total_records IS 'Total number of sensitivity records in the upload';
COMMENT ON COLUMN crif_uploads.valid_records IS 'Number of successfully processed records';
COMMENT ON COLUMN crif_uploads.error_records IS 'Number of records with processing errors';
