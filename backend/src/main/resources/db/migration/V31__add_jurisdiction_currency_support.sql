-- V31: Add base currency support to SA-CCR calculations for jurisdiction-specific reporting

-- Add base_currency column to sa_ccr_calculations table
ALTER TABLE sa_ccr_calculations 
ADD COLUMN base_currency VARCHAR(3);

-- Update existing records to have a default base currency based on jurisdiction
UPDATE sa_ccr_calculations 
SET base_currency = 
    CASE 
        WHEN jurisdiction = 'US' THEN 'USD'
        WHEN jurisdiction = 'EU' THEN 'EUR'
        WHEN jurisdiction = 'UK' THEN 'GBP'
        WHEN jurisdiction = 'CA' THEN 'CAD'
        WHEN jurisdiction = 'JP' THEN 'JPY'
        WHEN jurisdiction = 'AU' THEN 'AUD'
        WHEN jurisdiction = 'SG' THEN 'SGD'
        WHEN jurisdiction = 'HK' THEN 'HKD'
        ELSE 'USD'
    END
WHERE base_currency IS NULL;

-- Add index for jurisdiction-specific queries
CREATE INDEX idx_sa_ccr_calculations_jurisdiction_date ON sa_ccr_calculations(jurisdiction, calculation_date);

-- Add comment for documentation
COMMENT ON COLUMN sa_ccr_calculations.base_currency IS 'Base currency for SA-CCR exposure calculation (jurisdiction-specific)';