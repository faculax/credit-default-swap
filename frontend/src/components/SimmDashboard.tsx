import React, { useState, useEffect } from 'react';

interface CrifUpload {
  uploadId: string;
  fileName: string;
  status?: string;
  recordsTotal: number;
  recordsValid: number;
  recordsError: number;
  uploadDate: string;
  portfolioId?: string;
  valuationDate?: string;
  currency?: string;
  errorMessage?: string;
}

interface SimmCalculation {
  calculationId: string;
  portfolioId: string;
  status?: string;
  totalInitialMargin?: number;
  calculationDate: string;
  completedAt?: string;
  errorMessage?: string;
}

interface SimmResult {
  resultId: number;
  productClass: string;
  riskClass: string;
  bucket: string;
  initialMargin: number;
  calculationStep: string;
}

const SimmDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'upload' | 'calculate' | 'results'>('upload');
  const [uploads, setUploads] = useState<CrifUpload[]>([]);
  const [calculations, setCalculations] = useState<SimmCalculation[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // CRIF Upload State
  const [dragOver, setDragOver] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [portfolioId, setPortfolioId] = useState<string>('');

  // Calculation State
  const [calculationPortfolio, setCalculationPortfolio] = useState<string>('');
  const [calculationDate, setCalculationDate] = useState<string>(new Date().toISOString().split('T')[0]);

  // Results State
  const [selectedCalculation, setSelectedCalculation] = useState<string>('');
  const [calculationResults, setCalculationResults] = useState<SimmResult[]>([]);
  const [auditTrail, setAuditTrail] = useState<any[]>([]);

  useEffect(() => {
    loadCalculations();
    loadCrifUploads();
  }, []);

  const loadCalculations = async () => {
    try {
      const response = await fetch('/api/simm/portfolio/ALL/calculations');
      if (response.ok) {
        const data = await response.json();
        setCalculations(data);
      }
    } catch (err) {
      console.error('Failed to load calculations:', err);
    }
  };

  const loadCrifUploads = async () => {
    try {
      const response = await fetch('/api/simm/crif/uploads');
      if (response.ok) {
        const data = await response.json();
        setUploads(data);
      }
    } catch (err) {
      console.error('Failed to load CRIF uploads:', err);
    }
  };

  const handleFileUpload = async () => {
    if (!uploadFile) {
      setError('Please select a file');
      return;
    }

    setLoading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', uploadFile);
    if (portfolioId) {
      formData.append('portfolioId', portfolioId);
    }

    try {
      const response = await fetch('/api/simm/crif/upload', {
        method: 'POST',
        body: formData,
      });

      const data = await response.json();

      if (response.ok) {
        setSuccess(`File uploaded successfully. Upload ID: ${data.uploadId || 'Generated'}`);
        // Refresh the uploads list from server
        loadCrifUploads();
        setUploadFile(null);
        setPortfolioId('');
      } else {
        setError(data.error || 'Upload failed');
      }
    } catch (err) {
      setError('Upload failed: ' + (err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleCalculation = async () => {
    if (!calculationPortfolio) {
      setError('Please enter a portfolio ID');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/simm/calculate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          portfolioId: calculationPortfolio,
          calculationDate: calculationDate,
          createdBy: 'User',
        }),
      });

      const data = await response.json();

      if (response.ok) {
        setSuccess(`✅ Calculation completed successfully! ID: ${data.calculationId || 'Generated'} | Total IM: ${data.totalInitialMargin ? '$' + (data.totalInitialMargin / 1000000).toFixed(1) + 'M' : 'N/A'}`);
        loadCalculations();
        setCalculationPortfolio('');
        
        // Clear success message after 10 seconds
        setTimeout(() => setSuccess(null), 10000);
      } else {
        setError(data.error || 'Calculation failed');
      }
    } catch (err) {
      setError('Calculation failed: ' + (err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const loadCalculationResults = async (calculationId: string) => {
    try {
      const [resultsResponse, auditResponse] = await Promise.all([
        fetch(`/api/simm/calculation/${calculationId}/results`),
        fetch(`/api/simm/calculation/${calculationId}/audit`),
      ]);

      if (resultsResponse.ok) {
        const results = await resultsResponse.json();
        setCalculationResults(results);
      }

      if (auditResponse.ok) {
        const audit = await auditResponse.json();
        setAuditTrail(audit);
      }
    } catch (err) {
      console.error('Failed to load calculation results:', err);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = () => {
    setDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      setUploadFile(files[0]);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
    }).format(amount);
  };

  const getStatusIcon = (status?: string) => {
    if (!status) {
      return <span className="text-fd-text-muted">⏳</span>;
    }
    
    switch (status.toLowerCase()) {
      case 'completed':
        return <span className="text-green-400">✅</span>;
      case 'failed':
        return <span className="text-red-400">❌</span>;
      case 'processing':
      case 'pending':
        return <span className="text-yellow-400">⏳</span>;
      default:
        return <span className="text-fd-text-muted">⏳</span>;
    }
  };

  return (
    <div className="min-h-screen bg-fd-dark p-6">
      <div className="max-w-7xl mx-auto">
        {/* Page Header */}
        <div className="mb-8">
          <div className="flex items-center space-x-3 mb-4">
            <div className="w-10 h-10 bg-fd-green/20 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-fd-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
              </svg>
            </div>
            <div>
              <h1 className="text-3xl font-bold text-fd-text">SIMM Calculator</h1>
              <p className="text-fd-text-muted mt-1">
                Standard Initial Margin Model - ISDA SIMM 2.6+
              </p>
            </div>
          </div>

          {/* Tab Navigation */}
          <div className="flex space-x-1 bg-fd-darker rounded-lg p-1 w-fit">
            {[
              { key: 'upload', label: 'CRIF Upload', icon: (
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                </svg>
              )},
              { key: 'calculate', label: 'Calculate SIMM', icon: (
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                </svg>
              )},
              { key: 'results', label: 'Results & Audit', icon: (
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
                </svg>
              )},
            ].map(({ key, label, icon }) => (
              <button
                key={key}
                onClick={() => setActiveTab(key as any)}
                className={`px-6 py-3 rounded-md text-sm font-medium transition-colors flex items-center space-x-2 ${
                  activeTab === key
                    ? 'bg-fd-green text-fd-dark'
                    : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50'
                }`}
              >
                {icon}
                <span>{label}</span>
              </button>
            ))}
          </div>
        </div>

      {/* Error/Success Messages */}
      {error && (
        <div className="mb-4 p-4 bg-red-500/10 border border-red-500/20 rounded-md flex items-center">
          <span className="text-red-400 mr-2">⚠️</span>
          <span className="text-red-400">{error}</span>
          <button onClick={() => setError(null)} className="ml-auto">
            <span className="text-red-400">✖️</span>
          </button>
        </div>
      )}

      {success && (
        <div className="mb-4 p-4 bg-green-500/10 border border-green-500/20 rounded-md flex items-center">
          <span className="text-green-400 mr-2">✅</span>
          <span className="text-green-400">{success}</span>
          <button onClick={() => setSuccess(null)} className="ml-auto">
            <span className="text-green-400">✖️</span>
          </button>
        </div>
      )}

      {/* Tab Content */}
      {activeTab === 'upload' && (
        <div className="space-y-6">
          <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
            <h2 className="text-xl font-semibold text-fd-text mb-4">Upload CRIF File</h2>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-fd-text mb-2">
                Portfolio ID (optional)
              </label>
              <input
                type="text"
                value={portfolioId}
                onChange={(e) => setPortfolioId(e.target.value)}
                placeholder="Enter portfolio ID"
                className="w-full p-3 bg-fd-input border border-fd-border rounded-md text-fd-text placeholder-fd-text-muted focus:ring-fd-green focus:border-fd-green"
              />
            </div>

            <div
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                dragOver ? 'border-fd-green bg-fd-green/10' : 'border-fd-border'
              }`}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              {uploadFile ? (
                <div>
                  <span className="text-6xl">📄</span>
                  <p className="text-sm font-medium text-fd-text">{uploadFile.name}</p>
                  <p className="text-xs text-fd-text-muted">{Math.round(uploadFile.size / 1024)} KB</p>
                  <button
                    onClick={() => setUploadFile(null)}
                    className="mt-2 text-sm text-red-400 hover:text-red-300"
                  >
                    Remove file
                  </button>
                </div>
              ) : (
                <div>
                  <span className="text-6xl mb-4 block">📤</span>
                  <p className="text-lg font-medium text-fd-text mb-2">
                    Drop your CRIF file here, or click to browse
                  </p>
                  <p className="text-sm text-fd-text-muted">CSV format, max 100MB</p>
                  <input
                    type="file"
                    accept=".csv"
                    onChange={(e) => e.target.files && setUploadFile(e.target.files[0])}
                    className="hidden"
                    id="file-upload"
                  />
                  <label
                    htmlFor="file-upload"
                    className="mt-4 inline-block bg-fd-green text-fd-dark px-4 py-2 rounded-md hover:bg-fd-green-hover cursor-pointer font-medium"
                  >
                    Choose File
                  </label>
                </div>
              )}
            </div>

            <button
              onClick={handleFileUpload}
              disabled={!uploadFile || loading}
              className="mt-4 w-full bg-fd-green text-fd-dark py-3 px-4 rounded-md hover:bg-fd-green-hover disabled:bg-fd-border disabled:cursor-not-allowed font-medium"
            >
              {loading ? 'Uploading...' : 'Upload CRIF File'}
            </button>
          </div>

          {/* Upload History */}
          {uploads.length > 0 && (
            <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
              <h3 className="text-lg font-semibold text-fd-text mb-4">Recent Uploads</h3>
              <div className="space-y-3">
                {uploads.map((upload) => (
                  <div key={upload.uploadId} className="flex items-center justify-between p-3 bg-fd-dark rounded-md border border-fd-border">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-1">
                        <p className="font-medium text-fd-text">{upload.fileName}</p>
                        {upload.portfolioId && (
                          <span className="px-2 py-1 text-xs font-medium bg-fd-green/20 text-fd-green rounded-md">
                            {upload.portfolioId}
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-fd-text-muted">
                        {upload.recordsValid} valid, {upload.recordsError} errors ({upload.recordsTotal} total)
                        {upload.uploadDate && (
                          <span className="ml-3">
                            • Uploaded: {new Date(upload.uploadDate).toLocaleDateString()}
                          </span>
                        )}
                      </p>
                    </div>
                    <div className="flex items-center">
                      {getStatusIcon(upload.status)}
                      <span className="ml-2 text-sm font-medium text-fd-text">{upload.status || 'Unknown'}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {activeTab === 'calculate' && (
        <div className="space-y-6">
          <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
            <h2 className="text-xl font-semibold text-fd-text mb-4">Execute SIMM Calculation</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
              <div>
                <label className="block text-sm font-medium text-fd-text mb-2">
                  Portfolio ID
                </label>
                <input
                  type="text"
                  value={calculationPortfolio}
                  onChange={(e) => setCalculationPortfolio(e.target.value)}
                  placeholder="Enter portfolio ID"
                  className="w-full p-3 bg-fd-input border border-fd-border rounded-md text-fd-text placeholder-fd-text-muted focus:ring-fd-green focus:border-fd-green"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-fd-text mb-2">
                  Calculation Date
                </label>
                <input
                  type="date"
                  value={calculationDate}
                  onChange={(e) => setCalculationDate(e.target.value)}
                  className="w-full p-3 bg-fd-input border border-fd-border rounded-md text-fd-text focus:ring-fd-green focus:border-fd-green"
                />
              </div>
            </div>

            <button
              onClick={handleCalculation}
              disabled={!calculationPortfolio || loading}
              className="w-full bg-fd-green text-fd-dark py-3 px-4 rounded-md hover:bg-fd-green-hover disabled:bg-fd-border disabled:cursor-not-allowed font-medium"
            >
              {loading ? 'Calculating...' : 'Calculate Initial Margin'}
            </button>
          </div>

          {/* Calculation History */}
          {calculations.length > 0 && (
            <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
              <h3 className="text-lg font-semibold text-fd-text mb-4">Calculation History</h3>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-fd-border">
                  <thead className="bg-fd-dark">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Portfolio
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Date
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Initial Margin
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-fd-darker divide-y divide-fd-border">
                    {calculations.map((calc) => (
                      <tr key={calc.calculationId}>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                          {calc.portfolioId}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                          {calc.calculationDate}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            {getStatusIcon(calc.status)}
                            <span className="ml-2 text-sm text-fd-text">{calc.status || 'Unknown'}</span>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {calc.totalInitialMargin ? formatCurrency(calc.totalInitialMargin) : '-'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <button
                            onClick={() => {
                              setSelectedCalculation(calc.calculationId);
                              setActiveTab('results');
                              loadCalculationResults(calc.calculationId);
                            }}
                            className="text-fd-green hover:text-fd-green-hover"
                          >
                            View Details
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {activeTab === 'results' && (
        <div className="space-y-6">
          <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
            <h2 className="text-xl font-semibold text-fd-text mb-4">Calculation Results & Audit Trail</h2>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-fd-text mb-2">
                Select Calculation
              </label>
              <select
                value={selectedCalculation}
                onChange={(e) => {
                  setSelectedCalculation(e.target.value);
                  if (e.target.value) {
                    loadCalculationResults(e.target.value);
                  }
                }}
                className="w-full p-3 bg-fd-input border border-fd-border rounded-md text-fd-text focus:ring-fd-green focus:border-fd-green"
              >
                <option value="">Select a calculation...</option>
                {calculations.map((calc) => (
                  <option key={calc.calculationId} value={calc.calculationId}>
                    {calc.portfolioId} - {calc.calculationDate} ({calc.status || 'Unknown'})
                  </option>
                ))}
              </select>
            </div>

            {calculationResults.length > 0 && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-semibold text-fd-text mb-3">Results Breakdown</h3>
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-fd-border">
                      <thead className="bg-fd-dark">
                        <tr>
                          <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                            Product Class
                          </th>
                          <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                            Risk Class
                          </th>
                          <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                            Bucket
                          </th>
                          <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                            Initial Margin
                          </th>
                          <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                            Step
                          </th>
                        </tr>
                      </thead>
                      <tbody className="bg-fd-darker divide-y divide-fd-border">
                        {calculationResults.map((result) => (
                          <tr key={result.resultId}>
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                              {result.productClass}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                              {result.riskClass}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                              {result.bucket}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                              {formatCurrency(result.initialMargin)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                              {result.calculationStep}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {auditTrail.length > 0 && (
                  <div>
                    <h3 className="text-lg font-semibold text-fd-text mb-3">Audit Trail</h3>
                    <div className="space-y-2">
                      {auditTrail.map((audit, index) => (
                        <div key={index} className="p-3 bg-fd-dark rounded-md border border-fd-border">
                          <div className="flex justify-between items-start">
                            <span className="font-medium text-sm text-fd-text">{audit.step}</span>
                            <span className="text-xs text-fd-text-muted">{audit.timestamp}</span>
                          </div>
                          <p className="text-sm text-fd-text-muted mt-1">{audit.description}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Help Section */}
      <div className="mt-12 bg-fd-darker rounded-lg border border-fd-border p-6">
        <h3 className="text-lg font-semibold text-fd-text mb-4">SIMM Calculator Guide</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <div className="w-6 h-6 bg-fd-green/20 rounded-full flex items-center justify-center">
                <span className="text-fd-green text-xs font-bold">1</span>
              </div>
              <h4 className="font-medium text-fd-text">CRIF File Requirements</h4>
            </div>
            <ul className="text-sm text-fd-text-muted space-y-1 ml-8">
              <li>• CSV format with ISDA CRIF structure</li>
              <li>• Maximum file size: 100MB</li>
              <li>• Required columns: ProductClass, RiskType, Qualifier</li>
              <li>• Amount in base currency or USD</li>
            </ul>
          </div>

          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <div className="w-6 h-6 bg-blue-500/20 rounded-full flex items-center justify-center">
                <span className="text-blue-400 text-xs font-bold">2</span>
              </div>
              <h4 className="font-medium text-fd-text">Calculation Process</h4>
            </div>
            <ul className="text-sm text-fd-text-muted space-y-1 ml-8">
              <li>• PENDING: Awaiting calculation</li>
              <li>• PROCESSING: Computing SIMM components</li>
              <li>• COMPLETED: Results available</li>
              <li>• FAILED: Error in calculation (check logs)</li>
            </ul>
          </div>

          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <div className="w-6 h-6 bg-orange-500/20 rounded-full flex items-center justify-center">
                <span className="text-orange-400 text-xs font-bold">3</span>
              </div>
              <h4 className="font-medium text-fd-text">SIMM Methodology</h4>
            </div>
            <ul className="text-sm text-fd-text-muted space-y-1 ml-8">
              <li>• ISDA SIMM 2.6+ compliant</li>
              <li>• Risk factor bucketing by asset class</li>
              <li>• Correlation adjustments applied</li>
              <li>• Portfolio margin netting</li>
            </ul>
          </div>
        </div>

        <div className="mt-6 pt-6 border-t border-fd-border">
          <h4 className="font-medium text-fd-text mb-2">Sample CRIF Format (ISDA Standard):</h4>
          <div className="bg-fd-dark rounded-md p-3 font-mono text-sm text-fd-text-muted overflow-x-auto">
            <div>ProductClass,RiskType,Qualifier,Bucket,Label1,Label2,Amount,AmountCurrency,AmountUSD</div>
            <div>Credit,Risk_IRCurve,EUR,1,2Y,,50000,EUR,55000</div>
            <div>RatesFX,Risk_IRCurve,USD,,1Y,,75000,USD,75000</div>
            <div>Equity,Risk_Equity,S&P500,1,,,25000,USD,25000</div>
          </div>
        </div>
      </div>
      </div>
    </div>
  );
};

export default SimmDashboard;