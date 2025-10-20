import React, { useState, useEffect } from 'react';
import { CDSTradeResponse } from '../../services/cdsTradeService';
import { lifecycleService } from '../../services/lifecycleService';

interface LifecycleTimelineProps {
  trade: CDSTradeResponse;
  onTradeUpdated?: () => void;
}

interface TimelineEvent {
  id: string;
  type: 'creation' | 'status_change';
  timestamp: string;
  title: string;
  description: string;
  status?: 'completed' | 'pending' | 'upcoming';
  icon: string;
  color: string;
}

const LifecycleTimeline: React.FC<LifecycleTimelineProps> = ({ trade, onTradeUpdated }) => {
  const [loading, setLoading] = useState(true);
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [showTerminateModal, setShowTerminateModal] = useState(false);
  const [terminationDate, setTerminationDate] = useState('');
  const [terminationReason, setTerminationReason] = useState('');
  const [terminating, setTerminating] = useState(false);
  const [terminateError, setTerminateError] = useState<string | null>(null);

  const loadLifecycleData = async () => {
    setLoading(true);
    try {
      // Build timeline events
      buildTimelineEvents();
    } catch (error) {
      console.error('Failed to load lifecycle data:', error);
      // Build basic timeline
      buildTimelineEvents();
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLifecycleData();
    // Set default termination date to today
    setTerminationDate(new Date().toISOString().split('T')[0]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trade.id]);

  const buildTimelineEvents = () => {
    const timelineEvents: TimelineEvent[] = [];

    // 1. Trade Creation - Use Trade Date instead of createdAt
    timelineEvents.push({
      id: 'creation',
      type: 'creation',
      timestamp: trade.tradeDate,
      title: 'Trade Created',
      description: `CDS-${trade.id} booked on ${formatDate(trade.tradeDate)}`,
      status: 'completed',
      icon: '🎯',
      color: 'rgb(0, 240, 0)' // fd-green
    });

    // 2. Status Changes
    if (trade.updatedAt && trade.updatedAt !== trade.createdAt) {
      timelineEvents.push({
        id: 'status_change',
        type: 'status_change',
        timestamp: trade.updatedAt,
        title: 'Status Updated',
        description: `Current status: ${trade.tradeStatus.replace(/_/g, ' ')}`,
        status: 'completed',
        icon: '🔄',
        color: 'rgb(0, 255, 195)'
      });
    }

    // Sort by timestamp
    timelineEvents.sort((a, b) => 
      new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    setEvents(timelineEvents);
  };

  const handleTerminateTrade = async () => {
    if (!terminationDate) {
      setTerminateError('Termination date is required');
      return;
    }

    setTerminating(true);
    setTerminateError(null);

    try {
      await lifecycleService.fullyTerminate(
        trade.id,
        terminationDate,
        terminationReason || 'Trader initiated termination'
      );
      
      setShowTerminateModal(false);
      
      // Notify parent component to refresh trade data
      if (onTradeUpdated) {
        onTradeUpdated();
      }
      
      // Reload lifecycle data
      loadLifecycleData();
    } catch (error) {
      console.error('Failed to terminate trade:', error);
      setTerminateError(error instanceof Error ? error.message : 'Failed to terminate trade');
    } finally {
      setTerminating(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-fd-green"></div>
        <span className="ml-3 text-fd-text-muted">Loading lifecycle timeline...</span>
      </div>
    );
  }

  return (
    <div className="mt-4">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-fd-text mb-2">Trade Lifecycle</h3>
          <p className="text-fd-text-muted text-sm">
            Visual timeline of key events and milestones for CDS-{trade.id}
          </p>
        </div>
        
        {/* Terminate Trade Button - Only show for ACTIVE trades */}
        {trade.tradeStatus === 'ACTIVE' && (
          <button
            onClick={() => setShowTerminateModal(true)}
            className="px-6 py-2 bg-red-600 hover:bg-red-700 text-white font-medium rounded transition-colors flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
            Terminate Trade
          </button>
        )}
      </div>

      {/* Terminate Trade Modal */}
      {showTerminateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-fd-darker border border-fd-border rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-semibold text-fd-text mb-4">Terminate Trade CDS-{trade.id}</h3>
            
            <div className="mb-4 p-3 bg-red-900/20 border border-red-700 rounded text-sm text-red-400">
              <strong>Warning:</strong> This action will fully terminate the trade. The notional will be reduced to zero.
            </div>

            <div className="space-y-4 mb-6">
              <div>
                <label className="block text-fd-text-muted text-sm mb-2">
                  Termination Date <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  value={terminationDate}
                  onChange={(e) => setTerminationDate(e.target.value)}
                  className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded text-fd-text focus:outline-none focus:border-fd-green"
                />
              </div>

              <div>
                <label className="block text-fd-text-muted text-sm mb-2">
                  Reason (optional)
                </label>
                <textarea
                  value={terminationReason}
                  onChange={(e) => setTerminationReason(e.target.value)}
                  placeholder="Enter reason for termination..."
                  rows={3}
                  className="w-full px-3 py-2 bg-fd-dark border border-fd-border rounded text-fd-text focus:outline-none focus:border-fd-green resize-none"
                />
              </div>
            </div>

            {terminateError && (
              <div className="mb-4 p-3 bg-red-900/20 border border-red-700 rounded text-sm text-red-400">
                {terminateError}
              </div>
            )}

            <div className="flex gap-3 justify-end">
              <button
                onClick={() => {
                  setShowTerminateModal(false);
                  setTerminateError(null);
                }}
                disabled={terminating}
                className="px-6 py-2 bg-fd-darker border border-fd-border text-fd-text rounded hover:bg-fd-dark transition-colors disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleTerminateTrade}
                disabled={terminating || !terminationDate}
                className="px-6 py-2 bg-red-600 hover:bg-red-700 text-white font-medium rounded transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {terminating ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                    Terminating...
                  </>
                ) : (
                  'Confirm Termination'
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Stats Summary */}
      <div className="grid grid-cols-2 gap-4 mb-8">
        <div className="bg-fd-dark rounded-lg p-4 border border-fd-border">
          <div className="text-fd-text-muted text-xs uppercase mb-1">Total Events</div>
          <div className="text-fd-text text-2xl font-bold">{events.length}</div>
        </div>
        <div className="bg-fd-dark rounded-lg p-4 border border-fd-border">
          <div className="text-fd-text-muted text-xs uppercase mb-1">Current Status</div>
          <div className="text-fd-green text-2xl font-bold">
            {trade.tradeStatus.replace(/_/g, ' ')}
          </div>
        </div>
      </div>

      {/* Timeline Visualization */}
      <div className="relative">
        {/* Vertical line */}
        <div className="absolute left-8 top-0 bottom-0 w-0.5 bg-fd-border"></div>

        {/* Timeline Events */}
        <div className="space-y-6">
          {events.map((event, index) => (
            <div key={event.id} className="relative pl-20">
              {/* Icon */}
              <div 
                className="absolute left-0 w-16 h-16 rounded-full flex items-center justify-center text-3xl border-4 border-fd-darker"
                style={{ backgroundColor: event.color }}
              >
                {event.icon}
              </div>

              {/* Arrow connector */}
              <svg 
                className="absolute left-16 top-6" 
                width="16" 
                height="4" 
                viewBox="0 0 16 4"
              >
                <path 
                  d="M0 2 L14 2 L12 0 M14 2 L12 4" 
                  stroke="rgb(60, 75, 97)" 
                  strokeWidth="1" 
                  fill="none"
                />
              </svg>

              {/* Event Card */}
              <div 
                className={`bg-fd-dark rounded-lg p-4 border-l-4 shadow-lg transition-all hover:shadow-xl hover:scale-[1.01]`}
                style={{ borderLeftColor: event.color }}
              >
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <h4 className="text-fd-text font-semibold text-lg">{event.title}</h4>
                    <p className="text-fd-text-muted text-sm mt-1">{event.description}</p>
                  </div>
                  {event.status && (
                    <span 
                      className={`inline-flex items-center px-2.5 py-0.5 rounded text-xs font-medium ${
                        event.status === 'completed' 
                          ? 'bg-green-500/20 text-green-400'
                          : event.status === 'pending'
                          ? 'bg-yellow-500/20 text-yellow-400'
                          : 'bg-blue-500/20 text-blue-400'
                      }`}
                    >
                      {event.status === 'completed' ? '✓ Completed' : 
                       event.status === 'pending' ? '◷ Pending' : '→ Upcoming'}
                    </span>
                  )}
                </div>
                <div className="flex items-center text-xs text-fd-text-muted mt-3">
                  <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                  {formatDateTime(event.timestamp)}
                </div>
              </div>

              {/* Connection line to next event */}
              {index < events.length - 1 && (
                <div className="absolute left-8 top-20 w-0.5 h-6 bg-fd-border"></div>
              )}
            </div>
          ))}
        </div>

        {/* End marker */}
        <div className="relative pl-20 mt-6">
          <div className="absolute left-0 w-16 h-16 rounded-full flex items-center justify-center text-2xl border-4 border-fd-darker bg-fd-border">
            {trade.tradeStatus === 'ACTIVE' ? '▶' : '■'}
          </div>
          
          {/* Arrow connector */}
          <svg 
            className="absolute left-16 top-6" 
            width="16" 
            height="4" 
            viewBox="0 0 16 4"
          >
            <path 
              d="M0 2 L14 2 L12 0 M14 2 L12 4" 
              stroke="rgb(60, 75, 97)" 
              strokeWidth="1" 
              fill="none"
            />
          </svg>

          {/* End marker card - styled consistently */}
          <div className="bg-fd-dark rounded-lg p-4 border-l-4 border-fd-border shadow-lg">
            <h4 className="text-fd-text font-semibold text-lg mb-2">
              {trade.tradeStatus === 'ACTIVE' ? 'Trade Active' : 'Trade Lifecycle Ended'}
            </h4>
            <p className="text-fd-text-muted text-sm">
              {trade.tradeStatus === 'ACTIVE' 
                ? 'Trade is currently active and ongoing...'
                : `Status: ${trade.tradeStatus.replace(/_/g, ' ')}`}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LifecycleTimeline;
