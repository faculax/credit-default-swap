import React, { useState } from 'react';
import { CDSTradeResponse } from '../../services/cdsTradeService';
import { stressTestService, StressImpactResult, StressScenarioRequest } from '../../services/stressTestService';
import StressLoadingScreen from './StressLoadingScreen';
import JsonViewer from './JsonViewer';
import WaterfallChart from './WaterfallChart';

interface StressPanelProps {
  trade: CDSTradeResponse;
}

const StressPanel: React.FC<StressPanelProps> = ({ trade }) => {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<StressImpactResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [executionTimeMs, setExecutionTimeMs] = useState<number | null>(null);
  
  // UI state
  const [tableExpanded, setTableExpanded] = useState(true);
  const [showRawJson, setShowRawJson] = useState(false);
  const [waterfallFullscreen, setWaterfallFullscreen] = useState(false);
  
  // Configuration state
  const [selectedRecoveryRates, setSelectedRecoveryRates] = useState<number[]>([30, 20]);
  const [selectedSpreadShifts, setSelectedSpreadShifts] = useState<number[]>([50, 100, 200]);
  const [selectedYieldShifts, setSelectedYieldShifts] = useState<number[]>([]);
  const [combinedEnabled, setCombinedEnabled] = useState(false);

  // Available options
  const recoveryOptions = [40, 30, 20, 10];
  const spreadOptions = [-300, -200, -100, -50, -25, 25, 50, 100, 200, 300];
  const yieldOptions = [10, 25, 50, 100];
  const combinedOptions = [
    { label: 'Independent', value: false },
    { label: 'Combined', value: true }
  ];

  const toggleValue = (value: number, selected: number[], setter: (v: number[]) => void) => {
    if (selected.includes(value)) {
      setter(selected.filter((v) => v !== value));
    } else {
      setter([...selected, value].sort((a, b) => a - b));
    }
  };

  const calculateScenarioCount = () => {
    let count = 0;
    
    // Always count individual scenarios
    count += selectedRecoveryRates.length;
    count += selectedSpreadShifts.length;
    count += selectedYieldShifts.length;
    
    // If combined mode is enabled, add combined scenarios
    if (combinedEnabled) {
      // Need at least 2 dimensions to run combined scenarios
      const dims = [];
      if (selectedRecoveryRates.length > 0) dims.push(selectedRecoveryRates.length);
      if (selectedSpreadShifts.length > 0) dims.push(selectedSpreadShifts.length);
      if (selectedYieldShifts.length > 0) dims.push(selectedYieldShifts.length);
      
      if (dims.length >= 2) {
        // Backend runs full 3D matrix for combined scenarios
        const r = selectedRecoveryRates.length || 1;
        const s = selectedSpreadShifts.length || 1;
        const y = selectedYieldShifts.length || 1;
        
        count += r * s * y;
      }
    }
    
    return count;
  };

  const handleRunStress = async () => {
    const scenarioCount = calculateScenarioCount();
    if (scenarioCount === 0) {
      setError('Please select at least one scenario');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);
    setExecutionTimeMs(null);

    const startTime = Date.now();

    try {
      const request: StressScenarioRequest = {
        tradeId: trade.id,
        recoveryRates: selectedRecoveryRates.length > 0 ? selectedRecoveryRates : undefined,
        spreadShifts: selectedSpreadShifts.length > 0 ? selectedSpreadShifts : undefined,
        yieldCurveShifts: selectedYieldShifts.length > 0 ? selectedYieldShifts : undefined,
        combined: combinedEnabled,
      };

      const stressResult = await stressTestService.analyzeStress(request);
      const endTime = Date.now();
      setExecutionTimeMs(endTime - startTime);
      setResult(stressResult);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to run stress analysis');
      console.error('Stress analysis error:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number | null | undefined, currency: string = 'USD'): string => {
    if (value === null || value === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  if (loading) {
    return <StressLoadingScreen scenarioCount={calculateScenarioCount()} />;
  }

  return (
    <div className="space-y-6">
      {/* Configuration Panel */}
      <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
        <h3 className="text-lg font-semibold text-fd-text mb-4">Stress Scenario Configuration</h3>

        {/* Recovery Rates */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-fd-text mb-2">
            Recovery Rates (%) <span className="text-fd-text-muted font-normal">— Current: {trade.recoveryRate}%</span>
          </label>
          <div className="flex flex-wrap gap-2">
            {recoveryOptions.map((rate) => (
              <button
                key={rate}
                onClick={() => toggleValue(rate, selectedRecoveryRates, setSelectedRecoveryRates)}
                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  selectedRecoveryRates.includes(rate)
                    ? 'bg-fd-green text-fd-dark'
                    : 'bg-fd-dark text-fd-text hover:bg-fd-border border border-fd-border'
                }`}
              >
                {rate}%
              </button>
            ))}
          </div>
        </div>

        {/* Spread Shifts */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-fd-text mb-2">
            Spread Shifts (bp) <span className="text-fd-text-muted font-normal">— Current: {trade.spread.toFixed(0)}bp</span>
          </label>
          <div className="flex flex-col gap-2">
            {/* Positive spreads */}
            <div className="flex flex-wrap gap-2">
              {spreadOptions.filter(s => s > 0).map((shift) => (
                <button
                  key={shift}
                  onClick={() => toggleValue(shift, selectedSpreadShifts, setSelectedSpreadShifts)}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                    selectedSpreadShifts.includes(shift)
                      ? 'bg-fd-green text-fd-dark'
                      : 'bg-fd-dark text-fd-text hover:bg-fd-border border border-fd-border'
                  }`}
                >
                  +{shift}bp
                </button>
              ))}
            </div>
            {/* Negative spreads */}
            <div className="flex flex-wrap gap-2">
              {spreadOptions.filter(s => s < 0).sort((a, b) => b - a).map((shift) => (
                <button
                  key={shift}
                  onClick={() => toggleValue(shift, selectedSpreadShifts, setSelectedSpreadShifts)}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                    selectedSpreadShifts.includes(shift)
                      ? 'bg-fd-green text-fd-dark'
                      : 'bg-fd-dark text-fd-text hover:bg-fd-border border border-fd-border'
                  }`}
                >
                  {shift}bp
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Yield Curve Shifts */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-fd-text mb-2">
            Yield Curve Shifts (bp)
          </label>
          <div className="flex flex-wrap gap-2">
            {yieldOptions.map((shift) => (
              <button
                key={shift}
                onClick={() => toggleValue(shift, selectedYieldShifts, setSelectedYieldShifts)}
                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  selectedYieldShifts.includes(shift)
                    ? 'bg-fd-green text-fd-dark'
                    : 'bg-fd-dark text-fd-text hover:bg-fd-border border border-fd-border'
                }`}
              >
                +{shift}bp
              </button>
            ))}
          </div>
        </div>

        {/* Combined Scenarios */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-fd-text mb-2">
            Scenario Mode
          </label>
          <div className="flex flex-wrap gap-2">
            {combinedOptions.map((option) => (
              <button
                key={option.label}
                onClick={() => setCombinedEnabled(option.value)}
                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  combinedEnabled === option.value
                    ? 'bg-fd-green text-fd-dark'
                    : 'bg-fd-dark text-fd-text hover:bg-fd-border border border-fd-border'
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>
          <p className="text-xs text-fd-text-muted mt-2">
            {combinedEnabled 
              ? 'Combined: Tests individual scenarios PLUS all recovery × spread combinations'
              : 'Independent: Tests only individual recovery rates, spread shifts, and yield shifts'}
          </p>
        </div>

        {/* Run Button */}
        <div className="flex items-center justify-between pt-4 border-t border-fd-border">
          <div className="text-sm text-fd-text-muted">
            {calculateScenarioCount()} scenario{calculateScenarioCount() !== 1 ? 's' : ''} selected
          </div>
          <button
            onClick={handleRunStress}
            disabled={calculateScenarioCount() === 0}
            className="px-6 py-2 bg-fd-green text-fd-dark font-medium rounded hover:bg-fd-green-hover transition-colors disabled:bg-fd-text-muted/50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            Run Stress Analysis
          </button>
        </div>

        {error && (
          <div className="mt-4 p-3 bg-red-900/20 border border-red-700 rounded text-sm text-red-400">
            {error}
          </div>
        )}
      </div>

      {/* Results */}
      {result && (
        <div className="space-y-6">
          {/* Summary Table */}
          <div className="bg-fd-darker rounded-lg border border-fd-border overflow-hidden">
            <div className="bg-fd-dark px-6 py-4 border-b border-fd-border flex items-center justify-between">
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setTableExpanded(!tableExpanded)}
                  className="text-fd-text hover:text-fd-green transition-colors"
                >
                  <svg 
                    className={`w-5 h-5 transition-transform ${tableExpanded ? 'rotate-90' : ''}`} 
                    fill="none" 
                    stroke="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
                <h3 className="text-lg font-semibold text-fd-text">Scenario Impact Analysis</h3>
              </div>
              {executionTimeMs !== null && (
                <span className="text-sm text-fd-text-muted">
                  Calculated in {(executionTimeMs / 1000).toFixed(2)}s
                </span>
              )}
            </div>

            {tableExpanded && (
              <>
                {/* Base Case */}
                <div className="px-6 py-4 bg-fd-dark/50 border-b border-fd-border">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <span className="text-sm text-fd-text-muted">BASE CASE: NPV =</span>
                      <span className="ml-2 text-lg font-semibold text-fd-text">
                        {formatCurrency(result.baseNpv, result.currency)}
                      </span>
                    </div>
                    <div>
                      <span className="text-sm text-fd-text-muted">JTD =</span>
                      <span className="ml-2 text-lg font-semibold text-fd-text">
                        {formatCurrency(result.baseJtd, result.currency)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Scenarios Table */}
                <div className="overflow-x-auto max-h-96 overflow-y-auto">
                  <table className="min-w-full divide-y divide-fd-border">
                    <thead className="bg-fd-dark sticky top-0 z-10">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Scenario
                        </th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          ΔNPV (Mark-to-Market)
                        </th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          ΔJTD
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-fd-darker divide-y divide-fd-border">
                      {result.scenarios.map((scenario, index) => (
                        <tr key={index} className="hover:bg-fd-dark transition-colors">
                          <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                            {scenario.scenarioName}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                            <span className={scenario.severe ? 'text-red-400 font-semibold' : 'text-fd-text'}>
                              {formatCurrency(scenario.deltaNpv, result.currency)}
                              {scenario.severe && ' ⚠️'}
                            </span>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-fd-text">
                            {formatCurrency(scenario.deltaJtd, result.currency)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>

          {/* Waterfall Chart */}
          <div className="bg-fd-darker rounded-lg border border-fd-border overflow-hidden">
            <div className="bg-fd-dark px-6 py-4 border-b border-fd-border flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-fd-text">Waterfall Chart: Scenario Contribution Analysis</h3>
                <p className="text-xs text-fd-text-muted mt-1">
                  Each scenario shows its independent impact from the base case NPV
                  {result.scenarios.length > 10 && (
                    <span className="ml-1 text-fd-green">
                      • Showing unique single-factor scenarios + top combined scenarios (out of {result.scenarios.length} total)
                    </span>
                  )}
                </p>
              </div>
              <button
                onClick={() => setWaterfallFullscreen(true)}
                className="px-4 py-2 bg-fd-green text-fd-dark font-medium rounded hover:bg-fd-green-hover transition-colors flex items-center gap-2"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
                </svg>
                Fullscreen
              </button>
            </div>
            <WaterfallChart 
              baseNpv={result.baseNpv}
              scenarios={result.scenarios}
              currency={result.currency}
              buySellProtection={result.buySellProtection}
            />
          </div>

          {/* Raw JSON Response Toggle */}
          <div className="bg-fd-darker rounded-lg border border-fd-border overflow-hidden">
            <button
              onClick={() => setShowRawJson(!showRawJson)}
              className="w-full bg-fd-dark px-6 py-4 border-b border-fd-border flex items-center justify-between hover:bg-fd-border transition-colors"
            >
              <div className="flex items-center gap-3">
                <svg 
                  className={`w-5 h-5 text-fd-text transition-transform ${showRawJson ? 'rotate-90' : ''}`} 
                  fill="none" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
                </svg>
                <h3 className="text-lg font-semibold text-fd-text">Raw Stress Analysis Response</h3>
              </div>
              <span className="text-xs text-fd-text-muted">
                {showRawJson ? 'Click to hide' : 'Click to show'}
              </span>
            </button>
            {showRawJson && (
              <div className="p-6">
                <JsonViewer data={result} title="" />
              </div>
            )}
          </div>

          {/* Fullscreen Waterfall Modal */}
          {waterfallFullscreen && (
            <div className="fixed inset-0 bg-fd-darker z-50 flex flex-col">
              <div className="bg-fd-dark border-b border-fd-border px-6 py-4 flex items-center justify-between">
                <h2 className="text-xl font-semibold text-fd-text">Waterfall Chart: Scenario Contribution Analysis</h2>
                <button
                  onClick={() => setWaterfallFullscreen(false)}
                  className="px-4 py-2 bg-fd-green text-fd-dark font-medium rounded hover:bg-fd-green-hover transition-colors flex items-center gap-2"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                  Close
                </button>
              </div>
              <div className="flex-1 p-6 overflow-auto bg-fd-darker">
                <WaterfallChart 
                  baseNpv={result.baseNpv}
                  scenarios={result.scenarios}
                  currency={result.currency}
                  buySellProtection={result.buySellProtection}
                  fullscreen={true}
                />
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default StressPanel;
