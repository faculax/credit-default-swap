-- V32: Insert comprehensive jurisdiction-specific SA-CCR parameters

-- Clean up any existing test data first to avoid conflicts
DELETE FROM sa_ccr_supervisory_parameters WHERE jurisdiction IN ('CA', 'JP', 'AU', 'SG', 'HK');

-- ========== CANADA (OSFI Implementation) ==========
-- Canadian parameters - mostly aligned with Basel III but with OSFI-specific adjustments
INSERT INTO sa_ccr_supervisory_parameters (jurisdiction, asset_class, parameter_type, tenor_bucket, credit_quality, parameter_value, effective_date, created_at) VALUES

-- Alpha factor (standard Basel III)
('CA', 'ALL', 'ALPHA_FACTOR', NULL, NULL, 1.4, '2020-01-01', CURRENT_TIMESTAMP),

-- Credit supervisory factors
('CA', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'IG', 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('CA', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'HY', 0.013, '2020-01-01', CURRENT_TIMESTAMP),
('CA', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'UNRATED', 0.015, '2020-01-01', CURRENT_TIMESTAMP), -- Slightly higher for unrated

-- Interest Rate supervisory factors
('CA', 'IR', 'SUPERVISORY_FACTOR', 'LESS_THAN_1Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('CA', 'IR', 'SUPERVISORY_FACTOR', 'Y1_TO_2Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('CA', 'IR', 'SUPERVISORY_FACTOR', 'Y2_TO_5Y', NULL, 0.0075, '2020-01-01', CURRENT_TIMESTAMP),
('CA', 'IR', 'SUPERVISORY_FACTOR', 'GREATER_THAN_5Y', NULL, 0.015, '2020-01-01', CURRENT_TIMESTAMP),

-- Other asset classes
('CA', 'FX', 'SUPERVISORY_FACTOR', NULL, NULL, 0.04, '2020-01-01', CURRENT_TIMESTAMP),
('CA', 'EQUITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.32, '2020-01-01', CURRENT_TIMESTAMP),
('CA', 'COMMODITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.18, '2020-01-01', CURRENT_TIMESTAMP),

-- ========== JAPAN (JFSA Implementation) ==========
-- Japanese parameters - conservative approach with higher factors for some asset classes
('JP', 'ALL', 'ALPHA_FACTOR', NULL, NULL, 1.4, '2020-01-01', CURRENT_TIMESTAMP),

-- Credit supervisory factors (slightly more conservative)
('JP', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'IG', 0.006, '2020-01-01', CURRENT_TIMESTAMP), -- Slightly higher
('JP', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'HY', 0.015, '2020-01-01', CURRENT_TIMESTAMP), -- More conservative
('JP', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'UNRATED', 0.020, '2020-01-01', CURRENT_TIMESTAMP), -- Higher penalty

-- Interest Rate supervisory factors
('JP', 'IR', 'SUPERVISORY_FACTOR', 'LESS_THAN_1Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('JP', 'IR', 'SUPERVISORY_FACTOR', 'Y1_TO_2Y', NULL, 0.006, '2020-01-01', CURRENT_TIMESTAMP), -- Slightly higher
('JP', 'IR', 'SUPERVISORY_FACTOR', 'Y2_TO_5Y', NULL, 0.008, '2020-01-01', CURRENT_TIMESTAMP), -- More conservative
('JP', 'IR', 'SUPERVISORY_FACTOR', 'GREATER_THAN_5Y', NULL, 0.018, '2020-01-01', CURRENT_TIMESTAMP), -- Higher long-term risk

-- Other asset classes (more conservative)
('JP', 'FX', 'SUPERVISORY_FACTOR', NULL, NULL, 0.045, '2020-01-01', CURRENT_TIMESTAMP), -- Slightly higher
('JP', 'EQUITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.35, '2020-01-01', CURRENT_TIMESTAMP), -- More conservative
('JP', 'COMMODITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.20, '2020-01-01', CURRENT_TIMESTAMP), -- Higher commodity risk

-- ========== AUSTRALIA (APRA Implementation) ==========
-- Australian parameters - generally aligned with Basel III
('AU', 'ALL', 'ALPHA_FACTOR', NULL, NULL, 1.4, '2020-01-01', CURRENT_TIMESTAMP),

-- Credit supervisory factors
('AU', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'IG', 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'HY', 0.013, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'UNRATED', 0.013, '2020-01-01', CURRENT_TIMESTAMP),

-- Interest Rate supervisory factors
('AU', 'IR', 'SUPERVISORY_FACTOR', 'LESS_THAN_1Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'IR', 'SUPERVISORY_FACTOR', 'Y1_TO_2Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'IR', 'SUPERVISORY_FACTOR', 'Y2_TO_5Y', NULL, 0.0075, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'IR', 'SUPERVISORY_FACTOR', 'GREATER_THAN_5Y', NULL, 0.015, '2020-01-01', CURRENT_TIMESTAMP),

-- Other asset classes
('AU', 'FX', 'SUPERVISORY_FACTOR', NULL, NULL, 0.04, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'EQUITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.32, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'COMMODITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.18, '2020-01-01', CURRENT_TIMESTAMP),

-- ========== SINGAPORE (MAS Implementation) ==========
-- Singapore parameters - more conservative alpha factor
('SG', 'ALL', 'ALPHA_FACTOR', NULL, NULL, 1.5, '2020-01-01', CURRENT_TIMESTAMP), -- Higher alpha factor

-- Credit supervisory factors (slightly more conservative)
('SG', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'IG', 0.0055, '2020-01-01', CURRENT_TIMESTAMP), -- Slightly higher
('SG', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'HY', 0.014, '2020-01-01', CURRENT_TIMESTAMP), -- More conservative
('SG', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'UNRATED', 0.016, '2020-01-01', CURRENT_TIMESTAMP), -- Higher penalty

-- Interest Rate supervisory factors
('SG', 'IR', 'SUPERVISORY_FACTOR', 'LESS_THAN_1Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('SG', 'IR', 'SUPERVISORY_FACTOR', 'Y1_TO_2Y', NULL, 0.0055, '2020-01-01', CURRENT_TIMESTAMP),
('SG', 'IR', 'SUPERVISORY_FACTOR', 'Y2_TO_5Y', NULL, 0.008, '2020-01-01', CURRENT_TIMESTAMP),
('SG', 'IR', 'SUPERVISORY_FACTOR', 'GREATER_THAN_5Y', NULL, 0.016, '2020-01-01', CURRENT_TIMESTAMP),

-- Other asset classes (more conservative)
('SG', 'FX', 'SUPERVISORY_FACTOR', NULL, NULL, 0.042, '2020-01-01', CURRENT_TIMESTAMP),
('SG', 'EQUITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.34, '2020-01-01', CURRENT_TIMESTAMP),
('SG', 'COMMODITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.19, '2020-01-01', CURRENT_TIMESTAMP),

-- ========== HONG KONG (HKMA Implementation) ==========
-- Hong Kong parameters - generally aligned with Basel III but with some local adjustments
('HK', 'ALL', 'ALPHA_FACTOR', NULL, NULL, 1.4, '2020-01-01', CURRENT_TIMESTAMP),

-- Credit supervisory factors
('HK', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'IG', 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'HY', 0.013, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'CREDIT', 'SUPERVISORY_FACTOR', NULL, 'UNRATED', 0.014, '2020-01-01', CURRENT_TIMESTAMP), -- Slightly higher for unrated

-- Interest Rate supervisory factors
('HK', 'IR', 'SUPERVISORY_FACTOR', 'LESS_THAN_1Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'IR', 'SUPERVISORY_FACTOR', 'Y1_TO_2Y', NULL, 0.005, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'IR', 'SUPERVISORY_FACTOR', 'Y2_TO_5Y', NULL, 0.0075, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'IR', 'SUPERVISORY_FACTOR', 'GREATER_THAN_5Y', NULL, 0.015, '2020-01-01', CURRENT_TIMESTAMP),

-- Other asset classes
('HK', 'FX', 'SUPERVISORY_FACTOR', NULL, NULL, 0.04, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'EQUITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.32, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'COMMODITY', 'SUPERVISORY_FACTOR', NULL, NULL, 0.18, '2020-01-01', CURRENT_TIMESTAMP);

-- Add some correlation parameters for advanced calculations
INSERT INTO sa_ccr_supervisory_parameters (jurisdiction, asset_class, parameter_type, parameter_value, effective_date, created_at) VALUES

-- Cross-asset correlations (generally 0 for different asset classes)
('CA', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0, '2020-01-01', CURRENT_TIMESTAMP),
('JP', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0, '2020-01-01', CURRENT_TIMESTAMP),
('SG', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'ALL', 'CORRELATION_CROSS_ASSET', 0.0, '2020-01-01', CURRENT_TIMESTAMP),

-- Credit correlations
('CA', 'CREDIT', 'CORRELATION_SAME_BUCKET', 0.5, '2020-01-01', CURRENT_TIMESTAMP),
('JP', 'CREDIT', 'CORRELATION_SAME_BUCKET', 0.45, '2020-01-01', CURRENT_TIMESTAMP), -- Slightly lower in Japan
('AU', 'CREDIT', 'CORRELATION_SAME_BUCKET', 0.5, '2020-01-01', CURRENT_TIMESTAMP),
('SG', 'CREDIT', 'CORRELATION_SAME_BUCKET', 0.48, '2020-01-01', CURRENT_TIMESTAMP), -- More conservative
('HK', 'CREDIT', 'CORRELATION_SAME_BUCKET', 0.5, '2020-01-01', CURRENT_TIMESTAMP),

-- Interest rate correlations
('CA', 'IR', 'CORRELATION_SAME_BUCKET', 0.999, '2020-01-01', CURRENT_TIMESTAMP),
('JP', 'IR', 'CORRELATION_SAME_BUCKET', 0.999, '2020-01-01', CURRENT_TIMESTAMP),
('AU', 'IR', 'CORRELATION_SAME_BUCKET', 0.999, '2020-01-01', CURRENT_TIMESTAMP),
('SG', 'IR', 'CORRELATION_SAME_BUCKET', 0.999, '2020-01-01', CURRENT_TIMESTAMP),
('HK', 'IR', 'CORRELATION_SAME_BUCKET', 0.999, '2020-01-01', CURRENT_TIMESTAMP);