import React, { useState } from 'react';
import { apiUrl } from '../../config/api';

interface GeneratedMarginStatement {
  nettingSetId: string;
  ccpName: string;
  accountId: string;
  statementDate: string;
  variationMarginNet: number;
  initialMarginRequired: number;
  excessMargin: number;
  currency: string;
  tradeCount: number;
  totalNotional: number;
  calculationStatus: string;
  processingTime: string;
}

const AutomatedStatementGenerator: React.FC<{ 
  onGenerationSuccess: () => void;
  onGenerationError: (error: string) => void;
}> = ({ onGenerationSuccess, onGenerationError }) => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [generatedStatements, setGeneratedStatements] = useState<GeneratedMarginStatement[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [duplicateWarning, setDuplicateWarning] = useState<string | null>(null);

  const generateStatements = async () => {
    setIsGenerating(true);
    setDuplicateWarning(null); // Clear any previous warnings
    try {
      const response = await fetch(apiUrl(`/margin-statements/actions/generate-automated?statementDate=${selectedDate}`), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const result = await response.json();
      
      if (!response.ok) {
        // Check if it's a 409 Conflict (duplicate)
        if (response.status === 409) {
          const message = result.message || result.error || 'Statements already exist for this date';
          setDuplicateWarning(message);
          setShowResults(false);
          // Don't call onGenerationError for duplicates - it's not really an error
          return;
        }
        throw new Error(result.message || result.error || `HTTP error! status: ${response.status}`);
      }

      if (result.success && result.generatedStatements) {
        setGeneratedStatements(result.generatedStatements);
        setShowResults(true);
        onGenerationSuccess();
      } else {
        throw new Error(result.error || 'Generation failed');
      }
    } catch (error) {
      console.error('Error generating statements:', error);
      onGenerationError(error instanceof Error ? error.message : 'Unknown error occurred');
    } finally {
      setIsGenerating(false);
    }
  };

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  };

  const formatNumber = (num: number) => {
    return new Intl.NumberFormat('en-US').format(num);
  };

  return (
    <div className="space-y-6">
      {/* Generation Controls */}
      <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
        <div className="flex items-center space-x-3 mb-6">
          <div className="w-10 h-10 bg-green-500/20 rounded-full flex items-center justify-center">
            <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
            </svg>
          </div>
          <div>
            <h2 className="text-xl font-semibold text-fd-text">Automated Statement Generation</h2>
            <p className="text-fd-text-muted">Generate VM/IM statements using existing CCP data and netting sets</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-fd-text mb-2">
              Statement Date
            </label>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              max={new Date().toISOString().split('T')[0]}
              className="w-full px-4 py-3 bg-fd-dark border border-fd-border rounded-lg text-fd-text 
                         focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50"
            />
          </div>

          <div className="flex items-end">
            <button
              onClick={generateStatements}
              disabled={isGenerating}
              className={`w-full px-6 py-3 rounded-lg font-medium transition-colors flex items-center justify-center space-x-2 ${
                isGenerating
                  ? 'bg-gray-600 text-gray-300 cursor-not-allowed'
                  : 'bg-green-600 hover:bg-green-700 text-white'
              }`}
            >
              {isGenerating ? (
                <>
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Generating...
                </>
              ) : (
                <>
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                  </svg>
                  Generate Statements
                </>
              )}
            </button>
          </div>
        </div>

        {/* Info Panel */}
        <div className="mt-6 p-4 bg-blue-500/10 border border-blue-500/20 rounded-lg">
          <div className="flex items-start space-x-3">
            <svg className="w-5 h-5 text-blue-400 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <div>
              <h4 className="font-medium text-blue-300 mb-1">Automated Generation Benefits</h4>
              <ul className="text-sm text-blue-200/80 space-y-1">
                <li>• Uses existing SA-CCR calculations and SIMM results</li>
                <li>• Eliminates manual file uploads and processing errors</li>
                <li>• Real-time calculation based on current trade positions</li>
                <li>• Automatic netting set grouping by CCP and currency</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Duplicate Warning Notification */}
      {duplicateWarning && (
        <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-4 animate-pulse">
          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0">
              <svg className="w-6 h-6 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
              </svg>
            </div>
            <div className="flex-1">
              <h4 className="font-medium text-yellow-300 mb-1">Duplicate Statements Detected</h4>
              <p className="text-sm text-yellow-200/80">{duplicateWarning}</p>
              <p className="text-xs text-yellow-200/60 mt-2">
                Statements for this date already exist in the system. Please select a different date or delete the existing statements first.
              </p>
            </div>
            <button
              onClick={() => setDuplicateWarning(null)}
              className="flex-shrink-0 text-yellow-400/60 hover:text-yellow-400 transition-colors"
              aria-label="Dismiss notification"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
        </div>
      )}

      {/* Results Display */}
      {showResults && (
        <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
          <h3 className="text-lg font-semibold text-fd-text mb-6">Generated Statements ({generatedStatements.length})</h3>
          
          {generatedStatements.length === 0 ? (
            <div className="text-center py-8">
              <svg className="w-12 h-12 text-fd-text-muted mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
              </svg>
              <p className="text-fd-text-muted">No active positions found for the selected date</p>
            </div>
          ) : (
            <div className="space-y-4">
              {generatedStatements.map((statement, index) => (
                <div key={index} className="bg-fd-dark rounded-lg border border-fd-border p-4">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div>
                      <h4 className="font-medium text-fd-text mb-2">{statement.nettingSetId}</h4>
                      <div className="space-y-1 text-sm">
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">CCP:</span>
                          <span className="text-fd-text">{statement.ccpName}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">Account:</span>
                          <span className="text-fd-text">{statement.accountId}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">Trades:</span>
                          <span className="text-fd-text">{formatNumber(statement.tradeCount)}</span>
                        </div>
                      </div>
                    </div>

                    <div>
                      <h5 className="font-medium text-fd-text mb-2">Margin Amounts</h5>
                      <div className="space-y-1 text-sm">
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">Variation Margin:</span>
                          <span className={`text-fd-text ${statement.variationMarginNet < 0 ? 'text-red-400' : 'text-green-400'}`}>
                            {formatCurrency(statement.variationMarginNet, statement.currency)}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">Initial Margin:</span>
                          <span className="text-fd-text">
                            {formatCurrency(statement.initialMarginRequired, statement.currency)}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">Excess Margin:</span>
                          <span className="text-green-400">
                            {formatCurrency(statement.excessMargin, statement.currency)}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div>
                      <h5 className="font-medium text-fd-text mb-2">Calculation Details</h5>
                      <div className="space-y-1 text-sm">
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">Total Notional:</span>
                          <span className="text-fd-text">
                            {formatCurrency(statement.totalNotional, statement.currency)}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">Status:</span>
                          <span className="text-green-400">{statement.calculationStatus}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-fd-text-muted">Processing:</span>
                          <span className="text-fd-text-muted">{statement.processingTime}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default AutomatedStatementGenerator;