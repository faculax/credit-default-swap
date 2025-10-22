import React, { useState, useEffect } from 'react';
import { Bond, bondService } from '../../services/bondService';
import { REFERENCE_ENTITIES, SECTORS } from '../../data/referenceData';

interface BondCreationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (bond: Bond) => void;
}

interface FormErrors {
  [key: string]: string;
}

const SENIORITY_OPTIONS = [
  { value: 'SR_UNSEC', label: 'Senior Unsecured' },
  { value: 'SR_SEC', label: 'Senior Secured' },
  { value: 'SUBORD', label: 'Subordinated' },
];

const COUPON_FREQUENCY_OPTIONS = [
  { value: 'ANNUAL', label: 'Annual' },
  { value: 'SEMI_ANNUAL', label: 'Semi-Annual' },
  { value: 'QUARTERLY', label: 'Quarterly' },
];

const DAY_COUNT_OPTIONS = [
  { value: 'ACT_ACT', label: 'ACT/ACT' },
  { value: 'THIRTY_360', label: '30/360' },
];

const CURRENCY_OPTIONS = ['USD', 'EUR', 'GBP', 'JPY'];

const BondCreationModal: React.FC<BondCreationModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [formData, setFormData] = useState<Partial<Bond>>({
    currency: 'USD',
    couponFrequency: 'SEMI_ANNUAL',
    dayCount: 'ACT_ACT',
    seniority: 'SR_UNSEC',
    settlementDays: 2,
    faceValue: 100,
    priceConvention: 'CLEAN',
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [userModifiedSector, setUserModifiedSector] = useState(false);

  // Auto-populate sector when issuer changes
  useEffect(() => {
    if (!userModifiedSector && formData.issuer) {
      const entity = REFERENCE_ENTITIES.find((e) => e.code === formData.issuer);
      if (entity?.sector) {
        setFormData((prev) => ({
          ...prev,
          sector: entity.sector,
        }));
      }
    }
  }, [formData.issuer, userModifiedSector]);

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    if (!formData.issuer?.trim()) {
      newErrors.issuer = 'Issuer is required';
    }

    if (!formData.notional || formData.notional <= 0) {
      newErrors.notional = 'Notional must be positive';
    }

    if (!formData.couponRate || formData.couponRate < 0) {
      newErrors.couponRate = 'Coupon rate must be >= 0';
    }

    if (formData.couponRate && formData.couponRate > 1) {
      newErrors.couponRate = 'Coupon rate exceeds 100% - please verify';
    }

    if (!formData.issueDate) {
      newErrors.issueDate = 'Issue date is required';
    }

    if (!formData.maturityDate) {
      newErrors.maturityDate = 'Maturity date is required';
    }

    if (
      formData.issueDate &&
      formData.maturityDate &&
      new Date(formData.issueDate) >= new Date(formData.maturityDate)
    ) {
      newErrors.maturityDate = 'Maturity date must be after issue date';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (field: keyof Bond, value: any) => {
    // Track if user manually edits the sector
    if (field === 'sector') {
      setUserModifiedSector(true);
    }

    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));

    if (errors[field]) {
      setErrors((prev) => ({
        ...prev,
        [field]: '',
      }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      const bond = await bondService.createBond(formData as Bond);
      onSuccess(bond);
      resetForm();
      onClose();
    } catch (error: any) {
      setErrors({ submit: error.message || 'Failed to create bond' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setFormData({
      currency: 'USD',
      couponFrequency: 'SEMI_ANNUAL',
      dayCount: 'ACT_ACT',
      seniority: 'SR_UNSEC',
      settlementDays: 2,
      faceValue: 100,
      priceConvention: 'CLEAN',
    });
    setErrors({});
    setUserModifiedSector(false);
  };

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-fd-darker rounded-lg shadow-xl border border-fd-border max-w-4xl w-full max-h-[90vh] overflow-y-auto m-4"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="sticky top-0 bg-fd-darker border-b border-fd-border px-6 py-4 flex justify-between items-center z-10">
          <h2 className="text-xl font-semibold text-fd-text">Create New Bond</h2>
          <button
            onClick={onClose}
            className="text-fd-text-muted hover:text-fd-text transition-colors"
            aria-label="Close modal"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Issuer & Credit Info Section */}
            <div className="md:col-span-2">
              <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                Issuer & Credit Information
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Issuer <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.issuer || ''}
                onChange={(e) => handleInputChange('issuer', e.target.value)}
                className={`w-full px-3 py-2 bg-fd-dark border ${errors.issuer ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent`}
              >
                <option value="">Select Issuer</option>
                {REFERENCE_ENTITIES.map((entity) => (
                  <option key={entity.code} value={entity.code}>
                    {entity.code} - {entity.name}
                  </option>
                ))}
              </select>
              {errors.issuer && <p className="mt-1 text-sm text-red-400">{errors.issuer}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Seniority <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.seniority}
                onChange={(e) => handleInputChange('seniority', e.target.value)}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent"
              >
                {SENIORITY_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">Sector</label>
              <select
                value={formData.sector || ''}
                onChange={(e) => handleInputChange('sector', e.target.value)}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent"
              >
                <option value="">Auto-suggested from issuer</option>
                {SECTORS.map((sector) => (
                  <option key={sector.code} value={sector.code}>
                    {sector.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">ISIN</label>
              <input
                type="text"
                value={formData.isin || ''}
                onChange={(e) => handleInputChange('isin', e.target.value)}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent placeholder-fd-text-muted"
                placeholder="Optional"
                maxLength={12}
              />
            </div>

            {/* Economics Section */}
            <div className="md:col-span-2 mt-4">
              <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                Bond Economics
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Currency <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.currency}
                onChange={(e) => handleInputChange('currency', e.target.value)}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent"
              >
                {CURRENCY_OPTIONS.map((curr) => (
                  <option key={curr} value={curr}>
                    {curr}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Notional <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                value={formData.notional || ''}
                onChange={(e) => handleInputChange('notional', parseFloat(e.target.value))}
                className={`w-full px-3 py-2 bg-fd-dark border ${errors.notional ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent placeholder-fd-text-muted`}
              />
              {errors.notional && <p className="mt-1 text-sm text-red-400">{errors.notional}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Coupon Rate (decimal) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                step="0.000001"
                value={formData.couponRate || ''}
                onChange={(e) => handleInputChange('couponRate', parseFloat(e.target.value))}
                className={`w-full px-3 py-2 bg-fd-dark border ${errors.couponRate ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent placeholder-fd-text-muted`}
                placeholder="e.g., 0.045 for 4.5%"
              />
              {errors.couponRate && (
                <p className="mt-1 text-sm text-red-400">{errors.couponRate}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Coupon Frequency
              </label>
              <select
                value={formData.couponFrequency}
                onChange={(e) => handleInputChange('couponFrequency', e.target.value)}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent"
              >
                {COUPON_FREQUENCY_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Day Count Convention
              </label>
              <select
                value={formData.dayCount}
                onChange={(e) => handleInputChange('dayCount', e.target.value)}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent"
              >
                {DAY_COUNT_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Dates Section */}
            <div className="md:col-span-2 mt-4">
              <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                Dates
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Issue Date <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                value={formData.issueDate || ''}
                onChange={(e) => handleInputChange('issueDate', e.target.value)}
                className={`w-full px-3 py-2 bg-fd-dark border ${errors.issueDate ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent`}
              />
              {errors.issueDate && <p className="mt-1 text-sm text-red-400">{errors.issueDate}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Maturity Date <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                value={formData.maturityDate || ''}
                onChange={(e) => handleInputChange('maturityDate', e.target.value)}
                className={`w-full px-3 py-2 bg-fd-dark border ${errors.maturityDate ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent`}
              />
              {errors.maturityDate && (
                <p className="mt-1 text-sm text-red-400">{errors.maturityDate}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">Settlement Days</label>
              <input
                type="number"
                value={formData.settlementDays || ''}
                onChange={(e) => handleInputChange('settlementDays', parseInt(e.target.value))}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent placeholder-fd-text-muted"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">Face Value</label>
              <input
                type="number"
                step="0.01"
                value={formData.faceValue || ''}
                onChange={(e) => handleInputChange('faceValue', parseFloat(e.target.value))}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent placeholder-fd-text-muted"
              />
            </div>
          </div>

          {/* Error Message */}
          {errors.submit && (
            <div className="mt-4 p-3 bg-red-900/20 border border-red-500/30 rounded-md">
              <p className="text-sm text-red-400">{errors.submit}</p>
            </div>
          )}

          {/* Action Buttons */}
          <div className="mt-6 flex justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-fd-text bg-fd-dark border border-fd-border rounded-md hover:bg-fd-darker focus:outline-none focus:ring-2 focus:ring-fd-green"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 text-sm font-medium text-fd-dark bg-fd-green border border-transparent rounded-md hover:bg-fd-green-hover focus:outline-none focus:ring-2 focus:ring-fd-green disabled:bg-fd-green/50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? 'Creating...' : 'Create Bond'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BondCreationModal;
