import React, { useState, useEffect } from 'react';
import eodService from '../services/eodService';
import {
  EodValuationJob,
  EodJobSummary,
  JobStatus,
  StepStatus,
} from '../types/accounting';

const EodJobMonitor: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [currentJob, setCurrentJob] = useState<EodValuationJob | null>(null);
  const [recentJobs, setRecentJobs] = useState<EodJobSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [isTriggeringJob, setIsTriggeringJob] = useState(false);

  const fetchCurrentJob = React.useCallback(async () => {
    if (!selectedDate) return;

    setLoading(true);
    setError(null);

    try {
      const job = await eodService.getJobByDate(selectedDate);
      setCurrentJob(job);
    } catch (err) {
      console.error('Error fetching EOD job:', err);
      setError('Failed to fetch EOD job data.');
      setCurrentJob(null);
    } finally {
      setLoading(false);
    }
  }, [selectedDate]);

  const fetchRecentJobs = React.useCallback(async () => {
    try {
      const jobs = await eodService.getRecentJobs(10);
      setRecentJobs(jobs);
    } catch (err) {
      console.error('Error fetching recent jobs:', err);
    }
  }, []);

  useEffect(() => {
    fetchCurrentJob();
    fetchRecentJobs();
  }, [fetchCurrentJob, fetchRecentJobs]);

  useEffect(() => {
    if (!autoRefresh) return;

    const interval = setInterval(() => {
      if (currentJob && currentJob.status === JobStatus.IN_PROGRESS) {
        fetchCurrentJob();
      }
    }, 5000); // Refresh every 5 seconds

    return () => clearInterval(interval);
  }, [autoRefresh, currentJob, fetchCurrentJob]);

  const handleTriggerJob = async () => {
    // Check if a job already exists for this date
    if (currentJob) {
      const proceed = window.confirm(
        `A job already exists for ${selectedDate} (Status: ${currentJob.status}).\n\n` +
        'Do you want to trigger a new job anyway? This may fail if the existing job is not completed.'
      );
      if (!proceed) return;
    }

    const dryRun = window.confirm(
      `Trigger EOD Valuation Job for ${selectedDate}?\n\n` +
      'Click OK for DRY RUN (no database changes)\n' +
      'Click Cancel for LIVE RUN'
    );

    setIsTriggeringJob(true);
    setError(null);

    try {
      const result = await eodService.triggerValuationJob({
        valuationDate: selectedDate,
        dryRun,
      });

      alert(`Success! Job ${result.jobId} triggered for ${selectedDate}.`);
      setAutoRefresh(true);
      await fetchCurrentJob();
      await fetchRecentJobs();
    } catch (err) {
      console.error('Error triggering job:', err);
      const errorMessage = err instanceof Error ? err.message : 'Unknown error occurred';
      
      // Handle 409 Conflict specifically
      if (errorMessage.includes('CONFLICT_409') || errorMessage.includes('Conflict') || errorMessage.includes('409')) {
        const friendlyMessage = `A job already exists for ${selectedDate}. Please wait for it to complete or view the existing job below.`;
        setError(friendlyMessage);
        alert(
          `❌ Job Already Exists\n\n` +
          `An EOD job for ${selectedDate} is already running or completed.\n\n` +
          `Please check the "Current Job" section below to view its status.\n\n` +
          `Tip: Refresh the page to see the latest job status.`
        );
        // Try to fetch and display the existing job
        await fetchCurrentJob();
      } else {
        setError(errorMessage);
        alert(`Failed to trigger job: ${errorMessage}`);
      }
    } finally {
      setIsTriggeringJob(false);
    }
  };

  const handleCancelJob = async () => {
    if (!currentJob || !window.confirm('Cancel this EOD job?')) return;

    try {
      await eodService.cancelJob(currentJob.jobId);
      alert('Job cancelled successfully');
      await fetchCurrentJob();
    } catch (err) {
      console.error('Error cancelling job:', err);
      alert('Failed to cancel job');
    }
  };

  const formatDuration = (seconds: number | undefined) => {
    if (!seconds) return '-';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  };

  const formatDateTime = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString();
  };

  const getStatusBadge = (status: JobStatus | StepStatus) => {
    const colors = {
      CREATED: 'bg-blue-400/20 text-blue-400',
      PENDING: 'bg-blue-400/20 text-blue-400',
      IN_PROGRESS: 'bg-yellow-400/20 text-yellow-400',
      COMPLETED: 'bg-green-400/20 text-green-400',
      FAILED: 'bg-red-400/20 text-red-400',
      CANCELLED: 'bg-gray-400/20 text-gray-400',
      SKIPPED: 'bg-gray-400/20 text-gray-400',
    };

    return (
      <span className={`px-2 py-1 text-xs rounded font-medium ${colors[status]}`}>
        {status}
      </span>
    );
  };

  const getStepProgress = (job: EodValuationJob) => {
    const completed = job.steps.filter(
      (s) => s.status === StepStatus.COMPLETED || s.status === StepStatus.SKIPPED
    ).length;
    return `${completed}/${job.steps.length}`;
  };

  return (
    <div className="p-6 max-w-screen-2xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-fd-text mb-2">EOD Valuation Job Monitor</h1>
        <p className="text-fd-muted">Monitor end-of-day valuation job execution and progress</p>
      </div>

      {/* Date Selector and Actions */}
      <div className="bg-fd-card border border-fd-border rounded-lg p-6 mb-6">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex-1 min-w-[200px]">
            <label htmlFor="job-date" className="block text-sm font-medium text-fd-text mb-2">
              Valuation Date
            </label>
            <input
              type="date"
              id="job-date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="w-full px-4 py-2 border border-fd-border rounded-md bg-fd-card text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-primary"
            />
          </div>

          <div className="flex gap-2 items-end">
            <button
              onClick={handleTriggerJob}
              disabled={isTriggeringJob || loading}
              className="px-4 py-2 bg-fd-primary text-white rounded-md hover:bg-fd-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isTriggeringJob ? 'Triggering...' : 'Trigger Job'}
            </button>

            {currentJob && currentJob.status === JobStatus.IN_PROGRESS && (
              <button
                onClick={handleCancelJob}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
              >
                Cancel Job
              </button>
            )}

            <button
              onClick={() => {
                fetchCurrentJob();
                fetchRecentJobs();
              }}
              disabled={loading}
              className="px-4 py-2 bg-fd-card border border-fd-border text-fd-text rounded-md hover:bg-fd-hover disabled:opacity-50 transition-colors"
            >
              Refresh
            </button>

            <label className="flex items-center gap-2 px-4 py-2 bg-fd-card border border-fd-border rounded-md text-fd-text cursor-pointer">
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
                className="rounded"
              />
              <span className="text-sm">Auto Refresh</span>
            </label>
          </div>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-900/20 border border-red-500 text-red-400 rounded-lg p-4 mb-6">
          {error}
        </div>
      )}

      {/* Current Job Details */}
      {currentJob ? (
        <div className="bg-fd-card border border-fd-border rounded-lg p-6 mb-6">
          <h2 className="text-xl font-bold text-fd-text mb-4">Current Job Details</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <div>
              <div className="text-sm text-fd-muted mb-1">Job ID</div>
              <div className="text-lg font-mono text-fd-text">{currentJob.jobId}</div>
            </div>

            <div>
              <div className="text-sm text-fd-muted mb-1">Status</div>
              <div>{getStatusBadge(currentJob.status)}</div>
            </div>

            <div>
              <div className="text-sm text-fd-muted mb-1">Progress</div>
              <div className="text-lg font-bold text-fd-text">{getStepProgress(currentJob)} Steps</div>
            </div>

            <div>
              <div className="text-sm text-fd-muted mb-1">Duration</div>
              <div className="text-lg font-bold text-fd-text">
                {formatDuration(currentJob.durationSeconds)}
              </div>
            </div>

            <div>
              <div className="text-sm text-fd-muted mb-1">Total Trades</div>
              <div className="text-lg font-bold text-fd-text">{currentJob.totalTrades}</div>
            </div>

            <div>
              <div className="text-sm text-fd-muted mb-1">Successful</div>
              <div className="text-lg font-bold text-green-400">{currentJob.successfulTrades}</div>
            </div>

            <div>
              <div className="text-sm text-fd-muted mb-1">Failed</div>
              <div className="text-lg font-bold text-red-400">{currentJob.failedTrades}</div>
            </div>

            <div>
              <div className="text-sm text-fd-muted mb-1">Mode</div>
              <div className="text-lg font-bold text-fd-text">
                {currentJob.dryRun ? 'DRY RUN' : 'LIVE'}
              </div>
            </div>
          </div>

          {/* Job Timeline */}
          <div className="mb-6">
            <div className="text-sm text-fd-muted mb-2">Start Time</div>
            <div className="text-sm text-fd-text">{formatDateTime(currentJob.startTime)}</div>
            {currentJob.endTime && (
              <>
                <div className="text-sm text-fd-muted mt-2 mb-2">End Time</div>
                <div className="text-sm text-fd-text">{formatDateTime(currentJob.endTime)}</div>
              </>
            )}
          </div>

          {/* Step Progress */}
          <div>
            <h3 className="text-lg font-bold text-fd-text mb-4">Execution Steps</h3>
            <div className="space-y-2">
              {currentJob.steps
                .sort((a, b) => a.stepNumber - b.stepNumber)
                .map((step) => (
                  <div
                    key={step.id}
                    className="flex items-center gap-4 p-4 border border-fd-border rounded-lg hover:bg-fd-hover"
                  >
                    <div className="flex-shrink-0 w-8 text-center">
                      <span className="text-lg font-bold text-fd-muted">{step.stepNumber}</span>
                    </div>

                    <div className="flex-1">
                      <div className="font-medium text-fd-text">{step.stepName}</div>
                      <div className="text-xs text-fd-muted mt-1">
                        Processed: {step.recordsProcessed} | Success: {step.recordsSuccessful} | Failed:{' '}
                        {step.recordsFailed}
                      </div>
                      {step.errorMessage && (
                        <div className="text-xs text-red-400 mt-1">{step.errorMessage}</div>
                      )}
                    </div>

                    <div className="flex-shrink-0">{getStatusBadge(step.status)}</div>

                    <div className="flex-shrink-0 w-20 text-right text-sm text-fd-muted">
                      {formatDuration(step.durationSeconds)}
                    </div>
                  </div>
                ))}
            </div>
          </div>
        </div>
      ) : loading ? (
        <div className="bg-fd-card border border-fd-border rounded-lg p-12 text-center text-fd-muted mb-6">
          Loading job data...
        </div>
      ) : (
        <div className="bg-fd-card border border-fd-border rounded-lg p-12 text-center text-fd-muted mb-6">
          No EOD job found for {selectedDate}. Trigger a new job to get started.
        </div>
      )}

      {/* Recent Jobs */}
      <div className="bg-fd-card border border-fd-border rounded-lg p-6">
        <h2 className="text-xl font-bold text-fd-text mb-4">Recent Jobs</h2>

        {recentJobs.length === 0 ? (
          <div className="text-center py-8 text-fd-muted">No recent jobs</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="text-left text-sm text-fd-muted border-b border-fd-border">
                  <th className="pb-3 pr-4">Job ID</th>
                  <th className="pb-3 pr-4">Date</th>
                  <th className="pb-3 pr-4">Status</th>
                  <th className="pb-3 pr-4">Progress</th>
                  <th className="pb-3 pr-4">Trades</th>
                  <th className="pb-3 pr-4">Duration</th>
                  <th className="pb-3 pr-4">Started</th>
                </tr>
              </thead>
              <tbody>
                {recentJobs.map((job) => (
                  <tr
                    key={job.jobId}
                    onClick={() => {
                      setSelectedDate(job.valuationDate);
                    }}
                    className="border-b border-fd-border hover:bg-fd-hover cursor-pointer"
                  >
                    <td className="py-3 pr-4">
                      <span className="font-mono text-sm">{job.jobId}</span>
                    </td>
                    <td className="py-3 pr-4">{job.valuationDate}</td>
                    <td className="py-3 pr-4">{getStatusBadge(job.status)}</td>
                    <td className="py-3 pr-4">
                      {job.completedSteps}/{job.totalSteps} steps
                    </td>
                    <td className="py-3 pr-4">{job.totalTrades}</td>
                    <td className="py-3 pr-4">{formatDuration(job.durationSeconds)}</td>
                    <td className="py-3 pr-4">{formatDateTime(job.startTime)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default EodJobMonitor;

