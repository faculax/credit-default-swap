import React, { useState } from 'react';

export type CreditEventType = 'BANKRUPTCY' | 'FAILURE_TO_PAY' | 'RESTRUCTURING' | 'OBLIGATION_DEFAULT' | 'REPUDIATION_MORATORIUM';
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
  isLoading?: boolean;
}

const CreditEventModal: React.FC<CreditEventModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  tradeId,
  isLoading = false
}) => {
  const [formData, setFormData] = useState<CreateCreditEventRequest>({
    eventType: 'BANKRUPTCY',
    eventDate: '',
    noticeDate: '',
    settlementMethod: 'CASH',
    comments: ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const eventTypeOptions: { value: CreditEventType; label: string }[] = [
    { value: 'BANKRUPTCY', label: 'Bankruptcy' },
    { value: 'FAILURE_TO_PAY', label: 'Failure to Pay' },
    { value: 'RESTRUCTURING', label: 'Restructuring' },
    { value: 'OBLIGATION_DEFAULT', label: 'Obligation Default' },
    { value: 'REPUDIATION_MORATORIUM', label: 'Repudiation/Moratorium' }
  ];

  const settlementMethodOptions: { value: SettlementMethod; label: string }[] = [
    { value: 'CASH', label: 'Cash Settlement' },
    { value: 'PHYSICAL', label: 'Physical Settlement' }
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
        eventType: 'BANKRUPTCY',
        eventDate: '',
        noticeDate: '',
        settlementMethod: 'CASH',
        comments: ''
      });
      setErrors({});
      onClose();
    } catch (error) {
      console.error('Error submitting credit event:', error);
    }
  };

  const handleInputChange = (field: keyof CreateCreditEventRequest, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold text-gray-900">
            Record Credit Event
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
            disabled={isLoading}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="eventType" className="block text-sm font-medium text-gray-700 mb-1">
              Event Type *
            </label>
            <select
              id="eventType"
              value={formData.eventType}
              onChange={(e) => handleInputChange('eventType', e.target.value)}
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                errors.eventType ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={isLoading}
            >
              {eventTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {errors.eventType && (
              <p className="mt-1 text-sm text-red-600">{errors.eventType}</p>
            )}
          </div>

          <div>
            <label htmlFor="eventDate" className="block text-sm font-medium text-gray-700 mb-1">
              Event Date *
            </label>
            <input
              type="date"
              id="eventDate"
              value={formData.eventDate}
              onChange={(e) => handleInputChange('eventDate', e.target.value)}
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                errors.eventDate ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={isLoading}
            />
            {errors.eventDate && (
              <p className="mt-1 text-sm text-red-600">{errors.eventDate}</p>
            )}
          </div>

          <div>
            <label htmlFor="noticeDate" className="block text-sm font-medium text-gray-700 mb-1">
              Notice Date *
            </label>
            <input
              type="date"
              id="noticeDate"
              value={formData.noticeDate}
              onChange={(e) => handleInputChange('noticeDate', e.target.value)}
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                errors.noticeDate ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={isLoading}
            />
            {errors.noticeDate && (
              <p className="mt-1 text-sm text-red-600">{errors.noticeDate}</p>
            )}
          </div>

          <div>
            <label htmlFor="settlementMethod" className="block text-sm font-medium text-gray-700 mb-1">
              Settlement Method *
            </label>
            <select
              id="settlementMethod"
              value={formData.settlementMethod}
              onChange={(e) => handleInputChange('settlementMethod', e.target.value)}
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                errors.settlementMethod ? 'border-red-500' : 'border-gray-300'
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
              <p className="mt-1 text-sm text-red-600">{errors.settlementMethod}</p>
            )}
          </div>

          <div>
            <label htmlFor="comments" className="block text-sm font-medium text-gray-700 mb-1">
              Comments
            </label>
            <textarea
              id="comments"
              value={formData.comments}
              onChange={(e) => handleInputChange('comments', e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Optional comments about the credit event..."
              disabled={isLoading}
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 border border-gray-300 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
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