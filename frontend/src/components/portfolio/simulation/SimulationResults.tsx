import React from 'react';
import { SimulationResponse } from '../../../services/simulationService';
import SimulationStatusBadge from './SimulationStatusBadge';
import MetricsCard from './MetricsCard';
import ContributorsTable from './ContributorsTable';

interface SimulationResultsProps {
  simulation: SimulationResponse;
  onCancel: () => void;
  onDownload: () => void;
  onReset: () => void;
}

const SimulationResults: React.FC<SimulationResultsProps> = ({
  simulation,
  onCancel,
  onDownload,
  onReset,
}) => {
  const isRunning = simulation.status === 'RUNNING' || simulation.status === 'QUEUED';
  const isComplete = simulation.status === 'COMPLETE';
  const isFailed = simulation.status === 'FAILED';

  // For display, use the horizon with most data (prefer 5Y, then 3Y, then 1Y)
  const displayHorizon = simulation.horizons?.find(h => h.tenor === '5Y') 
    || simulation.horizons?.find(h => h.tenor === '3Y')
    || simulation.horizons?.[0];

  return (
    <div className="space-y-6">
      {/* Status Header */}
      <div className="bg-fd-darker rounded-lg border border-fd-border p-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-medium text-fd-text">Simulation Status</h3>
            <p className="text-sm text-fd-text-muted mt-1">Run ID: {simulation.runId}</p>
          </div>
          <SimulationStatusBadge status={simulation.status} />
        </div>

        {simulation.errorMessage && (
          <div className="mt-4 p-3 bg-red-500/10 border border-red-500/20 rounded text-red-400 text-sm">
            <strong>Error:</strong> {simulation.errorMessage}
          </div>
        )}

        <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <span className="text-fd-text-muted">Valuation Date:</span>
            <p className="text-fd-text font-medium">{simulation.valuationDate || 'N/A'}</p>
          </div>
          <div>
            <span className="text-fd-text-muted">Paths:</span>
            <p className="text-fd-text font-medium">{simulation.paths?.toLocaleString() || 'N/A'}</p>
          </div>
          <div>
            <span className="text-fd-text-muted">Horizons:</span>
            <p className="text-fd-text font-medium">
              {simulation.horizons?.map(h => h.tenor).join(', ') || 'N/A'}
            </p>
          </div>
          <div>
            <span className="text-fd-text-muted">Seed:</span>
            <p className="text-fd-text font-medium">
              {simulation.seedUsed ?? 'Random'}
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="mt-4 flex gap-3">
          {isRunning && (
            <button
              onClick={onCancel}
              className="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded text-sm font-medium transition-colors"
            >
              Cancel Simulation
            </button>
          )}
          {isComplete && (
            <>
              <button
                onClick={onDownload}
                className="bg-fd-green hover:bg-fd-green-hover text-fd-dark px-4 py-2 rounded text-sm font-medium transition-colors"
              >
                Download Results (JSON)
              </button>
              <button
                onClick={onReset}
                className="bg-fd-border hover:bg-fd-text-muted text-fd-text px-4 py-2 rounded text-sm font-medium transition-colors"
              >
                Run New Simulation
              </button>
            </>
          )}
          {isFailed && (
            <button
              onClick={onReset}
              className="bg-fd-border hover:bg-fd-text-muted text-fd-text px-4 py-2 rounded text-sm font-medium transition-colors"
            >
              Try Again
            </button>
          )}
        </div>
      </div>

      {/* Metrics Grid - Show ALL Horizons */}
      {isComplete && simulation.horizons && simulation.horizons.length > 0 && (
        <>
          {simulation.horizons.map((horizon, idx) => (
            <div key={horizon.tenor}>
              <h3 className="text-lg font-medium text-fd-text mb-4">
                Risk Metrics ({horizon.tenor})
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <MetricsCard
                  label="Probability of Any Default"
                  value={`${((horizon.pAnyDefault || horizon.panyDefault || 0) * 100).toFixed(2)}%`}
                  description="Likelihood that at least one entity defaults"
                />
                <MetricsCard
                  label="Expected Loss"
                  value={`$${horizon.loss.mean.toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}`}
                  description="Mean loss across all scenarios"
                />
                <MetricsCard
                  label="VaR 95%"
                  value={`$${horizon.loss.var95.toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}`}
                  description="Loss not exceeded in 95% of scenarios"
                />
                <MetricsCard
                  label="VaR 99%"
                  value={`$${horizon.loss.var99.toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}`}
                  description="Loss not exceeded in 99% of scenarios"
                />
                <MetricsCard
                  label="Expected Shortfall 97.5%"
                  value={`$${horizon.loss.es97_5.toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}`}
                  description="Mean loss in worst 2.5% of scenarios"
                />
                <MetricsCard
                  label="Diversification Benefit"
                  value={`${horizon.diversification.benefitPct.toFixed(2)}%`}
                  description="Risk reduction from portfolio diversification"
                />
              </div>
            </div>
          ))}

          {/* Contributors Table */}
          {simulation.contributors && simulation.contributors.length > 0 && (
            <div>
              <h3 className="text-lg font-medium text-fd-text mb-4">
                Top Contributors to Expected Loss
              </h3>
              <ContributorsTable contributors={simulation.contributors} />
            </div>
          )}
        </>
      )}

      {/* Loading State */}
      {isRunning && (
        <div className="bg-fd-darker rounded-lg border border-fd-border p-8 text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-fd-green mx-auto mb-4"></div>
          <p className="text-fd-text-muted">
            {simulation.status === 'QUEUED' ? 'Simulation queued...' : 'Running Monte Carlo simulation...'}
          </p>
          <p className="text-fd-text-muted text-sm mt-2">
            This may take a minute or two depending on the number of paths
          </p>
        </div>
      )}
    </div>
  );
};

export default SimulationResults;
