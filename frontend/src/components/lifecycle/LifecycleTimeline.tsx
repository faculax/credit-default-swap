import React, { useState, useEffect } from 'react';
import { CDSTradeResponse } from '../../services/cdsTradeService';

interface LifecycleTimelineProps {
  trade: CDSTradeResponse;
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

const LifecycleTimeline: React.FC<LifecycleTimelineProps> = ({ trade }) => {
  const [loading, setLoading] = useState(true);
  const [events, setEvents] = useState<TimelineEvent[]>([]);

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
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-fd-text mb-2">Trade Lifecycle</h3>
        <p className="text-fd-text-muted text-sm">
          Visual timeline of key events and milestones for CDS-{trade.id}
        </p>
      </div>

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
