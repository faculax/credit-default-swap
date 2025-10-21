-- V46: Create simm_parameter_sets table for SIMM version management
-- This table stores ISDA SIMM parameter versions

CREATE TABLE simm_parameter_sets (
    id BIGSERIAL PRIMARY KEY,
    version_name VARCHAR(50) NOT NULL,
    isda_version VARCHAR(20) NOT NULL,
    effective_date DATE NOT NULL,
    end_date DATE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_simm_parameter_sets_version ON simm_parameter_sets(version_name);
CREATE INDEX idx_simm_parameter_sets_isda_version ON simm_parameter_sets(isda_version);
CREATE INDEX idx_simm_parameter_sets_effective_date ON simm_parameter_sets(effective_date);
CREATE INDEX idx_simm_parameter_sets_active ON simm_parameter_sets(is_active) WHERE is_active = true;

-- Comments
COMMENT ON TABLE simm_parameter_sets IS 'ISDA SIMM parameter set versions for IM calculations';
COMMENT ON COLUMN simm_parameter_sets.version_name IS 'Friendly version name';
COMMENT ON COLUMN simm_parameter_sets.isda_version IS 'ISDA SIMM version (e.g., 2.6, 2.7)';
COMMENT ON COLUMN simm_parameter_sets.effective_date IS 'Date from which this parameter set is effective';
COMMENT ON COLUMN simm_parameter_sets.end_date IS 'Date until which this parameter set is effective';
COMMENT ON COLUMN simm_parameter_sets.is_active IS 'Whether this is the currently active parameter set';
