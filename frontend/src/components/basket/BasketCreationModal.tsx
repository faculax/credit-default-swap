import React, { useState } from 'react';
import { Basket, BasketType, BasketConstituent } from '../../types/basket';
import { basketService } from '../../services/basketService';
import { REFERENCE_ENTITIES } from '../../data/referenceData';

interface BasketCreationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (basket: Basket) => void;
}

interface FormErrors {
  [key: string]: string;
}

const BASKET_TYPE_OPTIONS = [
  { value: 'FIRST_TO_DEFAULT', label: 'First-to-Default (FTD)' },
  { value: 'NTH_TO_DEFAULT', label: 'N-th-to-Default' },
  { value: 'TRANCHETTE', label: 'Tranchette (Loss Slice)' }
];

const PREMIUM_FREQUENCY_OPTIONS = [
  { value: 'QUARTERLY', label: 'Quarterly' },
  { value: 'SEMI_ANNUAL', label: 'Semi-Annual' },
  { value: 'ANNUAL', label: 'Annual' }
];

const DAY_COUNT_OPTIONS = [
  { value: 'ACT_360', label: 'ACT/360' },
  { value: 'ACT_365', label: 'ACT/365' },
  { value: 'THIRTY_360', label: '30/360' }
];

const CURRENCY_OPTIONS = ['USD', 'EUR', 'GBP', 'JPY'];

const SENIORITY_OPTIONS = [
  { value: 'SR_UNSEC', label: 'Senior Unsecured' },
  { value: 'SR_SEC', label: 'Senior Secured' },
  { value: 'SUBORD', label: 'Subordinated' }
];

