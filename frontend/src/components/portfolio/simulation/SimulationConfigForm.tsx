import React, { useState } from 'react';
import { SimulationRequest } from '../../../services/simulationService';

interface SimulationConfigFormProps {
  onSubmit: (request: SimulationRequest) => void;
  isSubmitting: boolean;
}

const SimulationConfigForm: React.FC<SimulationConfigFormProps> = ({ onSubmit, isSubmitting }) => {
  const today = new Date().toISOString().split('T')[0];

  const [valuationDate, setValuationDate] = useState(today);
  const [paths, setPaths] = useState(20000);
  const [selectedHorizons, setSelectedHorizons] = useState<string[]>(['3Y', '5Y']);
  const [beta, setBeta] = useState(0.35);
  const [seed, setSeed] = useState<string>('');

  const availableHorizons = ['1Y', '2Y', '3Y', '5Y', '7Y', '10Y'];
  const pathOptions = [10000, 20000, 50000, 100000];

  const toggleHorizon = (horizon: string) => {
    setSelectedHorizons((prev) =>
      prev.includes(horizon) ? prev.filter((h) => h !== horizon) : [...prev, horizon]
    );
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (selectedHorizons.length === 0) {
      alert('Please select at least one horizon');
      return;
    }

    const request: SimulationRequest = {
      valuationDate,
      horizons: selectedHorizons.sort((a, b) => {
        const aYears = parseInt(a);
        const bYears = parseInt(b);
        return aYears - bYears;
      }),
      paths,
      factorModel: {
        type: 'ONE_FACTOR',
        systemicLoadingDefault: beta,
      },
      stochasticRecovery: {
        enabled: false,
      },
      seed: seed ? parseInt(seed) : undefined,
    };

    onSubmit(request);
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-fd-darker rounded-lg border border-fd-border p-6 space-y-6"
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Valuation Date */}
        <div>
          <label htmlFor="valuationDate" className="block text-sm font-medium text-fd-text mb-2">
            Valuation Date
          </label>
          <input
            type="date"
            id="valuationDate"
            value={valuationDate}
            onChange={(e) => setValuationDate(e.target.value)}
            className="w-full bg-fd-dark border border-fd-border rounded-md px-3 py-2 text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green"
            required
          />
        </div>

        {/* Paths */}
        <div>
          <label htmlFor="paths" className="block text-sm font-medium text-fd-text mb-2">
            Monte Carlo Paths
          </label>
          <select
            id="paths"
            value={paths}
            onChange={(e) => setPaths(parseInt(e.target.value))}
            className="w-full bg-fd-dark border border-fd-border rounded-md px-3 py-2 text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green"
          >
            {pathOptions.map((option) => (
              <option key={option} value={option}>
                {option.toLocaleString()} paths
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Horizons */}
      <div>
        <label className="block text-sm font-medium text-fd-text mb-2">
          Horizons (select one or more)
        </label>
        <div className="flex flex-wrap gap-2">
          {availableHorizons.map((horizon) => (
            <button
              key={horizon}
              type="button"
              onClick={() => toggleHorizon(horizon)}
              className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                selectedHorizons.includes(horizon)
                  ? 'bg-fd-green text-fd-dark'
                  : 'bg-fd-dark text-fd-text hover:bg-fd-border'
              }`}
            >
              {horizon}
            </button>
          ))}
        </div>
      </div>

      {/* Beta (Systemic Loading) */}
      <div>
        <label htmlFor="beta" className="block text-sm font-medium text-fd-text mb-2">
          Default Systemic Loading (β) - {beta.toFixed(2)}
        </label>
        <input
          type="range"
          id="beta"
          min="0"
          max="0.95"
          step="0.05"
          value={beta}
          onChange={(e) => setBeta(parseFloat(e.target.value))}
          className="w-full"
        />
        <div className="flex justify-between text-xs text-fd-text-muted mt-1">
          <span>Low Correlation (0.0)</span>
          <span>High Correlation (0.95)</span>
        </div>
      </div>

      {/* Seed (Optional) */}
      <div>
        <label htmlFor="seed" className="block text-sm font-medium text-fd-text mb-2">
          Random Seed (optional - for reproducibility)
        </label>
        <input
          type="number"
          id="seed"
          value={seed}
          onChange={(e) => setSeed(e.target.value)}
          placeholder="Leave blank for random seed"
          className="w-full bg-fd-dark border border-fd-border rounded-md px-3 py-2 text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green"
        />
        <p className="text-xs text-fd-text-muted mt-1">
          Using the same seed with identical inputs produces identical results
        </p>
      </div>

      {/* Submit Button */}
      <div className="flex justify-end pt-4">
        <button
          type="submit"
          disabled={isSubmitting || selectedHorizons.length === 0}
          className="bg-fd-green hover:bg-fd-green-hover text-fd-dark font-medium py-2 px-6 rounded transition-colors disabled:bg-fd-text-muted/50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? 'Starting Simulation...' : 'Run Simulation'}
        </button>
      </div>
    </form>
  );
};

export default SimulationConfigForm;
