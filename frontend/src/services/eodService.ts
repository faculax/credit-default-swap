import { apiUrl } from '../config/api';
import {
  EodValuationJob,
  EodJobSummary,
  TriggerJobRequest,
  TriggerJobResponse,
} from '../types/accounting';

class EodService {
  /**
   * Trigger a new EOD valuation job
   */
  async triggerValuationJob(request: TriggerJobRequest): Promise<TriggerJobResponse> {
    const response = await fetch(apiUrl('/eod/valuation-jobs/trigger'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      // Try to get the error message from the response body first
      let errorMessage = response.statusText;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorData.error || response.statusText;
      } catch (parseError) {
        // Couldn't parse JSON, use status text
      }
      
      // Handle 409 Conflict specially - this means a job already exists
      if (response.status === 409) {
        throw new Error(`CONFLICT_409: ${errorMessage}`);
      }
      
      throw new Error(`Failed to trigger EOD job: ${errorMessage}`);
    }

    return response.json();
  }

  /**
   * Get a specific EOD valuation job by ID
   */
  async getJob(jobId: string): Promise<EodValuationJob> {
    const response = await fetch(apiUrl(`/eod/valuation-jobs/${jobId}`));

    if (!response.ok) {
      throw new Error(`Failed to fetch EOD job: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get the most recent EOD valuation job
   */
  async getLatestJob(): Promise<EodValuationJob | null> {
    const response = await fetch(apiUrl('/eod/valuation-jobs/latest'));

    if (response.status === 404) {
      return null;
    }

    if (!response.ok) {
      throw new Error(`Failed to fetch latest EOD job: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get EOD job for a specific valuation date
   */
  async getJobByDate(valuationDate: string): Promise<EodValuationJob | null> {
    const response = await fetch(apiUrl(`/eod/valuation-jobs/date/${valuationDate}`));

    if (response.status === 404) {
      return null;
    }

    if (!response.ok) {
      throw new Error(`Failed to fetch EOD job for date: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get list of recent EOD jobs
   */
  async getRecentJobs(limit: number = 10): Promise<EodJobSummary[]> {
    const response = await fetch(apiUrl(`/eod/valuation-jobs/recent?limit=${limit}`));

    if (!response.ok) {
      throw new Error(`Failed to fetch recent EOD jobs: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Cancel a running EOD job
   */
  async cancelJob(jobId: string): Promise<{ message: string; status: string }> {
    const response = await fetch(apiUrl(`/eod/valuation-jobs/${jobId}/cancel`), {
      method: 'POST',
    });

    if (!response.ok) {
      throw new Error(`Failed to cancel EOD job: ${response.statusText}`);
    }

    return response.json();
  }
}

const eodService = new EodService();
export default eodService;