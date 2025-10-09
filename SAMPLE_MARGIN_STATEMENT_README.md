# Sample Margin Statement CSV

This file contains sample data for testing the Epic 8 Story 2 margin statement ingestion feature.

## File Details
- **Filename**: `sample_margin_statement.csv`
- **Format**: CSV (Comma-separated values)
- **CCP**: LCH (London Clearing House)
- **Statement Date**: 2025-10-08
- **Currency**: USD, EUR, GBP, JPY

## CSV Format
The CSV follows the LCH format expected by the system:
```
Account,Date,Position Type,Amount,Currency,Portfolio,Product Class
```

## Field Descriptions
- **Account**: Account number (e.g., ACC001, ACC002)
- **Date**: Statement effective date (YYYY-MM-DD format)
- **Position Type**: Type of margin position
  - `VM` or `VARIATION_MARGIN`: Daily variation margin
  - `IM` or `INITIAL_MARGIN`: Initial margin requirement
  - `EXCESS` or `EXCESS_COLLATERAL`: Excess collateral posted
- **Amount**: Margin amount (positive or negative decimal)
- **Currency**: 3-letter ISO currency code (USD, EUR, GBP, JPY)
- **Portfolio**: Portfolio identifier (optional)
- **Product Class**: Product classification (CDS, EQUITY, FX, RATES)

## Sample Data Included
The file contains 12 margin positions across 5 different accounts:
- **ACC001**: CDS positions in USD (VM: $1.25M, IM: $2.5M, Excess: $750K)
- **ACC002**: CDS positions in EUR (VM: -€850K, IM: €1.8M)
- **ACC003**: Equity positions in GBP (VM: £450K, IM: £900K)
- **ACC004**: FX positions in USD (VM: $2.1M, IM: $3.2M, Excess: $150K)
- **ACC005**: Rates positions in JPY (VM: -¥320K, IM: ¥1.2M)

## How to Test
1. Navigate to the margin statements upload page in the frontend
2. Use the following upload parameters:
   - **Statement ID**: `LCH-DAILY-20251008-001`
   - **CCP Name**: `LCH`
   - **Member Firm**: `TEST_FIRM_001`
   - **Account Number**: `MASTER_001`
   - **Statement Date**: `2025-10-08`
   - **Currency**: `USD`
   - **Format**: `CSV`
3. Upload the `sample_margin_statement.csv` file
4. Verify the statement processes successfully and positions are created

## Expected Results
- Statement should be uploaded with status `PENDING`
- Parser should process the CSV and create 12 margin positions
- Status should change to `PROCESSED`
- All positions should be retrievable via the API endpoints