const BasketCreationModal: React.FC<BasketCreationModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [formData, setFormData] = useState<Partial<Basket>>({
    type: 'FIRST_TO_DEFAULT',
    currency: 'USD',
    premiumFrequency: 'QUARTERLY',
    dayCount: 'ACT_360',
    constituents: []
  });

  const [constituents, setConstituents] = useState<Partial<BasketConstituent>[]>([]);
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Calculate normalized weights dynamically
  const calculateNormalizedWeights = () => {
    const totalWeight = constituents.reduce((sum, c) => sum + (c.weight || 0), 0);
    
    if (totalWeight === 0) {
      // Equal weights if none provided
      return constituents.map(() => 1 / constituents.length);
    }
    
    // Normalize to sum to 1
    return constituents.map(c => (c.weight || 0) / totalWeight);
  };

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    if (!formData.name?.trim()) {
      newErrors.name = 'Basket name is required';
    }

    if (!formData.type) {
      newErrors.type = 'Basket type is required';
    }

    if (formData.type === 'NTH_TO_DEFAULT' && (!formData.nth || formData.nth < 1)) {
      newErrors.nth = 'N-th value must be >= 1';
    }

    if (formData.type === 'NTH_TO_DEFAULT' && formData.nth && formData.nth > constituents.length) {
      newErrors.nth = `N-th value cannot exceed number of constituents (${constituents.length})`;
    }

    if (formData.type === 'TRANCHETTE') {
      if (formData.attachmentPoint === undefined || formData.attachmentPoint < 0 || formData.attachmentPoint > 1) {
        newErrors.attachmentPoint = 'Attachment point must be between 0 and 1';
      }
      if (formData.detachmentPoint === undefined || formData.detachmentPoint < 0 || formData.detachmentPoint > 1) {
        newErrors.detachmentPoint = 'Detachment point must be between 0 and 1';
      }
      if (formData.attachmentPoint !== undefined && formData.detachmentPoint !== undefined &&
          formData.attachmentPoint >= formData.detachmentPoint) {
        newErrors.detachmentPoint = 'Detachment point must be greater than attachment point';
      }
    }

    if (!formData.notional || formData.notional <= 0) {
      newErrors.notional = 'Notional must be positive';
    }

    if (!formData.maturityDate) {
      newErrors.maturityDate = 'Maturity date is required';
    }

    if (constituents.length === 0) {
      newErrors.constituents = 'At least one constituent is required';
    }

    // Check for duplicate issuers
    const issuerSet = new Set<string>();
    constituents.forEach((c, idx) => {
      if (c.issuer) {
        if (issuerSet.has(c.issuer)) {
          newErrors[`constituent_${idx}_issuer`] = 'Duplicate issuer';
        }
        issuerSet.add(c.issuer);
      } else {
        newErrors[`constituent_${idx}_issuer`] = 'Issuer is required';
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (field: keyof Basket, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));

    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  const handleConstituentChange = (index: number, field: keyof BasketConstituent, value: any) => {
    const updated = [...constituents];
    updated[index] = { ...updated[index], [field]: value };
    setConstituents(updated);

    const errorKey = `constituent_${index}_${field}`;
    if (errors[errorKey]) {
      setErrors(prev => ({
        ...prev,
        [errorKey]: ''
      }));
    }
  };

  const addConstituent = () => {
    setConstituents([...constituents, { seniority: 'SR_UNSEC' }]);
  };

  const removeConstituent = (index: number) => {
    setConstituents(constituents.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      const normalizedWeights = calculateNormalizedWeights();
      
      const basketRequest = {
        ...formData,
        constituents: constituents.map((c, idx) => ({
          issuer: c.issuer!,
          weight: normalizedWeights[idx],
          recoveryOverride: c.recoveryOverride,
          seniority: c.seniority || 'SR_UNSEC',
          sector: c.sector
        }))
      };

      const basket = await basketService.createBasket(basketRequest as any);
      onSuccess(basket);
      resetForm();
      onClose();
    } catch (error: any) {
      setErrors({ submit: error.message || 'Failed to create basket' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setFormData({
      type: 'FIRST_TO_DEFAULT',
      currency: 'USD',
      premiumFrequency: 'QUARTERLY',
      dayCount: 'ACT_360',
      constituents: []
    });
    setConstituents([]);
    setErrors({});
  };

  if (!isOpen) return null;

  const normalizedWeights = calculateNormalizedWeights();

  return (
    <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-fd-darker rounded-lg shadow-xl border border-fd-border max-w-6xl w-full max-h-[90vh] overflow-y-auto m-4" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="sticky top-0 bg-fd-darker border-b border-fd-border px-6 py-4 flex justify-between items-center z-10">
          <h2 className="text-xl font-semibold text-fd-text">
            Create New Basket Derivative
          </h2>
          <button
            onClick={onClose}
            className="text-fd-text-muted hover:text-fd-text transition-colors"
            aria-label="Close modal"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            {/* Basic Information Section */}
            <div className="md:col-span-2">
              <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                Basic Information
              </h3>
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Basket Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.name || ''}
                onChange={(e) => handleInputChange('name', e.target.value)}
                className={`w-full px-3 py-2 bg-fd-dark border ${errors.name ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent`}
                placeholder="e.g., US_IG_TECH_FTD_5Y"
              />
              {errors.name && <p className="mt-1 text-sm text-red-400">{errors.name}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Basket Type <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.type}
                onChange={(e) => handleInputChange('type', e.target.value as BasketType)}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent"
              >
                {BASKET_TYPE_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
              {errors.type && <p className="mt-1 text-sm text-red-400">{errors.type}</p>}
            </div>

            {/* Conditional Fields for N-th-to-Default */}
            {formData.type === 'NTH_TO_DEFAULT' && (
              <div>
                <label className="block text-sm font-medium text-fd-text mb-1">
                  N-th Value <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  min="1"
                  value={formData.nth || ''}
                  onChange={(e) => handleInputChange('nth', parseInt(e.target.value))}
                  className={`w-full px-3 py-2 bg-fd-dark border ${errors.nth ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent`}
                  placeholder="e.g., 2 for 2nd-to-default"
                />
                {errors.nth && <p className="mt-1 text-sm text-red-400">{errors.nth}</p>}
              </div>
            )}

            {/* Conditional Fields for Tranchette */}
            {formData.type === 'TRANCHETTE' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-fd-text mb-1">
                    Attachment Point (0-1) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    max="1"
                    value={formData.attachmentPoint || ''}
                    onChange={(e) => handleInputChange('attachmentPoint', parseFloat(e.target.value))}
                    className={`w-full px-3 py-2 bg-fd-dark border ${errors.attachmentPoint ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent`}
                    placeholder="e.g., 0.03 for 3%"
                  />
                  {errors.attachmentPoint && <p className="mt-1 text-sm text-red-400">{errors.attachmentPoint}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-fd-text mb-1">
                    Detachment Point (0-1) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    max="1"
                    value={formData.detachmentPoint || ''}
                    onChange={(e) => handleInputChange('detachmentPoint', parseFloat(e.target.value))}
                    className={`w-full px-3 py-2 bg-fd-dark border ${errors.detachmentPoint ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent`}
                    placeholder="e.g., 0.07 for 7%"
                  />
                  {errors.detachmentPoint && <p className="mt-1 text-sm text-red-400">{errors.detachmentPoint}</p>}
                </div>
              </>
            )}

            {/* Economics Section */}
            <div className="md:col-span-2 mt-4">
              <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                Economics
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
                {CURRENCY_OPTIONS.map(curr => (
                  <option key={curr} value={curr}>{curr}</option>
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
                className={`w-full px-3 py-2 bg-fd-dark border ${errors.notional ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent`}
                placeholder="e.g., 10000000"
              />
              {errors.notional && <p className="mt-1 text-sm text-red-400">{errors.notional}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-fd-text mb-1">
                Premium Frequency
              </label>
              <select
                value={formData.premiumFrequency}
                onChange={(e) => handleInputChange('premiumFrequency', e.target.value)}
                className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent"
              >
                {PREMIUM_FREQUENCY_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
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
                {DAY_COUNT_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
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
              {errors.maturityDate && <p className="mt-1 text-sm text-red-400">{errors.maturityDate}</p>}
            </div>

            {/* Constituents Section */}
            <div className="md:col-span-2 mt-4">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide">
                  Basket Constituents <span className="text-red-500">*</span>
                </h3>
                <button
                  type="button"
                  onClick={addConstituent}
                  className="px-3 py-1 text-sm bg-fd-green text-fd-dark rounded-md hover:bg-fd-green-hover focus:outline-none focus:ring-2 focus:ring-fd-green"
                >
                  + Add Constituent
                </button>
              </div>

              {errors.constituents && <p className="mb-2 text-sm text-red-400">{errors.constituents}</p>}

              {constituents.map((constituent, index) => {
                const normalizedWeight = normalizedWeights[index];
                return (
                  <div key={index} className="mb-4 p-4 bg-fd-dark border border-fd-border rounded-md">
                    <div className="flex justify-between items-center mb-3">
                      <h4 className="text-sm font-medium text-fd-text">Constituent #{index + 1}</h4>
                      <button
                        type="button"
                        onClick={() => removeConstituent(index)}
                        className="text-red-400 hover:text-red-300 text-sm"
                      >
                        Remove
                      </button>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                      <div>
                        <label className="block text-xs font-medium text-fd-text-muted mb-1">
                          Issuer <span className="text-red-500">*</span>
                        </label>
                        <select
                          value={constituent.issuer || ''}
                          onChange={(e) => handleConstituentChange(index, 'issuer', e.target.value)}
                          className={`w-full px-2 py-1 text-sm bg-fd-darker border ${errors[`constituent_${index}_issuer`] ? 'border-red-500' : 'border-fd-border'} text-fd-text rounded-md focus:outline-none focus:ring-1 focus:ring-fd-green`}
                        >
                          <option value="">Select Issuer</option>
                          {REFERENCE_ENTITIES.map((entity) => (
                            <option key={entity.code} value={entity.code}>
                              {entity.code} - {entity.name}
                            </option>
                          ))}
                        </select>
                        {errors[`constituent_${index}_issuer`] && (
                          <p className="mt-1 text-xs text-red-400">{errors[`constituent_${index}_issuer`]}</p>
                        )}
                      </div>

                      <div>
                        <label className="block text-xs font-medium text-fd-text-muted mb-1">
                          Weight (optional, normalized)
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          value={constituent.weight || ''}
                          onChange={(e) => handleConstituentChange(index, 'weight', parseFloat(e.target.value) || 0)}
                          className="w-full px-2 py-1 text-sm bg-fd-darker border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-1 focus:ring-fd-green"
                          placeholder={`Auto: ${normalizedWeight.toFixed(4)}`}
                        />
                        {normalizedWeight !== undefined && (
                          <p className="mt-1 text-xs text-fd-text-muted">
                            Normalized: {(normalizedWeight * 100).toFixed(2)}%
                          </p>
                        )}
                      </div>

                      <div>
                        <label className="block text-xs font-medium text-fd-text-muted mb-1">
                          Seniority
                        </label>
                        <select
                          value={constituent.seniority || 'SR_UNSEC'}
                          onChange={(e) => handleConstituentChange(index, 'seniority', e.target.value)}
                          className="w-full px-2 py-1 text-sm bg-fd-darker border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-1 focus:ring-fd-green"
                        >
                          {SENIORITY_OPTIONS.map(opt => (
                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-medium text-fd-text-muted mb-1">
                          Recovery Override (0-1)
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          max="1"
                          value={constituent.recoveryOverride || ''}
                          onChange={(e) => handleConstituentChange(index, 'recoveryOverride', parseFloat(e.target.value))}
                          className="w-full px-2 py-1 text-sm bg-fd-darker border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-1 focus:ring-fd-green"
                          placeholder="Optional"
                        />
                      </div>

                      <div className="md:col-span-2">
                        <label className="block text-xs font-medium text-fd-text-muted mb-1">
                          Sector (optional)
                        </label>
                        <input
                          type="text"
                          value={constituent.sector || ''}
                          onChange={(e) => handleConstituentChange(index, 'sector', e.target.value)}
                          className="w-full px-2 py-1 text-sm bg-fd-darker border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-1 focus:ring-fd-green"
                          placeholder="e.g., TECH, ENERGY"
                        />
                      </div>
                    </div>
                  </div>
                );
              })}
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
              {isSubmitting ? 'Creating...' : 'Create Basket'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BasketCreationModal;
