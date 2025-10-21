-- Add variation margin and initial margin columns to margin_statements table
-- This supports the Epic 8 reconciliation dashboard

ALTER TABLE margin_statements 
ADD COLUMN variation_margin DECIMAL(15, 2),
ADD COLUMN initial_margin DECIMAL(15, 2);

-- Add indexes for performance on dashboard queries
CREATE INDEX idx_margin_statements_variation_margin ON margin_statements(variation_margin) WHERE variation_margin IS NOT NULL;
CREATE INDEX idx_margin_statements_initial_margin ON margin_statements(initial_margin) WHERE initial_margin IS NOT NULL;

-- Insert some sample data for testing the dashboard
INSERT INTO margin_statements (
    statement_id, ccp_name, member_firm, account_number, statement_date, currency, 
    statement_format, file_name, variation_margin, initial_margin, status
) VALUES 
('MS001', 'LCH', 'Goldman Sachs', 'GS001', CURRENT_DATE - INTERVAL '1 day', 'USD', 'CSV', 'margin_statement_gs_20251012.csv', 150000.00, 2500000.00, 'PROCESSED'),
('MS002', 'CME', 'JP Morgan', 'JPM001', CURRENT_DATE - INTERVAL '1 day', 'USD', 'XML', 'margin_statement_jpm_20251012.xml', -75000.00, 1800000.00, 'PROCESSED'),
('MS003', 'ICE', 'Morgan Stanley', 'MS001', CURRENT_DATE - INTERVAL '1 day', 'USD', 'JSON', 'margin_statement_ms_20251012.json', 325000.00, 3200000.00, 'PROCESSED'),
('MS004', 'LCH', 'Bank of America', 'BOA001', CURRENT_DATE - INTERVAL '1 day', 'USD', 'CSV', 'margin_statement_boa_20251012.csv', 0.00, 1950000.00, 'PROCESSED'),
('MS005', 'CME', 'Citigroup', 'CITI001', CURRENT_DATE - INTERVAL '1 day', 'USD', 'XML', 'margin_statement_citi_20251012.xml', 425000.00, 2750000.00, 'FAILED'),
('MS006', 'ICE', 'Wells Fargo', 'WF001', CURRENT_DATE - INTERVAL '1 day', 'USD', 'CSV', 'margin_statement_wf_20251012.csv', -125000.00, 1650000.00, 'DISPUTED'),
('MS007', 'LCH', 'UBS', 'UBS001', CURRENT_DATE, 'USD', 'JSON', 'margin_statement_ubs_20251013.json', 275000.00, 2850000.00, 'PENDING'),
('MS008', 'CME', 'Deutsche Bank', 'DB001', CURRENT_DATE, 'USD', 'XML', 'margin_statement_db_20251013.xml', 185000.00, 2150000.00, 'PROCESSING');

COMMENT ON COLUMN margin_statements.variation_margin IS 'Daily variation margin amount - positive means payment required, negative means payment received';
COMMENT ON COLUMN margin_statements.initial_margin IS 'Required initial margin amount for positions covered by this statement';