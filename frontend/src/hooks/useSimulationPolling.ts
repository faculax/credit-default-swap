import { useEffect, useState, useRef } from 'react';
import { SimulationResponse, simulationService } from '../services/simulationService';

/**
 * Custom hook to poll simulation status until completion
 */
export function useSimulationPolling(runId: string | null, enabled: boolean = true) {
  const [simulation, setSimulation] = useState<SimulationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (!runId || !enabled) {
      return;
    }

    const fetchSimulation = async () => {
      try {
        setLoading(true);
        setError(null);
        const result = await simulationService.getSimulationResults(runId);
        setSimulation(result);

        // Stop polling if terminal state reached
        if (['COMPLETE', 'FAILED', 'CANCELED'].includes(result.status)) {
          if (intervalRef.current) {
            clearInterval(intervalRef.current);
            intervalRef.current = null;
          }
        }
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to fetch simulation';
        setError(errorMessage);
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
          intervalRef.current = null;
        }
      } finally {
        setLoading(false);
      }
    };

    // Initial fetch
    fetchSimulation();

    // Set up polling interval (every 2 seconds)
    intervalRef.current = setInterval(fetchSimulation, 2000);

    // Cleanup on unmount or when runId changes
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [runId, enabled]);

  return { simulation, loading, error };
}
