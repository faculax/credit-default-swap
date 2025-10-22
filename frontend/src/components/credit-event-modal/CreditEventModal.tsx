import React, { useState } from 'react';
import { REFERENCE_ENTITIES } from '../../data/referenceData';

export type CreditEventType =
  | 'BANKRUPTCY'
  | 'FAILURE_TO_PAY'
  | 'RESTRUCTURING'
  | 'OBLIGATION_DEFAULT'
  | 'REPUDIATION_MORATORIUM'
  | 'PAYOUT';
export type SettlementMethod = 'CASH' | 'PHYSICAL';

export interface CreateCreditEventRequest {
  eventType: CreditEventType;
  eventDate: string;
  noticeDate: string;
  settlementMethod: SettlementMethod;
  comments?: string;
}

interface CreditEventModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (request: CreateCreditEventRequest) => Promise<void>;
  tradeId: number;
  referenceEntity?: string;
  isLoading?: boolean;
}

const CreditEventModal: React.FC<CreditEventModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  tradeId,
  referenceEntity,
  isLoading = false
}) => {
  // Get today's date in YYYY-MM-DD format
  const getTodayDate = () => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  };

  // Helper function to get full name of reference entity
  const getReferenceEntityFullName = (code: string): string => {
    const entity = REFERENCE_ENTITIES.find(e => e.code === code);
    return entity ? entity.name : code;
  };

  const [formData, setFormData] = useState<CreateCreditEventRequest>({
    eventType: 'RESTRUCTURING',
    eventDate: getTodayDate(),
    noticeDate: getTodayDate(),
    settlementMethod: 'PHYSICAL',
    comments: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const eventTypeOptions: { value: CreditEventType; label: string }[] = [
    { value: 'RESTRUCTURING', label: 'Restructuring' },
    { value: 'BANKRUPTCY', label: 'Bankruptcy' },
  ];

  const settlementMethodOptions: { value: SettlementMethod; label: string }[] = [
    { value: 'PHYSICAL', label: 'Physical Settlement' },
    { value: 'CASH', label: 'Cash Settlement' },
  ];

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.eventType) {
      newErrors.eventType = 'Event type is required';
    }

    if (!formData.eventDate) {
      newErrors.eventDate = 'Event date is required';
    } else if (new Date(formData.eventDate) > new Date()) {
      newErrors.eventDate = 'Event date cannot be in the future';
    }

    if (!formData.noticeDate) {
      newErrors.noticeDate = 'Notice date is required';
    } else if (formData.eventDate && new Date(formData.noticeDate) < new Date(formData.eventDate)) {
      newErrors.noticeDate = 'Notice date must be on or after event date';
    }

    if (!formData.settlementMethod) {
      newErrors.settlementMethod = 'Settlement method is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      await onSubmit(formData);
      // Reset form on successful submission
      setFormData({
        eventType: 'RESTRUCTURING',
        eventDate: getTodayDate(),
        noticeDate: getTodayDate(),
        settlementMethod: 'PHYSICAL',
        comments: '',
      });
      setErrors({});
      onClose();
    } catch (error) {
      console.error('Error submitting credit event:', error);
    }
  };

  const handleInputChange = (field: keyof CreateCreditEventRequest, value: string) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));

    // Clear error when user starts typing
    if (errors[field]) {
      setErrors((prev) => ({
        ...prev,
        [field]: '',
      }));
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold text-fd-text">Record Credit Event</h2>
          <button
            onClick={onClose}
            className="text-fd-text-muted hover:text-fd-text transition-colors"
            disabled={isLoading}
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

        {/* Reference Entity Information */}
        {referenceEntity && (
          <div className="mb-4 p-3 bg-fd-dark rounded-lg border border-fd-border">
            <div className="flex items-center gap-2">
              <svg className="w-5 h-5 text-fd-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <div className="flex-1">
                <p className="text-xs text-fd-text-muted uppercase tracking-wide">Reference Entity</p>
                <p className="text-fd-green font-semibold">{getReferenceEntityFullName(referenceEntity)}</p>
                <p className="text-fd-text-muted text-xs mt-0.5">{referenceEntity}</p>
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="eventType" className="block text-sm font-medium text-fd-text mb-1">
              Event Type *
            </label>
            <select
              id="eventType"
              value={formData.eventType}
              onChange={(e) => handleInputChange('eventType', e.target.value)}
              className={`w-full px-3 py-2 bg-fd-dark border rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green text-fd-text ${
                errors.eventType ? 'border-red-500' : 'border-fd-border'
              }`}
              disabled={isLoading}
            >
              {eventTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {errors.eventType && <p className="mt-1 text-sm text-red-400">{errors.eventType}</p>}
          </div>

          <div>
            <label htmlFor="eventDate" className="block text-sm font-medium text-fd-text mb-1">
              Event Date *
            </label>
            <input
              type="date"
              id="eventDate"
              value={formData.eventDate}
              onChange={(e) => handleInputChange('eventDate', e.target.value)}
              className={`w-full px-3 py-2 bg-fd-dark border rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green text-fd-text ${
                errors.eventDate ? 'border-red-500' : 'border-fd-border'
              }`}
              disabled={isLoading}
            />
            {errors.eventDate && <p className="mt-1 text-sm text-red-400">{errors.eventDate}</p>}
          </div>

          <div>
            <label htmlFor="noticeDate" className="block text-sm font-medium text-fd-text mb-1">
              Notice Date *
            </label>
            <input
              type="date"
              id="noticeDate"
              value={formData.noticeDate}
              onChange={(e) => handleInputChange('noticeDate', e.target.value)}
              className={`w-full px-3 py-2 bg-fd-dark border rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green text-fd-text ${
                errors.noticeDate ? 'border-red-500' : 'border-fd-border'
              }`}
              disabled={isLoading}
            />
            {errors.noticeDate && <p className="mt-1 text-sm text-red-400">{errors.noticeDate}</p>}
          </div>

          <div>
            <label
              htmlFor="settlementMethod"
              className="block text-sm font-medium text-fd-text mb-1"
            >
              Settlement Method *
            </label>
            <select
              id="settlementMethod"
              value={formData.settlementMethod}
              onChange={(e) => handleInputChange('settlementMethod', e.target.value)}
              className={`w-full px-3 py-2 bg-fd-dark border rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green text-fd-text ${
                errors.settlementMethod ? 'border-red-500' : 'border-fd-border'
              }`}
              disabled={isLoading}
            >
              {settlementMethodOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {errors.settlementMethod && (
              <p className="mt-1 text-sm text-red-400">{errors.settlementMethod}</p>
            )}
          </div>

          <div>
            <label htmlFor="comments" className="block text-sm font-medium text-fd-text mb-1">
              Comments
            </label>
            <textarea
              id="comments"
              value={formData.comments}
              onChange={(e) => handleInputChange('comments', e.target.value)}
              rows={3}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green text-fd-text placeholder-fd-text-muted"
              placeholder="Optional comments about the credit event..."
              disabled={isLoading}
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-fd-text bg-fd-dark border border-fd-border rounded-md hover:bg-fd-border focus:outline-none focus:ring-2 focus:ring-fd-green transition-colors"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-fd-dark bg-fd-green border border-transparent rounded-md hover:bg-fd-green-hover focus:outline-none focus:ring-2 focus:ring-fd-green disabled:opacity-50 transition-colors"
              disabled={isLoading}
            >
              {isLoading ? 'Recording...' : 'Record Credit Event'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreditEventModal;
