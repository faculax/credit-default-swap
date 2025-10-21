import React, { useState } from 'react';
import StatementUpload from './StatementUpload';
import StatementList from './StatementList';
import AutomatedStatementGenerator from './AutomatedStatementGenerator';

const MarginStatementsPage: React.FC = () => {
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [activeTab, setActiveTab] = useState<'generate' | 'upload' | 'list'>('generate');

  const handleUploadSuccess = (statementId: number) => {
    // Trigger refresh of the statement list
    setRefreshTrigger(prev => prev + 1);
    // Switch to list view to see the uploaded statement
    setActiveTab('list');
  };

  const handleUploadError = (error: string) => {
    console.error('Upload error:', error);
    // Error handling is done in the StatementUpload component
  };

  return (
    <div className="min-h-screen bg-fd-dark p-6">
      <div className="max-w-7xl mx-auto">
        {/* Page Header */}
        <div className="mb-8">
          <div className="flex items-center space-x-3 mb-4">
            <div className="w-10 h-10 bg-blue-500/20 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
              </svg>
            </div>
            <div>
              <h1 className="text-3xl font-bold text-fd-text">Margin Statements</h1>
              <p className="text-fd-text-muted mt-1">
                Generate automated VM/IM statements or upload external CCP statements
              </p>
            </div>
          </div>

          {/* Tab Navigation */}
          <div className="flex space-x-1 bg-fd-darker rounded-lg p-1 w-fit">
            <button
              onClick={() => setActiveTab('generate')}
              className={`px-6 py-3 rounded-md text-sm font-medium transition-colors flex items-center space-x-2 ${
                activeTab === 'generate'
                  ? 'bg-green-500 text-white'
                  : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50'
              }`}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
              </svg>
              <span>Auto Generate</span>
            </button>
            <button
              onClick={() => setActiveTab('upload')}
              className={`px-6 py-3 rounded-md text-sm font-medium transition-colors flex items-center space-x-2 ${
                activeTab === 'upload'
                  ? 'bg-blue-500 text-white'
                  : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50'
              }`}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
              </svg>
              <span>Upload Statement</span>
            </button>
            <button
              onClick={() => setActiveTab('list')}
              className={`px-6 py-3 rounded-md text-sm font-medium transition-colors flex items-center space-x-2 ${
                activeTab === 'list'
                  ? 'bg-blue-500 text-white'
                  : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50'
              }`}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
              </svg>
              <span>Statement History</span>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="space-y-6">
          {activeTab === 'generate' ? (
            <AutomatedStatementGenerator 
              onGenerationSuccess={() => {
                setRefreshTrigger(prev => prev + 1);
                setActiveTab('list');
              }}
              onGenerationError={handleUploadError}
            />
          ) : activeTab === 'upload' ? (
            <StatementUpload 
              onUploadSuccess={handleUploadSuccess}
              onUploadError={handleUploadError}
            />
          ) : (
            <StatementList refreshTrigger={refreshTrigger} />
          )}
        </div>

        {/* Help Section */}
        <div className="mt-12 bg-fd-darker rounded-lg border border-fd-border p-6">
          <h3 className="text-lg font-semibold text-fd-text mb-4">Statement Processing Guide</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div className="space-y-2">
              <div className="flex items-center space-x-2">
                <div className="w-6 h-6 bg-blue-500/20 rounded-full flex items-center justify-center">
                  <span className="text-blue-400 text-xs font-bold">1</span>
                </div>
                <h4 className="font-medium text-fd-text">Upload Requirements</h4>
              </div>
              <ul className="text-sm text-fd-text-muted space-y-1 ml-8">
                <li>• CSV, XML, or JSON format</li>
                <li>• Maximum file size: 50MB</li>
                <li>• Statement date cannot be future</li>
                <li>• Valid ISO currency codes</li>
              </ul>
            </div>

            <div className="space-y-2">
              <div className="flex items-center space-x-2">
                <div className="w-6 h-6 bg-green-500/20 rounded-full flex items-center justify-center">
                  <span className="text-green-400 text-xs font-bold">2</span>
                </div>
                <h4 className="font-medium text-fd-text">Processing Stages</h4>
              </div>
              <ul className="text-sm text-fd-text-muted space-y-1 ml-8">
                <li>• PENDING: Awaiting processing</li>
                <li>• PROCESSING: Being parsed & validated</li>
                <li>• PROCESSED: Successfully completed</li>
                <li>• FAILED: Error occurred (retryable)</li>
              </ul>
            </div>

            <div className="space-y-2">
              <div className="flex items-center space-x-2">
                <div className="w-6 h-6 bg-orange-500/20 rounded-full flex items-center justify-center">
                  <span className="text-orange-400 text-xs font-bold">3</span>
                </div>
                <h4 className="font-medium text-fd-text">Validation Rules</h4>
              </div>
              <ul className="text-sm text-fd-text-muted space-y-1 ml-8">
                <li>• Initial Margin must be non-negative</li>
                <li>• Variation Margin can be negative</li>
                <li>• Account must exist for CCP</li>
                <li>• Automatic duplicate detection</li>
              </ul>
            </div>
          </div>

          <div className="mt-6 pt-6 border-t border-fd-border">
            <h4 className="font-medium text-fd-text mb-2">Sample CSV Format (LCH):</h4>
            <div className="bg-fd-dark rounded-md p-3 font-mono text-sm text-fd-text-muted overflow-x-auto">
              <div>Account,Date,Position Type,Amount,Currency,Portfolio,Product Class</div>
              <div>HOUSE-001,2024-10-08,VM,-15000.00,USD,PORT1,CDS</div>
              <div>HOUSE-001,2024-10-08,IM,250000.00,USD,PORT1,CDS</div>
              <div>HOUSE-001,2024-10-08,EXCESS,50000.00,USD,PORT1,CDS</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MarginStatementsPage;