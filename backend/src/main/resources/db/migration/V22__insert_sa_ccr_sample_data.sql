-- Sample data for SA-CCR implementation
-- Insert sample netting sets
INSERT INTO netting_sets (netting_set_id, counterparty_id, legal_agreement_type, agreement_date, governing_law, netting_eligible, collateral_agreement, created_at, updated_at) VALUES
('NS-JPMC-001', 'JPMORGAN CHASE', 'ISDA Master Agreement', '2020-01-15', 'NY', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NS-GSC-001', 'GOLDMAN SACHS', 'ISDA Master Agreement', '2019-06-20', 'NY', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NS-MSC-001', 'MORGAN STANLEY', 'ISDA Master Agreement', '2021-03-10', 'NY', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NS-CITI-001', 'CITIGROUP', 'ISDA Master Agreement', '2020-11-05', 'NY', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NS-BOA-001', 'BANK OF AMERICA', 'ISDA Master Agreement', '2021-01-20', 'NY', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert US supervisory parameters (Basel III standard values)
INSERT INTO sa_ccr_supervisory_parameters (jurisdiction, asset_class, parameter_type, tenor_bucket, credit_quality, parameter_value, effective_date, created_at) VALUES
-- Alpha factors
('US', 'ALL', 'ALPHA_FACTOR', NULL, NULL, 1.4, '2020-01-01', CURRENT_TIMESTAMP),
('EU', 'ALL', 'ALPHA_FACTOR', NULL, NULL, 1.4, '2020-01-01', CURRENT_TIMESTAMP),
('UK', 'ALL', 'ALPHA_FACTOR', NULL, NULL, 1.4, '2020-01-01', CURRENT_TIMESTAMP),

-- Credit supervisory factors by credit quality
('US', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'IG', 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'HY', 0.013, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'UNRATED', 0.013, '2020-01-01', CURRENT_TIMESTAMP),

-- Interest Rate supervisory factors by tenor
('US', 'IR', 'SUPERVISORY_FACTOR', 'LESS_THAN_1Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'IR', 'SUPERVISORY_FACTOR', 'Y1_TO_2Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'IR', 'SUPERVISORY_FACTOR', 'Y2_TO_5Y', NULL, 0.0075, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'IR', 'SUPERVISORY_FACTOR', 'GREATER_THAN_5Y', NULL, 0.015, '2020-01-01', CURRENT_TIMESTAMP),

-- Other asset class supervisory factors
('US', 'FX', 'SUPERVISORY_FACTOR', NULL, NULL, 0.04, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'EQUITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.32, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'COMMODITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.18, '2020-01-01', CURRENT_TIMESTAMP),

-- EU parameters (same values but different jurisdiction)
('EU', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'IG', 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('EU', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'HY', 0.013, '2020-01-01', CURRENT_TIMESTAMP),
('EU', 'IR', 'SUPERVISORY_FACTOR', 'LESS_THAN_1Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('EU', 'IR', 'SUPERVISORY_FACTOR', 'Y2_TO_5Y', NULL, 0.0075, '2020-01-01', CURRENT_TIMESTAMP),
('EU', 'IR', 'SUPERVISORY_FACTOR', 'GREATER_THAN_5Y', NULL, 0.015, '2020-01-01', CURRENT_TIMESTAMP),
('EU', 'FX', 'SUPERVISORY_FACTOR', NULL, NULL, 0.04, '2020-01-01', CURRENT_TIMESTAMP),
('EU', 'EQUITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.32, '2020-01-01', CURRENT_TIMESTAMP),

-- UK parameters
('UK', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'IG', 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('UK', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'HY', 0.013, '2020-01-01', CURRENT_TIMESTAMP),
('UK', 'IR', 'SUPERVISORY_FACTOR', 'LESS_THAN_1Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('UK', 'IR', 'SUPERVISORY_FACTOR', 'Y2_TO_5Y', NULL, 0.0075, '2020-01-01', CURRENT_TIMESTAMP),
('UK', 'FX', 'SUPERVISORY_FACTOR', NULL, NULL, 0.04, '2020-01-01', CURRENT_TIMESTAMP);

-- Insert correlation parameters for cross-asset diversification (advanced implementation)
INSERT INTO sa_ccr_supervisory_parameters (jurisdiction, asset_class, parameter_type, parameter_value, effective_date, created_at) VALUES
('US', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'CREDIT', 'CORRELATION_SAME_BUCKET', 0.5, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'CREDIT', 'CORRELATION_DIFF_BUCKET', 0.35, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'IR', 'CORRELATION_SAME_BUCKET', 0.999, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'IR', 'CORRELATION_DIFF_BUCKET', 0.0, '2020-01-01', CURRENT_TIMESTAMP);