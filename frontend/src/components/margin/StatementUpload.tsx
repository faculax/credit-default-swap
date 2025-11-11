import React, { useState, useRef } from 'react';

interface StatementUploadProps {
  onUploadSuccess?: (statementId: number) => void;
  onUploadError?: (error: string) => void;
}

interface UploadFormData {
  statementId: string;
  ccpName: string;
  memberFirm: string;
  accountNumber: string;
  statementDate: string;
  currency: string;
  format: string;
}

const StatementUpload: React.FC<StatementUploadProps> = ({ onUploadSuccess, onUploadError }) => {
  const [formData, setFormData] = useState<UploadFormData>({
    statementId: '',
    ccpName: 'LCH',
    memberFirm: '',
    accountNumber: '',
    statementDate: '',
    currency: 'USD',
    format: 'CSV'
  });

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showSuccessNotification, setShowSuccessNotification] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const ccpOptions = ['LCH', 'CME', 'EUREX', 'ICE'];
  const currencyOptions = ['USD', 'EUR', 'GBP', 'JPY', 'CHF'];
  const formatOptions = ['CSV', 'XML', 'JSON', 'PROPRIETARY'];

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    setError(null);
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelect(e.dataTransfer.files[0]);
    }
  };

  const handleFileSelect = (file: File) => {
    setSelectedFile(file);
    setError(null);
    setSuccess(null);

    // Auto-detect format based on file extension
    const extension = file.name.toLowerCase().split('.').pop();
    if (extension === 'csv') {
      setFormData(prev => ({ ...prev, format: 'CSV' }));
    } else if (extension === 'xml') {
      setFormData(prev => ({ ...prev, format: 'XML' }));
    } else if (extension === 'json') {
      setFormData(prev => ({ ...prev, format: 'JSON' }));
    }

    // Generate statement ID if not provided
    if (!formData.statementId) {
      const timestamp = new Date().toISOString().split('T')[0].replace(/-/g, '');
      const randomSuffix = Math.random().toString(36).substring(2, 8).toUpperCase();
      setFormData(prev => ({ 
        ...prev, 
        statementId: `${formData.ccpName}_${timestamp}_${randomSuffix}` 
      }));
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      handleFileSelect(e.target.files[0]);
    }
  };

  const validateForm = (): string | null => {
    if (!selectedFile) return 'Please select a file to upload';
    if (!formData.statementId.trim()) return 'Statement ID is required';
    if (!formData.memberFirm.trim()) return 'Member firm is required';
    if (!formData.accountNumber.trim()) return 'Account number is required';
    if (!formData.statementDate) return 'Statement date is required';
    
    // Validate file size (50MB limit)
    if (selectedFile.size > 50 * 1024 * 1024) {
      return 'File size must be less than 50MB';
    }

    // Validate date is not in future
    const statementDate = new Date(formData.statementDate);
    if (statementDate > new Date()) {
      return 'Statement date cannot be in the future';
    }

    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    setUploading(true);
    setError(null);
    setSuccess(null);
    setUploadProgress(0);

    try {
      const formDataToSend = new FormData();
      formDataToSend.append('file', selectedFile!);
      formDataToSend.append('statementId', formData.statementId);
      formDataToSend.append('ccpName', formData.ccpName);
      formDataToSend.append('memberFirm', formData.memberFirm);
      formDataToSend.append('accountNumber', formData.accountNumber);
      formDataToSend.append('statementDate', formData.statementDate);
      formDataToSend.append('currency', formData.currency);
      formDataToSend.append('format', formData.format);

      // Simulate upload progress
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => Math.min(prev + 10, 90));
      }, 200);

      const response = await fetch('/api/margin-statements/upload', {
        method: 'POST',
        body: formDataToSend
      });

      clearInterval(progressInterval);
      setUploadProgress(100);

      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.error || 'Upload failed');
      }

      setSuccess(`Statement uploaded successfully! ID: ${result.statementId}`);
      
      // Show success notification toast
      setShowSuccessNotification(true);
      setTimeout(() => {
        setShowSuccessNotification(false);
      }, 3000);
      
      // Reset form
      setSelectedFile(null);
      setFormData({
        statementId: '',
        ccpName: 'LCH',
        memberFirm: '',
        accountNumber: '',
        statementDate: '',
        currency: 'USD',
        format: 'CSV'
      });
      
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }

      if (onUploadSuccess) {
        onUploadSuccess(result.statementId);
      }

    } catch (err: any) {
      setError(err.message || 'Upload failed');
      if (onUploadError) {
        onUploadError(err.message);
      }
    } finally {
      setUploading(false);
      setTimeout(() => setUploadProgress(0), 2000);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
      {/* Success Notification Toast */}
      {showSuccessNotification && (
        <div className="fixed top-4 right-4 z-50 animate-slide-in-right">
          <div className="bg-green-500/20 border border-green-500/50 rounded-lg p-4 shadow-lg backdrop-blur-sm min-w-[320px]">
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0">
                <div className="w-10 h-10 bg-green-500/30 rounded-full flex items-center justify-center">
                  <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                  </svg>
                </div>
              </div>
              <div className="flex-1">
                <h4 className="font-semibold text-green-300 mb-1">Statement Uploaded Successfully!</h4>
                <p className="text-sm text-green-200/80">
                  {success}
                </p>
              </div>
              <button
                onClick={() => setShowSuccessNotification(false)}
                className="flex-shrink-0 text-green-400/60 hover:text-green-400 transition-colors"
                aria-label="Dismiss notification"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>
          </div>
        </div>
      )}
      
      <div className="flex items-center space-x-3 mb-6">
        <div className="w-8 h-8 bg-blue-500/20 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
          </svg>
        </div>
        <h2 className="text-2xl font-bold text-fd-text">Upload Margin Statement</h2>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* File Upload Area */}
        <div className="space-y-4">
          <label className="block text-sm font-medium text-fd-text">Statement File</label>
          <div
            className={`relative border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
              dragActive
                ? 'border-blue-400 bg-blue-400/10'
                : 'border-fd-border hover:border-fd-green'
            }`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <input
              ref={fileInputRef}
              type="file"
              onChange={handleFileInputChange}
              accept=".csv,.xml,.json,.txt"
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            />
            
            {selectedFile ? (
              <div className="space-y-2">
                <div className="w-12 h-12 bg-green-500/20 rounded-full flex items-center justify-center mx-auto">
                  <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </div>
                <div className="text-fd-text font-medium">{selectedFile.name}</div>
                <div className="text-fd-text-muted text-sm">{formatFileSize(selectedFile.size)}</div>
                {uploadProgress > 0 && uploadProgress < 100 && (
                  <div className="w-full bg-fd-border rounded-full h-2 mt-2">
                    <div 
                      className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${uploadProgress}%` }}
                    ></div>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-2">
                <div className="w-12 h-12 bg-fd-border rounded-full flex items-center justify-center mx-auto">
                  <svg className="w-6 h-6 text-fd-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                  </svg>
                </div>
                <div className="text-fd-text">
                  <span className="font-medium">Click to upload</span> or drag and drop
                </div>
                <div className="text-fd-text-muted text-sm">
                  CSV, XML, JSON files up to 50MB
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Form Fields */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-fd-text mb-2">Statement ID</label>
            <input
              type="text"
              name="statementId"
              value={formData.statementId}
              onChange={handleInputChange}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="AUTO_GENERATED_001"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-fd-text mb-2">CCP Name</label>
            <select
              name="ccpName"
              value={formData.ccpName}
              onChange={handleInputChange}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {ccpOptions.map(ccp => (
                <option key={ccp} value={ccp}>{ccp}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-fd-text mb-2">Member Firm</label>
            <input
              type="text"
              name="memberFirm"
              value={formData.memberFirm}
              onChange={handleInputChange}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Goldman Sachs"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-fd-text mb-2">Account Number</label>
            <input
              type="text"
              name="accountNumber"
              value={formData.accountNumber}
              onChange={handleInputChange}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="HOUSE-001"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-fd-text mb-2">Statement Date</label>
            <input
              type="date"
              name="statementDate"
              value={formData.statementDate}
              onChange={handleInputChange}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-fd-text mb-2">Currency</label>
            <select
              name="currency"
              value={formData.currency}
              onChange={handleInputChange}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {currencyOptions.map(currency => (
                <option key={currency} value={currency}>{currency}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-fd-text mb-2">Format</label>
            <select
              name="format"
              value={formData.format}
              onChange={handleInputChange}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {formatOptions.map(format => (
                <option key={format} value={format}>{format}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Error/Success Messages */}
        {error && (
          <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-3">
            <div className="flex items-center space-x-2">
              <svg className="w-5 h-5 text-red-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
              </svg>
              <span className="text-red-400 text-sm">{error}</span>
            </div>
          </div>
        )}

        {/* Submit Button */}
        <div className="flex justify-end space-x-4 pt-4 border-t border-fd-border">
          <button
            type="button"
            onClick={() => {
              setSelectedFile(null);
              setFormData({
                statementId: '',
                ccpName: 'LCH',
                memberFirm: '',
                accountNumber: '',
                statementDate: '',
                currency: 'USD',
                format: 'CSV'
              });
              setError(null);
              setSuccess(null);
              if (fileInputRef.current) {
                fileInputRef.current.value = '';
              }
            }}
            className="px-6 py-2 bg-fd-border text-fd-text rounded hover:bg-fd-border/80 transition-colors"
          >
            Clear
          </button>
          <button
            type="submit"
            disabled={uploading || !selectedFile}
            className="px-6 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
          >
            {uploading && (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
            )}
            <span>{uploading ? 'Uploading...' : 'Upload Statement'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};

export default StatementUpload;