// Simulation service for Monte Carlo correlated credit simulation

export interface SimulationRequest {
  valuationDate: string;
  horizons: string[];
  paths: number;
  factorModel?: {
    type: string;
    systemicLoadingDefault?: number;
    sectorOverrides?: Record<string, number>;
    idOverrides?: Record<string, number>;
  };
  stochasticRecovery?: {
    enabled: boolean;
  };
  seed?: number;
  includePerPath?: boolean;
}

export interface HorizonMetrics {
  tenor: string;
  pAnyDefault?: number;  // camelCase from fixed backend
  panyDefault?: number;  // lowercase from current backend - for backwards compatibility
  expectedDefaults: number;
  loss: {
    mean: number;
    var95: number;
    var99: number;
    es97_5: number;
  };
  diversification: {
    sumStandaloneEl: number;
    portfolioEl: number;
    benefitPct: number;
  };
}

export interface Contributor {
  entity: string;
  marginalElPct: number;
  beta: number;
  standaloneEl: number;
}

export interface SimulationResponse {
  runId: string;
  portfolioId: number;
  valuationDate: string;
  paths: number;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETE' | 'FAILED' | 'CANCELED';
  seedUsed?: number;
  horizons?: HorizonMetrics[];
  contributors?: Contributor[];
  settings?: {
    stochasticRecovery: boolean;
  };
  errorMessage?: string;
  runtimeMs?: number;
}

class SimulationService {
  private readonly baseUrl = '/api/credit-simulation';

  /**
   * Submit a new simulation run
   */
  async runSimulation(portfolioId: number, request: SimulationRequest): Promise<SimulationResponse> {
    const response = await fetch(`${this.baseUrl}/portfolio/${portfolioId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Failed to start simulation: ${error}`);
    }

    return response.json();
  }

  /**
   * Get simulation status and results
   */
  async getSimulationResults(runId: string): Promise<SimulationResponse> {
    const response = await fetch(`${this.baseUrl}/runs/${runId}`);

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Simulation not found');
      }
      const error = await response.text();
      throw new Error(`Failed to get simulation results: ${error}`);
    }

    return response.json();
  }

  /**
   * Cancel a running simulation
   */
  async cancelSimulation(runId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/runs/${runId}`, {
      method: 'DELETE',
    });

    if (!response.ok && response.status !== 404) {
      const error = await response.text();
      throw new Error(`Failed to cancel simulation: ${error}`);
    }
  }

  /**
   * Download simulation results as JSON
   */
  downloadResults(simulation: SimulationResponse) {
    const dataStr = JSON.stringify(simulation, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    
    const link = document.createElement('a');
    link.href = url;
    link.download = `simulation-run-${simulation.runId}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}

export const simulationService = new SimulationService();
