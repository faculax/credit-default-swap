// Reference data for CDS trade form dropdowns

export const REFERENCE_ENTITIES = [
  { code: 'AAPL', name: 'Apple Inc.' },
  { code: 'MSFT', name: 'Microsoft Corporation' },
  { code: 'JPM', name: 'JPMorgan Chase & Co.' },
  { code: 'BAC', name: 'Bank of America Corp.' },
  { code: 'WFC', name: 'Wells Fargo & Company' },
  { code: 'GS', name: 'Goldman Sachs Group Inc.' },
  { code: 'MS', name: 'Morgan Stanley' },
  { code: 'C', name: 'Citigroup Inc.' },
  { code: 'AMZN', name: 'Amazon.com Inc.' },
  { code: 'GOOGL', name: 'Alphabet Inc.' },
  { code: 'TSLA', name: 'Tesla Inc.' },
  { code: 'NFLX', name: 'Netflix Inc.' },
  { code: 'META', name: 'Meta Platforms Inc.' },
  { code: 'NVDA', name: 'NVIDIA Corporation' },
  { code: 'AMD', name: 'Advanced Micro Devices Inc.' }
];

export const COUNTERPARTIES = [
  { code: 'BARCLAYS', name: 'Barclays Capital Inc.' },
  { code: 'DEUTSCHE', name: 'Deutsche Bank AG' },
  { code: 'CITI', name: 'Citibank N.A.' },
  { code: 'JPMORGAN', name: 'J.P. Morgan Securities LLC' },
  { code: 'GOLDMAN', name: 'Goldman Sachs & Co. LLC' },
  { code: 'MORGAN_STANLEY', name: 'Morgan Stanley & Co. LLC' },
  { code: 'UBS', name: 'UBS Securities LLC' },
  { code: 'CREDIT_SUISSE', name: 'Credit Suisse Securities (USA) LLC' },
  { code: 'BOA', name: 'BofA Securities Inc.' },
  { code: 'WELLS_FARGO', name: 'Wells Fargo Securities LLC' }
];

export const CURRENCIES = [
  { code: 'USD', name: 'US Dollar' },
  { code: 'EUR', name: 'Euro' },
  { code: 'GBP', name: 'British Pound' },
  { code: 'JPY', name: 'Japanese Yen' },
  { code: 'CHF', name: 'Swiss Franc' },
  { code: 'CAD', name: 'Canadian Dollar' },
  { code: 'AUD', name: 'Australian Dollar' }
];

export const PREMIUM_FREQUENCIES = [
  { value: 'QUARTERLY', label: 'Quarterly' },
  { value: 'SEMI_ANNUAL', label: 'Semi-Annual' },
  { value: 'ANNUAL', label: 'Annual' }
];

export const DAY_COUNT_CONVENTIONS = [
  { value: 'ACT_360', label: 'Actual/360' },
  { value: 'ACT_365', label: 'Actual/365' },
  { value: '30_360', label: '30/360' },
  { value: 'ACT_ACT', label: 'Actual/Actual' }
];

export const RESTRUCTURING_CLAUSES = [
  { value: 'NO_RESTRUCTURING', label: 'No Restructuring (No R)' },
  { value: 'MODIFIED_RESTRUCTURING', label: 'Modified Restructuring (Mod R)' },
  { value: 'MODIFIED_MODIFIED_RESTRUCTURING', label: 'Modified Modified Restructuring (Mod Mod R)' },
  { value: 'FULL_RESTRUCTURING', label: 'Full Restructuring (Full R)' }
];

export const PAYMENT_CALENDARS = [
  { value: 'NYC', label: 'New York' },
  { value: 'LON', label: 'London' },
  { value: 'TARGET', label: 'TARGET (Europe)' },
  { value: 'TOK', label: 'Tokyo' },
  { value: 'SYD', label: 'Sydney' }
];

export const TRADE_STATUSES = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'SETTLED', label: 'Settled' },
  { value: 'CANCELLED', label: 'Cancelled' }
];

export interface CDSTrade {
  referenceEntity: string;
  notionalAmount: number;
  spread: number;
  maturityDate: string;
  effectiveDate: string;
  counterparty: string;
  tradeDate: string;
  currency: string;
  premiumFrequency: string;
  dayCountConvention: string;
  buySellProtection: 'BUY' | 'SELL';
  restructuringClause?: string;
  paymentCalendar: string;
  accrualStartDate: string;
  tradeStatus: string;
}