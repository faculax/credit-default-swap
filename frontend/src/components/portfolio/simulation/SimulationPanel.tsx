import React, { useState } from 'react';
import { simulationService, SimulationRequest } from '../../../services/simulationService';
import { useSimulationPolling } from '../../../hooks/useSimulationPolling';
import SimulationConfigForm from './SimulationConfigForm';
import SimulationResults from './SimulationResults';
import MetricsGlossaryModal from './MetricsGlossaryModal';

interface SimulationPanelProps {
  portfolioId: number;
}

const SimulationPanel: React.FC<SimulationPanelProps> = ({ portfolioId }) => {
  const [runId, setRunId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [showGlossary, setShowGlossary] = useState(false);

  const {
    simulation,
    loading: polling,
    error: pollingError,
  } = useSimulationPolling(runId, !!runId);

  const handleSubmit = async (request: SimulationRequest) => {
    try {
      setIsSubmitting(true);
      setSubmitError(null);

      const response = await simulationService.runSimulation(portfolioId, request);
      setRunId(response.runId);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to start simulation';
      setSubmitError(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = async () => {
    if (runId && window.confirm('Are you sure you want to cancel this simulation?')) {
      try {
        await simulationService.cancelSimulation(runId);
      } catch (err) {
        console.error('Failed to cancel simulation:', err);
      }
    }
  };

  const handleDownload = () => {
    if (simulation) {
      simulationService.downloadResults(simulation);
    }
  };

  const handleReset = () => {
    setRunId(null);
    setSubmitError(null);
  };

  return (
    <div className="space-y-6">
      {/* Header with Help Button */}
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-lg font-semibold text-fd-text">Monte Carlo Simulation</h3>
          <p className="text-sm text-fd-text-muted mt-1">
            Correlated default risk analysis with portfolio metrics
          </p>
        </div>
        <button
          onClick={() => setShowGlossary(true)}
          className="flex items-center space-x-2 px-3 py-2 text-sm font-medium text-fd-green hover:text-fd-green-hover transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <span>Help & Glossary</span>
        </button>
      </div>

      {/* Error Display */}
      {(submitError || pollingError) && (
        <div className="bg-red-900/20 border border-red-500/50 rounded-md p-4">
          <div className="flex">
            <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clipRule="evenodd"
              />
            </svg>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-400">Error</h3>
              <div className="mt-2 text-sm text-red-300">
                <p>{submitError || pollingError}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Configuration Form or Results */}
      {!runId || !simulation ? (
        <SimulationConfigForm onSubmit={handleSubmit} isSubmitting={isSubmitting} />
      ) : (
        <SimulationResults
          simulation={simulation}
          onCancel={handleCancel}
          onDownload={handleDownload}
          onReset={handleReset}
        />
      )}

      {/* Glossary Modal */}
      <MetricsGlossaryModal isOpen={showGlossary} onClose={() => setShowGlossary(false)} />
    </div>
  );
};

export default SimulationPanel;
