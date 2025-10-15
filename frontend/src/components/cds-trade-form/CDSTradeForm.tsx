import React, { useState } from 'react';
import {
  REFERENCE_ENTITIES,
  COUNTERPARTIES,
  CURRENCIES,
  PREMIUM_FREQUENCIES,
  DAY_COUNT_CONVENTIONS,
  RESTRUCTURING_CLAUSES,
  PAYMENT_CALENDARS,
  TRADE_STATUSES,
  CDSTrade
} from '../../data/referenceData';

interface FormErrors {
  [key: string]: string;
}

interface CDSTradeFormProps {
  onSubmit: (trade: CDSTrade) => void;
}

const CDSTradeForm: React.FC<CDSTradeFormProps> = ({ onSubmit }) => {
  const [formData, setFormData] = useState<Partial<CDSTrade>>({
    tradeDate: new Date().toISOString().split('T')[0],
    currency: 'USD',
    premiumFrequency: 'QUARTERLY',
    dayCountConvention: 'ACT_360',
    buySellProtection: 'BUY',
    paymentCalendar: 'NYC',
    tradeStatus: 'PENDING',
    recoveryRate: 40
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Required field validations
    if (!formData.referenceEntity) {
      newErrors.referenceEntity = 'Reference Entity is required';
    }

    if (!formData.notionalAmount || formData.notionalAmount <= 0) {
      newErrors.notionalAmount = 'Notional Amount must be greater than 0';
    }

    if (!formData.spread || formData.spread < 0) {
      newErrors.spread = 'Spread must be 0 or greater';
    }

    if (!formData.recoveryRate || formData.recoveryRate < 0 || formData.recoveryRate > 100) {
      newErrors.recoveryRate = 'Recovery Rate must be between 0 and 100';
    }

    if (!formData.maturityDate) {
      newErrors.maturityDate = 'Maturity Date is required';
    }

    if (!formData.effectiveDate) {
      newErrors.effectiveDate = 'Effective Date is required';
    }

    if (!formData.counterparty) {
      newErrors.counterparty = 'Counterparty is required';
    }

    if (!formData.tradeDate) {
      newErrors.tradeDate = 'Trade Date is required';
    }

    if (!formData.accrualStartDate) {
      newErrors.accrualStartDate = 'Accrual Start Date is required';
    }

    // Date validations
    if (formData.effectiveDate && formData.maturityDate && 
        new Date(formData.effectiveDate) >= new Date(formData.maturityDate)) {
      newErrors.maturityDate = 'Maturity Date must be after Effective Date';
    }

    if (formData.tradeDate && formData.effectiveDate && 
        new Date(formData.tradeDate) > new Date(formData.effectiveDate)) {
      newErrors.effectiveDate = 'Effective Date must be on or after Trade Date';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (field: keyof CDSTrade, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));

    // Clear error for this field when user starts typing
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  // Format number with commas for display
  const formatNumberWithCommas = (num: number | undefined): string => {
    if (!num && num !== 0) return '';
    return num.toLocaleString('en-US');
  };

  // Parse formatted string back to number
  const parseFormattedNumber = (str: string): number => {
    return parseFloat(str.replace(/,/g, '')) || 0;
  };

  // Handle notional amount input with formatting
  const handleNotionalChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value.replace(/,/g, ''); // Remove commas
    const numValue = parseFloat(rawValue);
    
    if (!isNaN(numValue) || rawValue === '') {
      handleInputChange('notionalAmount', rawValue === '' ? undefined : numValue);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    // Defensive normalization
    if (formData.tradeStatus === 'SETTLED' || formData.tradeStatus === 'CONFIRMED') {
      formData.tradeStatus = 'ACTIVE';
    }

    // Simulate API call delay
    setTimeout(() => {
      onSubmit(formData as CDSTrade);
      setIsSubmitting(false);
      
      // Reset form
      setFormData({
        tradeDate: new Date().toISOString().split('T')[0],
        currency: 'USD',
        premiumFrequency: 'QUARTERLY',
        dayCountConvention: 'ACT_360',
        buySellProtection: 'BUY',
        paymentCalendar: 'NYC',
        tradeStatus: 'PENDING',
        recoveryRate: 40
      });
    }, 1000);
  };

  const inputClassName = (fieldName: string) => {
    const baseClass = "w-full px-3 py-2 bg-fd-input border rounded-md text-fd-text placeholder-fd-text-muted focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-fd-green";
    const errorClass = "border-red-500";
    const normalClass = "border-fd-border";
    
    return `${baseClass} ${errors[fieldName] ? errorClass : normalClass}`;
  };

  const selectClassName = (fieldName: string) => {
    const baseClass = "w-full px-3 py-2 bg-fd-input border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-fd-green";
    const errorClass = "border-red-500";
    const normalClass = "border-fd-border";
    
    return `${baseClass} ${errors[fieldName] ? errorClass : normalClass}`;
  };

  const generateRandomData = () => {
    // Helper function to get random item from array
    const getRandomItem = (array: any[]) => array[Math.floor(Math.random() * array.length)];
    
    // Helper function to get random date within a range (days offset from today)
    const getRandomDate = (startDays: number, endDays: number) => {
      const start = new Date();
      start.setDate(start.getDate() + startDays);
      const end = new Date();
      end.setDate(end.getDate() + endDays);
      
      const randomTime = start.getTime() + Math.random() * (end.getTime() - start.getTime());
      return new Date(randomTime).toISOString().split('T')[0];
    };
    
    // Generate trades that started in the PAST for demo purposes
    // This allows demonstrating coupon payments immediately
    
    // Trade date: 24-48 months ago (2-4 years in the past) for more coupons
    const tradeDate = getRandomDate(-365 * 4, -365 * 2);
    
    // Effective date: 1-7 days after trade date (still in the past)
    const tradeDateObj = new Date(tradeDate);
    const effectiveDaysOffset = Math.floor(Math.random() * 7) + 1;
    const effectiveDateObj = new Date(tradeDateObj);
    effectiveDateObj.setDate(effectiveDateObj.getDate() + effectiveDaysOffset);
    const effectiveDate = effectiveDateObj.toISOString().split('T')[0];
    
    // Maturity date: 2-5 years from TODAY (in the future)
    const maturityDate = getRandomDate(365 * 2, 365 * 5);
    
    // Accrual start date: same as effective date
    const accrualStartDate = effectiveDate;
    
    // Generate round notional amounts (5M, 10M, 20M, 50M, 100M, 200M, 500M)
    const roundNotionals = [5000000, 10000000, 20000000, 50000000, 100000000, 200000000, 500000000];
    const notionalAmount = getRandomItem(roundNotionals);
    
    const randomData: Partial<CDSTrade> = {
      referenceEntity: getRandomItem(REFERENCE_ENTITIES).code,
      counterparty: getRandomItem(COUNTERPARTIES).code,
      currency: getRandomItem(CURRENCIES).code,
      notionalAmount,
      spread: Math.floor(Math.random() * 500) + 50, // 50 to 550 bps
      buySellProtection: Math.random() > 0.5 ? 'BUY' : 'SELL',
      tradeDate,
      effectiveDate,
      maturityDate,
      accrualStartDate,
      premiumFrequency: getRandomItem(PREMIUM_FREQUENCIES).value,
      dayCountConvention: getRandomItem(DAY_COUNT_CONVENTIONS).value,
      restructuringClause: Math.random() > 0.3 ? getRandomItem(RESTRUCTURING_CLAUSES).value : '', // 70% chance of having a clause
      paymentCalendar: getRandomItem(PAYMENT_CALENDARS).value,
      tradeStatus: 'ACTIVE', // Always generate ACTIVE trades for demo purposes
      recoveryRate: 40  // Default recovery rate
    };
    setFormData(randomData);
    setErrors({}); // Clear any existing errors
  };

  return (
    <div className="max-w-4xl mx-auto bg-fd-darker rounded-lg shadow-fd border border-fd-border p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-fd-text">New CDS Trade Entry</h2>
        <button
          type="button"
          onClick={generateRandomData}
          className="px-4 py-2 bg-fd-teal text-fd-dark font-medium rounded hover:bg-fd-cyan transition-colors flex items-center space-x-2"
        >
          <span>🎲</span>
          <span>Fill Random Data</span>
        </button>
      </div>
      
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Row 1: Reference Entity, Counterparty, Currency */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-fd-text font-medium mb-2">
              Reference Entity <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.referenceEntity || ''}
              onChange={(e) => handleInputChange('referenceEntity', e.target.value)}
              className={selectClassName('referenceEntity')}
            >
              <option value="">Select Reference Entity</option>
              {REFERENCE_ENTITIES.map((entity) => (
                <option key={entity.code} value={entity.code}>
                  {entity.code} - {entity.name}
                </option>
              ))}
            </select>
            {errors.referenceEntity && (
              <p className="text-red-500 text-sm mt-1">{errors.referenceEntity}</p>
            )}
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Counterparty <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.counterparty || ''}
              onChange={(e) => handleInputChange('counterparty', e.target.value)}
              className={selectClassName('counterparty')}
            >
              <option value="">Select Counterparty</option>
              {COUNTERPARTIES.map((cp) => (
                <option key={cp.code} value={cp.code}>
                  {cp.name}
                </option>
              ))}
            </select>
            {errors.counterparty && (
              <p className="text-red-500 text-sm mt-1">{errors.counterparty}</p>
            )}
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Currency <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.currency || 'USD'}
              onChange={(e) => handleInputChange('currency', e.target.value)}
              className={selectClassName('currency')}
            >
              {CURRENCIES.map((curr) => (
                <option key={curr.code} value={curr.code}>
                  {curr.code} - {curr.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Row 2: Notional Amount, Spread, Buy/Sell Protection */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-fd-text font-medium mb-2">
              Notional Amount <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formatNumberWithCommas(formData.notionalAmount)}
              onChange={handleNotionalChange}
              className={inputClassName('notionalAmount')}
              placeholder="e.g., 10,000,000"
            />
            {errors.notionalAmount && (
              <p className="text-red-500 text-sm mt-1">{errors.notionalAmount}</p>
            )}
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Spread (bps) <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={formData.spread || ''}
              onChange={(e) => handleInputChange('spread', parseFloat(e.target.value))}
              className={inputClassName('spread')}
              placeholder="e.g., 100"
            />
            {errors.spread && (
              <p className="text-red-500 text-sm mt-1">{errors.spread}</p>
            )}
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Recovery Rate (%) <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              step="1"
              min="0"
              max="100"
              value={formData.recoveryRate || 40}
              onChange={(e) => handleInputChange('recoveryRate', parseFloat(e.target.value))}
              className={inputClassName('recoveryRate')}
              placeholder="e.g., 40"
            />
            {errors.recoveryRate && (
              <p className="text-red-500 text-sm mt-1">{errors.recoveryRate}</p>
            )}
          </div>
        </div>

        {/* Row 3: Buy/Sell Protection */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-fd-text font-medium mb-2">
              Buy/Sell Protection <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.buySellProtection || 'BUY'}
              onChange={(e) => handleInputChange('buySellProtection', e.target.value)}
              className={selectClassName('buySellProtection')}
            >
              <option value="BUY">Buy Protection</option>
              <option value="SELL">Sell Protection</option>
            </select>
          </div>
        </div>

        {/* Row 4: Trade Date, Effective Date, Maturity Date */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-fd-text font-medium mb-2">
              Trade Date <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              value={formData.tradeDate || ''}
              onChange={(e) => handleInputChange('tradeDate', e.target.value)}
              className={inputClassName('tradeDate')}
            />
            {errors.tradeDate && (
              <p className="text-red-500 text-sm mt-1">{errors.tradeDate}</p>
            )}
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Effective Date <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              value={formData.effectiveDate || ''}
              onChange={(e) => handleInputChange('effectiveDate', e.target.value)}
              className={inputClassName('effectiveDate')}
            />
            {errors.effectiveDate && (
              <p className="text-red-500 text-sm mt-1">{errors.effectiveDate}</p>
            )}
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Maturity Date <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              value={formData.maturityDate || ''}
              onChange={(e) => handleInputChange('maturityDate', e.target.value)}
              className={inputClassName('maturityDate')}
            />
            {errors.maturityDate && (
              <p className="text-red-500 text-sm mt-1">{errors.maturityDate}</p>
            )}
          </div>
        </div>

        {/* Row 4: Premium Frequency, Day Count Convention, Accrual Start Date */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-fd-text font-medium mb-2">
              Premium Frequency <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.premiumFrequency || 'QUARTERLY'}
              onChange={(e) => handleInputChange('premiumFrequency', e.target.value)}
              className={selectClassName('premiumFrequency')}
            >
              {PREMIUM_FREQUENCIES.map((freq) => (
                <option key={freq.value} value={freq.value}>
                  {freq.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Day Count Convention <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.dayCountConvention || 'ACT_360'}
              onChange={(e) => handleInputChange('dayCountConvention', e.target.value)}
              className={selectClassName('dayCountConvention')}
            >
              {DAY_COUNT_CONVENTIONS.map((dcc) => (
                <option key={dcc.value} value={dcc.value}>
                  {dcc.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Accrual Start Date <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              value={formData.accrualStartDate || ''}
              onChange={(e) => handleInputChange('accrualStartDate', e.target.value)}
              className={inputClassName('accrualStartDate')}
            />
            {errors.accrualStartDate && (
              <p className="text-red-500 text-sm mt-1">{errors.accrualStartDate}</p>
            )}
          </div>
        </div>

        {/* Row 5: Payment Calendar, Restructuring Clause, Trade Status */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-fd-text font-medium mb-2">
              Payment Calendar <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.paymentCalendar || 'NYC'}
              onChange={(e) => handleInputChange('paymentCalendar', e.target.value)}
              className={selectClassName('paymentCalendar')}
            >
              {PAYMENT_CALENDARS.map((cal) => (
                <option key={cal.value} value={cal.value}>
                  {cal.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Restructuring Clause
            </label>
            <select
              value={formData.restructuringClause || ''}
              onChange={(e) => handleInputChange('restructuringClause', e.target.value)}
              className={selectClassName('restructuringClause')}
            >
              <option value="">None</option>
              {RESTRUCTURING_CLAUSES.map((clause) => (
                <option key={clause.value} value={clause.value}>
                  {clause.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-fd-text font-medium mb-2">
              Trade Status <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.tradeStatus || 'PENDING'}
              onChange={(e) => handleInputChange('tradeStatus', e.target.value)}
              className={selectClassName('tradeStatus')}
            >
              {TRADE_STATUSES.map((status) => (
                <option key={status.value} value={status.value}>
                  {status.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Submit Button */}
        <div className="flex justify-end space-x-4 pt-6 border-t border-fd-border">
          <button
            type="button"
            onClick={() => {
              setFormData({
                tradeDate: new Date().toISOString().split('T')[0],
                currency: 'USD',
                premiumFrequency: 'QUARTERLY',
                dayCountConvention: 'ACT_360',
                buySellProtection: 'BUY',
                paymentCalendar: 'NYC',
                tradeStatus: 'PENDING',
                recoveryRate: 40
              });
              setErrors({});
            }}
            className="px-6 py-2 bg-transparent border border-fd-border text-fd-text rounded hover:bg-fd-dark transition-colors"
          >
            Clear Form
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-6 py-2 bg-fd-green text-fd-dark font-medium rounded hover:bg-fd-green-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting ? 'Booking Trade...' : 'Book Trade'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CDSTradeForm;